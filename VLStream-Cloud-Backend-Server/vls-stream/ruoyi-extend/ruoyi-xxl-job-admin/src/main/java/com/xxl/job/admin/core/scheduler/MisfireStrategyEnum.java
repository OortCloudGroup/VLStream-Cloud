/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.util.I18nUtil;

/**
 * @author xuxueli 2020-10-29 21:11:23
 */
public enum MisfireStrategyEnum {

    /**
     * do nothing
     */
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    /**
     * fire once now
     */
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));

    private String title;

    MisfireStrategyEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem) {
        for (MisfireStrategyEnum item : MisfireStrategyEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
