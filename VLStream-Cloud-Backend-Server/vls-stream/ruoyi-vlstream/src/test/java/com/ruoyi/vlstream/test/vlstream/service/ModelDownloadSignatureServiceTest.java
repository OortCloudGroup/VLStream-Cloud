package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelDownloadSignatureServiceTest {

	@Test
	void signsAndValidatesRequestAndExpiryTogether() throws Exception {
		VlsModelDispatchProperties properties = new VlsModelDispatchProperties();
		properties.setSigningSecret("test-only-signing-secret");
		ModelDownloadSignatureService service = new ModelDownloadSignatureService();
		setField(service, "properties", properties);

		long expiresAt = System.currentTimeMillis() / 1000L + 300L;
		String signature = service.sign("request-1", expiresAt);

		assertTrue(service.verify("request-1", expiresAt, signature));
		assertFalse(service.verify("request-2", expiresAt, signature));
		assertFalse(service.verify("request-1", expiresAt, signature + "00"));
		assertFalse(service.verify("request-1", 1L, service.sign("request-1", 1L)));
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
