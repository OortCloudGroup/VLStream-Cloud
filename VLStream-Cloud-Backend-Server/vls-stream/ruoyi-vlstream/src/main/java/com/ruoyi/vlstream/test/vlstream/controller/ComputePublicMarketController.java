package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.compute.ComputePublicMarketService;
import java.util.Collections;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ComputePublicMarketController {
    private final ComputePublicMarketService market;
    private long windowStart;
    private int requests;
    // Bounded process-wide budget: no attacker-controlled identity map or forwarded-header trust.
    synchronized boolean admit(long now) {
        if (now - windowStart >= 60000 || now < windowStart) { windowStart = now; requests = 0; }
        return ++requests <= 120;
    }
    @GetMapping("/vlsCompute/public-market")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (!admit(System.currentTimeMillis())) {
            response.setHeader("Retry-After", "60");
            return ResponseEntity.status(429).body(Collections.singletonMap("code", "RATE_LIMITED"));
        }
        if (page < 1 || page > 100 || pageSize < 1 || pageSize > 20)
            return ResponseEntity.badRequest().body(Collections.singletonMap("code", "INVALID_PAGE"));
        try { return ResponseEntity.ok(market.list(page, pageSize)); }
        catch (RuntimeException error) {
            // Do not leak SQL, infrastructure, provider errors or internal debug reasons to anonymous visitors.
            return ResponseEntity.status(503).body(Collections.singletonMap("code", "MARKET_UNAVAILABLE"));
        }
    }
}
