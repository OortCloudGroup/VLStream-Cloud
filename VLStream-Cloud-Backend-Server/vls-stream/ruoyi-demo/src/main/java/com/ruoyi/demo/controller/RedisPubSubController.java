/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/redis/pubsub")
public class RedisPubSubController {

    /**
     *
     *
     * @param key channelKey
     * @param value
     */
    @GetMapping("/pub")
    public R<Void> pub(String key, String value) {
        RedisUtils.publish(key, value, consumer -> {
            System.out.println("发布通道 => " + key + ", 发送值 => " + value);
        });
        return R.ok("操作成功");
    }

    /**
     *
     *
     * @param key channelKey
     */
    @GetMapping("/sub")
    public R<Void> sub(String key) {
        RedisUtils.subscribe(key, String.class, msg -> {
            System.out.println("订阅通道 => " + key + ", 接收值 => " + msg);
        });
        return R.ok("操作成功");
    }

}
