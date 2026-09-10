package com.ruoyi.vlstream.test.vlstream.data;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetImportJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import static com.ruoyi.vlstream.test.vlstream.data.DataManagementService.map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatasetImportWorker {
    private final DatasetImportJobMapper jobs;
    private final DatasetUploadService uploads;
    private final DatasetSourceService sources;
    private final DatasetArchiveImporter importer;
    private final DataTransferService transfer;
    private final DataManagementService data;
    private final PublicDatasetDownloader downloader;
    @javax.annotation.Resource private VideoFrameService videoFrameService;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"dataset-import");t.setDaemon(true);return t;});
    private final ScheduledExecutorService leases=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"dataset-import-heartbeat");t.setDaemon(true);return t;});
    private final AtomicBoolean running=new AtomicBoolean();
    private final String workerId=UUID.randomUUID().toString();

    @Scheduled(fixedDelayString="${vlstream.data-management.scan-interval-ms:3000}")
    public void scan() {
        if(!running.compareAndSet(false,true))return;
        try {
            List<DatasetImportJob> pending=jobs.pending();
            if(pending.isEmpty()){running.set(false);return;}
            DatasetImportJob job=pending.get(0);
            if(jobs.claim(job.getId(),workerId)==0){running.set(false);return;}
            executor.submit(()->{try{process(job);}finally{running.set(false);}});
        }catch(RuntimeException e){running.set(false);log.warn("Dataset import scan failed: {}",e.getClass().getSimpleName());}
    }

    public void process(DatasetImportJob job) {
        String previous=TenantContextHolder.getTenantId();TenantContextHolder.setTenantId(job.getTenantId());
        ScheduledFuture<?> lease=leases.scheduleAtFixedRate(()->{
            String original=TenantContextHolder.getTenantId();TenantContextHolder.setTenantId(job.getTenantId());
            try{jobs.update(null,owned(job).set("heartbeat_at",new Date()));}catch(RuntimeException e){log.warn("Import heartbeat retry pending: jobId={}",job.getId());}
            finally{TenantContextHolder.setTenantId(original);}
        },30,30,TimeUnit.SECONDS);
        List<Map<String,Object>> results=new ArrayList<>();
        try {
            data.project(job.getDatasetId());
            if("video-frames".equals(job.getImportType())) {
                collect(results,videoFrameService.process(job,bytes->heartbeat(job,bytes)));
            } else if("local".equals(job.getImportType())) {
                Path file=transfer.tempFile("received-",".data");
                try {
                    requireSpace(file,job.getFileSize());
                    String hash;
                    try(InputStream input=uploads.storage(job).getObjectContent(job.getObjectKey())) {
                        hash=DatasetFileIO.copy(input,file,uploads.maxFileBytes(),bytes->heartbeat(job,bytes));
                    }
                    if(Files.size(file)!=job.getFileSize()||!hash.equals(job.getExpectedSha256()))throw new ServiceException("完整文件校验失败，请重新上传原文件");
                    collect(results,importer.importFile(job.getDatasetId(),file,job.getFilename(),job.getAnnotationFormat(),job.getSourceName(),ignored->heartbeat(job,job.getFileSize())));
                }finally{Files.deleteIfExists(file);}
            } else {
                DatasetSource source=sources.get(job.getSourceId());
                if("public".equals(job.getImportType())) {
                    Path file=transfer.tempFile("public-",".data");
                    try {
                        downloader.download(job.getRemotePath(),file,uploads.maxFileBytes(),bytes->heartbeat(job,bytes));
                        collect(results,importer.importFile(job.getDatasetId(),file,job.getFilename(),job.getAnnotationFormat(),job.getSourceName(),ignored->heartbeat(job,job.getTransferredBytes())));
                    }finally{Files.deleteIfExists(file);}
                } else importS3(job,source,results);
            }
            long failed=results.stream().filter(r->Boolean.FALSE.equals(r.get("success"))).count();
            long skipped=results.stream().filter(r->Boolean.TRUE.equals(r.get("duplicate"))).count();
            String state=failed==0?"COMPLETED":failed==results.size()?"FAILED":"PARTIAL";
            finish(job,state,results,failed,skipped,failed==0?"":"部分文件未导入，请查看结果并重试");
        }catch(Exception e) {
            String reason=e instanceof ServiceException?e.getMessage():"导入失败（"+e.getClass().getSimpleName()+"），请检查文件内容、来源权限或存储连接";
            finish(job,"FAILED",results,Math.max(1,results.stream().filter(r->Boolean.FALSE.equals(r.get("success"))).count()),
                results.stream().filter(r->Boolean.TRUE.equals(r.get("duplicate"))).count(),reason);
            log.warn("Dataset import failed: jobId={}, type={}",job.getId(),e.getClass().getSimpleName());
        }finally{lease.cancel(false);TenantContextHolder.setTenantId(previous);}
    }

    private void importS3(DatasetImportJob job,DatasetSource source,List<Map<String,Object>> results)throws IOException {
        AmazonS3 client=sources.client(source);
        try {
            List<S3ObjectSummary> files=new ArrayList<>();
            if("s3-directory".equals(job.getImportType())) {
                String token=null;
                do {
                    ListObjectsV2Result page=client.listObjectsV2(new ListObjectsV2Request().withBucketName(source.getBucketName())
                        .withPrefix(job.getRemotePath()).withContinuationToken(token).withMaxKeys(1000));
                    for(S3ObjectSummary file:page.getObjectSummaries()) if(DatasetArchiveImporter.isMedia(file.getKey())||file.getKey().toLowerCase(Locale.ROOT).endsWith(".zip"))files.add(file);
                    if(files.size()>10000)throw new ServiceException("目录超过 10000 个支持的文件，请缩小 Prefix");
                    token=page.getNextContinuationToken();heartbeat(job,job.getTransferredBytes());
                }while(token!=null);
            }else{
                ObjectMetadata metadata=client.getObjectMetadata(source.getBucketName(),job.getRemotePath());
                S3ObjectSummary file=new S3ObjectSummary();file.setKey(job.getRemotePath());file.setSize(metadata.getContentLength());file.setETag(metadata.getETag());files.add(file);
            }
            if(files.isEmpty())throw new ServiceException("此S3目录没有支持的图片、视频或ZIP");
            jobs.update(null,owned(job).set("total_files",files.size()));
            for(S3ObjectSummary sourceFile:files) {
                Path file=transfer.tempFile("s3-",".data");String name=DataTransferService.safeName(sourceFile.getKey());
                try {
                    if(sourceFile.getSize()>uploads.maxFileBytes())throw new ServiceException("来源文件超过当前大小上限");
                    requireSpace(file,sourceFile.getSize());
                    GetObjectRequest request=new GetObjectRequest(source.getBucketName(),sourceFile.getKey());
                    if(sourceFile.getETag()!=null)request.setMatchingETagConstraints(Collections.singletonList(sourceFile.getETag()));
                    try(S3Object object=client.getObject(request);InputStream input=object.getObjectContent()) {
                        DatasetFileIO.copy(input,file,uploads.maxFileBytes(),bytes->heartbeat(job,bytes));
                    }
                    if(Files.size(file)!=sourceFile.getSize())throw new ServiceException("来源文件大小发生变化，请重试");
                    collect(results,importer.importFile(job.getDatasetId(),file,name,job.getAnnotationFormat(),job.getSourceName(),ignored->heartbeat(job,job.getTransferredBytes())));
                }catch(Exception e){results.add(map("filename",name,"success",false,"message",e instanceof ServiceException?e.getMessage():"文件读取或解析失败"));}
                finally{Files.deleteIfExists(file);}
                heartbeat(job,job.getTransferredBytes());
            }
        }finally{client.shutdown();}
    }
    private void requireSpace(Path file,long bytes)throws IOException {
        if(Files.getFileStore(file).getUsableSpace()<bytes+512L*1024*1024)throw new ServiceException("当前暂存目录空间不足，请释放空间后重试");
    }
    private void collect(List<Map<String,Object>> results,Map<String,Object> response) {
        for(Object row:(List<?>)response.get("results"))results.add((Map<String,Object>)row);
    }
    private UpdateWrapper<DatasetImportJob> owned(DatasetImportJob job) {
        return new UpdateWrapper<DatasetImportJob>().eq("id",job.getId()).eq("tenant_id",job.getTenantId()).eq("job_state","PROCESSING").eq("worker_id",workerId);
    }
    private void heartbeat(DatasetImportJob job,Long bytes) {
        long value=bytes==null?0:bytes;job.setTransferredBytes(value);
        if(jobs.update(null,owned(job).set("heartbeat_at",new Date()).set("transferred_bytes",value))==0)throw new ServiceException("任务已由其他工作进程接管");
    }
    private void finish(DatasetImportJob job,String state,List<Map<String,Object>> results,long failed,long skipped,String error) {
        jobs.update(null,owned(job).set("job_state",state).set("total_files",Math.max(results.size(),failed))
            .set("imported_files",Math.max(0,results.size()-failed-skipped)).set("skipped_files",skipped).set("failed_files",failed)
            .set("result_json",data.writeJson(results.size()>500?results.subList(0,500):results)).set("error_message",error.length()>1000?error.substring(0,1000):error)
            .set("heartbeat_at",new Date()).set("update_time",new Date()));
    }

    @Scheduled(fixedDelay=3600000)
    public void cleanExpired() {
        String previous=TenantContextHolder.getTenantId();
        try {
            List<DatasetImportJob> expired=jobs.expired();
            for(DatasetImportJob job:expired) {
                TenantContextHolder.setTenantId(job.getTenantId());
                try {
                    uploads.expire(job.getId());
                }catch(RuntimeException e){log.warn("Import staging cleanup pending: jobId={}",job.getId());}
            }
        }finally{TenantContextHolder.setTenantId(previous);}
    }
    @PreDestroy public void stop(){executor.shutdownNow();leases.shutdownNow();}
}
