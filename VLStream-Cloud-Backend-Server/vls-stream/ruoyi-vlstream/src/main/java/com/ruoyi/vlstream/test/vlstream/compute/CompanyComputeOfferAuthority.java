package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Company publication authority is deployment owned, never inferred from a user's private node. */
@Component
public class CompanyComputeOfferAuthority {
    @Value("${vlstream.compute.company-offers.management-tenant-id:}")
    private String managementTenantId = "";

    protected String tenant() { return ComputeNodeService.tenant(); }
    protected String user() { return LoginHelper.getUserId(); }
    protected boolean loggedIn() { return StpUtil.isLogin(); }
    protected boolean permission(String name) { return StpUtil.hasPermission(name); }

    public String requirePublisher() {
        return requirePublisherForTenant(managementTenantId);
    }

    /** A linked, deployment-approved company node supplies its persisted tenant; clients cannot choose it. */
    public String requirePublisherForTenant(String expectedTenant) {
        try {
            if (expectedTenant == null || expectedTenant.trim().isEmpty()
                || !expectedTenant.equals(tenant()) || !loggedIn()
                || !permission("vls:compute:company-publish")
                || !permission("vls:compute:publish") || !permission("vls:compute:configure"))
                throw denied();
            String actor = user();
            if (actor == null || actor.trim().isEmpty() || actor.length() > 128) throw denied();
            return actor;
        } catch (ServiceException error) {
            throw error;
        } catch (RuntimeException error) {
            throw denied();
        }
    }

    private ServiceException denied() { return new ServiceException("公司供给管理未配置或当前用户无发布权限", 403); }
}
