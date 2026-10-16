package com.ruoyi.web.controller.compat.tenant;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import com.ruoyi.web.controller.compat.BladeTokenUserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.ServletRequest;
import java.util.Objects;

/** Reuses the existing gateway and bound multi-tenant session. No new token/user persistence. */
@Service
@RequiredArgsConstructor
public class ComputePlatformAuthorizationService implements ComputePlatformAuthorization {
    private final PlatformTenantClient client;
    private final PlatformTenantSessionStore sessions;
    private final BladeTokenUserStore localUsers;
    private final TokenProperties properties;
    private static final String ATTRIBUTE=ComputePlatformAuthorizationService.class.getName()+".context";

    @Override public Context check() {
        HttpServletRequest request=currentRequest(); Context result=new Context();
        boolean multi="multi".equalsIgnoreCase(properties.getTenantType()); result.setMode(multi?"multi":"single");
        if (!multi && !"single".equalsIgnoreCase(properties.getTenantType())) return deny(result,"UNAVAILABLE","平台授权模式配置未确认，收费操作已阻断");
        String localToken=request==null?null:TokenHeaderResolver.normalize(request.getHeader("Authorization"));
        if (localToken==null && request!=null) localToken=TokenHeaderResolver.normalize(request.getHeader("blade-auth"));
        SysUser local=localToken==null?null:localUsers.get(localToken);
        if (local==null || currentUserId()==null || !Objects.equals(currentUserId(),local.getUserId())
            || !Objects.equals(TenantContextHolder.getTenantId(),local.getTenantId())) return deny(result,"REQUIRED","请先登录 VLS，再授权登录平台账户");
        result.setVlsUserId(local.getUserId()); result.setVlsTenantId(local.getTenantId());
        Object cached=request.getAttribute(ATTRIBUTE);
        if (cached instanceof Context) return (Context)cached; // only this authenticated HTTP request
        String supplied=originalPlatformToken(request);
        if (supplied==null || supplied.equals(localToken)) return deny(result,"REQUIRED","收费算力需要独立的平台授权，本地登录令牌不能代替");
        PlatformGatewayHeaders headers; String token=supplied; PlatformTenantSession bound=null;
        if (multi) {
            bound=sessions.get(localToken);
            if (bound==null || !Objects.equals(bound.getTenantId(),local.getTenantId()) || !Objects.equals(supplied,bound.getPlatformAccessToken()))
                return deny(result,"CHANGED","平台会话缺失或账户已变化，请重新授权登录");
            token=bound.getPlatformAccessToken(); headers=bound.getGatewayHeaders();
        } else {
            try { headers=client.resolveGatewayHeaders(request); }
            catch (Exception e) { return deny(result,"UNAVAILABLE","平台授权校验配置暂不可用，请联系管理员"); }
        }
        try {
            PlatformIdentity identity=client.verifyToken(token,headers);
            if (identity==null || blank(identity.getUserId()) || blank(identity.getTenantId())) return deny(result,"UNAVAILABLE","平台授权未返回有效身份，请稍后重试");
            if (multi && (!Objects.equals(bound.getPlatformUserId(),identity.getUserId()) || !Objects.equals(bound.getTenantId(),identity.getTenantId())))
                return deny(result,"CHANGED","平台账户或租户已变化，请重新授权登录");
            result.setAuthorized(true); result.setPlatformUserId(identity.getUserId()); result.setPlatformTenantId(identity.getTenantId());
            result.setMessage("平台身份已校验；个人余额扣款、GPU 计量与整机占用/产物安全回存清理尚未接入，收费操作仍阻断；工作空间成本由平台承担");
            request.setAttribute(ATTRIBUTE,result); return result;
        } catch (Exception e) {
            // Never echo upstream messages, URLs, credentials or exception causes.
            return deny(result,"UNAVAILABLE","平台授权无法确认（可能已失效或服务不可用），请重新授权或稍后重试");
        }
    }
    protected HttpServletRequest currentRequest() { return ServletUtils.getRequest(); }
    protected String currentUserId() { return LoginHelper.getUserId(); }
    /** Compatibility wrappers normalize all auth headers to the local session.
     * Keep platform proof separate by reading the original inbound header;
     * it still requires the authenticated local actor and gateway verification. */
    private static String originalPlatformToken(HttpServletRequest request) {
        HttpServletRequest original=request;
        while(original instanceof HttpServletRequestWrapper) {
            ServletRequest nested=((HttpServletRequestWrapper)original).getRequest();
            if(!(nested instanceof HttpServletRequest) || nested==original)return null;
            original=(HttpServletRequest)nested;
        }
        return TokenHeaderResolver.normalize(original.getHeader("accesstoken"));
    }
    private static boolean blank(String value) { return value==null || value.trim().isEmpty(); }
    private Context deny(Context result,String reason,String message) { result.setReason(reason);result.setMessage(message);return result; }
}
