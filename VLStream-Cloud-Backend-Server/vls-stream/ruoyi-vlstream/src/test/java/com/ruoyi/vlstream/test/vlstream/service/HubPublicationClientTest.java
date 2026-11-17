package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.springframework.mock.http.client.MockClientHttpRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@Tag("dev")
class HubPublicationClientTest {
    final ModelHubProperties config=ModelHubCatalogServiceTest.config();
    final RestTemplate rest=new RestTemplate();
    final MockRestServiceServer server=MockRestServiceServer.createServer(rest);
    final HubPublicationClient client=new HubPublicationClient(config,rest);
    HubPublicationArtifacts.Prepared prepared() throws Exception {
        return new HubPublicationArtifacts.Prepared(new HubPublicationArtifacts.Source("测试模型","描述","目标检测","object_detection","v1"),Files.createTempDirectory("hub-test-"));
    }
    @Test void guestsNeverTransmitArtifacts() throws Exception {
        try(HubPublicationArtifacts.Prepared p=prepared()) {
            assertThrows(ServiceException.class,()->client.publish("model",p,true,"fixed",null,"test-guest",null));
            assertThrows(ServiceException.class,()->client.publish("model",p,true,"fixed",null,null,null));
        }
        server.verify();
    }
    @Test void publishesOnlyToFixedServiceWithRealIdentity() throws Exception {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/uploadFilesDirect"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("accesstoken","real-user"))
            .andExpect(header("tenantid","tenant-one"))
            .andRespond(withSuccess("{\"code\":200,\"data\":{\"uid\":\"remote-id\",\"status\":1,\"published_commit\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}}",MediaType.APPLICATION_JSON));
        try(HubPublicationArtifacts.Prepared p=prepared()) {
            assertEquals("remote-id",client.publish("model",p,true,"fixed",new JSONObject(),"real-user","tenant-one").getStr("uid"));
        }
        server.verify();
    }
    @Test void missingReceiptNeverClaimsSuccess() throws Exception {
        server.expect(requestTo(config.getBaseUrl()+"/aiDataset/v1/uploadFilesDirect"))
            .andRespond(withSuccess("{\"code\":200,\"data\":{}}",MediaType.APPLICATION_JSON));
        try(HubPublicationArtifacts.Prepared p=prepared()) {
            assertThrows(ServiceException.class,()->client.publish("dataset",p,false,"fixed",null,"real-user",null));
        }
        server.verify();
    }
    @Test void featuredUpdateUsesOriginalResourceWithoutUploadingFiles() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/setFeatured"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"uid\":\"original\",\"featured\":false,\"accessToken\":\"real-user\"}",true))
            .andRespond(withSuccess("{\"code\":200,\"data\":{\"uid\":\"original\",\"status\":1}}",MediaType.APPLICATION_JSON));
        assertEquals("original",client.setFeatured("model","original",false,"real-user","tenant").getStr("uid"));
        server.verify();
    }
    @Test void pendingReplayReturnsProcessingWithoutInventingCommit() throws Exception {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/uploadFilesDirect"))
            .andRespond(withSuccess("{\"code\":200,\"data\":{\"uid\":\"original\",\"status\":4}}",MediaType.APPLICATION_JSON));
        try(HubPublicationArtifacts.Prepared p=prepared()) {
            JSONObject result=client.publish("model",p,true,"fixed",null,"real-user",null);
            assertEquals(Integer.valueOf(4),result.getInt("status"));assertNull(result.getStr("published_commit"));
        }
        server.verify();
    }
    @Test void stablePublicationKeyAndMetadataBounds() {
        assertEquals(HubPublicationService.hash("same"),HubPublicationService.hash("same"));
        assertNotEquals(HubPublicationService.hash("same"),HubPublicationService.hash("different"));
        assertThrows(ServiceException.class,()->HubPublicationClient.value(new JSONObject().set("name","1234"),"name","default",3));
    }
    @Test void realHashKeyAndChineseBoundariesArePreservedInMultipartForBothKinds() throws Exception {
        String key="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String name=repeat("模",200), description=repeat("𠀀",500), category=repeat("类",100);
        for(String kind:new String[]{"model","dataset"}) {
            server.expect(requestTo(config.getBaseUrl()+("model".equals(kind)?"/aiModel/v1/":"/aiDataset/v1/")+"uploadFilesDirect"))
                .andExpect(request->{
                    assertTrue(request.getHeaders().getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA));
                    String body=((MockClientHttpRequest)request).getBodyAsString(StandardCharsets.UTF_8);
                    assertEquals("vls-"+key.substring(0,40),part(body,"alias"));
                    assertTrue(part(body,"alias").matches("[a-zA-Z0-9._-]{1,50}"));
                    assertEquals(key,part(body,"client_publish_key"));assertEquals("true",part(body,"async_commit"));
                    assertEquals(name,part(body,"name"));
                    assertEquals(description,part(body,"description"));
                    assertEquals(category,part(body,"category"));
                    assertEquals("object_detection",part(body,"model".equals(kind)?"model_type":"data_type"));
                }).andRespond(withSuccess("{\"code\":200,\"data\":{\"uid\":\"remote\",\"status\":1,\"published_commit\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}}",MediaType.APPLICATION_JSON));
        }
        for(String kind:new String[]{"model","dataset"}) {
            try(HubPublicationArtifacts.Prepared p=prepared()) {
                client.publish(kind,p,true,key,new JSONObject().set("name",name).set("description",description).set("category",category),"real-user","tenant");
            }
        }
        server.verify();
    }
    @Test void overlongChineseFieldsAndInvalidKeyAreRejectedBeforeHttp() throws Exception {
        try(HubPublicationArtifacts.Prepared p=prepared()) {
            assertThrows(ServiceException.class,()->client.publish("model",p,true,"fixed",new JSONObject().set("description",repeat("𠀀",501)),"real-user",null));
            assertThrows(ServiceException.class,()->client.publish("model",p,true,"fixed",new JSONObject().set("name",repeat("模",201)),"real-user",null));
            assertThrows(ServiceException.class,()->client.publish("dataset",p,true,"fixed",new JSONObject().set("category",repeat("类",101)),"real-user",null));
            assertThrows(ServiceException.class,()->client.publish("model",p,true,repeat("a",129),null,"real-user",null));
        }
        server.verify();
    }
    @Test void runeLengthAcceptsSupplementaryChineseExactlyAtLimit() {
        assertEquals("𠀀𠀀",HubPublicationClient.value(new JSONObject().set("name","𠀀𠀀"),"name","",2));
        assertThrows(ServiceException.class,()->HubPublicationClient.value(new JSONObject().set("name","𠀀𠀀𠀀"),"name","",2));
    }
    @Test void authenticationBusinessCodesForBothKindsAreSafe4401() throws Exception {
        for(String kind:new String[]{"model","dataset"}) for(int code:new int[]{4004,4444,401}) {
            server.reset();
            server.expect(requestTo(config.getBaseUrl()+(kind.equals("model")?"/aiModel/v1/":"/aiDataset/v1/")+"uploadFilesDirect"))
                .andRespond(withSuccess("{\"code\":"+code+",\"msg\":\"secret-token-debug\"}",MediaType.APPLICATION_JSON));
            try(HubPublicationArtifacts.Prepared p=prepared()) {
                ServiceException ex=assertThrows(ServiceException.class,()->client.publish(kind,p,false,"fixed",null,"real-user",null));
                assertEquals(Integer.valueOf(4401),ex.getCode());assertFalse(ex.getMessage().contains("secret-token-debug"));assertNull(ex.getCause());
            }
            server.verify();
        }
    }
    @Test void http401IsAuthenticationButServerFailureAndTimeoutRemainUnknown() throws Exception {
        for(String kind:new String[]{"model","dataset"}) for(int mode=0;mode<3;mode++) {
            server.reset();final int current=mode;
            server.expect(requestTo(config.getBaseUrl()+(kind.equals("model")?"/aiModel/v1/":"/aiDataset/v1/")+"uploadFilesDirect"))
                .andRespond(request->{
                    if(current==2) throw new java.net.SocketTimeoutException("secret-token-debug");
                    return withStatus(current==0?org.springframework.http.HttpStatus.UNAUTHORIZED:org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("secret-token-debug").createResponse(request);
                });
            try(HubPublicationArtifacts.Prepared p=prepared()) {
                ServiceException ex=assertThrows(ServiceException.class,()->client.publish(kind,p,false,"fixed",null,"real-user",null));
                assertEquals(current==0,HubPublicationClient.isAuthenticationRejected(ex));assertFalse(ex.getMessage().contains("secret-token-debug"));assertNull(ex.getCause());
            }
            server.verify();
        }
    }
    @Test void receiptLookupForBothKindsIsSmallJsonAndNeverMultipart() {
        for(String kind:new String[]{"model","dataset"}) {
            server.reset();
            server.expect(requestTo(config.getBaseUrl()+(kind.equals("model")?"/aiModel/v1/":"/aiDataset/v1/")+"publicationReceipt"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header("accesstoken","real-user")).andExpect(header("tenantid","tenant"))
                .andExpect(content().json("{\"accessToken\":\"real-user\",\"client_publish_key\":\"original-key\"}",true))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"state\":\"PUBLISHED\",\"principal\":{\"user_id\":\"platform-user\",\"tenant_id\":\"platform-tenant\"},\"resource\":{\"uid\":\"remote\",\"status\":1,\"published_commit\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}}}",MediaType.APPLICATION_JSON));
            assertEquals("PUBLISHED",client.receipt(kind,"original-key","real-user","tenant").getStr("state"));server.verify();
        }
    }
    @Test void invalidOrUnauthorizedReceiptDoesNotMasqueradeAsSuccess() {
        for(String response:new String[]{"{\"code\":4004,\"msg\":\"secret-token-debug\"}","{\"code\":200,\"data\":{\"state\":\"PUBLISHED\",\"principal\":{\"user_id\":\"platform-user\",\"tenant_id\":\"platform-tenant\"},\"resource\":{\"uid\":\"remote\",\"status\":1}}}"}) {
            server.reset();server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/publicationReceipt")).andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
            ServiceException ex=assertThrows(ServiceException.class,()->client.receipt("model","key","real-user",null));
            assertFalse(ex.getMessage().contains("secret-token-debug"));server.verify();
        }
    }
    @Test void receiptWithoutTrustedPrincipalCannotAuthorizeRecovery() {
        server.expect(requestTo(config.getBaseUrl()+"/aiModel/v1/publicationReceipt"))
            .andRespond(withSuccess("{\"code\":200,\"data\":{\"state\":\"NOT_FOUND\"}}",MediaType.APPLICATION_JSON));
        assertThrows(ServiceException.class,()->client.receipt("model","key","real-user",null));server.verify();
    }
    private static String repeat(String text,int count) { StringBuilder s=new StringBuilder();for(int i=0;i<count;i++)s.append(text);return s.toString(); }
    private static String part(String multipart,String name) {
        int marker=multipart.indexOf("name=\""+name+"\"");
        assertTrue(marker>=0,"Missing multipart field "+name);
        int start=multipart.indexOf("\r\n\r\n",marker)+4;
        int end=multipart.indexOf("\r\n--",start);
        assertTrue(start>=4 && end>=start,"Invalid multipart field "+name);
        return multipart.substring(start,end);
    }

}
