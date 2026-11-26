/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.vlstream.test.vlstream.service.ExternalEventImageService;
import com.ruoyi.vlstream.test.vlstream.service.ExternalEventImageService.ImageReference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExternalEventImageController {
    private final ExternalEventImageService images;

    /** The source accepts only the configured event image reader, independently of user sessions. */
    @SaIgnore
    @PostMapping(ExternalEventImageService.PATH)
    public ResponseEntity<String[]> previews(
        @RequestHeader(value = ExternalEventImageService.TOKEN_HEADER, required = false) String token,
        @RequestHeader(value = ExternalEventImageService.TENANT_HEADER, required = false) String tenant,
        @RequestBody List<ImageReference> references) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(images.signBoundImages(token, tenant, references));
        } catch (ResponseStatusException refused) {
            return ResponseEntity.status(refused.getStatus()).build();
        }
    }
}
