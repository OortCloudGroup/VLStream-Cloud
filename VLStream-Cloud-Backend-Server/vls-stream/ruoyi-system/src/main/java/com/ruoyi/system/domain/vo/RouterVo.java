/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * configurationinfo
 *
 * @author Lion Li
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouterVo {

    /**
     *
     */
    private String name;

    /**
     *
     */
    private String path;

    /**
     * whether , Set true will
     */
    private boolean hidden;

    /**
     * , Set noRedirect in in
     */
    private String redirect;

    /**
     * component
     */
    private String component;

    /**
     * parameter: {"id": 1, "name": "ry"}
     */
    private String query;

    /**
     * children 1 , will -- componentpage
     */
    private Boolean alwaysShow;

    /**
     * element
     */
    private MetaVo meta;

    /**
     * sub
     */
    private List<RouterVo> children;

}
