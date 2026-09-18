package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@Tag("dev")
class ModelHubCatalogServiceTest {
    final ModelHubProperties config = config();
    final RestTemplate rest = new RestTemplate();
    final MockRestServiceServer server = MockRestServiceServer.createServer(rest);
    final ModelHubCatalogService service = new ModelHubCatalogService(config, rest);

    static ModelHubProperties config() {
        ModelHubProperties p = new ModelHubProperties();
        p.setBaseUrl("http://hub.test/bus/apaas-deployment-service");
        p.setGuestToken("test-guest"); p.setAppId("test-app"); p.setSecretKey("test-key");
        return p;
    }

    @Test void publicCatalogUsesHeadersOnlyAndBoundsPageSize() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/modelList"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("accesstoken", "test-guest"))
            .andExpect(header("appid", "test-app")).andExpect(header("secretkey", "test-key"))
            .andExpect(header("requesttype", "app"))
            .andExpect(content().json("{\"page\":1,\"pagesize\":48,\"keyword\":\"安全\",\"category\":\"\"}",true))
            .andRespond(withSuccess("{\"code\":200,\"data\":{\"list\":[],\"counts\":0}}",MediaType.APPLICATION_JSON));
        assertEquals(0, service.list(0,100," 安全 ").getInt("counts"));
        server.verify();
    }

    @Test void guestCannotDownload() {
        for (String token : new String[]{null,"","test-guest"}) {
            assertThrows(ServiceException.class, () -> service.download("uid","file.om","",token,null,new MockHttpServletResponse()));
        }
        server.verify();
    }

    @Test void fileBrowsingUsesGuestIdentityWithoutUserLogin() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/space"))
            .andExpect(header("accesstoken","test-guest")).andExpect(headerDoesNotExist("tenantid"))
            .andExpect(content().json("{\"uid\":\"uid\",\"path\":\"weights\",\"branch\":\"main\",\"accessToken\":\"test-guest\"}",true))
            .andRespond(withSuccess("{\"code\":200,\"data\":[]}",MediaType.APPLICATION_JSON));
        assertNotNull(service.files("uid","weights","main"));
        server.verify();
    }

    @Test void failedDownloadJsonNeverBecomesModelFile() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/downloadFile"))
            .andExpect(content().json("{\"uid\":\"uid\",\"path\":\"model.om\",\"branch\":\"\",\"accessToken\":\"test-user\"}",true))
            .andRespond(withSuccess("{\"code\":4004,\"msg\":\"invalid\"}",MediaType.APPLICATION_JSON));
        MockHttpServletResponse output = new MockHttpServletResponse();
        assertThrows(ServiceException.class, () -> service.download("uid","model.om","","test-user",null,output));
        assertEquals(0,output.getContentAsByteArray().length);
        server.verify();
    }

    @Test void successfulDownloadStreamsExactBytes() {
        byte[] bytes = new byte[]{1,2,3,4};
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/downloadFile"))
            .andExpect(header("accesstoken","test-user"))
            .andExpect(content().json("{\"uid\":\"uid\",\"path\":\"weights/model.om\",\"branch\":\"main\",\"accessToken\":\"test-user\"}",true))
            .andRespond(withSuccess(bytes,MediaType.APPLICATION_OCTET_STREAM));
        MockHttpServletResponse output = new MockHttpServletResponse();
        service.download("uid","weights/model.om","main","test-user",null,output);
        assertArrayEquals(bytes,output.getContentAsByteArray());
        assertEquals("no-store",output.getHeader("Cache-Control"));
        server.verify();
    }

    @Test void unconfiguredServiceFailsClosed() {
        config.setGuestToken("");
        assertThrows(ServiceException.class, () -> service.list(1,12,""));
        server.verify();
    }

    @Test void singleFileDownloadRejectsEmptyPathInsteadOfDownloadingArchive() {
        assertThrows(ServiceException.class, () -> service.download("uid"," ","main","test-user",null,new MockHttpServletResponse()));
        server.verify();
    }

    @Test void categoriesUseGuestHeadersAndBody() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/categories"))
            .andExpect(header("accesstoken","test-guest"))
            .andExpect(content().json("{\"accessToken\":\"test-guest\"}",true))
            .andRespond(withSuccess("{\"code\":200,\"data\":[{\"category\":\"对象检测\",\"count\":2}]}",MediaType.APPLICATION_JSON));
        assertNotNull(service.categories());
        server.verify();
    }

    @Test void failedGuestConfigurationDoesNotAskForUserLogin() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/categories"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("{\"code\":4004,\"message\":\"invalid identity\"}"));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.categories());
        assertTrue(ex.getMessage().contains("4004"));
        assertFalse(ex.getMessage().contains("test-user"));
        assertTrue(ex.getMessage().contains("游客配置"));
        server.verify();
    }

    @Test void missingSpaceIsNotALoginFailure() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/space"))
            .andRespond(withSuccess("{\"code\":4101,\"msg\":\"该历史模型尚未建立Gitea空间\"}",MediaType.APPLICATION_JSON));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.files("uid","",""));
        assertTrue(ex.getMessage().contains("文件空间"));
        assertFalse(ex.getMessage().contains("登录"));
        server.verify();
    }
}
