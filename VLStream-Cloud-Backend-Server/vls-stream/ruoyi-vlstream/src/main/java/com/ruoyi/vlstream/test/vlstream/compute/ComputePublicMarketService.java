package com.ruoyi.vlstream.test.vlstream.compute;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Anonymous, read-only projection of published supply, never of user tasks or credentials. */
@Service
@RequiredArgsConstructor
public class ComputePublicMarketService {
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ConfiguredComputeRegistry configured;
    static final String FILTER = " FROM vls_compute_node n WHERE n.resource_scope='PLATFORM' AND n.enabled=1 AND (n.provider IS NULL OR n.provider<>'self')";
    @Data
    public static class Offer {
        private String name;
        private String gpuModel;
        private String billingType;
        private BigDecimal price;
        private String priceUnit;
        private String schedulingMode;
        private String powerState;
        private String healthState;
        private String availability;
        private boolean paidUseEnabled = false;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private BigDecimal referencePrice;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private String referenceCurrency;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private String referenceUnit;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private String referenceDate;
    }
    @Data
    public static class Page {
        private final List<Offer> items;
        private final long total;
        private final int page;
        private final int pageSize;
    }
    public Page list(int page, int size) {
        if (page < 1 || page > 100 || size < 1 || size > 20) throw new IllegalArgumentException("Invalid pagination");
        String filter=FILTER;
        if(configured!=null && !configured.allIds().isEmpty())filter+=" AND n.id NOT IN ("+configured.allIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))+")";
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + filter, Long.class);
        long now = System.currentTimeMillis();
        List<Offer> items = jdbc.query("SELECT n.name,n.gpu_model,n.billing_type,n.points_per_hour,n.scheduling_mode,"
            + "n.power_state,n.power_checked_at,n.probe_state,n.probe_time,n.capacity_state,n.capacity_checked_at,"
            + "n.provider_locked,n.power_operation_id,n.gpu_index,n.provider_gpu_amount,n.probe_json,"
            + "(SELECT p.public_state FROM vls_compute_public_instance p WHERE p.instance_key=n.public_instance_key) AS instance_state,"
            + "(SELECT p.workspace_dirty FROM vls_compute_public_instance p WHERE p.instance_key=n.public_instance_key) AS workspace_dirty"
            + filter + " ORDER BY n.id LIMIT ? OFFSET ?", (rs, index) -> {
                Offer offer = new Offer();
                offer.setName(publicText(rs.getString("name"), "GPU resource"));
                offer.setGpuModel(publicText(rs.getString("gpu_model"), "GPU"));
                offer.setBillingType(oneOf(rs.getString("billing_type"), "FREE", "POINTS"));
                BigDecimal price = rs.getBigDecimal("points_per_hour");
                if ("POINTS".equals(offer.getBillingType()) && price != null && price.signum() >= 0) {
                    offer.setPrice(price); offer.setPriceUnit("POINTS_PER_HOUR");
                }
                offer.setSchedulingMode(oneOf(rs.getString("scheduling_mode"), "SHARED_GPU_QUEUE", "INSTANCE_EXCLUSIVE"));
                offer.setPowerState(ComputeAvailability.fresh(rs.getTimestamp("power_checked_at"), now)
                    ? oneOf(rs.getString("power_state"), "RUNNING", "STOPPED", "STARTING", "STOPPING", "FAILED") : "UNKNOWN");
                offer.setHealthState(ComputeAvailability.fresh(rs.getTimestamp("probe_time"), now)
                    ? oneOf(rs.getString("probe_state"), "READY", "FAILED", "CHECKING") : "UNKNOWN");
                boolean capacityBlocked = ComputeAvailability.fresh(rs.getTimestamp("capacity_checked_at"), now)
                    && "UNAVAILABLE".equals(rs.getString("capacity_state"));
                ComputeNode evidence = new ComputeNode();
                evidence.setGpuIndex(rs.getInt("gpu_index"));
                evidence.setProviderGpuAmount((Integer) rs.getObject("provider_gpu_amount"));
                evidence.setPowerCheckedAt(rs.getTimestamp("power_checked_at"));
                evidence.setProbeTime(rs.getTimestamp("probe_time"));
                evidence.setProbeJson(rs.getString("probe_json"));
                capacityBlocked = capacityBlocked || rs.getBoolean("provider_locked")
                    || rs.getString("power_operation_id") != null || ComputeAvailability.noGpu(evidence, now);
                offer.setAvailability(availability(offer, capacityBlocked, rs.getString("instance_state"), rs.getBoolean("workspace_dirty")));
                return offer;
            }, size, (page - 1) * size);
        long databaseTotal=total==null?0:total;
        List<Offer> configuration=new java.util.ArrayList<>();
        if(configured!=null)for(ComputeNode node:configured.published()) {
            Offer offer=new Offer();offer.setName(publicText(node.getName(),"GPU resource"));offer.setGpuModel(publicText(node.getGpuModel(),"GPU"));
            offer.setBillingType("FREE");offer.setSchedulingMode(ComputeSharedQueueService.MODE);
            offer.setPowerState(ComputeAvailability.fresh(node.getPowerCheckedAt(),now)?oneOf(node.getPowerState(),"RUNNING"):"UNKNOWN");
            offer.setHealthState(ComputeAvailability.fresh(node.getProbeTime(),now)?oneOf(node.getProbeState(),"READY","ERROR"):"UNKNOWN");
            offer.setAvailability(availability(offer,false,null,false));configuration.add(offer);
        }
        int offset=(int)Math.max(0,(long)(page-1)*size-databaseTotal);
        if(items.size()<size && offset<configuration.size())items.addAll(configuration.subList(offset,Math.min(configuration.size(),offset+size-items.size())));
        return new Page(items, databaseTotal+configuration.size(), page, size);
    }
    // Dated presentation only, never billing or a live supplier quote. Match trusted
    // persisted identity, not public names/specifications; do not expose that identity.
    static void referencePrice(Offer offer, ComputeNode node) {
        if (!"POINTS".equals(offer.getBillingType()) || !"POINTS".equals(node.getBillingType()) || node.getPointsPerHour() != null
            || !Long.valueOf(792022144331380001L).equals(node.getId())
            || !"autodl".equals(node.getProvider())
            || !"autodl-pro-7920a22db144".equals(node.getServerGroup())
            || !Integer.valueOf(1).equals(node.getProviderGpuAmount())) return;
        offer.setReferencePrice(new BigDecimal("12.92"));
        offer.setReferenceCurrency("CNY");
        offer.setReferenceUnit("PER_INSTANCE_HOUR");
        offer.setReferenceDate("2026-10-19");
    }
    static String availability(Offer offer, boolean blocked, String instanceState, boolean dirty) {
        if ("POINTS".equals(offer.getBillingType())) return "PAID_DISABLED";
        if (!"FREE".equals(offer.getBillingType())) return "UNKNOWN";
        if (blocked || "STOPPED".equals(offer.getPowerState()) || "FAILED".equals(offer.getHealthState())) return "UNAVAILABLE";
        if (!"READY".equals(offer.getHealthState()) || !"RUNNING".equals(offer.getPowerState())) return "UNKNOWN";
        // Queue readiness still requires authenticated admission and a current environment check.
        if ("SHARED_GPU_QUEUE".equals(offer.getSchedulingMode())) return "QUEUE";
        if ("OCCUPIED".equals(instanceState)) return "BUSY";
        if ("FREE".equals(instanceState) && !dirty) return "AVAILABLE";
        return "UNAVAILABLE";
    }
    static String oneOf(String value, String... values) {
        return Arrays.asList(values).contains(value) ? value : "UNKNOWN";
    }
    static String publicText(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || value.length() > 100) return fallback;
        // Names/specifications are display fields, but must not smuggle addresses or credential-like text.
        if (value.matches("(?is).*(?:[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}|[:/@=\\\\]|password|token|secret|ssh|credential|[a-z0-9-]+\\.(?:com|net|org|cn|local|internal)\\b).*")) return fallback;
        return value.replaceAll("[\\p{Cntrl}]", "").trim();
    }
}
