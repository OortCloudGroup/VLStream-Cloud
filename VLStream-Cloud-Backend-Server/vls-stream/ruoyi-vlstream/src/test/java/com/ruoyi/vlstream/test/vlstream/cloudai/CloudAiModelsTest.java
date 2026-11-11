package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeNode;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class CloudAiModelsTest {
    @Test void catalogueOwnershipIsCorrelatedToOuterModel() {
        IVlsAlgorithmModelService repository=mock(IVlsAlgorithmModelService.class);
        ComputePublicTaskAccess access=mock(ComputePublicTaskAccess.class);
        CloudAiModels models=new CloudAiModels(repository,access,null,null,new ObjectMapper(),null,null,null);
        com.ruoyi.common.helper.TenantContextHolder.setTenantId("000000");
        try { models.choices(new ComputeNode()); }
        finally { com.ruoyi.common.helper.TenantContextHolder.clear(); }
        verify(access).filter(any(),eq("vls_algorithm_model.training_id"));
    }
}
