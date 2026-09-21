/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.io.FileUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.vo.PlatformLogoVo;
import com.ruoyi.system.service.IPlatformLogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/platform-logo")
public class PlatformLogoController {

    private static final long MAX_FILE_SIZE = 2L * 1024L * 1024L;
    private static final int MAX_IMAGE_EDGE = 4096;
    private static final String[] ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg"};

    private final IPlatformLogoService platformLogoService;

    @GetMapping("/current")
    public R<PlatformLogoVo> current() {
        return R.ok(platformLogoService.current());
    }

    @SaCheckPermission("system:platformLogo:list")
    @GetMapping
    public R<List<PlatformLogoVo>> list() {
        return R.ok(platformLogoService.list());
    }

    @SaCheckPermission("system:platformLogo:add")
    @Log(title = "平台标识", businessType = BusinessType.INSERT)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<PlatformLogoVo> create(@RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "description", required = false) String description) {
        validateImage(file);
        return R.ok(platformLogoService.create(file, description));
    }

    @SaCheckPermission("system:platformLogo:edit")
    @Log(title = "平台标识", businessType = BusinessType.UPDATE)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<PlatformLogoVo> update(@PathVariable Long id,
                                    @RequestPart(value = "file", required = false) MultipartFile file,
                                    @RequestParam(value = "description", required = false) String description) {
        if (file != null && !file.isEmpty()) {
            validateImage(file);
        }
        return R.ok(platformLogoService.update(id, file == null || file.isEmpty() ? null : file, description));
    }

    @SaCheckPermission("system:platformLogo:edit")
    @Log(title = "平台标识", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/active")
    public R<Void> activate(@PathVariable Long id) {
        platformLogoService.activate(id);
        return R.ok();
    }

    @SaCheckPermission("system:platformLogo:edit")
    @Log(title = "平台标识", businessType = BusinessType.UPDATE)
    @PutMapping("/default/active")
    public R<Void> activateDefault() {
        platformLogoService.activateDefault();
        return R.ok();
    }

    @SaCheckPermission("system:platformLogo:remove")
    @Log(title = "平台标识", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        platformLogoService.remove(id);
        return R.ok();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 LOGO 图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("LOGO 图片不能超过 2 MiB");
        }
        String extension = FileUtil.extName(file.getOriginalFilename());
        if (!StringUtils.equalsAnyIgnoreCase(extension, ALLOWED_EXTENSIONS)) {
            throw new IllegalArgumentException("LOGO 仅支持 PNG、JPG、JPEG 格式");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                || image.getWidth() > MAX_IMAGE_EDGE || image.getHeight() > MAX_IMAGE_EDGE) {
                throw new IllegalArgumentException("LOGO 图片无效或尺寸超过 4096×4096");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取 LOGO 图片", exception);
        }
    }
}
