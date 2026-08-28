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

/**
 *
 * <p>
 * data MQ
 * <p>
 * dataonly will
 * workflow in in data Get interface Get
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/queue/bounded")
public class BoundedQueueController {


    /**
     * data
     *
     * @param queueName
     * @param capacity
     */
    @GetMapping("/add")
    public R<Void> add(String queueName, int capacity) {
        // need to will in
        boolean b = QueueUtils.destroyQueue(queueName);
        log.info("通道: {} , 删除: {}", queueName, b);
        // Initialize Set
        if (QueueUtils.trySetBoundedQueueCapacity(queueName, capacity)) {
            log.info("通道: {} , 设置容量: {}", queueName, capacity);
        } else {
            log.info("通道: {} , 设置容量失败", queueName);
            return R.fail("操作失败");
        }
        for (int i = 0; i < 11; i++) {
            String data = "data-" + i;
            boolean flag = QueueUtils.addBoundedQueueObject(queueName, data);
            if (flag == false) {
                log.info("通道: {} , 发送数据: {} 失败, 通道已满", queueName, data);
            } else {
                log.info("通道: {} , 发送数据: {}", queueName, data);
            }
        }
        return R.ok("操作成功");
    }

    /**
     * Delete data
     *
     * @param queueName
     */
    @GetMapping("/remove")
    public R<Void> remove(String queueName) {
        String data = "data-" + 5;
        if (QueueUtils.removeQueueObject(queueName, data)) {
            log.info("通道: {} , 删除数据: {}", queueName, data);
        } else {
            return R.fail("操作失败");
        }
        return R.ok("操作成功");
    }

    /**
     * Get data
     *
     * @param queueName
     */
    @GetMapping("/get")
    public R<Void> get(String queueName) {
        String data;
        do {
            data = QueueUtils.getQueueObject(queueName);
            log.info("通道: {} , 获取数据: {}", queueName, data);
        } while (data != null);
        return R.ok("操作成功");
    }

}
