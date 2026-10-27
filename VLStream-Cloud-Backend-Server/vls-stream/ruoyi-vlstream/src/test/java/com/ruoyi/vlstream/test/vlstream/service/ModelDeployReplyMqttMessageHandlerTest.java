package com.ruoyi.vlstream.test.vlstream.service;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Tag("dev")
class ModelDeployReplyMqttMessageHandlerTest {
    @Test void errorEnvelopeCannotClaimSuccessfulDeployment() {
        ModelDispatchTaskService tasks = mock(ModelDispatchTaskService.class);
        ModelDeployReplyMqttMessageHandler handler = new ModelDeployReplyMqttMessageHandler(tasks);
        for (Integer code : new Integer[]{null, 500, 200}) {
            JSONObject envelope = new JSONObject().set("deviceId", "CAM-A").set("payload", new JSONObject()
                .set("sourceMsgId", "message-1").set("code", code).set("errCode", 0).set("msg", "fixture")
                .set("bizData", new JSONObject().set("requestId", "request-1").set("status", "SUCCESS").set("fileSha256", "a").set("classFileSha256", "b")));
            handler.handle(envelope, "{}");
            verify(tasks).applyHardwareReply(eq("message-1"), eq("request-1"), eq("CAM-A"), eq(Integer.valueOf(200).equals(code) ? "SUCCESS" : "FAILED"), eq("a"), eq("b"), eq("fixture"), eq("{}"));
            clearInvocations(tasks);
            if (Integer.valueOf(200).equals(code)) {
                envelope.getJSONObject("payload").set("errCode", 123); handler.handle(envelope, "{}");
                verify(tasks).applyHardwareReply(any(), any(), any(), eq("FAILED"), any(), any(), any(), any());
            }
        }
    }
}
