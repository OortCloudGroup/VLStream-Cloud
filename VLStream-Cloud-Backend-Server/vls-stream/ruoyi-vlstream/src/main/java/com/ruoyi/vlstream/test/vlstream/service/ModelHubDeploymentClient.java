package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Only fixed authenticated Hub operations; redirects and arbitrary URLs are forbidden. */
@Service
public class ModelHubDeploymentClient {
    private final ModelHubProperties properties;
    private final RestTemplate rest;
    @Autowired public ModelHubDeploymentClient(ModelHubProperties p) { this(p, new RestTemplate(factory())); }
    ModelHubDeploymentClient(ModelHubProperties p, RestTemplate r) { properties=p; rest=r; }
    private static SimpleClientHttpRequestFactory factory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection c,String m) throws IOException {
                super.prepareConnection(c,m); c.setInstanceFollowRedirects(false);
            }
        };
        f.setConnectTimeout(5000); f.setReadTimeout(60000); return f;
    }
    public JSONObject detail(String uid,String token,String tenant) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        request("modelDetail",new JSONObject().set("uid",uid),token,tenant,out,1024*1024,false);
        JSONObject result=JSONUtil.parseObj(new String(out.toByteArray(),StandardCharsets.UTF_8));
        if (!Integer.valueOf(200).equals(result.getInt("code")) || result.getJSONObject("data")==null)
            throw new ServiceException("Model Hub 模型不存在或当前账号无权访问");
        return result.getJSONObject("data");
    }
    public void download(String uid,String commit,String path,String token,String tenant,Path target,long limit) throws IOException {
        if (commit==null || !commit.matches("[0-9a-f]{40}")) throw new ServiceException("模型缺少固定提交版本");
        try(OutputStream out=Files.newOutputStream(target)) {
            request("downloadFile",new JSONObject().set("uid",uid).set("branch",commit).set("path",path),token,tenant,out,limit,!"vls-device-manifest.json".equals(path));
        }
    }
    private void request(String operation,JSONObject body,String token,String tenant,OutputStream output,long limit,boolean binary) {
        if(token==null || token.trim().isEmpty() || token.equals(properties.getGuestToken())) throw new ServiceException("下发前请登录 OortCloud");
        String base=properties.getBaseUrl();
        if(base==null || properties.getAppId()==null || properties.getSecretKey()==null) throw new ServiceException("Model Hub 服务未配置");
        body.set("accessToken",token);
        try {
            rest.execute(base.replaceAll("/+$","")+"/aiModel/v1/"+operation,HttpMethod.POST,req->{
                req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                req.getHeaders().set("accesstoken",token); req.getHeaders().set("appid",properties.getAppId());
                req.getHeaders().set("secretkey",properties.getSecretKey()); req.getHeaders().set("requesttype","app");
                if(tenant!=null&&!tenant.trim().isEmpty())req.getHeaders().set("tenantid",tenant);
                req.getBody().write(body.toString().getBytes(StandardCharsets.UTF_8));
            },res->{
                if(!res.getStatusCode().is2xxSuccessful())throw new ServiceException("Model Hub 请求未成功");
                MediaType type=res.getHeaders().getContentType();
                if(binary && type!=null && (type.includes(MediaType.APPLICATION_JSON)||type.getSubtype().endsWith("+json")))
                    throw new ServiceException("Model Hub 未返回授权文件");
                byte[] buffer=new byte[65536]; long size=0; int n;
                while((n=res.getBody().read(buffer))!=-1) {
                    size+=n; if(size>limit)throw new ServiceException("Model Hub 文件超过安全大小限制");
                    output.write(buffer,0,n);
                }
                if(size==0)throw new ServiceException("Model Hub 返回空内容");
                return null;
            });
        } catch(ServiceException ex){throw ex;} catch(Exception ex){throw new ServiceException("Model Hub 下载或授权校验失败，请核对登录状态");}
    }
}
