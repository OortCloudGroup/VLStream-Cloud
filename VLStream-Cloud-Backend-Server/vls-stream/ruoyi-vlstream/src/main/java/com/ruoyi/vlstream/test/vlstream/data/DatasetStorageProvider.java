package com.ruoyi.vlstream.test.vlstream.data;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import org.springframework.stereotype.Component;
/** Reuses the configured active OSS provider; upload jobs pin its configuration key. */
@Component
public class DatasetStorageProvider {
    public OssClient current() { return OssFactory.instance(); }
    public OssClient get(String key) { return OssFactory.instance(key); }
}
