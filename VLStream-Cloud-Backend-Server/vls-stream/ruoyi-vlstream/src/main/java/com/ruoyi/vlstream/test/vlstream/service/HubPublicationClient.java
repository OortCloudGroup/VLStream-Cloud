package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.Map;

/** Only the configured Hub is a publication destination; no caller URL or stored user credentials. */
@Service
public class HubPublicationClient {
    private final ModelHubProperties properties;
    private final RestTemplate rest;
    @Autowired public HubPublicationClient(ModelHubProperties properties) { this(properties, new RestTemplate(factory())); }
    HubPublicationClient(ModelHubProperties properties, RestTemplate rest) { this.properties=properties; this.rest=rest; }
    private static SimpleClientHttpRequestFactory factory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection c,String method) throws IOException {
                super.prepareConnection(c,method); c.setInstanceFollowRedirects(false);
            }
        };
        f.setConnectTimeout(5000); f.setReadTimeout(120000); f.setBufferRequestBody(false); return f;
    }
    public JSONObject publish(String kind, HubPublicationArtifacts.Prepared source, boolean featured,
        String key, JSONObject metadata, String token, String tenant) {
        if (StringUtils.isBlank(token) || token.equals(properties.getGuestToken())) throw authenticationRejected();
        String base=StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()),"/");
        if (StringUtils.isAnyBlank(base,properties.getAppId(),properties.getSecretKey())) throw new ServiceException("开发广场尚未配置");
        if (!"model".equals(kind) && !"dataset".equals(kind)) throw new ServiceException("发布资源类型无效");
        MultiValueMap<String,Object> body=new LinkedMultiValueMap<>();
        body.add("name",requiredValue(metadata,"name",source.getName(),200));
        body.add("description",value(metadata,"description",source.getDescription(),500));
        body.add("category",requiredValue(metadata,"category",source.getCategory(),100));
        // The dataset endpoint reads data_type, not model_type.
        body.add("model".equals(kind)?"model_type":"data_type",value(null,"type",source.getModelType(),100));
        if (key==null || !key.matches("[A-Za-z0-9._-]{1,128}")) throw new ServiceException("发布幂等标识无效");
        // Alias has a 50-rune upstream limit; never truncate the independent idempotency key.
        body.add("alias","vls-"+key.substring(0,Math.min(40,key.length())));
        body.add("featured",Boolean.toString(featured)); body.add("client_publish_key",key); body.add("async_commit","true");
        for(Map.Entry<String,Path> file:source.getFiles().entrySet()) {
            body.add("files",new FileSystemResource(file.getValue().toFile())); body.add("paths",file.getKey());
        }
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("accesstoken",token); headers.set("appid",properties.getAppId()); headers.set("secretkey",properties.getSecretKey());
        headers.set("requesttype","app"); if(StringUtils.isNotBlank(tenant)) headers.set("tenantid",tenant);
        try {
            ResponseEntity<String> response=rest.postForEntity(base+("model".equals(kind)?"/aiModel/v1/":"/aiDataset/v1/")+"uploadFilesDirect",new HttpEntity<>(body,headers),String.class);
            if(response.getStatusCodeValue()==401) throw authenticationRejected();
            JSONObject result=JSONUtil.parseObj(response.getBody());
            rejectAuthenticationCode(result);
            if(!response.getStatusCode().is2xxSuccessful() || !Integer.valueOf(200).equals(result.getInt("code")))
                throw new ServiceException("开发广场未确认发布成功，请核实登录、权限或发布状态后重试");
            JSONObject data=result.getJSONObject("data");
            if(data==null || StringUtils.isBlank(data.getStr("uid"))) throw new ServiceException("开发广场未返回发布资源标识，结果待核实");
            if (Integer.valueOf(4).equals(data.getInt("status"))) return data; // Accepted background commit; not publication success.
            if (!Integer.valueOf(1).equals(data.getInt("status"))) throw new ServiceException("开发广场仍在处理或原发布未完成，请核实原资源状态");
            if (data.getStr("published_commit")==null || !data.getStr("published_commit").matches("[0-9a-f]{40}")) throw new ServiceException("开发广场未返回固定发布版本，结果待核实");
            return data;
        } catch(ServiceException e) { throw e; }
        catch(HttpClientErrorException e) { if(e.getRawStatusCode()==401) throw authenticationRejected(); throw new ServiceException("发布结果未确认，请保留当前资源版本后重试；不要重复新建资源"); }
        catch(Exception e) { throw new ServiceException("发布结果未确认，请保留当前资源版本后重试；不要重复新建资源"); }
    }
    /** Small identity-scoped lookup only: never uploads artifacts or mutates the remote resource. */
    public JSONObject receipt(String kind,String key,String token,String tenant) {
        if(StringUtils.isBlank(token) || token.equals(properties.getGuestToken())) throw authenticationRejected();
        String base=StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()),"/");
        if(StringUtils.isAnyBlank(base,properties.getAppId(),properties.getSecretKey())) throw new ServiceException("开发广场尚未配置");
        if(!"model".equals(kind) && !"dataset".equals(kind)) throw new ServiceException("发布资源类型无效");
        if(key==null || !key.matches("[A-Za-z0-9._-]{1,128}")) throw new ServiceException("原发布幂等标识缺失，不能核实");
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accesstoken",token);headers.set("appid",properties.getAppId());headers.set("secretkey",properties.getSecretKey());headers.set("requesttype","app");
        if(StringUtils.isNotBlank(tenant)) headers.set("tenantid",tenant);
        JSONObject body=new JSONObject().set("accessToken",token).set("client_publish_key",key);
        try {
            ResponseEntity<String> response=rest.postForEntity(base+("model".equals(kind)?"/aiModel/v1/":"/aiDataset/v1/")+"publicationReceipt",new HttpEntity<>(body.toString(),headers),String.class);
            if(response.getStatusCodeValue()==401) throw authenticationRejected();
            JSONObject result=JSONUtil.parseObj(response.getBody());rejectAuthenticationCode(result);
            JSONObject data=result.getJSONObject("data");
            if(!response.getStatusCode().is2xxSuccessful() || !Integer.valueOf(200).equals(result.getInt("code")) || data==null)
                throw new ServiceException("开发广场发布结果暂时无法核实，请稍后再次核实；不会重新上传");
            JSONObject principal=data.getJSONObject("principal");
            if(principal==null || StringUtils.isAnyBlank(principal.getStr("user_id"),principal.getStr("tenant_id"))
                || principal.getStr("user_id").length()>128 || principal.getStr("tenant_id").length()>128)
                throw new ServiceException("开发广场未返回可信发布账号身份，不能继续操作");
            String state=data.getStr("state");
            if(!java.util.Arrays.asList("NOT_FOUND","PROCESSING","PUBLISHED","FAILED").contains(state)) throw new ServiceException("开发广场返回的发布状态无效，请稍后核实");
            if("PUBLISHED".equals(state)) {
                JSONObject resource=data.getJSONObject("resource");
                if(resource==null || StringUtils.isBlank(resource.getStr("uid")) || !Integer.valueOf(1).equals(resource.getInt("status"))
                    || resource.getStr("published_commit")==null || !resource.getStr("published_commit").matches("[0-9a-f]{40}"))
                    throw new ServiceException("开发广场未返回完整发布回执，请稍后核实");
            }
            return data;
        } catch(ServiceException ex) {throw ex;}
        catch(HttpClientErrorException ex) {if(ex.getRawStatusCode()==401) throw authenticationRejected();throw new ServiceException("开发广场发布结果暂时无法核实，请稍后再次核实；不会重新上传");}
        catch(Exception ex) {throw new ServiceException("开发广场发布结果暂时无法核实，请稍后再次核实；不会重新上传");}
    }
    public JSONObject setFeatured(String kind,String uid,boolean featured,String token,String tenant) {
        if (StringUtils.isBlank(token) || token.equals(properties.getGuestToken())) throw authenticationRejected();
        String base=StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()),"/");
        if (StringUtils.isAnyBlank(base,properties.getAppId(),properties.getSecretKey())) throw new ServiceException("开发广场尚未配置");
        if (!"model".equals(kind) && !"dataset".equals(kind)) throw new ServiceException("发布资源类型无效");
        JSONObject body=new JSONObject().set("uid",uid).set("featured",featured).set("accessToken",token);
        try {
            JSONObject result=rest.execute(base+("model".equals(kind)?"/aiModel/v1/":"/aiDataset/v1/")+"setFeatured",HttpMethod.POST, request->{
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                request.getHeaders().set("accesstoken",token); request.getHeaders().set("appid",properties.getAppId());
                request.getHeaders().set("secretkey",properties.getSecretKey()); request.getHeaders().set("requesttype","app");
                if(StringUtils.isNotBlank(tenant)) request.getHeaders().set("tenantid",tenant);
                request.getBody().write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }, response->JSONUtil.parseObj(org.springframework.util.StreamUtils.copyToString(response.getBody(),java.nio.charset.StandardCharsets.UTF_8)));
            rejectAuthenticationCode(result);
            JSONObject resource=result.getJSONObject("data");
            if(!Integer.valueOf(200).equals(result.getInt("code")) || resource==null || !uid.equals(resource.getStr("uid")) || !Integer.valueOf(1).equals(resource.getInt("status")))
                throw new ServiceException("无法更新原发布的精选状态，请确认当前 OortCloud 账号仍拥有该资源");
            return resource;
        } catch(ServiceException ex){throw ex;} catch(HttpClientErrorException ex){if(ex.getRawStatusCode()==401) throw authenticationRejected();throw new ServiceException("精选同步结果未确认，请核实原发布状态");} catch(Exception ex){throw new ServiceException("精选同步结果未确认，请核实原发布状态");}
    }
    // Dedicated business code: never use 401, which clears the independent local VLS session.
    static ServiceException authenticationRejected() {
        return new ServiceException("开发广场账户验证失败，请登录与开发广场相同环境的 OortCloud 账号后重试",4401);
    }
    static boolean isAuthenticationRejected(Exception ex) {
        return ex instanceof ServiceException && Integer.valueOf(4401).equals(((ServiceException)ex).getCode());
    }
    private static void rejectAuthenticationCode(JSONObject response) {
        Integer code=response.getInt("code");
        if(Integer.valueOf(4004).equals(code) || Integer.valueOf(4444).equals(code) || Integer.valueOf(401).equals(code)) throw authenticationRejected();
    }
    private static String requiredValue(JSONObject data,String key,String fallback,int max) {
        String result=value(data,key,fallback,max);
        if(StringUtils.isBlank(result)) throw new ServiceException("发布"+key+"不能为空");
        return result;
    }
    // Go validates text with utf8.RuneCountInString; UTF-16 length rejects supplementary Chinese incorrectly.
    static String value(JSONObject data,String key,String fallback,int max) {
        String value=data==null?null:data.getStr(key);
        value=StringUtils.defaultIfBlank(value,StringUtils.defaultString(fallback));
        if(value.codePointCount(0,value.length())>max) throw new ServiceException("发布"+key+"内容过长");
        return value;
    }
}
