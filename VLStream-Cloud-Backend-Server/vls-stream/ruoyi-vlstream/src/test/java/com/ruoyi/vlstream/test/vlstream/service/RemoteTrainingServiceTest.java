package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class RemoteTrainingServiceTest {

	@Test
	void exportCommandQuotesPathsAndRequiresANonEmptyArtifact() {
		RemoteServers server = new RemoteServers();
		server.setWorkDir("/data/work/model export");
		server.setCondaEnv("train env");
		String modelPath = "/data/work/model export/weights/测试 model.pt";
		String outputPath = "/data/work/model export/weights/测试 model.onnx";

		String command = new RemoteTrainingService()
			.buildExportCommand(server, modelPath, "onnx", outputPath);

		assertTrue(command.startsWith("bash -lc '"));
		assertTrue(command.contains("测试 model.pt"));
		assertTrue(command.contains("测试 model.onnx"));
		assertTrue(command.contains("[ -s"));
		assertFalse(command.contains("model=" + modelPath));
		assertFalse(command.contains("[ -f "));
	}
}
