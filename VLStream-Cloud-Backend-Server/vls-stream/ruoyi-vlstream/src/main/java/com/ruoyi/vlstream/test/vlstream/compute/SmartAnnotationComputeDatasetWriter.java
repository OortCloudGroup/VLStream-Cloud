package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/** Transfers frozen seeds and pending images to the selected, verified node. */
@Service
@RequiredArgsConstructor
public class SmartAnnotationComputeDatasetWriter {
    private final DataMediaStorage storage;
    private final SampleMediaInspector inspector;
    private final ObjectMapper json;

    public List<Map<String,Object>> upload(CloudTrainingJob job, DatasetSnapshot snapshot, ComputeSsh.Connection connection) throws IOException {
        List<AnnotationImage> upload=new ArrayList<>(SmartAnnotationService.unannotated(snapshot));
        if(upload.isEmpty())throw new com.ruoyi.common.exception.ServiceException("没有可预标注的有效图片；系统辅助文件不作为图片处理");
        Map<Long,List<AnnotationInstance>> annotations=snapshot.getInstances().stream().collect(Collectors.groupingBy(AnnotationInstance::getImageId));
        List<AnnotationImage> seeds=snapshot.getSamples().stream().filter(DatasetPartitioner::usable).filter(s->annotations.containsKey(s.getId())).collect(Collectors.toList());
        boolean active="active".equals(json.readTree(job.getOptionsJson()).path("mode").asText());
        Map<Long,String> seedHashes=new HashMap<>();
        List<AnnotationImage> partitionSeeds=new ArrayList<>();
        if(active) for(AnnotationImage seed:seeds) {
            String hash=seed.getContentSha256();
            if(hash==null || hash.isEmpty()) hash=DigestUtils.sha256Hex(readImage(seed));
            seedHashes.put(seed.getId(),hash);
            AnnotationImage member=new AnnotationImage();
            member.setId(seed.getId()); member.setDatasetSplit(seed.getDatasetSplit()); member.setContentSha256(hash);
            partitionSeeds.add(member);
        }
        Map<Long,String> split=active?SmartAnnotationWorker.partition(partitionSeeds):Collections.emptyMap();
        if(active && "image_classification".equals(snapshot.getAnnotationType()) && seeds.stream().noneMatch(s->"val".equals(s.getDatasetSplit()))) {
            DataRequests.Split request=new DataRequests.Split(); request.setMode("stratified");
            Map<Long,String> labels=new HashMap<>(); annotations.forEach((id,values)->labels.put(id,String.valueOf(values.get(0).getLabelId())));
            split=DatasetPartitioner.partition(partitionSeeds,labels,request);
        }
        TrainingDatasetLayout layout=new TrainingDatasetLayout(snapshot.getAnnotationType(),snapshot.getLabels());
        String root=job.getRunDir()+"/dataset";
        for(String directory:layout.directories())connection.mkdir(root+"/"+directory);
        if(active)upload.addAll(seeds);
        List<Map<String,Object>> images=new ArrayList<>(), members=new ArrayList<>();
        Set<String> trainHashes=new HashSet<>(),valHashes=new HashSet<>();
        for(AnnotationImage image:upload) {
            byte[] bytes=readImage(image);
            SampleMediaInspector.Inspection info;
            try { info=inspector.inspect(image.getOriginalName(),bytes); }
            catch(com.ruoyi.common.exception.ServiceException error) {
                throw new com.ruoyi.common.exception.ServiceException("图片 "+image.getId()+"（"+image.getOriginalName()+"）校验失败："+error.getMessage());
            }
            String expectedHash=seedHashes.getOrDefault(image.getId(),image.getContentSha256());
            if(expectedHash!=null && !expectedHash.isEmpty() && !expectedHash.equals(info.getSha256()))throw new IOException("图片与冻结版本摘要不一致");
            if(image.getMediaWidth()!=null && image.getMediaWidth()>0 && !image.getMediaWidth().equals(info.getWidth()) || image.getMediaHeight()!=null && image.getMediaHeight()>0 && !image.getMediaHeight().equals(info.getHeight()))throw new IOException("图片与冻结版本尺寸不一致");
            String subset=split.getOrDefault(image.getId(),"predict");
            if("train".equals(subset))trainHashes.add(info.getSha256());
            if("val".equals(subset))valHashes.add(info.getSha256());
            String extension=image.getOriginalName().substring(image.getOriginalName().lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
            if(!Arrays.asList("jpg","jpeg","png","bmp").contains(extension))throw new IOException("不支持的图片格式");
            String path=layout.imagePath(image.getId(),extension,subset,annotations.get(image.getId()));
            connection.put(root+"/"+path,new ByteArrayInputStream(bytes));
            if("predict".equals(subset)) {
                Map<String,Object> item=new LinkedHashMap<>();item.put("id",image.getId().toString());item.put("path","dataset/"+path);images.add(item);
            } else for(Map.Entry<String,byte[]> file:layout.annotations(image.getId(),subset,annotations.get(image.getId()),info.getWidth(),info.getHeight()).entrySet())
                connection.put(root+"/"+file.getKey(),new ByteArrayInputStream(file.getValue()));
            members.add(layout.sample(image.getId(),path,subset,info.getWidth(),info.getHeight(),info.getSha256()));
        }
        if(!Collections.disjoint(trainHashes,valHashes))throw new IOException("训练和验证图片内容重复");
        connection.putText(root+"/dataset.yaml",layout.yaml(root));
        connection.putText(root+"/vls-dataset.json",layout.manifest(job.getDatasetId(),job.getTenantId(),members));
        TrainingDatasetRuntimeService.copyScripts(connection,job.getRunDir(),"four_task_runtime.py","cloud_training_runner.py","check_cloud_environment.py","gpu_shared_queue.py");
        try(InputStream runner=new org.springframework.core.io.ClassPathResource("smart-annotation/runner.py").getInputStream()) {
            connection.put(job.getRunDir()+"/smart_annotation_runner.py",runner);
        }
        return images;
    }

    private byte[] readImage(AnnotationImage image) throws IOException {
        try(InputStream input=storage.read(image.getLocalPath()); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[32768];int read;
            while((read=input.read(buffer))!=-1) {
                if(output.size()+read>25*1024*1024)throw new IOException("图片超过25MiB");
                output.write(buffer,0,read);
            }
            return output.toByteArray();
        }
    }
}
