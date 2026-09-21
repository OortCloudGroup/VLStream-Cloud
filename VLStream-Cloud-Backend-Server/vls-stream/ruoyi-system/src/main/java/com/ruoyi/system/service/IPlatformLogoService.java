/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.PlatformLogoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPlatformLogoService {

    List<PlatformLogoVo> list();

    PlatformLogoVo current();

    PlatformLogoVo create(MultipartFile file, String description);

    PlatformLogoVo update(Long id, MultipartFile file, String description);

    void activate(Long id);

    void activateDefault();

    void remove(Long id);
}
