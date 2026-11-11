package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingSnapshot;
import java.math.BigInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ComputePaidRunBudgetTest {
    private final ObjectMapper json=new ObjectMapper();

    private BillingSnapshot snapshot() throws Exception {
        return BillingSnapshot.read("{\"price_snapshot_id\":\"fixture\",\"conversion_version\":\"fixture\",\"quota_unit\":\"quota\",\"approved_hourly_price\":\"13\",\"numerator\":\"500000\",\"denominator\":\"1\",\"effective_at\":\"2020-01-01T00:00:00Z\",\"valid_until\":\"2030-01-01T00:00:00Z\",\"accepted_rate\":{\"currency\":\"CNY\"}}",json);
    }

    @Test void derivesTheRunWindowFromTheAvailableBalance() throws Exception {
        assertEquals(3240,ComputePaidRunService.affordableRunSeconds(snapshot(),"6500000",null,2592000,300,60));
    }

    @Test void respectsAnOptionalAbsoluteCapAndTheTechnicalMaximum() throws Exception {
        assertEquals(193,ComputePaidRunService.affordableRunSeconds(snapshot(),"6500000",new BigInteger("1000000"),2592000,300,60));
        assertEquals(60,ComputePaidRunService.affordableRunSeconds(snapshot(),"6500000",null,60,300,60));
    }

    @Test void rejectsABalanceBelowTheMinimumSafeWindow() throws Exception {
        assertThrows(ServiceException.class,()->ComputePaidRunService.affordableRunSeconds(snapshot(),"10000",null,2592000,300,60));
    }
}
