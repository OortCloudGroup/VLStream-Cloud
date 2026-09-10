package com.ruoyi.vlstream.test.vlstream.data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class PublicDatasetDownloaderTest {
    @Test void normalizesKnownFilePagesWithoutExecutingRepositoryCode(){PublicDatasetDownloader client=new PublicDatasetDownloader();assertEquals("https://raw.githubusercontent.com/a/b/main/image.png",client.normalize("https://github.com/a/b/blob/main/image.png"));assertEquals("https://huggingface.co/datasets/a/b/resolve/main/image.png",client.normalize("https://huggingface.co/datasets/a/b/blob/main/image.png"));}
    @Test void rejectsLocalAndMetadataNetworks(){assertThrows(java.net.UnknownHostException.class,()->PublicDatasetDownloader.publicAddresses("127.0.0.1"));assertThrows(java.net.UnknownHostException.class,()->PublicDatasetDownloader.publicAddresses("169.254.169.254"));assertThrows(java.net.UnknownHostException.class,()->PublicDatasetDownloader.publicAddresses("10.0.0.1"));}
    @Test void rejectsCredentialsAndNonHttpSchemes(){PublicDatasetDownloader client=new PublicDatasetDownloader();assertThrows(RuntimeException.class,()->client.uri("file:///etc/passwd"));assertThrows(RuntimeException.class,()->client.uri("https://user:password@example.com/a.zip"));}
}
