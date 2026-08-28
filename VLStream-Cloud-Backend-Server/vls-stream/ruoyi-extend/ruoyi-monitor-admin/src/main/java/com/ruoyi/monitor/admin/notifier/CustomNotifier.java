/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.monitor.admin.notifier;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * CustomeventnotificationProcess
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class CustomNotifier extends AbstractEventNotifier {

    protected CustomNotifier(InstanceRepository repository) {
        super(repository);
    }

    @Override
    @SuppressWarnings("all")
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        return Mono.fromRunnable(() -> {
            // instance event
            if (event instanceof InstanceStatusChangedEvent) {
                String registName = instance.getRegistration().getName();
                String instanceId = event.getInstance().getValue();
                String status = ((InstanceStatusChangedEvent) event).getStatusInfo().getStatus();
                log.info("Instance Status Change: [{}],[{}],[{}]", registName, instanceId, status);
            }

        });
    }

}
