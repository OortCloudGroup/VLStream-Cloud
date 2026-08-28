/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.utils;

import com.ruoyi.workflow.domain.Job;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ConvertCronUtils {
    /**
     * before Job parameterConvert to CRON .
     *
     * @param job before taskparameterobject
     * @return CRON
     */
    public static String convertToCron(Job job) {
        String cron;
        int type = job.getTypes();

        switch (type) {
            case 1: // 每天
                // job.run array in to "HH:mm:ss"
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
                // to : start / * ?
                cron = String.format("%s %s %s %d/%d * ?", hmsInterval[2], hmsInterval[1], hmsInterval[0],
                    startDay, job.getInterval());
                break;
            case 3: // 每周
                // job.run is ( "1" represents ), Quartz in to 1=SUN,2=MON,...,7=SAT
                int inputWeekDay = Integer.parseInt(job.getRun().get(0));
                // algorithm: before 1 , Quartz in to 2, (inputWeekDay % 7) + 1
                int quartzWeekDay = inputWeekDay % 7 + 1;
                // trg_time to int , to HHmmss, 170633 17:06:33, need to Format
                String triggerTimeWeek = String.format("%06d", job.getTrgTime());
                String h_week = triggerTimeWeek.substring(0, 2);
                String m_week = triggerTimeWeek.substring(2, 4);
                String s_week = triggerTimeWeek.substring(4, 6);
                // to : ? *
                cron = String.format("%s %s %s ? * %d", s_week, m_week, h_week, quartzWeekDay);
                break;
            case 4: // 每月
                // trg_time to int , to HHmmss, 170633 17:06:33, need to Format
                String triggerTimeMonth = String.format("%06d", job.getTrgTime());
                String h_month = triggerTimeMonth.substring(0, 2);
                String m_month = triggerTimeMonth.substring(2, 4);
                String s_month = triggerTimeMonth.substring(4, 6);
                // job.run , , "10,15,2,5"
                String daysOfMonth = String.join(",", job.getRun());
                // to : * ?
                cron = String.format("%s %s %s %s * ?", s_month, m_month, h_month, daysOfMonth);
                break;
            default:
                throw new IllegalArgumentException("不支持的定时类型：" + type);
        }
        return cron;
    }
}
