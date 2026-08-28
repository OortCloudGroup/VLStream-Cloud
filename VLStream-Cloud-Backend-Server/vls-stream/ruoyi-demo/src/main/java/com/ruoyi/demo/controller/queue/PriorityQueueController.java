/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.controller.queue;

import cn.hutool.core.util.RandomUtil;
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
 * only will
 * workflow in in data Get interface Get
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/queue/priority")
public class PriorityQueueController {

    /**
     * data
     *
     * @param queueName
     */
    @GetMapping("/add")
    public R<Void> add(String queueName) {
        // need to will in
        boolean b = QueueUtils.destroyQueue(queueName);
        log.info("通道: {} , 删除: {}", queueName, b);

        for (int i = 0; i < 10; i++) {
            int randomNum = RandomUtil.randomInt(10);
            PriorityDemo data = new PriorityDemo();
            data.setName("data-" + i);
            data.setOrderNum(randomNum);
            if (QueueUtils.addPriorityQueueObject(queueName, data)) {
                log.info("通道: {} , 发送数据: {}", queueName, data);
            } else {
                log.info("通道: {} , 发送数据: {}, 发送失败", queueName, data);
            }
        }
        return R.ok("操作成功");
    }

    /**
     * Delete data
     *
     * @param queueName
     * @param name object
     * @param orderNum
     */
    @GetMapping("/remove")
    public R<Void> remove(String queueName, String name, Integer orderNum) {
        PriorityDemo data = new PriorityDemo();
        data.setName(name);
        data.setOrderNum(orderNum);
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
        PriorityDemo data;
        do {
            data = QueueUtils.getQueueObject(queueName);
            log.info("通道: {} , 获取数据: {}", queueName, data);
        } while (data != null);
        return R.ok("操作成功");
    }

}
