package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.*;
import com.jcraft.jsch.HostKey;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.websocket.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import static com.ruoyi.vlstream.test.vlstream.compute.AutoDlRuntimeSsh.Stage;
import static com.ruoyi.vlstream.test.vlstream.compute.AutoDlRuntimeSsh.failure;

/** Read only the SSH public host key through the instance's provider-issued HTTPS terminal.
 * No SSH connection/TOFU, token in URL, private-key read, kernel, or user terminal reuse. */
@Component
public class AutoDlJupyterHostProof {
    private final ObjectMapper json=new ObjectMapper();
    private final RestTemplate http=AutoDlProPowerProvider.transport();
    public AutoDlJupyterHostProof(){
        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory=(org.springframework.http.client.HttpComponentsClientHttpRequestFactory)http.getRequestFactory();
        factory.setConnectTimeout(3000);factory.setReadTimeout(3000);factory.setConnectionRequestTimeout(3000);
    }
    protected WebSocketContainer container(){return ContainerProvider.getWebSocketContainer();}
    String read(JsonNode snapshot) throws IOException {
        if(snapshot==null||!snapshot.isObject())throw failure(Stage.SNAPSHOT_SHAPE,null);
        String base=origin(snapshot.path("jupyter_domain").asText());
        final String token=snapshot.path("jupyter_token").asText();
        if(token.isEmpty()||token.length()>4096||token.chars().anyMatch(c->c<=32||c==127))throw failure(Stage.JUPYTER_TOKEN,null);
        HttpHeaders headers=new HttpHeaders();headers.set("Authorization","token "+token);headers.setContentType(MediaType.APPLICATION_JSON);
        String terminal=null;Session socket=null;
        Stage stage=Stage.JUPYTER_TERMINAL_CREATE;
        try {
            String response=http.exchange(base+"/api/terminals",HttpMethod.POST,new HttpEntity<>("{}",headers),String.class).getBody();
            stage=Stage.JUPYTER_TERMINAL_RESPONSE;
            if(response==null||response.length()>16384)throw failure(stage,null);
            terminal=json.readTree(response).path("name").asText();
            if(!terminal.matches("[A-Za-z0-9_-]{1,80}")){terminal=null;throw failure(stage,null);}
            CompletableFuture<String> proof=new CompletableFuture<>();
            String marker="VLS_HOST_PROOF_"+UUID.randomUUID().toString().replace("-","");
            String command="printf '\\n"+marker+"\\n'; cat /etc/ssh/ssh_host_ed25519_key.pub; printf '\\n"+marker+"_END\\n'; exit\r";
            ClientEndpointConfig config=ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator(){
                @Override public void beforeRequest(Map<String,List<String>> request){request.put("Authorization",Collections.singletonList("token "+token));request.put("Origin",Collections.singletonList(base));}
            }).build();
            config.getUserProperties().put("org.apache.tomcat.websocket.IO_TIMEOUT_MS",5000L);
            config.getUserProperties().put("org.apache.tomcat.websocket.MAX_REDIRECTIONS",0);
            stage=Stage.JUPYTER_WEBSOCKET;
            WebSocketContainer container=container();container.setDefaultMaxTextMessageBufferSize(32768);
            Endpoint endpoint=new Endpoint(){
                @Override public void onOpen(Session session,EndpointConfig ignored){
                    StringBuilder stdout=new StringBuilder();
                    session.addMessageHandler(new MessageHandler.Whole<String>(){public void onMessage(String message){try{
                        JsonNode frame=json.readTree(message);
                        if(frame.isArray()&&frame.size()>1&&"stdout".equals(frame.get(0).asText())){
                            stdout.append(frame.get(1).asText());if(stdout.length()>32768)throw failure(Stage.JUPYTER_STDOUT,null);
                            // A line containing only the nonce cannot be the echoed shell command.
                            String key=extract(stdout.toString(),marker);if(key!=null)proof.complete(key);
                        }
                    }catch(Exception invalid){proof.completeExceptionally(failure(Stage.JUPYTER_STDOUT,invalid));}}});
                    try{session.getBasicRemote().sendText(json.writeValueAsString(Arrays.asList("stdin",command)));}catch(Exception invalid){proof.completeExceptionally(failure(Stage.JUPYTER_WEBSOCKET,invalid));}
                }
                @Override public void onError(Session session,Throwable error){proof.completeExceptionally(failure(Stage.JUPYTER_WEBSOCKET,error));}
            };
            socket=container.connectToServer(endpoint,config,URI.create("wss"+base.substring(5)+"/terminals/websocket/"+terminal));
            stage=Stage.JUPYTER_PROOF_WAIT;
            return proof.get(8,TimeUnit.SECONDS);
        }catch(Exception invalid){throw failure(stage,invalid);}
        finally{
            if(socket!=null)try{socket.close();}catch(Exception ignored){}
            if(terminal!=null)try{http.exchange(base+"/api/terminals/"+terminal,HttpMethod.DELETE,new HttpEntity<Void>(headers),Void.class);}catch(Exception ignored){}
            headers.clear();
        }
    }
    static String origin(String value) throws IOException {
        try{
            URI uri=URI.create("https://"+value);
            if(uri.getHost()==null||!uri.getHost().matches("[A-Za-z0-9][A-Za-z0-9.-]*\\.(?:autodl|seetacloud)\\.com")||uri.getHost().contains("..")
                ||uri.getUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null
                ||!uri.getPath().isEmpty()||!(uri.getPort()==-1||uri.getPort()==443||uri.getPort()==8443))throw failure(Stage.JUPYTER_ORIGIN,null);
            return uri.toString();
        }catch(Exception invalid){throw failure(Stage.JUPYTER_ORIGIN,invalid);}
    }
    static String extract(String text,String marker) throws IOException {
        String normalized=text.replace("\r","");int from=normalized.indexOf("\n"+marker+"\n");
        if(from<0)return null;int end=normalized.indexOf("\n"+marker+"_END",from+marker.length()+2);if(end<0)return null;
        Matcher match=Pattern.compile("(?m)^ssh-ed25519 ([A-Za-z0-9+/=]+)(?: [^\\n]*)?$").matcher(normalized.substring(from+marker.length()+2,end));
        if(!match.find())throw failure(Stage.JUPYTER_HOST_KEY,null);String key=match.group(1);if(match.find())throw failure(Stage.JUPYTER_HOST_KEY,null);
        try{byte[] wire=Base64.getDecoder().decode(key);if(wire.length!=51||java.nio.ByteBuffer.wrap(wire,15,4).getInt()!=32||!"ssh-ed25519".equals(new HostKey("provider-proof",wire).getType()))throw failure(Stage.JUPYTER_HOST_KEY,null);}catch(Exception invalid){throw failure(Stage.JUPYTER_HOST_KEY,invalid);}
        return key;
    }
}
