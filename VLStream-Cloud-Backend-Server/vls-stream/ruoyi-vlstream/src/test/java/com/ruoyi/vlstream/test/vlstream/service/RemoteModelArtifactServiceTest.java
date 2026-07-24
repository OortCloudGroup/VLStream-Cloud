package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteModelArtifactServiceTest {

	private final RemoteModelArtifactService service = new RemoteModelArtifactService();

	@Test
	void resolvesEverySupportedTrainingArtifact() throws Exception {
		AlgorithmTraining training = new AlgorithmTraining();
		training.setModelOutputPath("/models/a.pt");
		training.setOnnxModelOutputPath("/models/a.onnx");
		training.setRknnModelOutputPath("/models/a.rknn");
		training.setInt8RknnModelOutputPath("/models/a-int8.rknn");
		training.setOmModelOutputPath("/models/a.om");

		assertEquals("/models/a.pt", service.resolvePath(training, "pt"));
		assertEquals("/models/a.onnx", service.resolvePath(training, "ONNX"));
		assertEquals("/models/a.rknn", service.resolvePath(training, "rknn"));
		assertEquals("/models/a-int8.rknn", service.resolvePath(training, "int8-rknn"));
		assertEquals("/models/a.om", service.resolvePath(training, "om"));
	}

	@Test
	void rejectsUnsupportedOrMissingArtifacts() {
		AlgorithmTraining training = new AlgorithmTraining();
		assertThrows(IllegalArgumentException.class,
			() -> service.resolvePath(training, "tensorflow"));
		assertThrows(FileNotFoundException.class,
			() -> service.resolvePath(training, "om"));
	}
}
