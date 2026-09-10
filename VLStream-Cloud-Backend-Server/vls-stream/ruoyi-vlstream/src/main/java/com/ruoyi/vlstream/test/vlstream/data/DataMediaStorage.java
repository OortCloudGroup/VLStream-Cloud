package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.vlstream.test.vlstream.service.impl.AnnotationImageObjectKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Component
public class DataMediaStorage {
    @Value("${vlstream.annotation-media.public-endpoint:}")
    private String publicEndpoint;

    public void put(String key, byte[] bytes, String contentType) {
        OssFactory.instance().upload(bytes, key, contentType);
    }

    public void putFile(String key, java.nio.file.Path file, String contentType) {
        OssFactory.instance().uploadFile(file.toFile(), key, contentType);
    }

    public InputStream read(String key) {
        OssClient client = OssFactory.instance();
        return client.getObjectContent(AnnotationImageObjectKey.normalize(key, client.getBucketName()));
    }

    public String preview(String key) {
        OssClient client = OssFactory.instance();
        return client.getPrivateUrl(AnnotationImageObjectKey.normalize(key, client.getBucketName()), 600, publicEndpoint);
    }

    public void remove(String key) {
        OssFactory.instance().delete(key);
    }
}
