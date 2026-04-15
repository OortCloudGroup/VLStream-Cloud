package com.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vlstream.dto.UserCenterUser;
import com.vlstream.entity.LocalUser;
import com.vlstream.mapper.LocalUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户同步服务
 */
@Slf4j
@Service
public class UserSyncService {

    @Autowired
    private LocalUserMapper localUserMapper;

    @Autowired
    private UserCenterApiService userCenterApiService;

    /**
     * 同步用户信息到本地
     */
    public LocalUser syncUserToLocal(String accessToken) {
        try {
            log.info("开始同步用户信息，token: {}", accessToken);

            // 1. 调用统一用户中心验证token
            UserCenterUser userInfo = userCenterApiService.verifyToken(accessToken);
            
            if (userInfo == null) {
                log.error("Token验证失败: {}", accessToken);
                throw new RuntimeException("Token验证失败");
            }

            log.info("Token验证成功，用户信息: {}", userInfo.getUserName());

            // 2. 检查本地是否已存在该用户
            LocalUser localUser = localUserMapper.selectOne(
                new QueryWrapper<LocalUser>().eq("user_id", userInfo.getUserId())
            );

            if (localUser == null) {
                // 3. 创建新用户
                localUser = new LocalUser();
                localUser.setUserId(userInfo.getUserId());
                localUser.setTenantId(userInfo.getTenantId());
                localUser.setLoginId(userInfo.getLoginId());
                localUser.setUserName(userInfo.getUserName());
                localUser.setLoginTime(parseDateTime(userInfo.getLoginTime()));
                localUser.setLoginIp(userInfo.getLoginIP());
                localUser.setLoginType(userInfo.getLoginType());
                localUser.setClient(userInfo.getClient());
                localUser.setAccessToken(accessToken);
                localUser.setTokenExpireTime(calculateTokenExpireTime());
                localUser.setStatus(1);
                localUser.setForm(4); // 来自统一用户中心

                localUserMapper.insert(localUser);
                log.info("创建新用户成功: {}", userInfo.getUserName());
            } else {
                // 4. 更新现有用户信息
                localUser.setUserName(userInfo.getUserName());
                localUser.setLoginTime(parseDateTime(userInfo.getLoginTime()));
                localUser.setLoginIp(userInfo.getLoginIP());
                localUser.setAccessToken(accessToken);
                localUser.setTokenExpireTime(calculateTokenExpireTime());

                localUserMapper.updateById(localUser);
                log.info("更新用户信息成功: {}", userInfo.getUserName());
            }

            return localUser;

        } catch (Exception e) {
            log.error("同步用户信息失败", e);
            throw new RuntimeException("用户同步失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取本地用户信息
     */
    public LocalUser getLocalUser(String userId) {
        return localUserMapper.selectOne(
            new QueryWrapper<LocalUser>().eq("user_id", userId)
        );
    }

    /**
     * 更新本地用户信息
     */
    public LocalUser updateLocalUser(String userId, LocalUser userData) {
        LocalUser existingUser = getLocalUser(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        // 更新字段
        if (userData.getUserName() != null) {
            existingUser.setUserName(userData.getUserName());
        }
        if (userData.getLoginTime() != null) {
            existingUser.setLoginTime(userData.getLoginTime());
        }
        if (userData.getLoginIp() != null) {
            existingUser.setLoginIp(userData.getLoginIp());
        }
        if (userData.getAccessToken() != null) {
            existingUser.setAccessToken(userData.getAccessToken());
        }
        if (userData.getTokenExpireTime() != null) {
            existingUser.setTokenExpireTime(userData.getTokenExpireTime());
        }

        localUserMapper.updateById(existingUser);
        return existingUser;
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn("日期时间解析失败: {}, 使用当前时间", dateTimeStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 计算token过期时间（24小时后）
     */
    private LocalDateTime calculateTokenExpireTime() {
        return LocalDateTime.now().plusHours(24);
    }
} 