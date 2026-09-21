/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class TunnelTokenServiceTest {
    @Test
    void derivesStableVersionedServiceTokensWithoutPersistingPlaintext() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setServiceSigningSecret("0123456789abcdef0123456789abcdef");
        TunnelTokenService service = new TunnelTokenService(properties);

        String randomOne = service.randomToken(32);
        String randomTwo = service.randomToken(32);
        assertNotEquals(randomOne, randomTwo);
        assertNotEquals(randomOne, service.hash(randomOne));

        String generationOne = service.deriveServiceToken(42L, 1L);
        assertEquals(generationOne, service.deriveServiceToken(42L, 1L));
        assertNotEquals(generationOne, service.deriveServiceToken(42L, 2L));
        assertTrue(service.secureEquals("same", "same"));
        assertFalse(service.secureEquals("same", "different"));
    }

    @Test
    void rejectsShortServiceSigningSecret() {
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setServiceSigningSecret("too-short");
        TunnelTokenService service = new TunnelTokenService(properties);
        assertThrows(TunnelApiException.class, () -> service.deriveServiceToken(1L, 1L));
    }
}
