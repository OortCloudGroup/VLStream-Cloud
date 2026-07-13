/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import java.util.List;
import java.util.Map;

public interface IVlsWebRtcService {

    Map<String, Object> getConfig();

    Map<String, Object> getStatus();

    Map<String, Object> refresh();

    Map<String, Object> startStream(String deviceId, String rtspUrl, Map<String, Object> options);

    Map<String, Object> stopStream(String streamId);

    Map<String, Object> validateRtspStream(String rtspUrl);

    List<Map<String, Object>> getActiveStreams();

    Map<String, Object> checkStream(String streamId);
}
