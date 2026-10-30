package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Per-original-run, memory-only password context. No endpoint/password enters node rows or job snapshots. */
@Component
@RequiredArgsConstructor
public class AutoDlRuntimeSsh {
    private final ConfiguredComputeRegistry configured;
    private final AutoDlProPowerProvider provider;
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private AutoDlJupyterHostProof hostProof;
    private final Map<String,Binding> bindings=new ConcurrentHashMap<>();
    private final Set<String> conflicts=new HashSet<>();

    boolean dynamic(ComputeNode node) {return configured.official(node)&&configured.definition(node).getPrivateKeyFile()==null;}
    void prepare(String runId,ComputeNode node) throws IOException {
        configured.verifyOfficial(node);
        if(!dynamic(node))return;
        List<Map<String,Object>> rows=active(node);
        if(rows.size()!=1||!runId.equals(String.valueOf(rows.get(0).get("run_id"))))throw denied();
        JsonNode snapshot=provider.connectionSnapshot(node);
        ConfiguredComputeProperties.Resource resource=configured.definition(node);
        if(resource.getHostKey()==null&&resource.getKnownHostsFile()==null) {
            ConfiguredComputeProperties.Resource temporary=new ConfiguredComputeProperties.Resource();org.springframework.beans.BeanUtils.copyProperties(resource,temporary);
            synchronized(bindings){Binding old=bindings.get(node.getPublicInstanceKey());if(old!=null&&old.run.equals(runId))temporary.setHostKey(old.key);}
            if(temporary.getHostKey()==null){if(hostProof==null)throw denied();temporary.setHostKey(hostProof.read(snapshot));}
            resource=temporary;
        }
        Binding next=parse(runId,node,snapshot,resource);
        snapshot=null;
        synchronized(bindings) {
            if(conflicts.contains(runId)){next.clear();throw denied();}
            Binding old=bindings.get(node.getPublicInstanceKey());
            if(old!=null&&old.run.equals(runId)&&!old.sameEndpoint(next)){conflicts.add(runId);bindings.remove(node.getPublicInstanceKey());old.clear();next.clear();throw denied();}
            if(old!=null)old.clear();bindings.put(node.getPublicInstanceKey(),next);
        }
    }
    Binding require(ComputeNode node) throws IOException {
        configured.verifyOfficial(node);
        synchronized(bindings) {
            Binding value=bindings.get(node.getPublicInstanceKey());
            List<Map<String,Object>> rows=active(node);
            if(value==null||rows.size()!=1||!value.run.equals(String.valueOf(rows.get(0).get("run_id")))
                ||!Objects.equals(value.tenant,node.getTenantId())||!Objects.equals(value.instance,node.getProviderInstanceId()))throw denied();
            return value.copy();
        }
    }
    private List<Map<String,Object>> active(ComputeNode node) {
        return jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key=? AND node_id=? AND state IN ('STARTING','RUNNING','STOPPING')",node.getPublicInstanceKey(),node.getId());
    }
    void clear(ComputeNode node) {synchronized(bindings){Binding old=bindings.remove(node.getPublicInstanceKey());if(old!=null)old.clear();}}

    static Binding parse(String run,ComputeNode node,JsonNode snapshot,ConfiguredComputeProperties.Resource resource) throws IOException {
        try {
            if(!snapshot.isObject())throw denied();
            String host=snapshot.path("proxy_host").asText();JsonNode port=snapshot.path("ssh_port");
            if(!host.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]{0,250})\\.(?:autodl|seetacloud)\\.com")||host.contains("..")
                ||!port.isIntegralNumber()||!port.canConvertToInt()||port.asInt()<1||port.asInt()>65535)throw denied();
            String password=snapshot.path("root_password").asText();
            if(password.isEmpty()||password.length()>4096||password.chars().anyMatch(c->c<32||c==127))throw denied();
            String key=resource.getHostKey();
            if(key==null||key.isEmpty())key=knownHost(resource.getKnownHostsFile(),host,port.asInt());
            if(!"ssh-ed25519".equals(new HostKey(host,Base64.getDecoder().decode(key)).getType()))throw denied();
            return new Binding(run,node.getTenantId(),node.getProviderInstanceId(),host,port.asInt(),key,password.toCharArray());
        }catch(Exception invalid){throw denied();}
    }
    static String knownHost(String file,String host,int port) throws Exception {
        if(file==null)throw denied();Path path=Paths.get(file);
        if(!path.isAbsolute()||!path.normalize().equals(path)||Files.isSymbolicLink(path)||!Files.isRegularFile(path)||Files.size(path)>262144)throw denied();
        JSch jsch=new JSch();jsch.setKnownHosts(path.toString());
        HostKey[] keys=jsch.getHostKeyRepository().getHostKey(port==22?host:"["+host+"]:"+port,"ssh-ed25519");
        if(keys==null||keys.length!=1)throw denied();return keys[0].getKey();
    }
    private static IOException denied(){return new IOException("官方动态SSH连接信息或可信主机绑定尚未确认");}
    @javax.annotation.PreDestroy public void destroy(){synchronized(bindings){bindings.values().forEach(Binding::clear);bindings.clear();conflicts.clear();}}
    static final class Binding implements AutoCloseable {
        final String run,tenant,instance,host,key;final int port;private final char[] password;
        Binding(String run,String tenant,String instance,String host,int port,String key,char[] password){this.run=run;this.tenant=tenant;this.instance=instance;this.host=host;this.port=port;this.key=key;this.password=password;}
        boolean sameEndpoint(Binding b){return host.equals(b.host)&&port==b.port&&key.equals(b.key)&&tenant.equals(b.tenant)&&instance.equals(b.instance);}
        Binding copy(){return new Binding(run,tenant,instance,host,port,key,password.clone());}
        ComputeNode endpoint(ComputeNode original){ComputeNode copy=new ComputeNode();org.springframework.beans.BeanUtils.copyProperties(original,copy);copy.setHost(host);copy.setPort(port);copy.setUsername("root");copy.setHostKey(key);return copy;}
        void authenticate(Session session){session.setPassword(new String(password));}
        void clear(){Arrays.fill(password,'\0');}
        public void close(){clear();}
    }
}
