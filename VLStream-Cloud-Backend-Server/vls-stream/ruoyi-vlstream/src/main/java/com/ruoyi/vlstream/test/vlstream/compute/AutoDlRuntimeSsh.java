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

    public enum Stage { TRUST_CONFIGURATION, ACTIVE_RUN, SNAPSHOT_REQUEST, SNAPSHOT_SHAPE, SSH_ENDPOINT,
        SSH_PASSWORD, HOST_TRUST, HOST_KEY, KNOWN_HOSTS, ENDPOINT_CONFLICT, BINDING_CONTEXT,
        JUPYTER_ORIGIN, JUPYTER_TOKEN, JUPYTER_TERMINAL_CREATE, JUPYTER_TERMINAL_RESPONSE,
        JUPYTER_WEBSOCKET, JUPYTER_STDOUT, JUPYTER_HOST_KEY, JUPYTER_PROOF_WAIT, UNKNOWN }
    public static final class PreparationException extends IOException {
        private final Stage stage;
        private final String failureType;
        private PreparationException(Stage stage,String failureType){super(stage.name());this.stage=stage;this.failureType=failureType;}
    }
    static PreparationException failure(Stage stage,Throwable cause) {
        Throwable current=cause;
        for(int i=0;current!=null&&i<16;i++,current=current.getCause())
            if(current instanceof PreparationException)return (PreparationException)current;
        return new PreparationException(stage,safeFailureType(cause));
    }
    /** Fixed labels only. Never render a transport exception, message, response, URL or credential. */
    public static String safeDiagnostic(Throwable failure) {
        for(int i=0;failure!=null&&i<16;i++,failure=failure.getCause())
            if(failure instanceof PreparationException)return ((PreparationException)failure).stage.name();
        return Stage.UNKNOWN.name();
    }
    public static String safeFailureType(Throwable failure) {
        String fallback="OTHER";
        for(int i=0;failure!=null&&i<16;i++,failure=failure.getCause()) {
            if(failure instanceof PreparationException)return ((PreparationException)failure).failureType;
            if(failure instanceof java.util.concurrent.TimeoutException||failure instanceof java.net.SocketTimeoutException
                ||failure instanceof org.apache.http.conn.ConnectTimeoutException)return "TIMEOUT";
            if(failure instanceof javax.net.ssl.SSLException)return "TLS";
            if(failure instanceof java.net.UnknownHostException)return "DNS";
            if(failure instanceof java.net.ConnectException)return "CONNECT";
            if(failure instanceof org.springframework.web.client.HttpStatusCodeException)return "HTTP";
            if(failure instanceof javax.websocket.DeploymentException)return "WEBSOCKET";
            if(failure instanceof com.fasterxml.jackson.core.JsonProcessingException)return "JSON";
            if(failure instanceof JSchException)return "SSH";
            if(failure instanceof ComputeProviderException)return "PROVIDER";
            if(failure instanceof IOException)fallback="IO";
            else if(failure instanceof IllegalArgumentException)fallback="INVALID_INPUT";
            else if(failure instanceof IllegalStateException)fallback="STATE";
        }
        return fallback;
    }

    boolean dynamic(ComputeNode node) {return configured.official(node)&&configured.definition(node).getPrivateKeyFile()==null;}
    void prepare(String runId,ComputeNode node) throws IOException {
        Stage stage=Stage.TRUST_CONFIGURATION;
        try {
        configured.verifyOfficial(node);
        if(!dynamic(node))return;
        stage=Stage.ACTIVE_RUN;
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw failure(stage,null);
        List<Map<String,Object>> rows=active(node);
        if(rows.size()!=1||!runId.equals(String.valueOf(rows.get(0).get("run_id"))))throw failure(stage,null);
        stage=Stage.SNAPSHOT_REQUEST;
        JsonNode snapshot=provider.connectionSnapshot(node);
        stage=Stage.HOST_TRUST;
        ConfiguredComputeProperties.Resource resource=configured.definition(node);
        if(resource.getHostKey()==null&&resource.getKnownHostsFile()==null) {
            ConfiguredComputeProperties.Resource temporary=new ConfiguredComputeProperties.Resource();org.springframework.beans.BeanUtils.copyProperties(resource,temporary);
            synchronized(bindings){Binding old=bindings.get(node.getPublicInstanceKey());if(old!=null&&old.run.equals(runId))temporary.setHostKey(old.key);}
            if(temporary.getHostKey()==null){if(hostProof==null)throw failure(stage,null);temporary.setHostKey(hostProof.read(snapshot));}
            resource=temporary;
        }
        Binding next=parse(runId,node,snapshot,resource);
        snapshot=null;
        synchronized(bindings) {
            if(conflicts.contains(runId)){next.clear();throw failure(Stage.ENDPOINT_CONFLICT,null);}
            Binding old=bindings.get(node.getPublicInstanceKey());
            if(old!=null&&old.run.equals(runId)&&!old.sameEndpoint(next)){conflicts.add(runId);bindings.remove(node.getPublicInstanceKey());old.clear();next.clear();throw failure(Stage.ENDPOINT_CONFLICT,null);}
            if(old!=null)old.clear();bindings.put(node.getPublicInstanceKey(),next);
        }
        }catch(Exception invalid){throw failure(stage,invalid);}
    }
    Binding require(ComputeNode node) throws IOException {
        Stage stage=Stage.TRUST_CONFIGURATION;
        try {
        configured.verifyOfficial(node);
        stage=Stage.BINDING_CONTEXT;
        boolean missing;
        synchronized(bindings) {missing=!bindings.containsKey(node.getPublicInstanceKey());}
        if(missing) {
            // Another backend may have won the paid-run worker lease. Recover only
            // this original active run, through the same authenticated/pinned path.
            if(!"RUNNING".equals(node.getPowerState())
                ||org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw failure(stage,null);
            List<Map<String,Object>> rows=active(node);
            if(rows.size()!=1)throw failure(Stage.ACTIVE_RUN,null);
            prepare(String.valueOf(rows.get(0).get("run_id")),node);
        }
        synchronized(bindings) {
            Binding value=bindings.get(node.getPublicInstanceKey());
            List<Map<String,Object>> rows=active(node);
            if(value==null||rows.size()!=1||!value.run.equals(String.valueOf(rows.get(0).get("run_id")))
                ||!Objects.equals(value.tenant,node.getTenantId())||!Objects.equals(value.instance,node.getProviderInstanceId()))throw failure(stage,null);
            return value.copy();
        }
        }catch(Exception invalid){throw failure(stage,invalid);}
    }
    private List<Map<String,Object>> active(ComputeNode node) {
        return jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key=? AND node_id=? AND state IN ('STARTING','RUNNING','STOPPING')",node.getPublicInstanceKey(),node.getId());
    }
    void clear(ComputeNode node) {synchronized(bindings){Binding old=bindings.remove(node.getPublicInstanceKey());if(old!=null)old.clear();}}

    static Binding parse(String run,ComputeNode node,JsonNode snapshot,ConfiguredComputeProperties.Resource resource) throws IOException {
        Stage stage=Stage.SNAPSHOT_SHAPE;
        try {
            if(snapshot==null||!snapshot.isObject())throw failure(stage,null);
            stage=Stage.SSH_ENDPOINT;
            String host=snapshot.path("proxy_host").asText();JsonNode port=snapshot.path("ssh_port");
            if(!host.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]{0,250})\\.(?:autodl|seetacloud)\\.com")||host.contains("..")
                ||!port.isIntegralNumber()||!port.canConvertToInt()||port.asInt()<1||port.asInt()>65535)throw failure(stage,null);
            stage=Stage.SSH_PASSWORD;
            String password=snapshot.path("root_password").asText();
            if(password.isEmpty()||password.length()>4096||password.chars().anyMatch(c->c<32||c==127))throw failure(stage,null);
            stage=Stage.HOST_KEY;
            String key=resource.getHostKey();
            if(key==null||key.isEmpty())key=knownHost(resource.getKnownHostsFile(),host,port.asInt());
            if(!"ssh-ed25519".equals(new HostKey(host,Base64.getDecoder().decode(key)).getType()))throw failure(stage,null);
            return new Binding(run,node.getTenantId(),node.getProviderInstanceId(),host,port.asInt(),key,password.toCharArray());
        }catch(Exception invalid){throw failure(stage,invalid);}
    }
    static String knownHost(String file,String host,int port) throws Exception {
        try {
        if(file==null)throw failure(Stage.KNOWN_HOSTS,null);Path path=Paths.get(file);
        if(!path.isAbsolute()||!path.normalize().equals(path)||Files.isSymbolicLink(path)||!Files.isRegularFile(path)||Files.size(path)>262144)throw failure(Stage.KNOWN_HOSTS,null);
        JSch jsch=new JSch();jsch.setKnownHosts(path.toString());
        HostKey[] keys=jsch.getHostKeyRepository().getHostKey(port==22?host:"["+host+"]:"+port,"ssh-ed25519");
        if(keys==null||keys.length!=1)throw failure(Stage.KNOWN_HOSTS,null);return keys[0].getKey();
        }catch(Exception invalid){throw failure(Stage.KNOWN_HOSTS,invalid);}
    }
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
