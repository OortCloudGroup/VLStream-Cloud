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
    String read(JsonNode snapshot) throws IOException {
        String base=origin(snapshot.path("jupyter_domain").asText());
        final String token=snapshot.path("jupyter_token").asText();
        if(token.isEmpty()||token.length()>4096||token.chars().anyMatch(c->c<=32||c==127))throw denied();
        HttpHeaders headers=new HttpHeaders();headers.set("Authorization","token "+token);headers.setContentType(MediaType.APPLICATION_JSON);
        String terminal=null;Session socket=null;
        try {
            String response=http.exchange(base+"/api/terminals",HttpMethod.POST,new HttpEntity<>("{}",headers),String.class).getBody();
            if(response==null||response.length()>16384)throw denied();
            terminal=json.readTree(response).path("name").asText();
            if(!terminal.matches("[A-Za-z0-9_-]{1,80}")){terminal=null;throw denied();}
            CompletableFuture<String> proof=new CompletableFuture<>();
            String marker="VLS_HOST_PROOF_"+UUID.randomUUID().toString().replace("-","");
            String command="printf '\\n"+marker+"\\n'; cat /etc/ssh/ssh_host_ed25519_key.pub; printf '\\n"+marker+"_END\\n'; exit\r";
            ClientEndpointConfig config=ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator(){
                @Override public void beforeRequest(Map<String,List<String>> request){request.put("Authorization",Collections.singletonList("token "+token));request.put("Origin",Collections.singletonList(base));}
            }).build();
            config.getUserProperties().put("org.apache.tomcat.websocket.IO_TIMEOUT_MS",5000L);
            config.getUserProperties().put("org.apache.tomcat.websocket.MAX_REDIRECTIONS",0);
            WebSocketContainer container=ContainerProvider.getWebSocketContainer();container.setDefaultMaxTextMessageBufferSize(32768);
            Endpoint endpoint=new Endpoint(){
                @Override public void onOpen(Session session,EndpointConfig ignored){
                    StringBuilder stdout=new StringBuilder();
                    session.addMessageHandler(new MessageHandler.Whole<String>(){public void onMessage(String message){try{
                        JsonNode frame=json.readTree(message);
                        if(frame.isArray()&&frame.size()>1&&"stdout".equals(frame.get(0).asText())){
                            stdout.append(frame.get(1).asText());if(stdout.length()>32768)throw denied();
                            // A line containing only the nonce cannot be the echoed shell command.
                            String key=extract(stdout.toString(),marker);if(key!=null)proof.complete(key);
                        }
                    }catch(Exception invalid){proof.completeExceptionally(denied());}}});
                    try{session.getBasicRemote().sendText(json.writeValueAsString(Arrays.asList("stdin",command)));}catch(Exception invalid){proof.completeExceptionally(denied());}
                }
                @Override public void onError(Session session,Throwable error){proof.completeExceptionally(denied());}
            };
            socket=container.connectToServer(endpoint,config,URI.create("wss"+base.substring(5)+"/terminals/websocket/"+terminal));
            return proof.get(8,TimeUnit.SECONDS);
        }catch(Exception failure){throw denied();}
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
                ||!uri.getPath().isEmpty()||!(uri.getPort()==-1||uri.getPort()==443||uri.getPort()==8443))throw denied();
            return uri.toString();
        }catch(Exception invalid){throw denied();}
    }
    static String extract(String text,String marker) throws IOException {
        String normalized=text.replace("\r","");int from=normalized.indexOf("\n"+marker+"\n");
        if(from<0)return null;int end=normalized.indexOf("\n"+marker+"_END",from+marker.length()+2);if(end<0)return null;
        Matcher match=Pattern.compile("(?m)^ssh-ed25519 ([A-Za-z0-9+/=]+)(?: [^\\n]*)?$").matcher(normalized.substring(from+marker.length()+2,end));
        if(!match.find())throw denied();String key=match.group(1);if(match.find())throw denied();
        try{byte[] wire=Base64.getDecoder().decode(key);if(wire.length!=51||java.nio.ByteBuffer.wrap(wire,15,4).getInt()!=32||!"ssh-ed25519".equals(new HostKey("provider-proof",wire).getType()))throw denied();}catch(Exception invalid){throw denied();}
        return key;
    }
    private static IOException denied(){return new IOException("供应商已认证终端未取得可信SSH主机公钥");}
}
