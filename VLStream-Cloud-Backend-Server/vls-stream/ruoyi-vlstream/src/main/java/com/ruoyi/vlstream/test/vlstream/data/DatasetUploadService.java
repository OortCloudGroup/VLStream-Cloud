package com.ruoyi.vlstream.test.vlstream.data;

import com.amazonaws.services.s3.model.PartETag;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetImportJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import static com.ruoyi.vlstream.test.vlstream.data.DataManagementService.map;

@Service
@RequiredArgsConstructor
public class DatasetUploadService {
    public static final int CHUNK_SIZE = 8 * 1024 * 1024;
    private final DatasetImportJobMapper jobs;
    private final DataManagementService data;
    private final DataTransferService transfer;
    private final DatasetStorageProvider storageProvider;
    @Value("${vlstream.data-management.max-file-bytes:17179869184}") private long maxFileBytes = 16L * 1024 * 1024 * 1024;

    public long maxFileBytes() { return Math.max(DatasetFileIO.FOUR_GIB, maxFileBytes); }
    public DatasetImportJob get(Long id) {
        DatasetImportJob job = jobs.selectOne(new QueryWrapper<DatasetImportJob>().eq("id", id).eq("tenant_id", data.tenant()));
        if (job == null) throw new ServiceException("导入任务不存在或无权访问");
        data.project(job.getDatasetId()); return job;
    }
    public List<DatasetImportJob> list(Long datasetId) {
        if (datasetId != null) data.project(datasetId);
        return jobs.selectList(new QueryWrapper<DatasetImportJob>().eq("tenant_id", data.tenant()).eq(datasetId != null, "dataset_id", datasetId)
            .orderByDesc("id").last("LIMIT 100"));
    }
    public Map<String,Object> status(Long id) {
        DatasetImportJob job = get(id);
        List<DatasetUploadPart> parts = jobs.parts(id);
        return map("job", job, "parts", parts, "uploadedBytes", parts.stream().mapToLong(DatasetUploadPart::getPartSize).sum());
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> initialize(DatasetImportRequests.Upload request) {
        data.beginAnnotationEdit(request.getDatasetId());
        if (request.getFileSize() <= 0 || request.getFileSize() > maxFileBytes()) throw new ServiceException("单文件超过当前容量上限；4 GiB 以上为尽力导入");
        String name = DataTransferService.safeName(request.getFilename());
        if (!name.matches("(?i).+\\.(jpg|jpeg|png|bmp|mp4|mov|zip)$")) throw new ServiceException("不支持此文件格式");
        DatasetImportJob old = jobs.selectOne(new QueryWrapper<DatasetImportJob>().eq("tenant_id", data.tenant()).eq("dataset_id", request.getDatasetId())
            .eq("expected_sha256", request.getSha256()).eq("file_size", request.getFileSize()).eq("annotation_format", request.getAnnotationFormat())
            .eq("source_name", request.getSourceName()).eq("import_type", "local").in("job_state", "UPLOADING", "COMPLETING", "QUEUED", "PROCESSING", "FAILED")
            .gt("expires_at", new Date()).orderByDesc("id").last("LIMIT 1"));
        if (old != null) return status(old.getId());
        OssClient storage = storageProvider.current();
        DatasetImportJob job = createJob(request.getDatasetId(), "local", name, request.getAnnotationFormat(), request.getSourceName());
        job.setFileSize(request.getFileSize()); job.setExpectedSha256(request.getSha256());
        job.setStorageConfig(storage.getConfigKey()); job.setStorageBucket(storage.getBucketName());
        job.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        job.setObjectKey("dataset-imports/" + data.tenant() + "/" + job.getId() + "/original");
        job.setJobState("UPLOADING");
        job.setMultipartId(storage.beginMultipart(job.getObjectKey(), "application/octet-stream"));
        try { jobs.insert(job); } catch (RuntimeException e) { storage.abortMultipart(job.getObjectKey(), job.getMultipartId()); throw e; }
        return map("job", job, "parts", Collections.emptyList(), "uploadedBytes", 0L);
    }

    public DatasetImportJob createJob(Long datasetId, String type, String name, String format, String source) {
        data.project(datasetId);
        DatasetImportJob job = new DatasetImportJob(); job.setDatasetId(datasetId); job.setTenantId(data.tenant()); job.setImportType(type);
        job.setFilename(name); job.setAnnotationFormat(format); job.setSourceName(source); job.setFileSize(0L); job.setExpectedSha256("");
        job.setChunkSize(CHUNK_SIZE); job.setTransferredBytes(0L); job.setTotalFiles(0); job.setImportedFiles(0); job.setSkippedFiles(0); job.setFailedFiles(0);
        job.setErrorMessage(""); job.setJobState("QUEUED"); job.setIsDeleted(0); job.setStatus(1); job.setExpiresAt(new Date(System.currentTimeMillis()+7L*24*3600*1000));
        return job;
    }

    public OssClient storage(DatasetImportJob job) {
        OssClient storage = storageProvider.get(job.getStorageConfig());
        if (!Objects.equals(storage.getBucketName(), job.getStorageBucket())) throw new ServiceException("上传使用的存储配置已更换 Bucket，请恢复原配置或重新上传");
        return storage;
    }

    @Transactional(rollbackFor=Exception.class)
    public void part(Long id, int number, String sha256, MultipartFile file) throws IOException {
        DatasetImportJob job = jobs.lock(id, data.tenant());
        if (job == null) throw new ServiceException("上传任务不存在");
        data.project(job.getDatasetId());
        if (!"UPLOADING".equals(job.getJobState())) throw new ServiceException("当前任务不能接收分片");
        long count = (job.getFileSize() + job.getChunkSize() - 1) / job.getChunkSize();
        if (number < 1 || number > count) throw new ServiceException("无效分片编号");
        long expected = Math.min(job.getChunkSize(), job.getFileSize() - (long)(number - 1)*job.getChunkSize());
        if (file.getSize() != expected || !sha256.matches("[a-f0-9]{64}")) throw new ServiceException("分片大小或校验和无效");
        DatasetUploadPart old = jobs.parts(id).stream().filter(p -> p.getPartNumber() == number).findFirst().orElse(null);
        if (old != null && sha256.equals(old.getSha256()) && old.getPartSize() == expected) return;
        Path temp = transfer.tempFile("chunk-", ".part");
        try {
            String actual;
            try (InputStream input = file.getInputStream()) { actual = DatasetFileIO.copy(input, temp, expected, bytes -> { }); }
            if (Files.size(temp) != expected || !sha256.equals(actual)) throw new ServiceException("分片校验失败，请重传");
            String etag = storage(job).uploadPart(job.getObjectKey(), job.getMultipartId(), number, temp.toFile());
            DatasetUploadPart part = new DatasetUploadPart(); part.setJobId(id); part.setPartNumber(number); part.setPartSize(expected); part.setSha256(actual); part.setEtag(etag);
            jobs.savePart(part);
            job.setExpiresAt(new Date(System.currentTimeMillis()+7L*24*3600*1000)); jobs.updateById(job);
        } finally { Files.deleteIfExists(temp); }
    }

    @Transactional(rollbackFor=Exception.class)
    public DatasetImportJob complete(Long id) {
        DatasetImportJob job = jobs.lock(id, data.tenant());
        if (job == null) throw new ServiceException("上传任务不存在"); data.project(job.getDatasetId());
        if (Arrays.asList("QUEUED","PROCESSING","COMPLETED","PARTIAL").contains(job.getJobState())) return job;
        if (!Arrays.asList("UPLOADING","COMPLETING").contains(job.getJobState())) throw new ServiceException("任务状态不允许完成上传");
        List<DatasetUploadPart> parts = jobs.parts(id);
        validateParts(job, parts);
        OssClient storage = storage(job);
        // A previous complete call may have succeeded at S3 while the database transaction rolled back.
        boolean exists;
        try { exists = storage.getObjectMetadata(job.getObjectKey()).getContentLength() == job.getFileSize(); }
        catch (com.amazonaws.services.s3.model.AmazonS3Exception e) { if (e.getStatusCode() != 404) throw e; exists = false; }
        if (!exists) storage.completeMultipart(job.getObjectKey(), job.getMultipartId(), parts.stream().map(p -> new PartETag(p.getPartNumber(),p.getEtag())).collect(Collectors.toList()));
        job.setJobState("QUEUED"); job.setTransferredBytes(job.getFileSize()); job.setErrorMessage(""); jobs.updateById(job); return job;
    }

    static void validateParts(DatasetImportJob job, List<DatasetUploadPart> parts) {
        long count = (job.getFileSize()+job.getChunkSize()-1)/job.getChunkSize();
        if (parts.size() != count) throw new ServiceException("分片尚未全部上传");
        for (int i=0;i<parts.size();i++) {
            long expected = Math.min(job.getChunkSize(),job.getFileSize()-(long)i*job.getChunkSize());
            if (parts.get(i).getPartNumber()!=i+1 || parts.get(i).getPartSize()!=expected) throw new ServiceException("分片不完整，请继续上传");
        }
    }

    @Transactional(rollbackFor=Exception.class)
    public void cancel(Long id) {
        DatasetImportJob job = jobs.lock(id,data.tenant()); if (job==null) throw new ServiceException("任务不存在");
        data.project(job.getDatasetId());
        if ("PROCESSING".equals(job.getJobState())) throw new ServiceException("正在导入的任务请等待结果，上传阶段可暂停或取消");
        if (Arrays.asList("COMPLETED","PARTIAL","CANCELLED").contains(job.getJobState())) return;
        if ("local".equals(job.getImportType())) {
            if ("UPLOADING".equals(job.getJobState())) storage(job).abortMultipart(job.getObjectKey(),job.getMultipartId());
            else storage(job).delete(job.getObjectKey());
        }
        job.setJobState("CANCELLED"); jobs.updateById(job); jobs.clearParts(id);
    }
    public void retry(Long id) {
        DatasetImportJob job=get(id);
        if (!Arrays.asList("FAILED","PARTIAL").contains(job.getJobState())) throw new ServiceException("仅失败或部分成功的任务可重试");
        if (job.getExpiresAt().before(new Date())) throw new ServiceException("任务已过期，请重新创建导入");
        jobs.update(null,new UpdateWrapper<DatasetImportJob>().eq("id",id).eq("tenant_id",data.tenant()).in("job_state","FAILED","PARTIAL")
            .set("job_state","QUEUED").set("error_message","").set("heartbeat_at",null));
    }

    @Transactional(rollbackFor=Exception.class)
    public void expire(Long id) {
        DatasetImportJob job=jobs.lock(id,data.tenant());
        if(job==null||job.getObjectKey()==null||job.getExpiresAt().after(new Date())||Arrays.asList("PROCESSING","QUEUED").contains(job.getJobState()))return;
        if("UPLOADING".equals(job.getJobState())) storage(job).abortMultipart(job.getObjectKey(),job.getMultipartId());
        else storage(job).delete(job.getObjectKey());
        String state=Arrays.asList("UPLOADING","FAILED","PARTIAL","COMPLETING").contains(job.getJobState())?"EXPIRED":job.getJobState();
        jobs.update(null,new UpdateWrapper<DatasetImportJob>().eq("id",id).eq("tenant_id",data.tenant()).set("job_state",state).set("object_key",null).set("multipart_id",null));
        jobs.clearParts(id);
    }
}
