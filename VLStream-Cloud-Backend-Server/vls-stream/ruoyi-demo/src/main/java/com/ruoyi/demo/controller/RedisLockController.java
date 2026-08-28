/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.controller;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.ruoyi.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;


/**
 *
 *
 * @author shenxinquan
 */
@Slf4j
@RestController
@RequestMapping("/demo/redisLock")
public class RedisLockController {

    @Autowired
    private LockTemplate lockTemplate;

    /**
     * lock4j
     */
    @Lock4j(keys = {"#key"})
    @GetMapping("/testLock4j")
    public R<String> testLock4j(String key, String value) {
        System.out.println("start:" + key + ",time:" + LocalTime.now().toString());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end :" + key + ",time:" + LocalTime.now().toString());
        return R.ok("操作成功", value);
    }

    /**
     * lock4j
     */
    @GetMapping("/testLock4jLockTemplate")
    public R<String> testLock4jLockTemplate(String key, String value) {
        final LockInfo lockInfo = lockTemplate.lock(key, 30000L, 5000L, RedissonLockExecutor.class);
        if (null == lockInfo) {
            throw new RuntimeException("业务处理中,请稍后再试");
        }
        // Get successfully, Process
        try {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                //
            }
            System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName());
        } finally {
            //
            lockTemplate.releaseLock(lockInfo);
        }
        // finish
        return R.ok("操作成功", value);
    }

}
