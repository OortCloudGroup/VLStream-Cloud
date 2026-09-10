package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.function.LongConsumer;

/** Public file links only; repository code, scripts and HTML are never executed. */
@Component
public class PublicDatasetDownloader {
    public String normalize(String value) {
        URI uri = uri(value); String host = uri.getHost().toLowerCase(java.util.Locale.ROOT); String path = uri.getRawPath();
        if ("github.com".equals(host) && path.contains("/blob/")) return "https://raw.githubusercontent.com" + path.replaceFirst("/blob/", "/");
        if ("huggingface.co".equals(host) && path.contains("/blob/")) return "https://huggingface.co" + path.replaceFirst("/blob/", "/resolve/");
        if ("gitee.com".equals(host) && path.contains("/blob/")) return "https://gitee.com" + path.replaceFirst("/blob/", "/raw/");
        return uri.toASCIIString();
    }
    public URI uri(String value) {
        try {
            URI uri = URI.create(value);
            if (!java.util.Arrays.asList("http","https").contains(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null)
                throw new IllegalArgumentException();
            return uri;
        } catch (RuntimeException e) { throw new ServiceException("请提供有效的 HTTP(S) 文件下载地址"); }
    }
    static InetAddress[] publicAddresses(String host) throws UnknownHostException {
        InetAddress[] addresses=InetAddress.getAllByName(host);
        for(InetAddress address:addresses) {
            byte[] raw=address.getAddress();
            boolean special = address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()
                || (raw.length==16 && (raw[0]&0xfe)==0xfc)
                || (raw.length==4 && ((raw[0]&255)==0 || (raw[0]&255)>=224 || ((raw[0]&255)==100 && (raw[1]&255)>=64 && (raw[1]&255)<=127)));
            if(special) throw new UnknownHostException("公开数据来源不能访问本机、内网或保留网络地址");
        }
        return addresses;
    }
    public String download(String url, Path destination, long maxBytes, LongConsumer progress) throws IOException {
        String current=normalize(url);
        RequestConfig config=RequestConfig.custom().setConnectTimeout(15000).setConnectionRequestTimeout(15000).setSocketTimeout(60000).build();
        try(CloseableHttpClient client=HttpClients.custom().setDnsResolver(PublicDatasetDownloader::publicAddresses).setDefaultRequestConfig(config)
            .disableRedirectHandling().disableAutomaticRetries().disableContentCompression().build()) {
            for(int redirect=0;redirect<6;redirect++) {
                URI address=uri(current);
                HttpGet get=new HttpGet(address); get.setHeader("User-Agent","VLStream-Dataset-Import/1.0");
                try(CloseableHttpResponse response=client.execute(get)) {
                    int status=response.getStatusLine().getStatusCode();
                    if(java.util.Arrays.asList(301,302,303,307,308).contains(status)) {
                        if(response.getFirstHeader("Location")==null) throw new ServiceException("下载重定向缺少地址");
                        URI next=address.resolve(response.getFirstHeader("Location").getValue());
                        if("https".equals(address.getScheme()) && !"https".equals(next.getScheme())) throw new ServiceException("下载地址发生不安全的协议降级");
                        current=next.toString(); continue;
                    }
                    if(status!=200 || response.getEntity()==null) throw new ServiceException("公开文件下载失败，HTTP " + status + "；受限、登录或付费资源暂不支持");
                    long length=response.getEntity().getContentLength();
                    if(length>maxBytes) throw new ServiceException("来源文件超过当前导入容量上限");
                    String contentType=response.getFirstHeader("Content-Type")==null ? "" : response.getFirstHeader("Content-Type").getValue();
                    if(contentType.contains("text/html")) throw new ServiceException("链接返回的是网页，请使用实际文件下载链接");
                    try(InputStream input=response.getEntity().getContent()) {
                        String digest=DatasetFileIO.copy(input,destination,maxBytes,progress);
                        if(length>=0 && java.nio.file.Files.size(destination)!=length) throw new ServiceException("远程文件下载不完整");
                        return digest;
                    }
                }
            }
        }
        throw new ServiceException("下载地址重定向次数过多");
    }
}
