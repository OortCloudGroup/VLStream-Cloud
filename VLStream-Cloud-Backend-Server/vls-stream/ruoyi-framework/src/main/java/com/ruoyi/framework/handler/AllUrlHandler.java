/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.handler;

import cn.hutool.core.util.ReUtil;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Get all Urlconfiguration
 *
 * @author Lion Li
 */
@Data
@Component
public class AllUrlHandler implements InitializingBean {

    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    private List<String> urls = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        Set<String> set = new HashSet<>();
        RequestMappingHandlerMapping mapping = SpringUtils.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        map.keySet().forEach(info -> {
            // Get path path variable to *
            Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                .forEach(url -> set.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, "*")));
        });
        urls.addAll(set);
    }

}
