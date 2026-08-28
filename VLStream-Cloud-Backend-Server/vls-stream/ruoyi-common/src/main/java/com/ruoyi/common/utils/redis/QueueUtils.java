/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils.redis;

import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 *
 * data MQ
 * need to redis 5.X
 *
 * @author Lion Li
 * @version 3.6.0 Add
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueUtils {

    private static final RedissonClient CLIENT = SpringUtils.getBean(RedissonClient.class);


    /**
     * Get instance
     */
    public static RedissonClient getClient() {
        return CLIENT;
    }

    /**
     * data
     *
     * @param queueName
     * @param data data
     */
    public static <T> boolean addQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.offer(data);
    }

    /**
     * Get data data null( )
     *
     * @param queueName
     */
    public static <T> T getQueueObject(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.poll();
    }

    /**
     * Delete data( )
     */
    public static <T> boolean removeQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.remove(data);
    }

    /**
     * all ( )
     */
    public static <T> boolean destroyQueue(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.delete();
    }

    /**
     * data
     *
     * @param queueName
     * @param data data
     * @param time
     */
    public static <T> void addDelayedQueueObject(String queueName, T data, long time) {
        addDelayedQueueObject(queueName, data, time, TimeUnit.MILLISECONDS);
    }

    /**
     * data
     *
     * @param queueName
     * @param data data
     * @param time
     * @param timeUnit
     */
    public static <T> void addDelayedQueueObject(String queueName, T data, long time, TimeUnit timeUnit) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        delayedQueue.offer(data, time, timeUnit);
    }

    /**
     * Get data data null
     *
     * @param queueName
     */
    public static <T> T getDelayedQueueObject(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        return delayedQueue.poll();
    }

    /**
     * Delete data
     */
    public static <T> boolean removeDelayedQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        return delayedQueue.remove(data);
    }

    /**
     * all
     */
    public static <T> void destroyDelayedQueue(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        delayedQueue.destroy();
    }

    /**
     * data
     *
     * @param queueName
     * @param data data
     */
    public static <T> boolean addPriorityQueueObject(String queueName, T data) {
        RPriorityBlockingQueue<T> priorityBlockingQueue = CLIENT.getPriorityBlockingQueue(queueName);
        return priorityBlockingQueue.offer(data);
    }

    /**
     * Set
     *
     * @param queueName
     * @param capacity
     */
    public static <T> boolean trySetBoundedQueueCapacity(String queueName, int capacity) {
        RBoundedBlockingQueue<T> boundedBlockingQueue = CLIENT.getBoundedBlockingQueue(queueName);
        return boundedBlockingQueue.trySetCapacity(capacity);
    }

    /**
     * Set
     *
     * @param queueName
     * @param capacity
     * @param destroy already in whether
     */
    public static <T> boolean trySetBoundedQueueCapacity(String queueName, int capacity, boolean destroy) {
        RBoundedBlockingQueue<T> boundedBlockingQueue = CLIENT.getBoundedBlockingQueue(queueName);
        if (boundedBlockingQueue.isExists() && destroy) {
            destroyQueue(queueName);
        }
        return boundedBlockingQueue.trySetCapacity(capacity);
    }

    /**
     * data
     *
     * @param queueName
     * @param data data
     * @return successfully true already false
     */
    public static <T> boolean addBoundedQueueObject(String queueName, T data) {
        RBoundedBlockingQueue<T> boundedBlockingQueue = CLIENT.getBoundedBlockingQueue(queueName);
        return boundedBlockingQueue.offer(data);
    }

    /**
     * ( all : etc.)
     */
    public static <T> void subscribeBlockingQueue(String queueName, Consumer<T> consumer) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        queue.subscribeOnElements(consumer);
    }

}
