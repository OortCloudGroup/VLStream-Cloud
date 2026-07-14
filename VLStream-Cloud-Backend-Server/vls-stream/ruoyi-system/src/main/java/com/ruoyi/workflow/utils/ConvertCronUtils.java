/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.utils;

import com.ruoyi.workflow.domain.Job;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ConvertCronUtils {
    /**
     * 将前端传递的 Job 参数转换为 CRON 表达式。
     *
     * @param job 前端传入的定时任务参数对象
     * @return 对应的 CRON 表达式字符串
     */
    public static String convertToCron(Job job) {
        String cron;
        int type = job.getTypes();

        switch (type) {
            case 1: // 每天
                // job.run 数组中存放的格式为 "HH:mm:ss"
                String[] hmsDaily = job.getRun().get(0).split(":");
                cron = String.format("%s %s %s * * ?", hmsDaily[2], hmsDaily[1], hmsDaily[0]);
                break;
            case 2: // 隔天（此处仅在同一月内有效，跨月需自行处理）
                String[] hmsInterval = job.getRun().get(0).split(":");
                Calendar calendar = Calendar.getInstance();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    calendar.setTime(sdf.parse(job.getStart()));
                } catch (ParseException e) {
                    throw new RuntimeException("日期解析失败：" + job.getStart(), e);
                }
                int startDay = calendar.get(Calendar.DAY_OF_MONTH);
                // 表达式为：秒 分 时 开始日/间隔 * ?
                cron = String.format("%s %s %s %d/%d * ?", hmsInterval[2], hmsInterval[1], hmsInterval[0],
                    startDay, job.getInterval());
                break;
            case 3: // 每周
                // job.run 传入的是星期几（例如传 "1" 代表星期一），Quartz 中星期定义为 1=SUN,2=MON,...,7=SAT
                int inputWeekDay = Integer.parseInt(job.getRun().get(0));
                // 映射算法：前端传1表示星期一，Quartz中星期一为2，即 (inputWeekDay % 7) + 1
                int quartzWeekDay = inputWeekDay % 7 + 1;
                // trg_time 为 int 类型，格式为 HHmmss，例如 170633 表示 17:06:33，需要格式化
                String triggerTimeWeek = String.format("%06d", job.getTrgTime());
                String h_week = triggerTimeWeek.substring(0, 2);
                String m_week = triggerTimeWeek.substring(2, 4);
                String s_week = triggerTimeWeek.substring(4, 6);
                // 表达式格式为：秒 分 时 ? * 星期
                cron = String.format("%s %s %s ? * %d", s_week, m_week, h_week, quartzWeekDay);
                break;
            case 4: // 每月
                // trg_time 为 int 类型，格式为 HHmmss，例如 170633 表示 17:06:33，需要格式化
                String triggerTimeMonth = String.format("%06d", job.getTrgTime());
                String h_month = triggerTimeMonth.substring(0, 2);
                String m_month = triggerTimeMonth.substring(2, 4);
                String s_month = triggerTimeMonth.substring(4, 6);
                // 当 job.run 包含多个日期时，用逗号连接，如 "10,15,2,5"
                String daysOfMonth = String.join(",", job.getRun());
                // 表达式格式为：秒 分 时 日 * ?
                cron = String.format("%s %s %s %s * ?", s_month, m_month, h_month, daysOfMonth);
                break;
            default:
                throw new IllegalArgumentException("不支持的定时类型：" + type);
        }
        return cron;
    }
}
