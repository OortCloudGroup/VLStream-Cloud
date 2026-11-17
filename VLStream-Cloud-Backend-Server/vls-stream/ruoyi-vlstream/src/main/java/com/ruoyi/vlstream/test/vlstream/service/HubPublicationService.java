package com.ruoyi.vlstream.test.vlstream.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Local recommendation and durable publication receipt, scoped to trusted source ownership. */
@Service
@RequiredArgsConstructor
public class HubPublicationService {
    private final JdbcTemplate jdbc;
    private final DataManagementService data;
    private final HubPublicationArtifacts artifacts;
    private final HubPublicationClient client;

    public Map<String,Object> status(String kind,Long id) {
        HubPublicationArtifacts.Source source=authorized(kind,id);
        Map<String,Object> row=record(kind,id);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("featured",row!=null && Boolean.TRUE.equals(asBoolean(row.get("featured"))));
        result.put("remoteUid",row==null?null:row.get("remote_uid"));
        result.put("publishedCommit",row==null?null:row.get("published_commit"));
        result.put("publishState",row==null?"DRAFT":row.get("publish_state"));
        result.put("publishedAt",row==null?null:row.get("published_at"));
        result.put("error",row==null?null:row.get("error_message"));
        result.put("version",source.getVersion()); result.put("name",source.getName());
        result.put("publishable",source.isPublishable()); result.put("reason",source.getReason());
        if(row!=null && "PUBLISHED".equals(row.get("publish_state")) && !Objects.equals(source.getVersion(),row.get("version_hash"))) result.put("publishState","CHANGED");
        return result;
    }
    public Map<String,Object> featured(String kind,Long id,boolean featured) {
        authorized(kind,id); ensure(kind,id);
        if(jdbc.update("UPDATE vls_hub_publication SET featured=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_state<>'PUBLISHING'",featured,viewer(),data.tenant(),kind,id)!=1)
            throw new ServiceException("资源正在发布，请完成后再修改精选状态");
        return status(kind,id);
    }
    public Map<String,Object> publish(String kind,Long id,JSONObject metadata,String token,String remoteTenant) {
        HubPublicationArtifacts.Source source=authorized(kind,id); ensure(kind,id);
        Map<String,Object> previous=record(kind,id);
        if("REMOTE_FAILED".equals(previous.get("publish_state")) || "UNCERTAIN".equals(previous.get("publish_state")) || "PROCESSING".equals(previous.get("publish_state")) || "PUBLISHING".equals(previous.get("publish_state"))) return reconcile(kind,id,metadata,token,remoteTenant);
        JSONObject frozen=publicationMetadata(source,metadata);
        if(validCommit(previous.get("published_commit")) && previous.get("remote_uid")!=null
            && Objects.equals(source.getVersion(),previous.get("version_hash")) && Objects.equals(metadataHash(frozen),previous.get("metadata_hash"))) return status(kind,id);
        return performPublish(kind,id,frozen,token,remoteTenant,previous,false,null);
    }
    /** Explicit user-authorized continuation only after identity-scoped proof that no receipt exists. */
    public Map<String,Object> resume(String kind,Long id,JSONObject metadata,String token,String remoteTenant) {
        HubPublicationArtifacts.Source source=authorized(kind,id);
        Map<String,Object> previous=record(kind,id);
        if(previous==null || !("UNCERTAIN".equals(previous.get("publish_state")) || "PUBLISHING".equals(previous.get("publish_state"))))
            throw new ServiceException("只有提交中断且尚未确认的原发布请求可以继续上传");
        if("PUBLISHING".equals(previous.get("publish_state")) && !stalePublishing(kind,id,(String)previous.get("publish_key"))) throw new ServiceException("原发布请求仍在上传，请稍后核实，不能重复上传");
        if(!hasRequestIdentity(previous)) throw new ServiceException("原请求未保存开发广场账号身份，不能确认安全继续上传；请先核对原账号和发布记录");
        JSONObject frozen=metadata==null?storedMetadata(previous):publicationMetadata(source,metadata);
        String key=(String)previous.get("publish_key");
        if(!matchesKey(kind,id,source.getVersion(),frozen,key,false) || !Objects.equals(source.getVersion(),previous.get("request_version_hash"))
            || !Objects.equals(metadataHash(frozen),metadataHash(storedMetadata(previous))))
            throw new ServiceException("原发布内容或精选语义与当前版本不一致，不能继续上传或更换原请求标识");
        JSONObject receipt=client.receipt(kind,key,token,remoteTenant);
        requireSamePrincipal(previous,receipt);
        if(!"NOT_FOUND".equals(receipt.getStr("state"))) throw new ServiceException("开发广场已有原请求记录，请核实发布结果，不能继续上传");
        return performPublish(kind,id,frozen,token,remoteTenant,previous,true,receipt);
    }
    private Map<String,Object> performPublish(String kind,Long id,JSONObject metadata,String token,String remoteTenant,Map<String,Object> previous,boolean resumed,JSONObject knownReceipt) {
        HubPublicationArtifacts.Source requestSource=authorized(kind,id);
        String requestKey=resumed?(String)previous.get("publish_key"):publicationKey(data.tenant(),kind,id,requestSource.getVersion(),metadata.getStr("name"),metadata.getStr("description"),metadata.getStr("category"));
        if(requestSource.getVersion()==null || !matchesKey(kind,id,requestSource.getVersion(),metadata,requestKey,false)) throw new ServiceException("固定发布产物尚未就绪或已变化");
        JSONObject preflight=knownReceipt==null?client.receipt(kind,requestKey,token,remoteTenant):knownReceipt;
        JSONObject principal=principal(preflight);
        if(resumed) requireSamePrincipal(previous,preflight);
        if(!"NOT_FOUND".equals(preflight.getStr("state"))) throw new ServiceException("开发广场已有原请求记录，请核实发布结果，不会重新上传");
        // Identity and the immutable request are durable before any artifact download or upload.
        if(jdbc.update("UPDATE vls_hub_publication SET publish_state='PUBLISHING',publish_key=?,request_remote_user_id=?,request_remote_tenant_id=?,request_version_hash=?,request_metadata_json=?,error_message=NULL,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_state=? AND (publish_key=? OR (publish_key IS NULL AND ? IS NULL)) AND (publish_state<>'PUBLISHING' OR updated_at<DATE_SUB(NOW(),INTERVAL 10 MINUTE))",requestKey,principal.getStr("user_id"),principal.getStr("tenant_id"),requestSource.getVersion(),metadata.toString(),viewer(),data.tenant(),kind,id,previous.get("publish_state"),previous.get("publish_key"),previous.get("publish_key"))!=1)
            throw new ServiceException("该资源已有发布请求正在处理，请稍后刷新状态");
        boolean confirmed=false, attempted=false;
        try(HubPublicationArtifacts.Prepared prepared=artifacts.prepare(kind,id)) {
            if(!Objects.equals(requestSource.getVersion(),prepared.getVersion()) || !matchesKey(kind,id,prepared.getVersion(),metadata,requestKey,false))
                throw new ServiceException("准备期间固定产物已变化，已停止上传，请核对原请求");
            String metadataHash=metadataHash(metadata),key=requestKey;
                attempted=true;
                JSONObject remote=client.publish(kind,prepared,false,key,metadata,token,remoteTenant);
                if(Integer.valueOf(4).equals(remote.getInt("status"))) {
                    jdbc.update("UPDATE vls_hub_publication SET publish_state='PROCESSING',version_hash=?,metadata_hash=?,remote_uid=?,published_commit=NULL,updated_at=CURRENT_TIMESTAMP,error_message=NULL WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_key=?",prepared.getVersion(),metadataHash,remote.getStr("uid"),data.tenant(),kind,id,key);
                } else jdbc.update("UPDATE vls_hub_publication SET publish_state='PUBLISHED',version_hash=?,metadata_hash=?,remote_uid=?,published_commit=?,published_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP,error_message=NULL WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_key=?",prepared.getVersion(),metadataHash,remote.getStr("uid"),remote.getStr("published_commit"),data.tenant(),kind,id,key);
            confirmed=true;
        } catch(Exception ex) {
            if (confirmed) return status(kind,id); // A temporary-file cleanup error cannot undo the verified receipt.
            // A network timeout is not proof that the public publication did not happen.
            boolean authenticationRejected=HubPublicationClient.isAuthenticationRejected(ex);
            String state=(attempted && !authenticationRejected) || "PUBLISHING".equals(previous.get("publish_state")) ? "UNCERTAIN" : String.valueOf(previous.get("publish_state"));
            if(authenticationRejected) jdbc.update("UPDATE vls_hub_publication SET publish_key=?,request_remote_user_id=?,request_remote_tenant_id=?,request_version_hash=?,request_metadata_json=? WHERE tenant_id=? AND resource_kind=? AND resource_id=?",previous.get("publish_key"),previous.get("request_remote_user_id"),previous.get("request_remote_tenant_id"),previous.get("request_version_hash"),previous.get("request_metadata_json"),data.tenant(),kind,id);
            String error=authenticationRejected ? ex.getMessage() : "开发广场发布未完成确认，请保留资源版本并核实原请求";
            jdbc.update("UPDATE vls_hub_publication SET publish_state=?,error_message=?,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND resource_kind=? AND resource_id=?",state,error,data.tenant(),kind,id);
            if(ex instanceof ServiceException) throw (ServiceException)ex;
            throw new ServiceException("开发广场发布未完成确认，请检查固定产物是否就绪，并核实原请求");
        }
        return status(kind,id);
    }
    public Map<String,Object> reconcile(String kind,Long id,JSONObject metadata,String token,String remoteTenant) {
        HubPublicationArtifacts.Source source=authorized(kind,id);
        Map<String,Object> row=record(kind,id);
        if(row==null || row.get("publish_key")==null) throw new ServiceException("没有可核实的原发布请求，请先发布");
        String key=String.valueOf(row.get("publish_key"));
        JSONObject receipt=client.receipt(kind,key,token,remoteTenant);
        JSONObject verifiedPrincipal=principal(receipt);
        if(hasRequestIdentity(row)) requireSamePrincipal(row,receipt);
        String state=receipt.getStr("state"),message;
        if("PUBLISHED".equals(state)) {
            JSONObject frozen=metadata==null && row.get("request_metadata_json")!=null?storedMetadata(row):publicationMetadata(source,metadata);
            String requestVersion=row.get("request_version_hash")==null?source.getVersion():String.valueOf(row.get("request_version_hash"));
            boolean matches=matchesKey(kind,id,requestVersion,frozen,key,true);
            if(matches) {
                JSONObject remote=receipt.getJSONObject("resource");
                int updated=jdbc.update("UPDATE vls_hub_publication SET publish_state='PUBLISHED',version_hash=?,metadata_hash=?,remote_uid=?,published_commit=?,published_at=COALESCE(published_at,CURRENT_TIMESTAMP),request_remote_user_id=?,request_remote_tenant_id=?,request_version_hash=?,request_metadata_json=?,updated_at=CURRENT_TIMESTAMP,error_message=NULL WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_key=? AND publish_state IN ('UNCERTAIN','PUBLISHING','PROCESSING','REMOTE_FAILED')",requestVersion,metadataHash(frozen),remote.getStr("uid"),remote.getStr("published_commit"),verifiedPrincipal.getStr("user_id"),verifiedPrincipal.getStr("tenant_id"),requestVersion,frozen.toString(),data.tenant(),kind,id,key);
                message=updated==1?"已核实开发广场发布成功，未重新上传":"已核实远端回执，本地状态已变化，请刷新";
            } else message="开发广场已发布，但本地源版本或发布信息已变化，未将当前版本标记为已发布；请核对原发布信息";
        } else if("PROCESSING".equals(state)) message="开发广场仍在处理原发布，请稍后核实；不会重新上传";
        else if("FAILED".equals(state)) {
            message="开发广场原发布失败，请核查失败原因；不会自动重新上传";
            jdbc.update("UPDATE vls_hub_publication SET publish_state='REMOTE_FAILED',error_message=?,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_key=? AND publish_state IN ('UNCERTAIN','PUBLISHING','PROCESSING','REMOTE_FAILED')",message,data.tenant(),kind,id,key);
        }
        else message="开发广场未找到原发布回执，请核对登录环境和原请求；不会自动重新上传";
        Map<String,Object> result=status(kind,id);result.put("receiptState",state);result.put("receiptMessage",message);
        result.put("resumeAllowed","NOT_FOUND".equals(state) && resumable(kind,id,source,row));return result;
    }
    private boolean stalePublishing(String kind,Long id,String key) {
        return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT COUNT(*) FROM vls_hub_publication WHERE tenant_id=? AND resource_kind=? AND resource_id=? AND publish_key=? AND publish_state='PUBLISHING' AND updated_at<DATE_SUB(NOW(),INTERVAL 10 MINUTE)",Integer.class,data.tenant(),kind,id,key));
    }
    private boolean resumable(String kind,Long id,HubPublicationArtifacts.Source source,Map<String,Object> row) {
        if(!hasRequestIdentity(row) || !("UNCERTAIN".equals(row.get("publish_state")) || ("PUBLISHING".equals(row.get("publish_state")) && stalePublishing(kind,id,(String)row.get("publish_key"))))) return false;
        try {
            return Objects.equals(source.getVersion(),row.get("request_version_hash")) && matchesKey(kind,id,source.getVersion(),storedMetadata(row),(String)row.get("publish_key"),false);
        } catch(ServiceException ex) {return false;}
    }
    private static JSONObject publicationMetadata(HubPublicationArtifacts.Source source,JSONObject metadata) {
        return new JSONObject().set("name",HubPublicationClient.value(metadata,"name",source.getName(),200))
            .set("description",HubPublicationClient.value(metadata,"description",source.getDescription(),500))
            .set("category",HubPublicationClient.value(metadata,"category",source.getCategory(),100));
    }
    private static JSONObject storedMetadata(Map<String,Object> row) {
        if(row.get("request_metadata_json")==null) throw new ServiceException("原请求未保存固定发布信息，不能继续上传");
        try {return cn.hutool.json.JSONUtil.parseObj(String.valueOf(row.get("request_metadata_json")));}
        catch(Exception ex){throw new ServiceException("原请求发布信息无法核实，不能继续上传");}
    }
    private static String metadataHash(JSONObject metadata) {return hash(String.join("\n",metadata.getStr("name"),metadata.getStr("description"),metadata.getStr("category")));}
    private boolean matchesKey(String kind,Long id,String version,JSONObject metadata,String key,boolean legacyFeatured) {
        if(version==null || key==null) return false;
        String name=metadata.getStr("name"),description=metadata.getStr("description"),category=metadata.getStr("category");
        if(name==null || description==null || category==null) return false;
        return key.equals(publicationKey(data.tenant(),kind,id,version,name,description,category))
            || key.equals(legacyKey(data.tenant(),kind,id,version,false,name,description,category))
            || (legacyFeatured && key.equals(legacyKey(data.tenant(),kind,id,version,true,name,description,category)));
    }
    private static JSONObject principal(JSONObject receipt) {
        JSONObject principal=receipt.getJSONObject("principal");
        if(principal==null || org.apache.commons.lang3.StringUtils.isAnyBlank(principal.getStr("user_id"),principal.getStr("tenant_id"))) throw new ServiceException("开发广场未返回可信账号身份，不能继续操作");
        return principal;
    }
    private static boolean hasRequestIdentity(Map<String,Object> row) {return row.get("request_remote_user_id")!=null && row.get("request_remote_tenant_id")!=null;}
    private static void requireSamePrincipal(Map<String,Object> row,JSONObject receipt) {
        JSONObject principal=principal(receipt);
        if(!hasRequestIdentity(row) || !Objects.equals(row.get("request_remote_user_id"),principal.getStr("user_id")) || !Objects.equals(row.get("request_remote_tenant_id"),principal.getStr("tenant_id")))
            throw new ServiceException("当前开发广场账号或租户与原发布请求不同，请切回原账号和环境后核实；不会重新上传");
    }
    private static boolean validCommit(Object value) { return value instanceof String && ((String)value).matches("[0-9a-f]{40}"); }
    static String publicationKey(String tenant,String kind,Long id,String version,String name,String description,String category) {
        return hash(String.join("\n","development-square-v2",tenant,kind,String.valueOf(id),version,name,description,category));
    }
    static String legacyKey(String tenant,String kind,Long id,String version,boolean featured,String name,String description,String category) {
        return hash(String.join("\n",tenant,kind,String.valueOf(id),version,Boolean.toString(featured),name,description,category));
    }
    private HubPublicationArtifacts.Source authorized(String kind,Long id) {
        if(id==null || id<=0 || (!"model".equals(kind)&&!"dataset".equals(kind))) throw new ServiceException("发布资源无效");
        if("dataset".equals(kind)) StpUtil.checkPermission("vls:menu:data-management");
        else StpUtil.checkPermissionOr("vls:menu:algorithm-model","vls:menu:algorithm-training");
        viewer(); return artifacts.inspect(kind,id);
    }
    private String viewer() { String user=LoginHelper.getUserId(); if(user==null || user.isEmpty()) throw new ServiceException("请重新登录"); return user; }
    private void ensure(String kind,Long id) { jdbc.update("INSERT IGNORE INTO vls_hub_publication(tenant_id,resource_kind,resource_id,updated_by) VALUES(?,?,?,?)",data.tenant(),kind,id,viewer()); }
    private Map<String,Object> record(String kind,Long id) { List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM vls_hub_publication WHERE tenant_id=? AND resource_kind=? AND resource_id=?",data.tenant(),kind,id); return rows.isEmpty()?null:rows.get(0); }
    private static Boolean asBoolean(Object value) { return Boolean.TRUE.equals(value) || (value instanceof Number && ((Number)value).intValue()!=0); }
    static String hash(String text) { try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)); StringBuilder out=new StringBuilder(); for(byte b:bytes) out.append(String.format("%02x",b)); return out.toString(); } catch(Exception ex) {throw new IllegalStateException(ex);} }
}
