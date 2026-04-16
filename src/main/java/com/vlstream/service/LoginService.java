package com.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vlstream.dto.LoginResponse;
import com.vlstream.dto.UserCenterUser;
import com.vlstream.entity.LocalUser;
import com.vlstream.mapper.LocalUserMapper;
import com.vlstream.utils.RSAUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Login service class
 */
@Slf4j
@Service
public class LoginService {

    @Autowired
    private LocalUserMapper localUserMapper;

    @Autowired
    private UserCenterApiService userCenterApiService;

    /**
     * Get tenant ID by tenant phrase
     */
    public String getTenantIdByPhrase(String phrase) {
        try {
            // 调用统一用户中心获取租户ID
            return userCenterApiService.getTenantIdByPhrase(phrase);
        } catch (Exception e) {
            log.error("获取租户ID失败: {}", phrase, e);
            throw new RuntimeException("获取租户ID失败: " + e.getMessage());
        }
    }

    /**
     * User login
     */
    public LoginResponse login(String encryptedUserInfo, String clientIp, String userAgent) {
        try {
            // 1. 解密用户信息
            String decryptedInfo = RSAUtils.decrypt(encryptedUserInfo);
            JSONObject loginData = JSON.parseObject(decryptedInfo);
            
            log.info("解密后的登录信息: {}", loginData.toJSONString());
            
            // 2. 验证时间戳（防重放攻击）
            long timestamp = loginData.getLong("timestamp");
            long currentTime = System.currentTimeMillis() / 1000;
            if (Math.abs(currentTime - timestamp) > 300) { // 5分钟有效期
                throw new RuntimeException("请求已过期，请重新登录");
            }
            
            // 3. 调用统一用户中心登录接口
            UserCenterUser userInfo = userCenterApiService.login(loginData);
            
            if (userInfo == null) {
                throw new RuntimeException("登录失败，用户名或密码错误");
            }
            
            // 4. 同步或更新本地用户信息
            LocalUser localUser = syncUserToLocal(userInfo, clientIp, userAgent);
            
            // 5. 构建登录响应
            LoginResponse response = new LoginResponse();
            response.setUserId(localUser.getUserId());
            response.setTenantId(localUser.getTenantId());
            response.setLoginId(localUser.getLoginId());
            response.setUserName(localUser.getUserName());
            response.setLoginTime(localUser.getLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.setLastRequestTime(localUser.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.setLoginIP(localUser.getLoginIp());
            response.setLogin_type(localUser.getLoginType());
            response.setClient(localUser.getClient());
            response.setAccessToken(localUser.getAccessToken());
            
            log.info("用户 {} 登录成功", localUser.getUserName());
            return response;
            
        } catch (Exception e) {
            log.error("登录失败", e);
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    /**
     * Verify token
     */
    public LocalUser verifyToken(String accessToken) {
        try {
            log.info("开始验证Token: {}", accessToken);
            
            // 1. 先查询本地数据库
            LocalUser localUser = localUserMapper.selectOne(
                new QueryWrapper<LocalUser>()
                    .eq("access_token", accessToken)
                    .eq("status", 1)
            );
            
            if (localUser != null) {
                log.info("找到本地用户记录: {}", localUser.getLoginId());
                
                // 2. 检查token是否过期
                if (localUser.getTokenExpireTime() != null && 
                    localUser.getTokenExpireTime().isBefore(LocalDateTime.now())) {
                    log.warn("Token已过期: {}", accessToken);
                    return null;
                }
                
                // 3. 调用统一用户中心验证token
                UserCenterUser userInfo = userCenterApiService.verifyToken(accessToken);
                if (userInfo != null) {
                    // 4. 更新用户信息
                    localUser.setLoginTime(parseDateTime(userInfo.getLoginTime()));
                    localUser.setLoginIp(userInfo.getLoginIP());
                    localUserMapper.updateById(localUser);
                    
                    return localUser;
                }
            } else {
                log.info("本地数据库中未找到token记录，尝试验证外部token");
                
                // 5. 本地没有找到，直接调用统一用户中心验证（SSO场景）
                UserCenterUser userInfo = userCenterApiService.verifyToken(accessToken);
                if (userInfo != null) {
                    log.info("外部token验证成功，用户: {}", userInfo.getUserName());
                    
                    // 6. 创建或更新本地用户记录
                    LocalUser existingUser = localUserMapper.selectOne(
                        new QueryWrapper<LocalUser>()
                            .eq("user_id", userInfo.getUserId())
                            .eq("status", 1)
                    );
                    
                    if (existingUser != null) {
                        // 更新现有用户的token
                        existingUser.setAccessToken(accessToken);
                        existingUser.setTokenExpireTime(calculateTokenExpireTime());
                        existingUser.setLoginTime(parseDateTime(userInfo.getLoginTime()));
                        existingUser.setLoginIp(userInfo.getLoginIP());
                        localUserMapper.updateById(existingUser);
                        log.info("更新现有用户token: {}", existingUser.getLoginId());
                        return existingUser;
                    } else {
                        // 创建新的本地用户记录
                        LocalUser newUser = syncUserToLocal(userInfo, "127.0.0.1", "web");
                        if (newUser != null) {
                            log.info("创建新的本地用户记录: {}", newUser.getLoginId());
                            return newUser;
                        }
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("验证Token失败", e);
            return null;
        }
    }

    /**
     * User logout
     */
    public boolean logout(String accessToken) {
        try {
            // 1. 调用统一用户中心退出接口
            boolean success = userCenterApiService.logout(accessToken);
            
            // 2. 清除本地token信息
            LocalUser localUser = localUserMapper.selectOne(
                new QueryWrapper<LocalUser>().eq("access_token", accessToken)
            );
            
            if (localUser != null) {
                localUser.setAccessToken(null);
                localUser.setTokenExpireTime(null);
                localUserMapper.updateById(localUser);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("退出失败", e);
            return false;
        }
    }

    /**
     * Verify user permission
     */
    public boolean verifyUserPermission(String accessToken, String serviceName, String pauth, String auth, String doAction) {
        try {
            // 1. 验证token有效性
            LocalUser user = verifyToken(accessToken);
            if (user == null) {
                return false;
            }
            
            // 2. 调用统一用户中心权限验证接口
            return userCenterApiService.verifyUserPermission(accessToken, serviceName, pauth, auth, doAction);
            
        } catch (Exception e) {
            log.error("权限验证失败", e);
            return false;
        }
    }

    /**
     * Sync user info to local
     */
    private LocalUser syncUserToLocal(UserCenterUser userInfo, String clientIp, String userAgent) {
        try {
            // 先按user_id查询
            LocalUser localUser = localUserMapper.selectOne(
                new QueryWrapper<LocalUser>().eq("user_id", userInfo.getUserId())
            );

            if (localUser == null) {
                // 再按login_id查询（防止重复）
                localUser = localUserMapper.selectOne(
                    new QueryWrapper<LocalUser>().eq("login_id", userInfo.getLoginId())
                );
            }

            if (localUser == null) {
                // 创建新用户
                localUser = new LocalUser();
                localUser.setUserId(userInfo.getUserId());
                localUser.setTenantId(userInfo.getTenantId());
                localUser.setLoginId(userInfo.getLoginId());
                localUser.setUserName(userInfo.getUserName());
                localUser.setLoginTime(parseDateTime(userInfo.getLoginTime()));
                localUser.setLoginIp(clientIp);
                localUser.setLoginType(userInfo.getLoginType());
                localUser.setClient(userInfo.getClient());
                localUser.setAccessToken(userInfo.getAccessToken());
                localUser.setTokenExpireTime(calculateTokenExpireTime());
                localUser.setStatus(1);
                localUser.setForm(4); // 来自统一用户中心

                localUserMapper.insert(localUser);
                log.info("创建新用户成功: {}", userInfo.getUserName());
            } else {
                // 更新现有用户信息
                localUser.setUserName(userInfo.getUserName());
                localUser.setLoginTime(parseDateTime(userInfo.getLoginTime()));
                localUser.setLoginIp(clientIp);
                localUser.setAccessToken(userInfo.getAccessToken());
                localUser.setTokenExpireTime(calculateTokenExpireTime());

                localUserMapper.updateById(localUser);
                log.info("更新用户信息成功: {}", userInfo.getUserName());
            }

            return localUser;

        } catch (Exception e) {
            log.error("同步用户信息失败", e);
            throw new RuntimeException("用户信息同步失败: " + e.getMessage());
        }
    }

    /**
     * Parse date time string
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
     * Calculate token expire time (24 hours later)
     */
    private LocalDateTime calculateTokenExpireTime() {
        return LocalDateTime.now().plusHours(24);
    }

    /**
     * Get tenant list
     */
    public Map<String, Object> getTenantList(String accessToken, Boolean isChild) {
        try {
            log.info("获取租户列表，accessToken: {}, isChild: {}", accessToken, isChild);
            
            // 调用统一用户中心API获取租户列表
            return userCenterApiService.getTenantList(accessToken, isChild);
            
        } catch (Exception e) {
            log.error("获取租户列表失败", e);
            throw new RuntimeException("获取租户列表失败: " + e.getMessage());
        }
    }
} 