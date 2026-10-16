package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Local resource ownership; legacy platform ownership never becomes a public consumer claim. */
@Service
@RequiredArgsConstructor
public class ComputeNodeOwnershipService {
    private final ComputePlatformAuthorization authorization;
    public ComputePlatformAuthorization.Context identity() {
        ComputePlatformAuthorization.Context actor=authorization.requireIdentity();
        if(actor==null || !actor.isAuthorized() || actor.getPlatformUserId()==null || actor.getPlatformUserId().trim().isEmpty()
            || actor.getVlsUserId()==null || actor.getVlsUserId().trim().isEmpty() || !Objects.equals(ComputeNodeService.tenant(),actor.getVlsTenantId()))
            throw new ServiceException("请授权当前平台用户以确认个人算力归属",47001);
        return actor;
    }
    protected String authenticatedUser() { return LoginHelper.getUserId(); }
    public String viewer() {
        String user = authenticatedUser();
        return user == null || user.trim().isEmpty() ? null
            : ModelArtifactObjectStore.hash("private-compute-vls-user\n" + ComputeNodeService.tenant() + "\n" + user);
    }
    public String requireViewer() {
        String viewer = viewer();
        if (viewer == null) throw new ServiceException("请先登录 VLS，以确认个人算力归属", 403);
        return viewer;
    }
    /** Existing SSO-owned rows stay with their verified owner; no inferred reassignment. */
    public String legacyViewer() {
        try { return ComputePublicInventoryService.subject(identity()); }
        catch (ServiceException e) { return null; }
    }
    public boolean owns(ComputeNode node) {
        String owner = node.getManagementOwnerSubject();
        return Objects.equals(ComputeNodeService.tenant(), node.getTenantId()) && owner != null
            && (Objects.equals(owner, viewer()) || Objects.equals(owner, legacyViewer()));
    }
    public boolean isLocalOwner(ComputeNode node) {
        String user = authenticatedUser();
        return user != null && !user.trim().isEmpty() && Objects.equals(ComputeNodeService.tenant(), node.getTenantId())
            && Objects.equals(node.getManagementOwnerSubject(), ModelArtifactObjectStore.hash(
                "private-compute-vls-user\n" + ComputeNodeService.tenant() + "\n" + user));
    }
    public void requireCreator(String creator) {
        if (creator == null || !Objects.equals(authenticatedUser(), creator))
            throw new ServiceException("只能提交当前用户自己的训练任务", 403);
    }
    public boolean administrator(){
        try{return LoginHelper.isAdmin() || StpUtil.hasPermission("vls:compute:manage-all");}catch(Exception e){return false;}
    }
    public boolean canManage(ComputeNode node) {
        if(!Objects.equals(ComputeNodeService.tenant(),node.getTenantId()))return false;
        if(administrator())return true;
        return owns(node);
    }
    public void requireManagement(ComputeNode node){if(!canManage(node))throw new ServiceException("实例不存在或不属于当前用户的管理范围",47003);}
    public void requirePrivateUse(ComputeNode node){
        if(!owns(node))
            throw new ServiceException("个人算力归属未确认或不属于当前用户；公共算力请从市场选择",47003);
    }
}
