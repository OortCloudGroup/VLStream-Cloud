package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import lombok.Data;

/** Platform identity proof only. It is neither a quota balance nor a New API payer binding. */
public interface ComputePlatformAuthorization {
    Context check();
    default Context requireIdentity() {
        Context context=check();
        if (!context.isAuthorized()) throw new ServiceException(context.getMessage(), "UNAVAILABLE".equals(context.getReason()) ? 47002 : 47001);
        return context;
    }
    @Data
    class Context {
        private boolean authorized;
        private String mode;
        private String vlsUserId;
        private String vlsTenantId;
        private String platformUserId;
        private String platformTenantId;
        private String reason;
        private String message;
    }
}
