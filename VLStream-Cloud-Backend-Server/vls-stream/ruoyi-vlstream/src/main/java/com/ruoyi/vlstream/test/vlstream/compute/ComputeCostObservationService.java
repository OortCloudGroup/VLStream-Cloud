package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ComputeCostObservationService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final ObjectMapper json;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private List<ComputeCostProvider> providers=new ArrayList<>();
    @Transactional(rollbackFor=Exception.class)
    public ComputeCostProvider.Observation query(Long id) {
        ComputeNode node=nodes.lockServer(id);
        if (!ComputeNodeService.tenant().equals(node.getTenantId())) throw new ServiceException("无权查询资源成本");
        ComputeCostProvider provider=providers.stream().filter(p->p.supports(node)).findFirst().orElseThrow(()->new ServiceException("普通实例或厂商接口未启用，需要人工核对实际算力成本"));
        try {
            ComputeCostProvider.Observation observation=provider.cost(node);
            jdbc.update("UPDATE vls_compute_node SET provider_cost_json=? WHERE id=?",json.writeValueAsString(observation),id);
            return observation;
        } catch(ComputeProviderException e) { throw new ServiceException(e.getMessage()); }
        catch(java.io.IOException e) { throw new ServiceException("成本观察保存失败"); }
    }
}
