/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class BladePasswordDecoderTest {

    private static final String PUBLIC_KEY =
        "049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64";
    private static final String PRIVATE_KEY =
        "7204a0e7dd3f23783fb04a82b7ae211441b12a8cb42694f9997e02babc4bf65b";
    private static final String SM_CRYPTO_MODE_0_CIPHER =
        "a5512f4520453fe24c34ee2cc6689ea647681d9bb702856f0c2227bfe5fd60db011a34ad12a7d92b8d1f754d1d13108b3f8faa3e186d1d8f89089440723530f8ea4eddf2781dc22452e5bb87262b29e88fa6129a1b58ea57e7da731ffbb8732dabecfe4b91b9a2517271f6";

    @Test
    void decryptsSmCryptoPasswordWithoutPrefix() {
        BladePasswordDecoder decoder = new BladePasswordDecoder(PUBLIC_KEY, PRIVATE_KEY);

        assertEquals("Password123", decoder.decode(SM_CRYPTO_MODE_0_CIPHER));
    }
}
