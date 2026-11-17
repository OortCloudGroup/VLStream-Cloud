package com.ruoyi.vlstream.test.vlstream.service;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
@Tag("dev")
class ModelHubDeploymentClientTest {
    final RestTemplate rest=new RestTemplate();
    final MockRestServiceServer server=MockRestServiceServer.createServer(rest);
    final com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties config=ModelHubCatalogServiceTest.config();
    final ModelHubDeploymentClient client=new ModelHubDeploymentClient(config,rest);
    final String sha="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    @Test void refusesGuestAndMutableBranch() throws Exception {
        assertThrows(ServiceException.class,()->client.detail("uid","test-guest",null));
        Path file=Files.createTempFile("hub-test-",".bin");
        try {assertThrows(ServiceException.class,()->client.download("uid","main","model.om","user","tenant",file,10));}
        finally {Files.deleteIfExists(file);}
        server.verify();
    }
    @Test void fixedCommitAndRealIdentityAreForwarded() throws Exception {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/downloadFile"))
            .andExpect(header("accesstoken","user")).andExpect(header("tenantid","tenant"))
            .andExpect(content().json("{\"uid\":\"uid\",\"branch\":\""+sha+"\",\"path\":\"model.om\",\"accessToken\":\"user\"}",true))
            .andRespond(withSuccess(new byte[]{1,2,3},MediaType.APPLICATION_OCTET_STREAM));
        Path file=Files.createTempFile("hub-test-",".bin");
        try {client.download("uid",sha,"model.om","user","tenant",file,3);assertEquals(3,Files.size(file));}
        finally {Files.deleteIfExists(file);}
        server.verify();
    }
    @Test void errorsAndOversizedFilesCannotBecomeModels() throws Exception {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/downloadFile"))
            .andRespond(withSuccess("{\"code\":401}",MediaType.APPLICATION_JSON));
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/downloadFile"))
            .andRespond(withSuccess(new byte[]{1,2,3},MediaType.APPLICATION_OCTET_STREAM));
        Path file=Files.createTempFile("hub-test-",".bin");
        try {
            assertThrows(ServiceException.class,()->client.download("uid",sha,"model.om","user","tenant",file,10));
            assertEquals(0,Files.size(file));
            assertThrows(ServiceException.class,()->client.download("uid",sha,"model.om","user","tenant",file,2));
        } finally {Files.deleteIfExists(file);}
        server.verify();
    }
}
