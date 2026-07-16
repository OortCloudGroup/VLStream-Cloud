/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.AlgorithmOrchestration;
import com.ruoyi.vlstream.mapper.VlsAlgorithmOrchestrationMapper;
import com.ruoyi.vlstream.service.IVlsAlgorithmOrchestrationService;
import org.springframework.stereotype.Service;

/** Real database service for algorithm orchestration definitions. */
@Service
public class VlsAlgorithmOrchestrationServiceImpl
    extends AbstractVlsTenantCrudService<VlsAlgorithmOrchestrationMapper, AlgorithmOrchestration>
    implements IVlsAlgorithmOrchestrationService {
}
