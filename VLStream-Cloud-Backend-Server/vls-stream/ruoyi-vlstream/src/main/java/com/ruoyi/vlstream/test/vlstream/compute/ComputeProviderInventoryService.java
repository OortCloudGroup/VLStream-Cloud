package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Admin discovery only. Never writes compute nodes, public claims, credentials or publication. */
@Service
@RequiredArgsConstructor
public class ComputeProviderInventoryService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final AutoDlProPowerProvider provider;
    private final PlatformTransactionManager transactions;
    @Data public static class Draft {
        private String instanceKey;
        @JsonIgnore private String tenantId;
        private String provider;
        private String accountScope;
        private String providerInstanceId;
        private String name;
        private String gpuSpec;
        private Integer gpuAmount;
        private String gpuModel;
        private String powerState;
        private BigDecimal paygPrice;
        private BigDecimal originPrice;
        private Date syncedAt;
        private Date stateCheckedAt;
        @JsonSerialize(using=ToStringSerializer.class) private Long nodeId;
        public String getStage(){return nodeId==null?"PENDING_ACCESS":"LINKED_PENDING_REVIEW";}
        public String getPriceUnit(){return "UNKNOWN";}
        public String getCapacityState(){return "UNKNOWN";}
        public boolean isObservationStale(){Date time=stateCheckedAt==null?syncedAt:stateCheckedAt;return time==null || System.currentTimeMillis()-time.getTime()>5*60*1000L;}
        public String getPowerState(){return isObservationStale()?"UNKNOWN":powerState;}
        public boolean isPublishable(){return false;}
    }
    private static class Source { ComputeNode node; String scope; }
    private Source source(Long id,boolean requireApi) {
        ComputeNode node=nodes.get(id);
        nodes.requireManagement(node);
        if(!ComputeNodeService.tenant().equals(node.getTenantId()))throw new ServiceException("无权同步其他资源管理方库存");
        if(!"autodl".equals(node.getProvider()) || !"AUTODL_PRO".equals(node.getProviderProduct()))throw new ServiceException("仅支持AutoDL容器Pro库存，普通实例和企业弹性接口未接入");
        ComputePublicInventoryService.Instance identity=nodes.publicInventory().read(node,false);
        if(identity==null || !"autodl".equals(identity.getProvider()) || !Objects.equals(identity.getProviderInstanceId(),node.getProviderInstanceId()))
            throw new ServiceException("请先核对厂商配置并登记稳定的非秘密公共账号范围");
        if(requireApi && !provider.supports(node))throw new ServiceException("Pro同步未启用：需已有后端加密凭据、API控制模式及服务端控制开关；未调用厂商");
        Source result=new Source();result.node=node;result.scope=identity.getAccountScope();return result;
    }
    public List<Draft> list(Long id) {
        Source source=source(id,false);
        return jdbc.query("SELECT i.*,(SELECT MIN(n.id) FROM vls_compute_node n WHERE BINARY n.tenant_id=BINARY i.tenant_id AND n.public_instance_key=i.instance_key) AS node_id FROM vls_compute_provider_inventory i WHERE BINARY i.tenant_id=BINARY ? AND i.provider='autodl' AND i.account_scope=? ORDER BY i.provider_instance_id",
            new BeanPropertyRowMapper<>(Draft.class),ComputeNodeService.tenant(),source.scope);
    }
    public String sync(Long id) {
        Source source=source(id,true);
        // One account sync across processes. Do not hold node/claim row locks during HTTP pagination.
        String lock="vls-inventory:"+ModelArtifactObjectStore.hash("autodl\n"+source.scope).substring(0,40);
        try(Connection connection=Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            if(!advisory(connection,"SELECT GET_LOCK(?,0)",lock))throw new ServiceException("该Pro账号正在同步，请稍后刷新草稿");
            try {
                Date observedAt=new Date();
                Map<String,ComputeInventoryProvider.Item> items=new LinkedHashMap<>();int max=-1,total=-1;
                for(int page=1;page<=50;page++) {
                    ComputeInventoryProvider.Page response=provider.inventoryPage(source.node,page,20);
                    if(max<0){max=response.getMaxPage();total=response.getTotal();}
                    if(response.getIndex()!=page || response.getMaxPage()!=max || response.getTotal()!=total
                        || response.getItems()==null || max>50 || total>1000 || max<0 || total<0
                        || response.getItems().isEmpty() && total>0 || total==0 && (!response.getItems().isEmpty() || max>1)
                        || total>0 && max<page)throw new ServiceException("Pro分页不完整或期间发生变化，原库存保留，请重新同步");
                    for(ComputeInventoryProvider.Item item:response.getItems()) {
                        ComputeInventoryProvider.Item previous=items.put(item.getInstanceId(),item);
                        if(previous!=null && !previous.equals(item))throw new ServiceException("重复实例的分页字段矛盾，原库存保留，请重新同步");
                    }
                    if(page>=max)break;
                }
                if(items.size()!=total)throw new ServiceException("Pro分页去重后的数量不完整，原库存保留，请重新同步");
                new TransactionTemplate(transactions).execute(tx->{
                    validateCurrent(source);
                    for(ComputeInventoryProvider.Item item:items.values()) {
                        String key=key(source.scope,item.getInstanceId());
                        List<String> owners=jdbc.queryForList("SELECT tenant_id FROM vls_compute_provider_inventory WHERE instance_key=? FOR UPDATE",String.class,key);
                        if(owners.stream().anyMatch(owner->!ComputeNodeService.tenant().equals(owner)))throw new ServiceException("账号范围库存已由另一管理方登记，请核对稳定账号引用");
                        jdbc.update("INSERT INTO vls_compute_provider_inventory(instance_key,tenant_id,provider,account_scope,provider_instance_id,name,gpu_spec,gpu_amount,power_state,synced_at) VALUES(?,?,'autodl',?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),gpu_spec=VALUES(gpu_spec),gpu_amount=VALUES(gpu_amount),power_state=IF(state_checked_at IS NULL OR state_checked_at<VALUES(synced_at),VALUES(power_state),power_state),state_checked_at=IF(state_checked_at IS NULL OR state_checked_at<VALUES(synced_at),NULL,state_checked_at),synced_at=VALUES(synced_at)",
                            key,ComputeNodeService.tenant(),source.scope,item.getInstanceId(),item.getName(),item.getGpuSpec(),item.getGpuAmount(),item.getPowerState(),observedAt);
                    }
                    return null;
                });
                return "已同步"+items.size()+"条Pro库存草稿；未删除缺失实例、修改接入配置或开放收费，请核对状态与手工接入";
            } finally { advisory(connection,"SELECT RELEASE_LOCK(?)",lock); }
        } catch(ServiceException e){throw e;}
        catch(Exception e){throw new ServiceException("Pro库存同步失败，原库存保留；请检查授权、配置或分页响应后重试");}
    }
    public String refresh(Long id,String instanceKey) {
        Source source=source(id,true);
        if(instanceKey==null || !instanceKey.matches("[a-f0-9]{64}"))throw new ServiceException("库存标识无效");
        List<Draft> drafts=jdbc.query("SELECT * FROM vls_compute_provider_inventory WHERE instance_key=? AND BINARY tenant_id=BINARY ? AND account_scope=?",new BeanPropertyRowMapper<>(Draft.class),instanceKey,ComputeNodeService.tenant(),source.scope);
        if(drafts.isEmpty())throw new ServiceException("库存草稿不存在或无权查询");
        // Read-only API request identity, never copy a different node's public key, SSH or workspace.
        ComputeNode target=new ComputeNode();target.setProvider("autodl");target.setProviderProduct("AUTODL_PRO");target.setControlMode("API");
        target.setProviderCredentialCipher(source.node.getProviderCredentialCipher());target.setProviderInstanceId(drafts.get(0).getProviderInstanceId());
        Date observedAt=new Date();
        ComputePowerProvider.Snapshot state;ComputeCostProvider.Observation cost;
        try{state=provider.status(target);cost=provider.cost(target);}catch(Exception e){throw new ServiceException("Pro状态查询失败，旧观察保留，请核对配置或稍后重试");}
        new TransactionTemplate(transactions).execute(tx->{validateCurrent(source);
            int updated=jdbc.update("UPDATE vls_compute_provider_inventory SET power_state=?,payg_price=?,origin_price=?,state_checked_at=? WHERE instance_key=? AND BINARY tenant_id=BINARY ? AND account_scope=? AND COALESCE(state_checked_at,synced_at)<=?",
                "RUNNING".equals(state.power)?"RUNNING":"UNKNOWN",safePrice(cost.getAmount()),safePrice(cost.getOriginalAmount()),observedAt,instanceKey,ComputeNodeService.tenant(),source.scope,observedAt);
            if(updated==0)throw new ServiceException("库存已有更新的观察，旧查询结果未写入，请刷新草稿");return null;});
        return "已更新白名单观察；GPU配额不等于库存，价格单位与完整停机终态待核实，未上架或启停";
    }
    private void validateCurrent(Source source) {
        ComputeNode current=nodes.lockServer(source.node.getId());
        nodes.requireManagement(current);
        if(!ComputeNodeService.tenant().equals(current.getTenantId()) || !Objects.equals(current.getManagementOwnerSubject(),source.node.getManagementOwnerSubject())
            || !Objects.equals(current.getPublicInstanceKey(),source.node.getPublicInstanceKey())
            || !Objects.equals(current.getProviderCredentialCipher(),source.node.getProviderCredentialCipher()) || !provider.supports(current))
            throw new ServiceException("同步期间账号配置或权限已变化，结果未写入，请刷新后重试");
    }
    static String key(String scope,String instance){return ModelArtifactObjectStore.hash("autodl\n"+scope+"\n"+instance);}
    private static BigDecimal safePrice(BigDecimal value){return value!=null && value.signum()>=0 && value.precision()<=24 && value.scale()<=8 && value.precision()-value.scale()<=16?value:null;}
    private static boolean advisory(Connection c,String sql,String name)throws Exception {
        try(PreparedStatement statement=c.prepareStatement(sql)){statement.setString(1,name);try(ResultSet result=statement.executeQuery()){return result.next() && result.getInt(1)==1;}}
    }
}
