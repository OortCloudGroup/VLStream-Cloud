/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

/**
 * Raised after the trained PT artifact has been persisted and is ready for
 * deployment-format conversion.
 */
public class TrainingModelReadyEvent {

	private final Long trainingId;
	private final String modelPath;

	public TrainingModelReadyEvent(Long trainingId, String modelPath) {
		this.trainingId = trainingId;
		this.modelPath = modelPath;
	}

	public Long getTrainingId() {
		return trainingId;
	}

	public String getModelPath() {
		return modelPath;
	}
}
