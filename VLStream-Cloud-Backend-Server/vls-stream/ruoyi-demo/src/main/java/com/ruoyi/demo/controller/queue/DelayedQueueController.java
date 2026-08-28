/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller.queue;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.redis.QueueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 *
 * <p>
 * data MQ
 * : 30 after Process
 * <p>
 * dataonly will
 * workflow in in data
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/queue/delayed")
public class DelayedQueueController {

    /**
     *
     *
     * @param queueName
     */
    @GetMapping("/subscribe")
    public R<Void> subscribe(String queueName) {
        log.info("通道: {} 监听中......", queueName);
        // item Initialize Set
        QueueUtils.subscribeBlockingQueue(queueName, (String orderNum) -> {
            //
            log.info("通道: {}, 收到数据: {}", queueName, orderNum);
        });
        return R.ok("操作成功");
    }

    /**
     * data
     *
     * @param queueName
     * @param orderNum
     * @param time ( )
     */
    @GetMapping("/add")
    public R<Void> add(String queueName, String orderNum, Long time) {
        QueueUtils.addDelayedQueueObject(queueName, orderNum, time, TimeUnit.SECONDS);
        //
        log.info("通道: {} , 发送数据: {}", queueName, orderNum);
        return R.ok("操作成功");
    }

    /**
     * Delete data
     *
     * @param queueName
     * @param orderNum
     */
    @GetMapping("/remove")
    public R<Void> remove(String queueName, String orderNum) {
        if (QueueUtils.removeDelayedQueueObject(queueName, orderNum)) {
            log.info("通道: {} , 删除数据: {}", queueName, orderNum);
        } else {
            return R.fail("操作失败");
        }
        return R.ok("操作成功");
    }

    /**
     *
     *
     * @param queueName
     */
    @GetMapping("/destroy")
    public R<Void> destroy(String queueName) {
        // need to will in
        QueueUtils.destroyDelayedQueue(queueName);
        return R.ok("操作成功");
    }

}
