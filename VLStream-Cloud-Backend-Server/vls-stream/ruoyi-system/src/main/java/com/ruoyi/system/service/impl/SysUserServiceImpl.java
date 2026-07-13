/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService, UserService {
    @Value("${user.excludedjobname}")
    private String excludedJobName;
    @Value("${dept.excludedUdid}")
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
     * 根据条件分页查询用户列表0
     *
     * @param user 用户信息
     * @return 用户信息集合信息
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
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
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
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
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
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        return baseMapper.selectUserByUserName(userName);
    }

    /**
     * 通过手机号查询用户
     *
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByPhonenumber(String phonenumber) {
        return baseMapper.selectUserByPhonenumber(phonenumber);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserById(String userId) {
        return baseMapper.selectUserById(userId);
    }

    /**
     * 查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
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
     * 查询用户所属岗位组
     *
     * @param userName 用户名
     * @return 结果
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
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUser user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, user.getUserName())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkPhoneUnique(SysUser user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getPhonenumber, user.getPhonenumber())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkEmailUnique(SysUser user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getEmail, user.getEmail())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验用户是否允许操作
     *
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user) {
        if (ObjectUtil.isNotNull(user.getUserId()) && user.isAdmin()) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
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
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUser user) {
        // 新增用户信息
        int rows = baseMapper.insert(user);
        // 新增用户岗位关联
        insertUserPost(user);
        // 新增用户与角色管理
        insertUserRole(user);
        return rows;
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user) {
        user.setCreateBy(user.getUserName());
        user.setUpdateBy(user.getUserName());
        return baseMapper.insert(user) > 0;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUser user) {
        String userId = user.getUserId();
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>().eq(SysUserRoleView::getUserId, userId));
        // 新增用户与角色管理
        insertUserRole(user);
        // 删除用户与岗位关联
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        // 新增用户与岗位管理
        insertUserPost(user);
        return baseMapper.updateById(user);
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(String userId, String[] roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>()
            .eq(SysUserRoleView::getUserId, userId));
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user) {
        return baseMapper.updateById(user);
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUser user) {
        return baseMapper.updateById(user);
    }

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getAvatar, avatar)
                .eq(SysUser::getUserName, userName)) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user) {
        return baseMapper.updateById(user);
    }

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(String userName, String password) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, password)
                .eq(SysUser::getUserName, userName));
    }

    /**
     * 新增用户角色信息
     *
     * @param user 用户对象
     */
    public void insertUserRole(SysUser user) {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * 新增用户岗位信息
     *
     * @param user 用户对象
     */
    public void insertUserPost(SysUser user) {
        Long[] posts = user.getPostIds();
        if (ArrayUtil.isNotEmpty(posts)) {
            // 新增用户与岗位管理
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
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(String userId, String [] roleIds) {
        if (ArrayUtil.isNotEmpty(roleIds)) {
            // 新增用户与角色管理
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
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(String userId) {
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>().eq(SysUserRoleView::getUserId, userId));
        // 删除用户与岗位表
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        return baseMapper.deleteById(userId);
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(String[] userIds) {
        for (String userId : userIds) {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        List<String> ids = Arrays.asList(userIds);
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleView>().in(SysUserRoleView::getUserId, ids));
        // 删除用户与岗位表
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, ids));
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public SysUser selectUserByUserId(String UserId,String  tenantId) {
        SysUser sysUser = baseMapper.selectUserByUserId(UserId, tenantId);
        return sysUser;
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
        // 判断是不是领导局，如果不是领导局，则下一层的领导为领导局的领导，如果是领导局，则无下一级
        // 获取本级领导
        SysUser sysUser = baseMapper.selectUserById(userId);
//        String oortJobname = sysUser.getOortJobname();
        List<SysUser> leaders = new ArrayList<>();
        JSONArray objects = JSONUtil.parseArray(sysUser.getDeptInfo());
        // 获取当前用户在该部门下的职位
        String oortJobname = null;
        Integer oortLevel = Integer.MAX_VALUE;
        for (Object obj : objects) {
            if (obj instanceof JSONObject) {
                JSONObject node = (JSONObject) obj;
                //代改正oort_udid
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
        // 判断当前用户是否有职位,无职位则获取本级所有领导
        if (StringUtils.isBlank(oortJobname)) { // 无职位
            String deptId = sysUser.getDeptId();
            leaders.addAll(baseMapper.selectLeadersByDeptId(deptId));
        } else { // 有职位
            String deptId = sysUser.getDeptId();
            List<SysUser> sysUsers = baseMapper.selectLeadersByDeptId(deptId);
            // 只留下级别比自己大的
            for (SysUser user : sysUsers) {
                JSONArray obj = JSONUtil.parseArray(user.getDeptInfo());
                // 获取当前领导的最高职位
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
                // 判断本人级别和领导级别
                if (oortLevel > level) {
                    leaders.add(user);
                }
            }
        }

        // 获取父部门的领导
        String deptId = sysUser.getDeptId();
        String parentId = deptMapper.selectByDeptId(deptId).getParentId();
        List<SysUser> parentleaders = baseMapper.selectLeadersByDeptId(parentId);
        leaders.addAll(parentleaders);
        // 若parentleaders为空，则领导为局领导
        if (CollectionUtils.isEmpty(parentleaders)) {
            SysDeptView sysDeptView = deptMapper.selectDeptByUdid(excludedUdid);
            parentleaders = baseMapper.selectLeadersByDeptId(sysDeptView.getDeptId());
            leaders.addAll(parentleaders);
        } else { // 若parentleaders不为空，则获取下一级领导
            // 获取爷部门的领导
            String  gandpId = deptMapper.selectByDeptId(parentId).getParentId();
            List<SysUser> gandpleaders = baseMapper.selectLeadersByDeptId(gandpId);
            // 若leaders为空，则领导为局领导
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
                                //去除当前登录用户，不能为候选审批人
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
                // 处理解析异常，根据实际情况处理，例如记录日志或者抛出特定异常
                e.printStackTrace(); // 记录异常到日志
                // 这里同样可以选择是否处理异常后继续或忽略这个user
            }
        }
        for (SysUser filteredLeader : filteredLeaders) {
            String deptId1 = filteredLeader.getDeptId();
            filteredLeader.setDeptName(deptMapper.selectByDeptId(deptId1).getDeptName());
        }
        return filteredLeaders;
    }

    /**
     * 旧的获取领导人方法,本身不是领导则获取本级领导,本身是领导则获取上级领导
     * @param userId 用户ID
     * @return
     */
    @Override
    public List<SysUser> getLeadersSuperior(String userId) {
        String[] split = excludedJobName.split(",");
        List<String> excludedJobNames = Arrays.asList(split);
        if (userId == null) {
            throw new RuntimeException(" 最高级领导无法发起流程");
        }
        // 判断是不是领导局，如果不是领导局，则下一层的领导为领导局的领导，如果是领导局，则无下一级
        // 判断当前人是不是领导，如果是领导，则查找上层领导
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
        if (a) { // 有职位，是领导
            // 获取父部门的领导
            String deptId = sysUser.getDeptId();
            String parentId = deptMapper.selectByDeptId(deptId).getParentId();
            leaders = baseMapper.selectLeadersByDeptId(parentId);
            // 若leaders为空，则领导为局领导
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
                               //去除当前登录用户，不能为候选审批人
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
                // 处理解析异常，根据实际情况处理，例如记录日志或者抛出特定异常
                e.printStackTrace(); // 记录异常到日志
                // 这里同样可以选择是否处理异常后继续或忽略这个user
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
