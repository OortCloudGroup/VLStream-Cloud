package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import java.util.List;
import java.math.BigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publication is attached to a real company-managed node; it never grants execution eligibility. */
@Service
@RequiredArgsConstructor
public class CompanyComputeOfferService {
    private final JdbcTemplate jdbc;
    private final CompanyComputeOfferAuthority authority;
    private static final String LINKED = " FROM vls_company_compute_offer c JOIN vls_compute_node n ON n.id=c.node_id"
        + " AND n.provider=c.provider AND n.provider_instance_id=c.provider_instance_id";
    private static final String PUBLISHED = LINKED + " WHERE c.published=1 AND n.resource_scope='PLATFORM'";
    public static final String PENDING = "待配置SSH身份；收费待开通。";

    @Data
    public static class Record {
        private String offerCode;
        private String displayName;
        private String gpuModel;
        private String provider;
        private String providerProduct;
        private String providerInstanceId;
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
        private Long nodeId;
        private boolean published;
    }

    public List<Record> managementList() {
        authority.requirePublisherForTenant(authority.tenant());
        return jdbc.query("SELECT c.offer_code,n.name AS display_name,n.gpu_model,n.provider,n.provider_product,n.provider_instance_id,c.node_id,c.published"
            + LINKED + " WHERE n.tenant_id=? ORDER BY c.offer_code", new BeanPropertyRowMapper<>(Record.class), authority.tenant());
    }

    @Transactional(rollbackFor=Exception.class)
    public void publish(String code, boolean published) {
        List<ComputeNode> nodes = jdbc.query("SELECT n.*" + LINKED + " WHERE c.offer_code=? FOR UPDATE", new BeanPropertyRowMapper<>(ComputeNode.class), code);
        if (nodes.isEmpty()) throw new ServiceException("公司供给不存在或尚未关联管理记录", 404);
        ComputeNode node=nodes.get(0);
        String actor=authority.requirePublisherForTenant(node.getTenantId());
        // This operation changes visibility only, never credentials, enabled, readiness, price or billing policy.
        jdbc.update("UPDATE vls_company_compute_offer SET published=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE offer_code=?",
            published?1:0,actor,code);
        jdbc.update("UPDATE vls_compute_node SET resource_scope=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",
            published?"PLATFORM":"COMPANY",node.getId(),node.getTenantId());
    }

    public boolean linked(Long id) {
        if(id==null)return false;
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM vls_company_compute_offer WHERE node_id=?",Integer.class,id);
        return count!=null && count>0;
    }

    public String code(Long id) {
        List<String> codes=jdbc.queryForList("SELECT offer_code FROM vls_company_compute_offer WHERE node_id=?",String.class,id);
        return codes.isEmpty()?null:codes.get(0);
    }

    public boolean canPublish(ComputeNode node) {
        try { authority.requirePublisherForTenant(node.getTenantId());return true; }
        catch(ServiceException denied) { return false; }
    }

    public List<ComputeNode> publisherManagementNodes() {
        try { authority.requirePublisherForTenant(authority.tenant()); }
        catch(ServiceException denied) { return java.util.Collections.emptyList(); }
        List<ComputeNode> nodes=jdbc.query("SELECT n.*"+LINKED+" WHERE n.tenant_id=?",new BeanPropertyRowMapper<>(ComputeNode.class),authority.tenant());
        // Preserve exact persisted tenant equality even on a case-insensitive database collation.
        nodes.removeIf(node -> !canPublish(node));
        return nodes;
    }

    public void requireNodePublisher(Long id) {
        List<String> tenants=jdbc.queryForList("SELECT n.tenant_id"+LINKED+" WHERE n.id=?",String.class,id);
        if(tenants.isEmpty())throw new ServiceException("公司供给不存在",404);
        authority.requirePublisherForTenant(tenants.get(0));
    }

    @Transactional(rollbackFor=Exception.class)
    public void publishNode(Long id,ComputeRequests.Offer request) {
        if(!"POINTS".equals(request.getBillingType()) || request.getPointsPerHour()!=null
            || !"INSTANCE_EXCLUSIVE".equals(request.getSchedulingMode()))
            throw new ServiceException("此公司实例仅管理收费待开通展示，不能修改计费或执行资格",403);
        publish(code(id),request.isPublished());
    }

    public long publishedCount() {
        Long count=jdbc.queryForObject("SELECT COUNT(*)"+PUBLISHED,Long.class);
        return count==null?0:count;
    }

    public List<ComputeMarketService.Resource> marketResources(int limit,int offset) {
        if(limit<1 || limit>200 || offset<0)throw new IllegalArgumentException("Invalid company pagination");
        return jdbc.query("SELECT n.*"+PUBLISHED+" ORDER BY c.offer_code LIMIT ? OFFSET ?",(rs,index)->{
            ComputeNode node=new BeanPropertyRowMapper<>(ComputeNode.class).mapRow(rs,index);
            ComputeMarketService.Resource resource=new ComputeMarketService.Resource();
            resource.setId(String.valueOf(node.getId()));
            resource.setName(displayText(node.getName(),"OortCloud GPU"));
            resource.setGpuModel(displayText(node.getGpuModel(),"GPU"));
            resource.setProvider(node.getProvider());resource.setScope("PLATFORM");
            resource.setBillingType("POINTS");resource.setSchedulingMode("INSTANCE_EXCLUSIVE");
            resource.setClaimState("PENDING_CONFIGURATION");
            resource.setPowerState("UNKNOWN");resource.setCapacityState("UNKNOWN");resource.setHealthState("UNTESTED");
            resource.setCanManage(canPublish(node));
            resource.setMessage(PENDING);
            return resource;
        },limit,offset);
    }

    /** Anonymous display contract; internal instance identity never leaves this mapper. */
    public List<ComputePublicMarketService.Offer> publicMarketOffers(int limit, int offset) {
        if (limit < 1 || limit > 20 || offset < 0) throw new IllegalArgumentException("Invalid public pagination");
        return jdbc.query("SELECT n.id,n.name,n.gpu_model,n.billing_type,n.points_per_hour,"
            + "n.provider,n.server_group,n.provider_gpu_amount" + PUBLISHED + " ORDER BY c.offer_code LIMIT ? OFFSET ?", (rs, index) -> {
                ComputeNode node = new BeanPropertyRowMapper<>(ComputeNode.class).mapRow(rs, index);
                ComputePublicMarketService.Offer offer = new ComputePublicMarketService.Offer();
                offer.setName(displayText(node.getName(), "GPU resource"));
                offer.setGpuModel(displayText(node.getGpuModel(), "GPU"));
                offer.setBillingType("POINTS");
                BigDecimal price = node.getPointsPerHour();
                if ("POINTS".equals(node.getBillingType()) && price != null && price.signum() >= 0) {
                    offer.setPrice(price);
                    offer.setPriceUnit("POINTS_PER_HOUR");
                }
                offer.setSchedulingMode("INSTANCE_EXCLUSIVE");
                offer.setPowerState("UNKNOWN");
                offer.setHealthState("UNKNOWN");
                offer.setAvailability("PAID_DISABLED");
                ComputePublicMarketService.referencePrice(offer, node);
                return offer;
            }, limit, offset);
    }

    static String displayText(String value,String fallback) {
        if(value==null || value.trim().isEmpty() || value.length()>100
            || value.matches("(?is).*(?:[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}|[:/@=\\\\]|password|token|secret|ssh|credential|[a-z0-9-]+\\.(?:com|net|org|cn|local|internal)\\b).*"))return fallback;
        return value.replaceAll("[\\p{Cntrl}]","").trim();
    }
}
