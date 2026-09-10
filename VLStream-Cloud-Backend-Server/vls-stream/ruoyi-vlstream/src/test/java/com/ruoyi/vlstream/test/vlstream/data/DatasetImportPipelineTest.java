package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetImportJobMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetSourceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named="VLS_IMPORT_MINIO_ACCESS",matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatasetImportPipelineTest {
    AnnotationConfigApplicationContext context;DataManagementService data;DatasetUploadService uploads;DatasetSourceService sources;
    DatasetImportWorker worker;DatasetImportJobMapper jobs;OssClient storage;
    @BeforeAll void open()throws Exception{
        System.setProperty("vlstream.data-management.temp-directory",DatasetImportTestConfiguration.artifacts().resolve("temporary").toString());
        System.setProperty("vlstream.data-management.allow-private-s3","true");
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.WARN);
        context=new AnnotationConfigApplicationContext(DatasetImportTestConfiguration.class);DatasetImportTestConfiguration.initialize(context.getBean(DataSource.class));
        com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor isolation=new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        isolation.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler(){
            @Override public net.sf.jsqlparser.expression.Expression getTenantId(){return new net.sf.jsqlparser.expression.StringValue(Objects.toString(TenantContextHolder.getTenantId(),"__NO_TENANT__"));}
            @Override public boolean ignoreTable(String table){return "vls_dataset_upload_part".equals(table);}
        }));
        context.getBean(org.mybatis.spring.SqlSessionTemplate.class).getConfiguration().addInterceptor(isolation);
        data=context.getBean(DataManagementService.class);uploads=context.getBean(DatasetUploadService.class);sources=context.getBean(DatasetSourceService.class);
        worker=context.getBean(DatasetImportWorker.class);jobs=context.getBean(DatasetImportJobMapper.class);storage=context.getBean(OssClient.class);
        Files.createDirectories(DatasetImportTestConfiguration.artifacts());Files.write(DatasetImportTestConfiguration.artifacts().resolve("classpath.txt"),System.getProperty("java.class.path").getBytes(StandardCharsets.UTF_8));
    }
    @AfterAll void close(){if(context!=null)context.close();}
    @BeforeEach void tenant(){TenantContextHolder.setTenantId("tenant-import-test");}
    @AfterEach void clear(){TenantContextHolder.clear();}

    @Test void resumeAndDeduplicatePartsThenImportViaCurrentMinio()throws Exception{
        Path file=video(20L*1024*1024,"resume.mp4");Long dataset=dataset("resume");
        DatasetImportRequests.Upload request=request(dataset,file);Map<String,Object> initial=uploads.initialize(request);DatasetImportJob job=(DatasetImportJob)initial.get("job");
        send(job,file,1);Map<String,Object> resumed=uploads.initialize(request);
        assertEquals(job.getId(),((DatasetImportJob)resumed.get("job")).getId());assertEquals(1,((List<?>)resumed.get("parts")).size());
        send(job,file,1);assertEquals(1,jobs.parts(job.getId()).size());
        assertThrows(ServiceException.class,()->uploads.complete(job.getId()));
        send(job,file,2);send(job,file,3);uploads.complete(job.getId());uploads.complete(job.getId());
        DatasetImportJob finished=finish(job.getId());assertEquals("COMPLETED",finished.getJobState(),finished.getErrorMessage());
        AnnotationImage sample=data.snapshot(dataset).getSamples().get(0);assertEquals(Files.size(file),sample.getFileSize());
        assertEquals(sample.getFileSize().longValue(),storage.getObjectMetadata(sample.getLocalPath()).getContentLength());
        assertEquals(request.getSha256(),sample.getContentSha256());Files.delete(file);
    }

    @Test void rejectsCorruptPartAndCrossTenantAccess()throws Exception{
        Path file=video(1024,"bad-part.mp4");DatasetImportJob job=(DatasetImportJob)uploads.initialize(request(dataset("bad"),file)).get("job");
        assertThrows(ServiceException.class,()->uploads.part(job.getId(),1,String.join("",Collections.nCopies(64,"0")),new MockMultipartFile("file",Files.readAllBytes(file))));
        assertTrue(jobs.parts(job.getId()).isEmpty());TenantContextHolder.setTenantId("other");assertThrows(ServiceException.class,()->uploads.status(job.getId()));
        TenantContextHolder.setTenantId("tenant-import-test");uploads.cancel(job.getId());assertEquals("CANCELLED",uploads.get(job.getId()).getJobState());Files.delete(file);
    }

    @Test void videoFramesBecomeAnnotatableSamplesAndKeepOriginsAcrossRetry()throws Exception {
        Path video=VideoFrameExtractorTest.playable();Long dataset=dataset("video-frames");
        DatasetImportJob upload=(DatasetImportJob)uploads.initialize(request(dataset,video)).get("job");send(upload,video,1);uploads.complete(upload.getId());
        assertEquals("COMPLETED",finish(upload.getId()).getJobState());AnnotationImage source=data.snapshot(dataset).getSamples().get(0);
        VideoFrameRequest request=new VideoFrameRequest();request.setDatasetId(dataset);request.setVideoId(source.getId());request.setMaxFrames(3);
        VideoFrameService service=context.getBean(VideoFrameService.class);DatasetImportJob task=service.enqueue(request);DatasetImportJob done=finish(task.getId());
        assertEquals("COMPLETED",done.getJobState(),done.getErrorMessage());assertEquals(3,done.getImportedFiles());
        List<AnnotationImage> members=data.snapshot(dataset).getSamples();assertEquals(4,members.size());
        VideoFrameRegistry registry=context.getBean(VideoFrameRegistry.class);
        for(AnnotationImage sample:members)if("image".equals(sample.getMediaType())) {
            assertEquals("unassigned",sample.getDatasetSplit());assertTrue(registry.origins(dataset,sample.getId()).stream().allMatch(origin->source.getId().equals(origin.getVideoId())));
            assertFalse(registry.origins(dataset,sample.getId()).isEmpty());
            try(InputStream input=storage.getObjectContent(sample.getLocalPath())){assertNotNull(javax.imageio.ImageIO.read(input));}
        }
        DatasetImportJob again=finish(service.enqueue(request).getId());assertEquals("COMPLETED",again.getJobState());assertEquals(3,again.getSkippedFiles());assertEquals(4,data.snapshot(dataset).getSamples().size());
        org.mybatis.spring.SqlSessionTemplate sql=context.getBean(org.mybatis.spring.SqlSessionTemplate.class);
        com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel label=new com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel();label.setAnnotationId(dataset);label.setName("frame-object");label.setIsDeleted(0);label.setStatus(1);
        sql.getMapper(com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationLabelMapper.class).insert(label);
        for(AnnotationImage sample:members)if("image".equals(sample.getMediaType())) {
            com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance instance=new com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance();
            instance.setAnnotationId(dataset);instance.setImageId(sample.getId());instance.setLabelId(label.getId());instance.setAnnotationType(com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum.rect);
            instance.setAnnotationData("{\"x\":10,\"y\":10,\"width\":20,\"height\":20}");instance.setIsDeleted(0);instance.setStatus(1);
            sql.getMapper(com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationInstanceMapper.class).insert(instance);
        }
        DatasetVersion version=data.split(dataset,new DataRequests.Split());assertEquals(2,version.getTrainCount());assertEquals(1,version.getValidationCount());
        assertEquals("unassigned",data.sample(dataset,source.getId()).getDatasetSplit());
        Long other=dataset("foreign-video");request.setDatasetId(other);assertThrows(ServiceException.class,()->service.enqueue(request));request.setDatasetId(dataset);
        request.setVideoId(members.stream().filter(sample->"image".equals(sample.getMediaType())).findFirst().get().getId());assertThrows(ServiceException.class,()->service.enqueue(request));
    }

    @Test void s3DirectoryImportReadsExternalPrefixAndEncryptsCredentials()throws Exception{
        Path image=DatasetImportTestConfiguration.artifacts().resolve("source-image.png");Files.write(image,SampleMediaInspectorTest.image(true,91));
        storage.uploadFile(image.toFile(),"external-fixture/image.png","image/png");
        DatasetImportRequests.Source request=new DatasetImportRequests.Source();request.setName("External S3 fixture");request.setEndpoint("http://127.0.0.1:9000");request.setBucketName(storage.getBucketName());request.setKeyPrefix("external-fixture/");
        request.setAccessKey(System.getenv("VLS_IMPORT_MINIO_ACCESS"));request.setSecretKey(System.getenv("VLS_IMPORT_MINIO_SECRET"));
        DatasetSource source=sources.save(null,request);
        String serialized=context.getBean(com.fasterxml.jackson.databind.ObjectMapper.class).writeValueAsString(source);assertFalse(serialized.contains(request.getSecretKey()));assertFalse(serialized.contains("credentialsCipher"));
        assertFalse(context.getBean(DatasetSourceMapper.class).selectById(source.getId()).getCredentialsCipher().equals(request.getSecretKey()));
        assertEquals(1,((List<?>)sources.browse(source.getId(),"external-fixture/",null).get("files")).size());
        DatasetImportRequests.Remote remote=new DatasetImportRequests.Remote();remote.setDatasetId(dataset("external"));remote.setSourceId(source.getId());remote.setDirectory(true);
        DatasetImportJob job=sources.enqueue(remote);assertEquals("COMPLETED",finish(job.getId()).getJobState());assertEquals(1,data.snapshot(remote.getDatasetId()).getSamples().size());
        storage.delete("external-fixture/image.png");Files.delete(image);
    }

    @Test void yoloAndVlsArchivesPreserveAnnotations()throws Exception{
        Path zip=DatasetImportTestConfiguration.artifacts().resolve("yolo.zip");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip))){entry(out,"data.yaml","names: [car]\n".getBytes(StandardCharsets.UTF_8));entry(out,"images/train/car.png",SampleMediaInspectorTest.image(true,92));entry(out,"labels/train/car.txt","0 0.5 0.5 0.4 0.4\n".getBytes(StandardCharsets.UTF_8));}
        Long dataset=dataset("yolo");DatasetImportRequests.Upload request=request(dataset,zip);request.setAnnotationFormat("yolo");
        DatasetImportJob job=(DatasetImportJob)uploads.initialize(request).get("job");send(job,zip,1);uploads.complete(job.getId());DatasetImportJob result=finish(job.getId());assertEquals("COMPLETED",result.getJobState(),result.getErrorMessage());
        assertEquals(1,data.snapshot(dataset).getInstances().size());
        Path exported=context.getBean(DataTransferService.class).exportArchive(data.snapshot(dataset));
        Long target=dataset("vls");context.getBean(DatasetArchiveImporter.class).importFile(target,exported,"sample.zip","vls","test",ignored->{});assertEquals(1,data.snapshot(target).getInstances().size());
        Files.delete(zip);Files.delete(exported);
    }

    @Test void largeVideoUsesLongOffsetsAtFourGibBoundary()throws Exception{
        Path file=video(DatasetFileIO.FOUR_GIB,"boundary-4g.mp4");
        try { SampleMediaInspector.Inspection inspection=new SampleMediaInspector().inspectFile(file.getFileName().toString(),file);assertEquals("video",inspection.getMediaType());assertEquals(64,inspection.getSha256().length()); }
        finally{Files.deleteIfExists(file);}
    }

    @Test @EnabledIfEnvironmentVariable(named="VLS_IMPORT_TEST_4G",matches="true")
    void fourGibMultipartResumeCompletesWithExactObjectAndSampleHashes()throws Exception{
        Path file=video(DatasetFileIO.FOUR_GIB,"upload-4g.mp4");Long dataset=dataset("4g-upload");
        try {
            DatasetImportRequests.Upload request=request(dataset,file);DatasetImportJob job=(DatasetImportJob)uploads.initialize(request).get("job");
            for(int part=1;part<=512;part++){
                send(job,file,part);
                if(part==128){Map<String,Object> resume=uploads.initialize(request);assertEquals(128,((List<?>)resume.get("parts")).size());System.out.println("4 GiB resume verified after 1 GiB");}
                if(part%64==0)System.out.println("4 GiB transfer parts "+part+"/512");
            }
            uploads.complete(job.getId());DatasetImportJob completed=finish(job.getId());assertEquals("COMPLETED",completed.getJobState(),completed.getErrorMessage());
            AnnotationImage sample=data.snapshot(dataset).getSamples().get(0);assertEquals(DatasetFileIO.FOUR_GIB,sample.getFileSize().longValue());assertEquals(request.getSha256(),sample.getContentSha256());
            assertEquals(DatasetFileIO.FOUR_GIB,storage.getObjectMetadata(sample.getLocalPath()).getContentLength());
            Files.write(DatasetImportTestConfiguration.artifacts().resolve("4g-evidence.json"),data.writeJson(DataManagementService.map("bytes",DatasetFileIO.FOUR_GIB,"sha256",sample.getContentSha256(),"parts",512,"resumedAfterParts",128,"state",completed.getJobState(),"storage","current local MinIO; isolated test bucket")).getBytes(StandardCharsets.UTF_8));
            storage.delete(sample.getLocalPath());storage.delete(job.getObjectKey());
        }finally{Files.deleteIfExists(file);}
    }

    @Test @EnabledIfEnvironmentVariable(named="VLS_IMPORT_TEST_ZIP64",matches="true")
    void zip64CanImportVideoLargerThanFourGibWithoutWholeFileAllocation()throws Exception{
        Path file=video(DatasetFileIO.FOUR_GIB+1024,"zip64-video.mp4");Path archive=DatasetImportTestConfiguration.artifacts().resolve("zip64-large.zip");Long dataset=dataset("zip64");
        try {
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(archive));InputStream input=Files.newInputStream(file)){
                zip.putNextEntry(new ZipEntry("video.mp4"));byte[] buffer=new byte[128*1024];int count;while((count=input.read(buffer))!=-1)zip.write(buffer,0,count);zip.closeEntry();
            }
            System.out.println("ZIP64 fixture prepared: expanded bytes "+Files.size(file));
            context.getBean(DatasetArchiveImporter.class).importFile(dataset,archive,"zip64.zip","none","zip64-test",ignored->{});
            AnnotationImage sample=data.snapshot(dataset).getSamples().get(0);assertEquals(Files.size(file),sample.getFileSize());
            assertEquals(Files.size(file),storage.getObjectMetadata(sample.getLocalPath()).getContentLength());
            Files.write(DatasetImportTestConfiguration.artifacts().resolve("zip64-evidence.json"),data.writeJson(DataManagementService.map("archiveBytes",Files.size(archive),"mediaBytes",sample.getFileSize(),"sha256",sample.getContentSha256(),"state","COMPLETED")).getBytes(StandardCharsets.UTF_8));
            storage.delete(sample.getLocalPath());
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(archive);}
    }

    DatasetImportJob finish(Long id)throws Exception{long deadline=System.currentTimeMillis()+300000;while(System.currentTimeMillis()<deadline){worker.scan();DatasetImportJob job=uploads.get(id);if(Arrays.asList("COMPLETED","FAILED","PARTIAL").contains(job.getJobState()))return job;Thread.sleep(200);}throw new AssertionError("Import worker timeout");}
    Long dataset(String name){DataRequests.Project request=new DataRequests.Project();request.setProjectCode("DS-"+UUID.randomUUID());request.setAnnotationName(name);return data.saveProject(null,request).getId();}
    DatasetImportRequests.Upload request(Long dataset,Path file)throws Exception{DatasetImportRequests.Upload request=new DatasetImportRequests.Upload();request.setDatasetId(dataset);request.setFilename(file.getFileName().toString());request.setFileSize(Files.size(file));request.setSha256(DatasetFileIO.sha256(file));return request;}
    void send(DatasetImportJob job,Path file,int part)throws Exception{long offset=(long)(part-1)*job.getChunkSize();byte[] bytes=new byte[(int)Math.min(job.getChunkSize(),Files.size(file)-offset)];try(RandomAccessFile input=new RandomAccessFile(file.toFile(),"r")){input.seek(offset);input.readFully(bytes);}java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");StringBuilder hash=new StringBuilder();for(byte b:digest.digest(bytes))hash.append(String.format("%02x",b&255));uploads.part(job.getId(),part,hash.toString(),new MockMultipartFile("file",bytes));}
    static Path video(long size,String name)throws Exception{Path file=DatasetImportTestConfiguration.artifacts().resolve(name);Files.createDirectories(file.getParent());try(RandomAccessFile output=new RandomAccessFile(file.toFile(),"rw")){output.setLength(size);output.writeInt(16);output.writeBytes("ftyp");output.writeBytes("isom");output.writeInt(0);output.writeInt(44);output.writeBytes("moov");output.writeInt(36);output.writeBytes("trak");output.writeInt(28);output.writeBytes("mdia");output.writeInt(20);output.writeBytes("hdlr");output.writeLong(0);output.writeBytes("vide");output.writeInt(1);output.writeBytes("mdat");output.writeLong(size-60);}return file;}
    static void entry(ZipOutputStream out,String name,byte[] content)throws IOException{out.putNextEntry(new ZipEntry(name));out.write(content);out.closeEntry();}
}
