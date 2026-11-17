package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Authenticated immutable Hub artifacts are not represented as invented training tasks. */
@Service
@RequiredArgsConstructor
public class ModelHubDeploymentService {
    private final ModelHubDeploymentClient client;
    private final ModelArtifactObjectStore storage;
    private final ModelDispatchAccess access;
    private final VlsAlgorithmMapper algorithms;
    private final ModelDispatchService dispatch;
    private final ModelDispatchTaskService tasks;

    public List<Map<String,Object>> deploy(String uid,String commit,Long algorithmId,List<String> devices,String operation,String token,String tenant) throws IOException {
        if(uid==null || !uid.matches("[A-Za-z0-9_-]{1,128}") || commit==null || !commit.matches("[0-9a-f]{40}"))
            throw new ServiceException("模型标识或固定提交版本无效");
        try { if(!UUID.fromString(operation).toString().equalsIgnoreCase(operation))throw new IllegalArgumentException(); }
        catch(Exception ex){throw new ServiceException("下发操作标识无效");}
        if(devices==null||devices.isEmpty()||devices.size()>20||new HashSet<>(devices).size()!=devices.size())throw new ServiceException("请选择 1 至 20 台不同设备");
        String localTenant=access.tenant(),owner=access.owner();
        Algorithm algorithm=algorithms.selectOne(Wrappers.<Algorithm>lambdaQuery().eq(Algorithm::getId,algorithmId)
            .eq(Algorithm::getTenantId,localTenant).eq(Algorithm::getIsDeleted,0));
        if(algorithm==null||(algorithm.getCategory()!=AlgorithmCategoryEnum.detect&&algorithm.getCategory()!=AlgorithmCategoryEnum.personDetect))
            throw new ServiceException("请选择当前租户的目标检测算法");
        for(String device:devices) {
            if(device==null||!device.matches("[A-Za-z0-9_.:-]{1,128}"))throw new ServiceException("设备标识无效");
            access.requireDevice(device,"om",true);
        }
        JSONObject detail=client.detail(uid,token,tenant);
        // Recovery does not redownload mutable upstream state and never republishes MQTT.
        // The current real Hub account and WVP device scope have still been revalidated above.
        List<Map<String,Object>> recovered=new ArrayList<>();
        for(String device:devices) {
            String request=requestId(localTenant,owner,operation,uid,commit,algorithmId,device);
            ModelDispatchTask task=tasks.getByRequestId(request);
            if(task==null) { recovered.clear(); break; }
            if(!localTenant.equals(task.getTenantId())||!device.equals(task.getDeviceId())||!algorithmId.equals(task.getAlgorithmId())
                ||task.getRemotePath()==null||!task.getRemotePath().startsWith("hub/"+uid+"/"+commit+"/"))
                throw new ServiceException("原下发任务与当前模型来源不一致");
            Map<String,Object> row=new LinkedHashMap<>(); row.put("deviceId",device); row.put("requestId",request);
            row.put("status",task.getDispatchStatus()); row.put("error",task.getFailureReason()); recovered.add(row);
        }
        if(recovered.size()==devices.size())return recovered;
        if(!commit.equals(detail.getStr("published_commit"))||!"vls-device-manifest.json".equals(detail.getStr("device_manifest_path")))
            throw new ServiceException("模型已更新或缺少设备产物契约，请刷新 Model Hub");
        Path manifest=Files.createTempFile("hub-manifest-",".json"), model=null, classes=null;
        try {
            client.download(uid,commit,"vls-device-manifest.json",token,tenant,manifest,65536);
            JSONObject spec=JSONUtil.parseObj(new String(Files.readAllBytes(manifest),StandardCharsets.UTF_8));
            validateManifest(spec);
            JSONObject m=spec.getJSONObject("model"),c=spec.getJSONObject("classes");
            model=Files.createTempFile("hub-model-",".om"); classes=Files.createTempFile("hub-classes-",".yaml");
            client.download(uid,commit,"model.om",token,tenant,model,m.getLong("size"));
            client.download(uid,commit,"classes.yaml",token,tenant,classes,c.getLong("size"));
            verify(model,m); verify(classes,c);
            String yaml;
            try { yaml=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(classes))).toString(); }
            catch(CharacterCodingException ex){throw new ServiceException("类别文件不是有效 UTF-8");}
            ModelClassFileService.validate(yaml); ModelClassFileService.validateAnnotationType(yaml,"object_detection");
            String archive="hub/"+uid+"/"+commit+"/"+m.getStr("sha256")+"/model.om";
            final Path fixedModel=model;
            if(!storage.archive(archive,"model.om",target->Files.copy(fixedModel,target,StandardCopyOption.REPLACE_EXISTING)))
                throw new ServiceException("模型归档进行中，请稍后核实原操作");
            ModelClassFileService.ClassFile classFile=new ModelClassFileService.ClassFile("classes.yaml",yaml,c.getLong("size"),c.getStr("sha256"));
            List<Map<String,Object>> results=new ArrayList<>();
            for(String device:devices) {
                String id=requestId(localTenant,owner,operation,uid,commit,algorithmId,device);
                Map<String,Object> row=new LinkedHashMap<>(); row.put("deviceId",device); row.put("requestId",id);
                try {
                    dispatch.dispatchHubArtifact(algorithmId,archive,m.getLong("size"),m.getStr("sha256"),classFile,device,id);
                    ModelDispatchTask task=tasks.getByRequestId(id);
                    row.put("status",task==null?"UNKNOWN":task.getDispatchStatus());
                    row.put("error",task==null?null:task.getFailureReason());
                } catch(Exception ex) {
                    ModelDispatchTask task=tasks.getByRequestId(id);
                    row.put("status",task==null?"FAILED":task.getDispatchStatus());
                    row.put("error",task==null?"设备下发未受理，请核实设备权限、在线状态和进行中的任务":task.getFailureReason());
                }
                results.add(row);
            }
            return results;
        } finally { Files.deleteIfExists(manifest); if(model!=null)Files.deleteIfExists(model); if(classes!=null)Files.deleteIfExists(classes); }
    }
    static String requestId(String tenant,String owner,String operation,String uid,String commit,Long algorithm,String device) {
        return UUID.nameUUIDFromBytes(JSONUtil.toJsonStr(Arrays.asList("hub",tenant,owner,operation,uid,commit,algorithm,device)).getBytes(StandardCharsets.UTF_8)).toString();
    }
    static void validateManifest(JSONObject spec) {
        if(!Integer.valueOf(1).equals(spec.getInt("schema_version"))||!"om".equals(spec.getStr("model_format"))
            ||!"Hi3519DV500".equals(spec.getStr("target_chip"))||!"svp-acl".equals(spec.getStr("runtime"))
            ||!"detection".equals(spec.getStr("algorithm_type")))throw new ServiceException("该模型未声明兼容的 Hi3519DV500 OM 检测产物");
        validateFile(spec.getJSONObject("model"),"model.om",100L*1024*1024);
        validateFile(spec.getJSONObject("classes"),"classes.yaml",1024*1024);
    }
    private static void validateFile(JSONObject file,String path,long max) {
        if(file==null||!path.equals(file.getStr("path"))||file.getLong("size")==null||file.getLong("size")<1||file.getLong("size")>max
            ||file.getStr("sha256")==null||!file.getStr("sha256").matches("[0-9a-f]{64}"))throw new ServiceException("模型产物路径、大小或 SHA-256 无效");
    }
    static void verify(Path file,JSONObject expected) throws IOException {
        if(Files.size(file)!=expected.getLong("size"))throw new ServiceException("Model Hub 产物大小不一致");
        try(InputStream in=Files.newInputStream(file)) {
            MessageDigest sha=MessageDigest.getInstance("SHA-256"); byte[] buffer=new byte[65536]; int n;
            while((n=in.read(buffer))!=-1)sha.update(buffer,0,n);
            StringBuilder hex=new StringBuilder(); for(byte v:sha.digest())hex.append(String.format("%02x",v&255));
            if(!hex.toString().equals(expected.getStr("sha256")))throw new ServiceException("Model Hub 产物 SHA-256 校验失败");
        } catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
