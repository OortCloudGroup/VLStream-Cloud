package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetImportJobMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongConsumer;

@Service @RequiredArgsConstructor
public class VideoFrameService {
    private final DataManagementService data;
    private final DatasetUploadService uploads;
    private final DatasetImportJobMapper jobs;
    private final DataTransferService transfer;
    private final DataMediaStorage storage;
    private final SampleMediaInspector inspector;
    private final VideoFrameExtractor extractor;
    private final VideoFrameRegistry registry;
    private final ObjectMapper json;

    @Transactional(rollbackFor=Exception.class)
    public DatasetImportJob enqueue(VideoFrameRequest request) {
        VideoFrameExtractor.validate(request);
        data.lockDatasetForTask(request.getDatasetId());
        AnnotationImage video = data.sample(request.getDatasetId(),request.getVideoId());
        if (!"video".equals(video.getMediaType())) throw new ServiceException("请选择视频文件进行切图");
        String parameters = data.writeJson(request);
        DatasetImportJob pending = jobs.selectOne(new QueryWrapper<DatasetImportJob>().eq("tenant_id",data.tenant()).eq("dataset_id",request.getDatasetId())
            .eq("import_type","video-frames").eq("remote_path",parameters).in("job_state","QUEUED","PROCESSING").last("LIMIT 1"));
        if (pending != null) return pending;
        DatasetImportJob job = uploads.createJob(request.getDatasetId(),"video-frames",video.getImageName(),"none","video:"+video.getId());
        job.setRemotePath(parameters); job.setObjectKey(video.getLocalPath()); job.setFileSize(video.getFileSize());
        job.setExpectedSha256(video.getContentSha256()); job.setProgressMessage("等待视频切图"); jobs.insert(job); return job;
    }

    public Map<String,Object> process(DatasetImportJob job, LongConsumer heartbeat) throws Exception {
        VideoFrameRequest request = json.readValue(job.getRemotePath(),VideoFrameRequest.class);
        VideoFrameExtractor.validate(request);
        if(!Objects.equals(request.getDatasetId(),job.getDatasetId()))throw new ServiceException("切图任务归属不一致");
        AnnotationImage video=data.sample(job.getDatasetId(),request.getVideoId());
        if(!"video".equals(video.getMediaType())||!Objects.equals(job.getObjectKey(),video.getLocalPath()))throw new ServiceException("原视频已被修改，请重新创建切图任务");
        Path marker=transfer.tempFile("video-frames-",".work");Files.delete(marker);Path directory=Files.createDirectory(marker);
        List<PendingSampleImport> staged=new ArrayList<>();boolean registered=false;
        try {
            Path local=directory.resolve("input.video");
            long expected=job.getFileSize()==null?0:job.getFileSize();
            if(expected<=0||Files.getFileStore(directory).getUsableSpace()<expected+512L*1024*1024)throw new ServiceException("视频为空或暂存磁盘空间不足");
            progress(job,"读取原视频");
            String hash;
            try(InputStream input=storage.read(job.getObjectKey())) { hash=DatasetFileIO.copy(input,local,expected,heartbeat); }
            if(Files.size(local)!=expected||(DataManagementService.hasText(job.getExpectedSha256())&&!hash.equals(job.getExpectedSha256())))throw new ServiceException("原视频大小或校验和不一致");
            List<VideoFrameExtractor.Extracted> frames=extractor.extract(local,directory.resolve("frames"),request,count->{
                heartbeat.accept(expected);progress(job,"已生成 "+count+" 张图片，尚未入库");
            });
            for(VideoFrameExtractor.Extracted frame:frames) {
                String name="video_"+video.getId()+"_"+frame.getTimestampMs()+"ms.jpg";
                String key="samples/"+data.tenant()+"/"+job.getDatasetId()+"/"+UUID.randomUUID()+".jpg";
                SampleMediaInspector.Inspection inspection=inspector.inspectFile(name,frame.getPath());
                PendingSampleImport item=new PendingSampleImport(name,name,key,Files.size(frame.getPath()),inspection);staged.add(item);
                storage.putFile(key,frame.getPath(),"image/jpeg");heartbeat.accept(expected);
                progress(job,"保存图片 "+staged.size()+" / "+frames.size());
            }
            // Check worker ownership immediately before the atomic database registration.
            heartbeat.accept(expected);
            Map<String,Object> report=registry.register(job,video,staged,frames);registered=true;
            List<?> rows=(List<?>)report.get("results");
            for(int i=0;i<rows.size();i++)if(Boolean.TRUE.equals(((Map<?,?>)rows.get(i)).get("duplicate")))cleanup(staged.get(i).getObjectKey());
            progress(job,"切图完成：生成 "+frames.size()+" 张，结果已加入数据集");
            return report;
        } finally {
            if(!registered)staged.forEach(item->cleanup(item.getObjectKey()));
            try(java.util.stream.Stream<Path> paths=Files.walk(directory)) { for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(path); }
        }
    }
    private void progress(DatasetImportJob job,String message) {
        jobs.update(null,new UpdateWrapper<DatasetImportJob>().eq("id",job.getId()).eq("tenant_id",data.tenant()).eq("job_state","PROCESSING").set("progress_message",message));
    }
    private void cleanup(String key){try{storage.remove(key);}catch(RuntimeException ignored){}}
}
