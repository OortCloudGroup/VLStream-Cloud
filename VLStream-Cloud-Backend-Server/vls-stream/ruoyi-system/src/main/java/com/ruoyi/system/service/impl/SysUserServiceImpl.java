/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.DataBaseHelper;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysPost;
import com.ruoyi.system.domain.SysUserPost;
import com.ruoyi.system.domain.SysUserRoleView;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * user layer Process
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService, UserService {
//    @Value("${user.excludedjobname}")
    private String excludedJobName;
//    @Value("${dept.excludedUdid}")
    private String excludedUdid;

    private final SysUserMapper baseMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleViewMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;

    @Override
    public TableDataInfo<SysUser> selectPageUserList(SysUser user, PageQuery pageQuery) {
        Page<SysUser> page = baseMapper.selectPageUserList(pageQuery.build(), this.buildQueryWrapper(user));
        return TableDataInfo.build(page);
    }

    /**
     * Query user list0
     *
     * @param user userinfo
     * @return userinfocollectioninfo
     */
    @Override
    public List<SysUser> selectUserList(SysUser user) {
        return baseMapper.selectUserList(this.buildQueryWrapper(user));
    }

    private Wrapper<SysUser> buildQueryWrapper(SysUser user) {
        Map<String, Object> params = user.getParams();
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", UserConstants.USER_NORMAL)
            .eq(ObjectUtil.isNotNull(user.getUserId()), "u.user_id", user.getUserId())
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), "u.nick_name", user.getNickName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "u.create_time", params.get("beginTime"), params.get("endTime"))
            .and(ObjectUtil.isNotNull(user.getDeptId()), w -> {
                List<SysDeptView> deptList = deptMapper.selectList(new LambdaQueryWrapper<SysDeptView>()
                    .select(SysDeptView::getDeptId)
                    .apply(DataBaseHelper.findInSet(user.getDeptId(), "ancestors")));
                List<String > ids = StreamUtils.toList(deptList, SysDeptView::getDeptId);
                ids.add(user.getDeptId());
                w.in("u.dept_id", ids);
            });
        return wrapper;
    }

    /**
     * Query already userrole list
     *
     * @param user userinfo
     * @return userinfocollectioninfo
     */
    @Override
    public TableDataInfo<SysUser> selectAllocatedList(SysUser user, PageQuery pageQuery) {
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", UserConstants.USER_NORMAL)
            .eq(ObjectUtil.isNotNull(user.getRoleId()), "r.role_id", user.getRoleId())
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), "u.nick_name", user.getNickName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber());
        Page<SysUser> page = baseMapper.selectAllocatedList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * Query not userrole list
     *
     * @param user userinfo
     * @return userinfocollectioninfo
     */
    @Override
    public TableDataInfo<SysUser> selectUnallocatedList(SysUser user, PageQuery pageQuery) {
        List<String> userIds = userRoleMapper.selectUserIdsByRoleId(user.getRoleId());
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", UserConstants.USER_NORMAL)
            .and(w -> w.ne("r.role_id", user.getRoleId()).or().isNull("r.role_id"))
            .notIn(CollUtil.isNotEmpty(userIds), "u.user_id", userIds)
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), "u.nick_name", user.getNickName())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber());
        Page<SysUser> page = baseMapper.selectUnallocatedList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * user Query user
     *
     * @param userName user
     * @return userobjectinfo
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        return baseMapper.selectUserByUserName(userName);
    }

    /**
     * Query user
     *
     * @param phonenumber
     * @return userobjectinfo
     */
    @Override
    public SysUser selectUserByPhonenumber(String phonenumber) {
        return baseMapper.selectUserByPhonenumber(phonenumber);
    }

    /**
     * user IDQuery user
     *
     * @param userId user ID
     * @return userobjectinfo
     */
    @Override
    public SysUser selectUserById(String userId) {
        return baseMapper.selectUserById(userId);
    }

    /**
     * Query user role
     *
     * @param userName user
     * @return
     */
    @Override
    public String selectUserRoleGroup(String userName) {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysRole::getRoleName);
    }

    /**
     * Query user
     *
     * @param userName user
     * @return
     */
    @Override
    public String selectUserPostGroup(String userName) {
//        List<com.ruoyi.system.domain.SysPost> list = postMapper.selectPostsByUserName(userName);
//        if (CollUtil.isEmpty(list)) {
//            return StringUtils.EMPTY;
//        }
//        return StreamUtils.join(list, com.ruoyi.system.domain.SysPost::getPostName);
        return null;
    }

    /**
     * Validate usernamewhether
     *
     * @param user userinfo
     * @return
     */
    @Override
    public boolean checkUserNameUnique(SysUser user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, user.getUserName())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * Validate whether
     *
     * @param user userinfo
     */
    @Override
    public boolean checkPhoneUnique(SysUser user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getPhonenumber, user.getPhonenumber())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * Validate emailwhether
     *
     * @param user userinfo
     */
    @Override
    public boolean checkEmailUnique(SysUser user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getEmail, user.getEmail())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * Validate userwhether operation
     *
     * @param user userinfo
     */
    @Override
    public void checkUserAllowed(SysUser user) {
        if (ObjectUtil.isNotNull(user.getUserId()) && user.isAdmin()) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * Validate userwhether data
     *
     * @param userId user ID
     */
    @Override
    public void checkUserDataScope(String userId) {
        if (!LoginHelper.isAdmin()) {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = this.selectUserList(user);
            if (CollUtil.isEmpty(users)) {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * Add userinfo
     *
     * @param user userinfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUser user) {
        // Add userinfo
        int rows = baseMapper.insert(user);
        // Add user
        insertUserPost(user);
        // Add user and role
        insertUserRole(user);
        return rows;
    }

    /**
     * userinfo
     *
     * @param user userinfo
     * @return
     */
    @Override
    public boolean registerUser(SysUser user) {
        user.setCreateBy(user.getUserName());
        user.setUpdateBy(user.getUserName());
        return baseMapper.insert(user) > 0;
    }

    /**
     * Update userinfo
     *
     * @param user userinfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUser user) {
        String userId = user.getUserId();
        // Delete user and role
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>().eq(SysUserRoleView::getUserId, userId));
        // Add user and role
        insertUserRole(user);
        // Delete user and
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        // Add user and
        insertUserPost(user);
        return baseMapper.updateById(user);
    }

    /**
     * user role
     *
     * @param userId user ID
     * @param roleIds role
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(String userId, String[] roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>()
            .eq(SysUserRoleView::getUserId, userId));
        insertUserRole(userId, roleIds);
    }

    /**
     * Update user
     *
     * @param user userinfo
     * @return
     */
    @Override
    public int updateUserStatus(SysUser user) {
        return baseMapper.updateById(user);
    }

    /**
     * Update user info
     *
     * @param user userinfo
     * @return
     */
    @Override
    public int updateUserProfile(SysUser user) {
        return baseMapper.updateById(user);
    }

    /**
     * Update user
     *
     * @param userName user
     * @param avatar
     * @return
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getAvatar, avatar)
                .eq(SysUser::getUserName, userName)) > 0;
    }

    /**
     * user
     *
     * @param user userinfo
     * @return
     */
    @Override
    public int resetPwd(SysUser user) {
        return baseMapper.updateById(user);
    }

    /**
     * user
     *
     * @param userName user
     * @param password
     * @return
     */
    @Override
    public int resetUserPwd(String userName, String password) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, password)
                .eq(SysUser::getUserName, userName));
    }

    /**
     * Add userroleinfo
     *
     * @param user userobject
     */
    public void insertUserRole(SysUser user) {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * Add user info
     *
     * @param user userobject
     */
    public void insertUserPost(SysUser user) {
        Long[] posts = user.getPostIds();
        if (ArrayUtil.isNotEmpty(posts)) {
            // Add user and
            List<SysUserPost> list = StreamUtils.toList(Arrays.asList(posts), postId -> {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                return up;
            });
            userPostMapper.insertBatch(list);
        }
    }

    /**
     * Add userroleinfo
     *
     * @param userId user ID
     * @param roleIds role
     */
    public void insertUserRole(String userId, String [] roleIds) {
        if (ArrayUtil.isNotEmpty(roleIds)) {
            // Add user and role
            List<SysUserRoleView> list = StreamUtils.toList(Arrays.asList(roleIds), roleId -> {
                SysUserRoleView ur = new SysUserRoleView();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            });
            userRoleMapper.insertBatch(list);
        }
    }

    /**
     * user IDDelete user
     *
     * @param userId user ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(String userId) {
        // Delete user and role
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>().eq(SysUserRoleView::getUserId, userId));
        // Delete user and
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        return baseMapper.deleteById(userId);
    }

    /**
     * Batch delete userinfo
     *
     * @param userIds need to Delete user ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(String[] userIds) {
        for (String userId : userIds) {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        List<String> ids = Arrays.asList(userIds);
        // Delete user and role
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>().in(SysUserRoleView::getUserId, ids));
        // Delete user and
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, ids));
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public SysUser selectUserByUserId(String UserId) {
        return baseMapper.selectUserByUserId(UserId);
    }

    @Override
    public List<SysUser> selectUserByUserIds(List<String> UserIds) {
        return baseMapper.selectUserByUserIds(UserIds);
    }


    @Override
    public List<SysUser> getLeaders(String userId) {
        String[] split = excludedJobName.split(",");
        List<String> excludedJobNames = Arrays.asList(split);
        if (userId == null) {
            throw new RuntimeException(" 最高级领导无法发起流程");
        }
        // Check if it is leader , if is leader , layer leader to leader leader, if is leader ,
        // Get leader
        SysUser sysUser = baseMapper.selectUserById(userId);
//        String oortJobname = sysUser.getOortJobname();
        List<SysUser> leaders = new ArrayList<>();
        JSONArray objects = JSONUtil.parseArray(sysUser.getDeptInfo());
        // Get current user in department
        String oortJobname = null;
        Integer oortLevel = Integer.MAX_VALUE;
        for (Object obj : objects) {
            if (obj instanceof JSONObject) {
                JSONObject node = (JSONObject) obj;
                // oort_udid
                if (sysUser.getDeptId().equals(node.getStr("oort_udid")) && node.containsKey("oort_jobname") && !"".equals(node.getStr("oort_jobname")) && !excludedJobNames.contains(node.getStr("oort_jobname"))) {
                    String newOortJobname = node.getStr("oort_jobname");
                    String[] split1 = newOortJobname.split(",");
                    for (String s : split1) {
                        SysPost sysPost = postMapper.selectById(s);
                        Integer newOortLevel = sysPost == null || sysPost.getPostSort() == null ? Integer.MAX_VALUE : sysPost.getPostSort();
                        if (oortLevel > newOortLevel) {
                            oortLevel = newOortLevel;
                            oortJobname = newOortJobname;
                        }
                    }
                }
            }
        }
        // Check current userwhether , Get all leader
        if (StringUtils.isBlank(oortJobname)) { //
            String deptId = sysUser.getDeptId();
            leaders.addAll(baseMapper.selectLeadersByDeptId(deptId));
        } else { //
            String deptId = sysUser.getDeptId();
            List<SysUser> sysUsers = baseMapper.selectLeadersByDeptId(deptId);
            // only
            for (SysUser user : sysUsers) {
                JSONArray obj = JSONUtil.parseArray(user.getDeptInfo());
                // Get current leader
                Integer level = Integer.MAX_VALUE;
                for (Object o : obj) {
                    if (o instanceof JSONObject) {
                        JSONObject node = (JSONObject) o;
                        if (node.containsKey("oort_jobname") && !"".equals(node.getStr("oort_jobname")) && !excludedJobNames.contains(node.getStr("oort_jobname"))) {
                            String newOortJobname = node.getStr("oort_jobname");
                            SysPost sysPost = postMapper.selectById(newOortJobname);
                            Integer newOortLevel = sysPost == null || sysPost.getPostSort() == null ? Integer.MAX_VALUE : sysPost.getPostSort();
                            if (level > newOortLevel) {
                                level = newOortLevel;
                            }
                        }
                    }
                }
                // Check and leader
                if (oortLevel > level) {
                    leaders.add(user);
                }
            }
        }

        // Get department leader
        String deptId = sysUser.getDeptId();
        String parentId = deptMapper.selectByDeptId(deptId).getParentId();
        List<SysUser> parentleaders = baseMapper.selectLeadersByDeptId(parentId);
        leaders.addAll(parentleaders);
        // parentleaders is empty, leader to bureau leader
        if (CollectionUtils.isEmpty(parentleaders)) {
            SysDeptView sysDeptView = deptMapper.selectDeptByUdid(excludedUdid);
            parentleaders = baseMapper.selectLeadersByDeptId(sysDeptView.getDeptId());
            leaders.addAll(parentleaders);
        } else { // parentleaders is empty, Get leader
            // Get department leader
            String  gandpId = deptMapper.selectByDeptId(parentId).getParentId();
            List<SysUser> gandpleaders = baseMapper.selectLeadersByDeptId(gandpId);
            // leaders is empty, leader to bureau leader
            if (CollectionUtils.isEmpty(gandpleaders)) {
                SysDeptView sysDeptView = deptMapper.selectDeptByUdid(excludedUdid);
                gandpleaders = baseMapper.selectLeadersByDeptId(sysDeptView.getDeptId());
            }
            leaders.addAll(gandpleaders);
        }

        List<SysUser> filteredLeaders = new ArrayList<>();
        for (SysUser user : leaders) {
            try {
                if (StringUtils.isNotBlank(user.getDeptInfo())) {
                    JSONArray deptInfoArray = (JSONArray) JSONUtil.parseArray(user.getDeptInfo());
                    boolean shouldInclude = false;
                    for (Object obj : deptInfoArray) {
                        if (obj instanceof JSONObject) {
                            JSONObject node = (JSONObject) obj;
                            if (node.containsKey("oort_jobname") && !"".equals(node.getStr("oort_jobname")) && !excludedJobNames.contains(node.getStr("oort_jobname"))) {
                                // current user, can to approver
                                if(selectUserById(userId).getUserId().equals(user.getUserId())){
                                    break;
                                }
                                shouldInclude = true;
                                break;
                            }
                        }
                    }
                    if (shouldInclude) {
                        filteredLeaders.add(user);
                    }
                }

            } catch (Exception e) {
                // Process Parse , Process , recordlog
                e.printStackTrace(); // record log
                // whether Process after user
            }
        }
        for (SysUser filteredLeader : filteredLeaders) {
            String deptId1 = filteredLeader.getDeptId();
            filteredLeader.setDeptName(deptMapper.selectByDeptId(deptId1).getDeptName());
        }
        return filteredLeaders;
    }

    /**
     * old Get leader method , is leader Get leader, is leader Get leader
     * @param userId user ID
     * @return
     */
    @Override
    public List<SysUser> getLeadersSuperior(String userId) {
        String[] split = excludedJobName.split(",");
        List<String> excludedJobNames = Arrays.asList(split);
        if (userId == null) {
            throw new RuntimeException(" 最高级领导无法发起流程");
        }
        // Check if it is leader , if is leader , layer leader to leader leader, if is leader ,
        // Check current is is leader, if is leader, find layer leader
        SysUser sysUser = baseMapper.selectUserById(userId);
//        String oortJobname = sysUser.getOortJobname();
        List<SysUser> leaders;
        JSONArray DeptInfos = JSONUtil.parseArray(sysUser.getDeptInfo());
        Boolean a = false;
        for (Object obj : DeptInfos) {
            if (obj instanceof JSONObject) {
                JSONObject node = (JSONObject) obj;
                if (node.containsKey("oort_jobname") && !"".equals(node.getStr("oort_jobname")) && !excludedJobNames.contains(node.getStr("oort_jobname"))) {
                    a = true;
                    break;
                }
            }
        }
        if (a) { // , is leader
            // Get department leader
            String deptId = sysUser.getDeptId();
            String parentId = deptMapper.selectByDeptId(deptId).getParentId();
            leaders = baseMapper.selectLeadersByDeptId(parentId);
            // leaders is empty, leader to bureau leader
            if (CollectionUtils.isEmpty(leaders)) {
                SysDeptView sysDeptView = deptMapper.selectDeptByUdid(excludedUdid);
                leaders = baseMapper.selectLeadersByDeptId(sysDeptView.getDeptId());
            }
        } else {
            String parentId = deptMapper.selectByDeptId( sysUser.getDeptId()).getParentId();
            leaders = baseMapper.selectLeadersByDeptId(parentId);
        }

        List<SysUser> filteredLeaders = new ArrayList<>();
        for (SysUser user : leaders) {
            try {
                if (StringUtils.isNotBlank(user.getDeptInfo())) {
                    JSONArray deptInfoArray = (JSONArray) JSONUtil.parseArray(user.getDeptInfo());
                    boolean shouldInclude = false;
                    for (Object obj : deptInfoArray) {
                        if (obj instanceof JSONObject) {
                            JSONObject node = (JSONObject) obj;
                            if (node.containsKey("oort_jobname") && !"".equals(node.getStr("oort_jobname")) && !excludedJobNames.contains(node.getStr("oort_jobname"))) {
                               // current user, can to approver
                                if(selectUserById(userId).getUserId().equals(user.getUserId())){
                                    break;
                                }
                                    shouldInclude = true;
                                    break;
                            }
                        }
                    }
                    if (shouldInclude) {
                        filteredLeaders.add(user);
                    }
                }

            } catch (Exception e) {
                // Process Parse , Process , recordlog
                e.printStackTrace(); // record log
                // whether Process after user
            }
        }
        for (SysUser filteredLeader : filteredLeaders) {
            String deptId1 = filteredLeader.getDeptId();
            filteredLeader.setDeptName(deptMapper.selectByDeptId(deptId1).getDeptName());
        }
        return filteredLeaders;
    }

    @Cacheable(cacheNames = CacheNames.SYS_USER_NAME, key = "#userId")
    @Override
    public String selectUserNameById(String userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserName).eq(SysUser::getUserId, userId));
        return ObjectUtil.isNull(sysUser) ? null : sysUser.getUserName();
    }

    @Override
    public String selectIdCardById(String userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getIdcard).eq(SysUser::getUserId, userId));
        return sysUser.getIdcard();
    }

//    @Cacheable(cacheNames = CacheNames.SYS_NICK_NAME, key = "#userId")
//    @Override
//    public String selectUserNameById(String userId) {
//        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
//            .select(SysUser::getUserName).eq(SysUser::getUserId, userId));
//        return ObjectUtil.isNull(sysUser) ? null : sysUser.getUserName();
//    }

    public List<SysUser> selectUserByDeptId(String deptId) {
        LambdaQueryWrapper<SysUser> eq = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDeptId, deptId);
        List<SysUser> sysUsers = baseMapper.selectList(eq);
        return sysUsers;
    }

    public List<SysUser> selectUserByUdid(String deptUdid) {
        LambdaQueryWrapper<SysUser> eq = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDeptId, deptUdid);
        List<SysUser> sysUsers = baseMapper.selectList(eq);
        return sysUsers;
    }
}
