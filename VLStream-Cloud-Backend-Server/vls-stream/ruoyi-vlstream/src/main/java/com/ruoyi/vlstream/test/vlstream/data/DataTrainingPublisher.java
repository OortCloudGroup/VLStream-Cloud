package com.ruoyi.vlstream.test.vlstream.data;

import com.jcraft.jsch.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.common.constant.CommonConstant;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Publishes one frozen snapshot to a new directory; never overwrites directories used by training. */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataTrainingPublisher {
    private final DataManagementService data;
    private final DataTransferService transfer;
    private final DataMediaStorage storage;
    private final YoloDatasetWriter writer;
    private final SampleMediaInspector inspector;
    private final VlsSshProperties ssh;

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean publish(Long projectId) {
        data.lockDatasetForTask(projectId);
        DatasetSnapshot current = data.snapshot(projectId);
        AnnotationTaskType taskType = AnnotationTaskType.of(current.getAnnotationType());
        if (current.getSamples().stream().noneMatch(s -> Arrays.asList("train", "val").contains(s.getDatasetSplit()))) {
            DataRequests.Split split = new DataRequests.Split();
            if (taskType == AnnotationTaskType.CLASSIFICATION) split.setMode("stratified");
            data.split(projectId, split);
        }
        DataRequests.Version request = new DataRequests.Version(); request.setName("训练生成快照"); request.setDescription("用于生成独立训练目录");
        DatasetVersion version = data.saveVersion(projectId, request);
        DatasetSnapshot snapshot = data.readSnapshot(version.getSnapshotJson());
        Map<Long, List<AnnotationInstance>> byImage = snapshot.getInstances().stream().collect(Collectors.groupingBy(AnnotationInstance::getImageId));
        List<AnnotationImage> members = snapshot.getSamples().stream().filter(DatasetPartitioner::usable)
            .filter(s -> byImage.containsKey(s.getId())).collect(Collectors.toList());
        if (members.stream().anyMatch(s -> !Arrays.asList("train", "val").contains(s.getDatasetSplit())))
            throw new ServiceException("存在尚未划分的合格已标注图片，请重新划分数据集");
        if (members.stream().noneMatch(s -> "train".equals(s.getDatasetSplit())) || members.stream().noneMatch(s -> "val".equals(s.getDatasetSplit())))
            throw new ServiceException("训练集和验证集必须均有有效样本，请重新划分");
        Map<String, String> hashes = new HashMap<>();
        for (AnnotationImage member : members) {
            if (DataManagementService.hasText(member.getContentSha256())) {
                String previous = hashes.putIfAbsent(member.getContentSha256(), member.getDatasetSplit());
                if (previous != null && !previous.equals(member.getDatasetSplit())) throw new ServiceException("相同文件内容出现在两个集合中，请重新划分");
            }
        }
        TrainingDatasetLayout layout = new TrainingDatasetLayout(snapshot.getAnnotationType(), snapshot.getLabels());
        List<Map<String, Object>> manifestSamples = new ArrayList<>();
        String directory = CommonConstant.BASE_DATASETS_PATH + "vls/annotation_" + projectId + "/version_" + version.getVersionNumber() + "_" + UUID.randomUUID().toString().substring(0, 8);
        Session session = null; ChannelSftp sftp = null;
        try {
            session = new JSch().getSession(ssh.getUsername(), ssh.getHost(), ssh.getPort());
            session.setPassword(ssh.getPassword()); session.setConfig("StrictHostKeyChecking", "no"); session.connect(30000);
            sftp = (ChannelSftp) session.openChannel("sftp"); sftp.connect(30000);
            for (String folder : layout.directories()) mkdir(sftp, directory + "/" + folder);
            List<String> calibration = new ArrayList<>();
            for (AnnotationImage sample : members) {
                Path temp = transfer.tempFile("training-", ".image");
                try {
                    try (InputStream input = storage.read(sample.getLocalPath()); OutputStream output = Files.newOutputStream(temp)) {
                        byte[] buffer = new byte[32768]; int count; long size = 0;
                        while ((count = input.read(buffer)) != -1) { size += count; if (size > 25 * 1024 * 1024) throw new ServiceException("训练图片超过 25 MB"); output.write(buffer, 0, count); }
                        if (sample.getFileSize() != null && size != sample.getFileSize()) throw new ServiceException("训练图片文件不完整：" + sample.getImageName());
                    }
                    SampleMediaInspector.Inspection inspection = inspector.inspect(sample.getOriginalName(), Files.readAllBytes(temp));
                    if (DataManagementService.hasText(sample.getContentSha256()) && !sample.getContentSha256().equals(inspection.getSha256()))
                        throw new ServiceException("训练图片内容与版本校验和不一致：" + sample.getImageName());
                    String previousSplit = hashes.putIfAbsent(inspection.getSha256(), sample.getDatasetSplit());
                    if (previousSplit != null && !previousSplit.equals(sample.getDatasetSplit())) throw new ServiceException("图片内容在训练集和验证集中重复，请先检查质量并重新划分");
                    String extension = sample.getOriginalName().substring(sample.getOriginalName().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                    String split = sample.getDatasetSplit();
                    String relativeImage = layout.imagePath(sample.getId(), extension, split, byImage.get(sample.getId()));
                    Map<String, byte[]> annotationFiles = layout.annotations(sample.getId(), split, byImage.get(sample.getId()), inspection.getWidth(), inspection.getHeight());
                    sftp.put(temp.toString(), directory + "/" + relativeImage);
                    for (Map.Entry<String, byte[]> file : annotationFiles.entrySet()) sftp.put(new ByteArrayInputStream(file.getValue()), directory + "/" + file.getKey());
                    manifestSamples.add(layout.sample(sample.getId(), relativeImage, split, inspection.getWidth(), inspection.getHeight(), inspection.getSha256()));
                    if ("train".equals(split) && calibration.size() < 20) calibration.add(directory + "/" + relativeImage);
                } finally { Files.deleteIfExists(temp); }
            }
            putText(sftp, directory + "/dataset.yaml", layout.yaml(directory));
            putText(sftp, directory + "/vls-dataset.json", layout.manifest(projectId, data.tenant(), manifestSamples));
            for (String resource : Arrays.asList("four_task_runtime.py", "run_training.py")) {
                try (InputStream input = new org.springframework.core.io.ClassPathResource("training/" + resource).getInputStream()) { sftp.put(input, directory + "/" + resource); }
            }
            putText(sftp, directory + "/coco_subset_20.txt", String.join("\n", calibration));
            putText(sftp, directory + "/version.json", data.writeJson(DataManagementService.map("versionId", String.valueOf(version.getId()), "versionNumber", version.getVersionNumber(), "train", version.getTrainCount(), "val", version.getValidationCount())));
            data.recordPublishedDataset(projectId, version.getId(), directory + "/dataset.yaml");
            return true;
        } catch (ServiceException e) { throw e; }
        catch (Exception e) {
            log.warn("Dataset publication failed: projectId={}, version={}", projectId, version.getVersionNumber(), e);
            throw new ServiceException("训练目录生成失败，请检查对象存储和 GPU 服务器连接；原有训练目录未覆盖");
        } finally {
            if (sftp != null) sftp.disconnect(); if (session != null) session.disconnect();
        }
    }

    private void mkdir(ChannelSftp sftp, String directory) throws SftpException {
        String path = "";
        for (String part : directory.split("/")) {
            if (part.isEmpty()) continue; path += "/" + part;
            try { sftp.stat(path); }
            catch (SftpException e) { if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) sftp.mkdir(path); else throw e; }
        }
    }

    private void putText(ChannelSftp sftp, String path, String content) throws SftpException {
        sftp.put(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), path);
    }
}
