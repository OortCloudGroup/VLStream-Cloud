package com.ruoyi.vlstream.test.vlstream.service;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class ModelHubDeploymentServiceTest {
    JSONObject manifest() {
        JSONObject file=new JSONObject().set("path","model.om").set("size",1).set("sha256",String.join("",java.util.Collections.nCopies(64,"a")));
        return new JSONObject().set("schema_version",1).set("model_format","om").set("target_chip","Hi3519DV500")
            .set("runtime","svp-acl").set("algorithm_type","detection").set("model",file)
            .set("classes",new JSONObject(file.toString()).set("path","classes.yaml"));
    }
    @Test void acceptsOnlyVerifiedCompatibilityContract() {
        ModelHubDeploymentService.validateManifest(manifest());
        for(String key:new String[]{"schema_version","model_format","target_chip","runtime","algorithm_type"}) {
            JSONObject m=manifest();m.remove(key);
            assertThrows(Exception.class,()->ModelHubDeploymentService.validateManifest(m));
        }
    }
    @Test void rejectsTraversalMissingHashesAndOversize() {
        JSONObject m=manifest();m.getJSONObject("model").set("path","../model.om");
        final JSONObject traversal=m;assertThrows(ServiceException.class,()->ModelHubDeploymentService.validateManifest(traversal));
        m=manifest();m.getJSONObject("classes").set("size",1048577);
        final JSONObject large=m;assertThrows(ServiceException.class,()->ModelHubDeploymentService.validateManifest(large));
        m=manifest();m.getJSONObject("model").remove("sha256");
        final JSONObject missing=m;assertThrows(ServiceException.class,()->ModelHubDeploymentService.validateManifest(missing));
    }
    @Test void stableIdsAreBoundToActorTenantVersionAndDevice() {
        String a=ModelHubDeploymentService.requestId("t","u","op","hub","commit",1L,"d");
        assertEquals(a,ModelHubDeploymentService.requestId("t","u","op","hub","commit",1L,"d"));
        assertNotEquals(a,ModelHubDeploymentService.requestId("other","u","op","hub","commit",1L,"d"));
        assertNotEquals(a,ModelHubDeploymentService.requestId("t","other","op","hub","commit",1L,"d"));
        assertNotEquals(a,ModelHubDeploymentService.requestId("t","u","op","hub","new",1L,"d"));
        assertNotEquals(a,ModelHubDeploymentService.requestId("t","u","op","hub","commit",1L,"other"));
    }
    @Test void verifiesRealBytesNotJustMetadata() throws Exception {
        Path file=Files.createTempFile("hub-test-",".bin");
        try {
            Files.write(file,new byte[]{97});
            JSONObject expected=new JSONObject().set("size",1).set("sha256","ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb");
            ModelHubDeploymentService.verify(file,expected);
            Files.write(file,new byte[]{98});
            assertThrows(ServiceException.class,()->ModelHubDeploymentService.verify(file,expected));
        } finally {Files.deleteIfExists(file);}
    }
}
