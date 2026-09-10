package com.ruoyi.vlstream.test.vlstream.data;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetSourceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetImportJobMapper;
import com.ruoyi.vlstream.test.vlstream.service.LlmApiKeyCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.*;
import static com.ruoyi.vlstream.test.vlstream.data.DataManagementService.map;

@Service
@RequiredArgsConstructor
public class DatasetSourceService {
    private final DatasetSourceMapper sources;
    private final DatasetImportJobMapper jobs;
    private final DatasetUploadService uploads;
    private final DataManagementService data;
    private final ObjectMapper json;
    private final LlmApiKeyCipher cipher;
    private final PublicDatasetDownloader publicDownloader;
    @Value("${vlstream.data-management.allow-private-s3:false}") private boolean allowPrivateS3;

    public IPage<DatasetSource> list(String keyword,int page) {
        return sources.selectPage(new Page<>(Math.max(1,page),20),new QueryWrapper<DatasetSource>().eq("tenant_id",data.tenant())
            .like(DataManagementService.hasText(keyword),"name",keyword).orderByDesc("id"));
    }
    public DatasetSource get(Long id) {
        DatasetSource source=sources.selectOne(new QueryWrapper<DatasetSource>().eq("id",id).eq("tenant_id",data.tenant()));
        if(source==null) throw new ServiceException("数据来源不存在或无权访问"); return source;
    }
    private DatasetSource lock(Long id) {
        DatasetSource source=sources.selectOne(new QueryWrapper<DatasetSource>().eq("id",id).eq("tenant_id",data.tenant()).last("FOR UPDATE"));
        if(source==null)throw new ServiceException("数据来源不存在或无权访问");return source;
    }
    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public DatasetSource save(Long id,DatasetImportRequests.Source request) {
        DatasetSource source=id==null?new DatasetSource():lock(id);
        if(id!=null && !Objects.equals(source.getSourceType(),request.getSourceType()))throw new ServiceException("来源类型不能更改，请新建另一来源");
        if(id!=null && jobs.selectCount(new QueryWrapper<DatasetImportJob>().eq("tenant_id",data.tenant()).eq("source_id",id).in("job_state","QUEUED","PROCESSING"))>0)
            throw new ServiceException("此来源仍有导入任务，请完成后再修改连接配置");
        URI endpoint=publicDownloader.uri(request.getEndpoint().trim());
        if("s3".equals(request.getSourceType())) {
            if(!DataManagementService.hasText(request.getBucketName()) || !request.getBucketName().matches("[A-Za-z0-9._-]{1,255}")) throw new ServiceException("请填写有效的 S3 Bucket 名称");
            if(endpoint.getRawQuery()!=null || !Arrays.asList("","/").contains(endpoint.getPath())) throw new ServiceException("S3 Endpoint 只填写服务地址，目录请使用 Prefix");
            validateS3Host(endpoint.getHost());
        }
        source.setName(request.getName().trim()); source.setTenantId(data.tenant()); source.setSourceType(request.getSourceType());
        source.setEndpoint(request.getEndpoint().trim()); source.setRegion(request.getRegion()); source.setBucketName(request.getBucketName());
        source.setKeyPrefix(request.getKeyPrefix()==null?"":request.getKeyPrefix()); source.setDescription(request.getDescription());
        if("s3".equals(source.getSourceType())) {
            if(request.isAnonymous()) source.setCredentialsCipher("");
            else if(DataManagementService.hasText(request.getAccessKey()) && DataManagementService.hasText(request.getSecretKey())) {
                try { source.setCredentialsCipher(cipher.encrypt(json.writeValueAsString(map("accessKey",request.getAccessKey(),"secretKey",request.getSecretKey(),"sessionToken",request.getSessionToken())))); }
                catch(Exception e) { throw new ServiceException("来源凭据保存失败，请检查服务端加密配置"); }
            } else if(!source.isCredentialsConfigured()) throw new ServiceException("请填写 Access Key/Secret Key，或选择匿名公开 Bucket");
        }
        source.setIsDeleted(0);source.setStatus(1);
        if(id==null) sources.insert(source); else sources.updateById(source);
        return source;
    }
    private void validateS3Host(String host) {
        try {
            if(!allowPrivateS3) PublicDatasetDownloader.publicAddresses(host);
            else for(java.net.InetAddress address:java.net.InetAddress.getAllByName(host))
                if(address.isLinkLocalAddress()||address.isAnyLocalAddress()||address.isMulticastAddress()) throw new IllegalArgumentException();
        } catch(Exception e) { throw new ServiceException("S3地址不可解析或不允许访问；内网对象存储需由管理员配置 allow-private-s3"); }
    }
    public AmazonS3 client(DatasetSource source) {
        validateS3Host(publicDownloader.uri(source.getEndpoint()).getHost());
        AWSCredentials credentials=new AnonymousAWSCredentials();
        if(source.isCredentialsConfigured()) {
            try {
                Map<?,?> value=json.readValue(cipher.decrypt(source.getCredentialsCipher()),Map.class);
                String session=Objects.toString(value.get("sessionToken"),"");
                credentials=session.isEmpty()?new BasicAWSCredentials(value.get("accessKey").toString(),value.get("secretKey").toString())
                    :new BasicSessionCredentials(value.get("accessKey").toString(),value.get("secretKey").toString(),session);
            } catch(Exception e) { throw new ServiceException("S3凭据读取失败，请重新保存来源配置"); }
        }
        ClientConfiguration config=new ClientConfiguration().withConnectionTimeout(15000).withSocketTimeout(60000).withMaxErrorRetry(2);
        config.setDnsResolver(host->{
            if(!allowPrivateS3)return PublicDatasetDownloader.publicAddresses(host);
            java.net.InetAddress[] addresses=java.net.InetAddress.getAllByName(host);
            for(java.net.InetAddress address:addresses)if(address.isLinkLocalAddress()||address.isAnyLocalAddress()||address.isMulticastAddress())throw new java.net.UnknownHostException("S3 endpoint uses a reserved network");
            return addresses;
        });
        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withClientConfiguration(config)
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(source.getEndpoint(),DataManagementService.hasText(source.getRegion())?source.getRegion():"us-east-1"))
            .withPathStyleAccessEnabled(true).build();
    }
    public String prefix(DatasetSource source,String path) {
        String root=source.getKeyPrefix()==null?"":source.getKeyPrefix();
        String selected=path==null||path.isEmpty()?root:path;
        if(!selected.startsWith(root)) throw new ServiceException("选择路径不在此来源配置的 Prefix 下"); return selected;
    }
    public Map<String,Object> browse(Long id,String path,String token) {
        DatasetSource source=get(id);
        if(!"s3".equals(source.getSourceType())) return map("files",Collections.singletonList(map("key",source.getEndpoint(),"name",source.getName())),"nextToken",null);
        AmazonS3 client=client(source);
        try {
            ListObjectsV2Result result=client.listObjectsV2(new ListObjectsV2Request().withBucketName(source.getBucketName()).withPrefix(prefix(source,path))
                .withContinuationToken(token).withMaxKeys(100));
            List<Map<String,Object>> files=new ArrayList<>();
            result.getObjectSummaries().forEach(object -> files.add(map("key",object.getKey(),"size",object.getSize(),"modified",object.getLastModified(),"etag",object.getETag())));
            return map("files",files,"nextToken",result.getNextContinuationToken());
        } catch(RuntimeException e) { throw new ServiceException("读取S3目录失败，请检查地址、Bucket和读取权限"); }
        finally { client.shutdown(); }
    }
    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public DatasetImportJob enqueue(DatasetImportRequests.Remote request) {
        DatasetSource source=lock(request.getSourceId()); data.project(request.getDatasetId());
        if(request.isDirectory()&&(!"s3".equals(source.getSourceType())||!"none".equals(request.getAnnotationFormat())))
            throw new ServiceException("目录导入暂支持无标注 S3 媒体；带标注请选择包含标注的 ZIP 对象");
        String path="s3".equals(source.getSourceType())?prefix(source,request.getPath()):publicDownloader.normalize(source.getEndpoint());
        String filename=request.isDirectory()?source.getName():DataTransferService.safeName("s3".equals(source.getSourceType())?path:publicDownloader.uri(path).getPath());
        if(!request.isDirectory() && !filename.matches("(?i).+\\.(jpg|jpeg|png|bmp|mp4|mov|zip)$")) throw new ServiceException("请填写图片、视频或 ZIP 的文件下载链接，暂不解析仓库首页/Parquet/TAR");
        DatasetImportJob job=uploads.createJob(request.getDatasetId(),"s3".equals(source.getSourceType())?(request.isDirectory()?"s3-directory":"s3-file"):"public",filename,request.getAnnotationFormat(),source.getName());
        job.setSourceId(source.getId()); job.setRemotePath(path); jobs.insert(job); return job;
    }
    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public void remove(Long id) {
        lock(id);
        if(jobs.selectCount(new QueryWrapper<DatasetImportJob>().eq("tenant_id",data.tenant()).eq("source_id",id).in("job_state","QUEUED","PROCESSING"))>0)
            throw new ServiceException("此来源仍有导入任务，请等待完成后删除");
        sources.deleteById(id);
    }
}
