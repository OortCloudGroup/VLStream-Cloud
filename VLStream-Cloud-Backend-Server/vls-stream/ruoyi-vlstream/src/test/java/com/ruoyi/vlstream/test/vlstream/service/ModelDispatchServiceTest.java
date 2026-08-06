package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class ModelDispatchServiceTest {

	private IVlsAlgorithmTrainingService trainingService;
	private RemoteModelArtifactService artifactService;
	private VlsModelDispatchProperties properties;
	private ModelDispatchService service;

	@BeforeEach
	void setUp() throws Exception {
		trainingService = mock(IVlsAlgorithmTrainingService.class);
		artifactService = mock(RemoteModelArtifactService.class);
		properties = new VlsModelDispatchProperties();
		properties.setPublicBaseUrl("http://192.168.88.31:8080");

		service = new ModelDispatchService();
		setField(service, "trainingService", trainingService);
		setField(service, "artifactService", artifactService);
		setField(service, "dispatchProperties", properties);

		when(artifactService.normalizeType("om")).thenReturn("om");
	}

	@Test
	void reportsMissingCompletedTrainingInsteadOfGenericDispatchFailure() {
		when(trainingService.list(any())).thenReturn(Collections.emptyList());

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.dispatch(2079813710632751106L, "2081669936341602305", "om"));

		assertEquals("算法 2079813710632751106 没有已完成的训练任务，无法下发 OM 模型",
			exception.getMessage());
	}

	@Test
	void reportsMissingSigningSecretBeforePublishing() throws Exception {
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(1L);
		when(trainingService.list(any())).thenReturn(Collections.singletonList(training));
		when(artifactService.resolvePath(training, "om")).thenReturn("/data/work/model.om");
		when(artifactService.inspect("/data/work/model.om"))
			.thenReturn(new RemoteModelArtifactService.ArtifactMetadata("model.om", 10L, repeat("a", 64)));

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.dispatch(1L, "2081669936341602305", "om"));

		assertEquals("未配置模型下载签名密钥 VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET",
			exception.getMessage());
	}

	private String repeat(String value, int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			builder.append(value);
		}
		return builder.toString();
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
