package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.properties.OssProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.service.LlmApiKeyCipher;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.*;

@org.springframework.boot.test.context.TestConfiguration
@Import(DataTestConfiguration.class)
public class DatasetImportTestConfiguration {
    static Path artifacts(){return DataTestConfiguration.root().resolve("codex/dataset-import-v2");}
    @Bean OssClient testOss() {
        String access=System.getenv("VLS_IMPORT_MINIO_ACCESS"), secret=System.getenv("VLS_IMPORT_MINIO_SECRET");
        if(access==null||secret==null)throw new IllegalStateException("Current local MinIO credentials must be supplied via environment");
        OssProperties properties=new OssProperties();properties.setEndpoint("127.0.0.1:9000");properties.setRegion("us-east-1");properties.setBucketName("vls-dataset-import-check");
        properties.setAccessKey(access);properties.setSecretKey(secret);properties.setIsHttps("N");properties.setAccessPolicy("0");
        return new OssClient("dataset-import-test",properties);
    }
    @Bean DatasetStorageProvider provider(OssClient storage){return new DatasetStorageProvider(){@Override public OssClient current(){return storage;}@Override public OssClient get(String key){return storage;}};}
    @Bean @Primary DataMediaStorage actualMedia(OssClient storage){return new DataMediaStorage(){
        @Override public void put(String key,byte[] bytes,String type){storage.upload(bytes,key,type);}
        @Override public void putFile(String key,Path file,String type){storage.uploadFile(file.toFile(),key,type);}
        @Override public java.io.InputStream read(String key){return storage.getObjectContent(key);}
        @Override public String preview(String key){return storage.getPrivateUrl(key,600);}
        @Override public void remove(String key){storage.delete(key);}
    };}
    @Bean DatasetImportJobMapper importJobs(SqlSessionTemplate sql){if(!sql.getConfiguration().hasMapper(DatasetImportJobMapper.class))sql.getConfiguration().addMapper(DatasetImportJobMapper.class);return sql.getMapper(DatasetImportJobMapper.class);}
    @Bean DatasetSourceMapper datasetSources(SqlSessionTemplate sql){if(!sql.getConfiguration().hasMapper(DatasetSourceMapper.class))sql.getConfiguration().addMapper(DatasetSourceMapper.class);return sql.getMapper(DatasetSourceMapper.class);}
    @Bean DatasetUploadService uploads(DatasetImportJobMapper jobs,DataManagementService data,DataTransferService transfer,DatasetStorageProvider provider){return new DatasetUploadService(jobs,data,transfer,provider);}
    @Bean PublicDatasetDownloader downloader(){return new PublicDatasetDownloader();}
    @Bean LlmApiKeyCipher testCipher(){VlsLlmReviewProperties p=new VlsLlmReviewProperties();p.setEncryptionKey("0123456789abcdef0123456789abcdef");return new LlmApiKeyCipher(p);}
    @Bean DatasetSourceService sourceService(DatasetSourceMapper sources,DatasetImportJobMapper jobs,DatasetUploadService uploads,DataManagementService data,ObjectMapper json,LlmApiKeyCipher cipher,PublicDatasetDownloader downloader){
        DatasetSourceService service=new DatasetSourceService(sources,jobs,uploads,data,json,cipher,downloader);ReflectionTestUtils.setField(service,"allowPrivateS3",true);return service;
    }
    @Bean DatasetArchiveImporter largeImporter(DataManagementService data,DataMediaStorage storage,SampleMediaInspector inspector,DataTransferService transfer){return new DatasetArchiveImporter(data,storage,inspector,transfer);}
    @Bean VideoFrameOriginMapper frameOrigins(SqlSessionTemplate sql){if(!sql.getConfiguration().hasMapper(VideoFrameOriginMapper.class))sql.getConfiguration().addMapper(VideoFrameOriginMapper.class);return sql.getMapper(VideoFrameOriginMapper.class);}
    @Bean VideoFrameRegistry frameRegistry(DataManagementService data,VideoFrameOriginMapper origins){return new VideoFrameRegistry(data,origins);}
    @Bean VideoFrameExtractor frameExtractor(){return new VideoFrameExtractor();}
    @Bean VideoFrameService frames(DataManagementService data,DatasetUploadService uploads,DatasetImportJobMapper jobs,DataTransferService transfer,DataMediaStorage storage,SampleMediaInspector inspector,VideoFrameExtractor extractor,VideoFrameRegistry registry,ObjectMapper json){return new VideoFrameService(data,uploads,jobs,transfer,storage,inspector,extractor,registry,json);}
    @Bean DatasetImportWorker worker(DatasetImportJobMapper jobs,DatasetUploadService uploads,DatasetSourceService sources,DatasetArchiveImporter importer,DataTransferService transfer,DataManagementService data,PublicDatasetDownloader downloader){return new DatasetImportWorker(jobs,uploads,sources,importer,transfer,data,downloader);}
    static void initialize(DataSource source)throws Exception{
        DataTestConfiguration.initialize(source);
        try(Connection connection=source.getConnection();Statement statement=connection.createStatement()){
            for(String table:new String[]{"vls_dataset_frame_origin","vls_dataset_upload_part","vls_dataset_import_job","vls_dataset_source"})statement.execute("DROP TABLE IF EXISTS "+table);
            Path migration=DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_013__dataset_sources_resumable_imports.sql");
            String text=new String(Files.readAllBytes(migration),java.nio.charset.StandardCharsets.UTF_8);
            for(String sql:text.split(";"))if(!sql.trim().isEmpty())statement.execute(sql);
            Path frames=DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_014__dataset_video_frame_origins.sql");
            for(String sql:new String(Files.readAllBytes(frames),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!sql.trim().isEmpty())statement.execute(sql);
        }
    }
}
