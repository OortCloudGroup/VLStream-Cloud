/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

//package com.ruoyi.web.controller.scheduled;
//
//import cn.hutool.http.HttpRequest;
//import cn.hutool.http.HttpResponse;
//import cn.hutool.json.JSONArray;
//import cn.hutool.json.JSONObject;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
//import com.ruoyi.common.constant.UserConstants;
//import com.ruoyi.common.core.domain.entity.SysDept;
//import com.ruoyi.common.core.domain.entity.SysUser;
//import com.ruoyi.system.mapper.SysDeptMapper;
//import com.ruoyi.system.mapper.SysUserMapper;
//import com.ruoyi.system.service.impl.SysUserServiceImpl;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.dao.DuplicateKeyException;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.Resource;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//**
// * user task
// */
//@Component
//@Transactional(rollbackFor = Exception.class)
//public class SysUserResourcesSync {
//
//    @Value("${synchronise.USER-SYNURL}")
//    private String userSynUrl;
//
//    @Resource
//    private SysUserMapper sysUserMapper;
//    @Resource
//    private SysDeptMapper sysDeptMapper;
//    @Resource
//    private SysUserServiceImpl sysUserService;
//    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//   // @Scheduled(cron = "0 */2 * * * ?")
//    public void syncDeptData() {
// System.out.println("startuserdata : " + new Date());
// System.out.println("userdata interface to : " + userSynUrl);
//
// // parameter, can need to Set parameter
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("oort_dcode", null);
//        requestBody.put("oort_udid", null);
//        requestBody.put("page", 1);
//        requestBody.put("pagesize", Integer.MAX_VALUE);
// // Query current data after new
//        Date latestUpdateTime = sysUserMapper.selectLatestUpdateTime();
//
//        if (latestUpdateTime != null) {
// requestBody.put("startDate", latestUpdateTime.getTime()+1000); // Convert to
//        } else {
// requestBody.put("startDate", null); // if latestUpdateTime to null, null
//        }
//        if(latestUpdateTime == null) {
// System.out.println("Query user full data");
//        } else {
// System.out.println("Query user"+ sdf.format(new Date(latestUpdateTime.getTime()+1000)) + " after data");
//        }
//        requestBody.put("tag", null);
//
// // POST Get data
//        JSONArray departmentList = fetchDataFromSyncAPI(requestBody);
//
// // Process data
//        if (CollectionUtils.isNotEmpty(departmentList)) {
//            List<SysUser> sysUserList = new ArrayList<>();
//            for (Object obj : departmentList) {
//                JSONObject deptJson = (JSONObject) obj;
// SysUser sysUser = parseUserJson(deptJson); // Parse JSON data to SysDept object
//                if (sysUser != null) {
//                    sysUserList.add(sysUser);
//                }
//                if(sysUser.getNickName().equals("cz")){
//                    System.out.println("sysUser = " + sysUser);
//                }
//            }
// sysUserMapper.insertOrUpdateBatch(sysUserList); // userdata
// // to user role
//            for (SysUser sysUser : sysUserList) {
//                String userId = sysUser.getUserId();
//                Long[] roleIds = {UserConstants.COMMON_ID};
//                try{
//                    if("admin".equals(sysUser.getUserName())) {
//                        roleIds = new Long[]{UserConstants.AD_ID};
//                    }
//                    sysUserService.insertUserRole(userId, roleIds);
//                } catch (DuplicateKeyException e){
//                    continue;
//                }
//
//            }
// System.out.println("userdata successfully, user : " + sysUserList.size());
//        } else {
// System.out.println("userdata successfully: user : 0");
//        }
//
// System.out.println("finishuserdata : " + sdf.format(new Date()));
//    }
//
//    private JSONArray fetchDataFromSyncAPI(JSONObject requestBody) {
// // POST
//        HttpResponse response = HttpRequest.post(userSynUrl)
//            .header("accept", "application/json")
//            .header("Content-Type", "application/json")
//            .body(requestBody.toString())
//            .execute();
//
// // Process
//        String responseBody = response.body();
//        JSONObject jsonObject = new JSONObject(responseBody);
//        JSONObject data = jsonObject.getJSONObject("data");
//        JSONArray departmentList = data.getJSONArray("list");
//        departmentList.forEach(System.out::println);
//        return departmentList;
//    }
//
//    private SysUser parseUserJson(JSONObject deptJson) {
//        SysUser sysUser = new SysUser();
//        sysUser.setOortUuid(deptJson.getStr("oort_uuid")); // uuid
// sysUser.setSex(String.valueOf(deptJson.getInt("oort_sex"))); //
// sysUser.setPhonenumber(deptJson.getStr("oort_phone")); //
// sysUser.setNickName(deptJson.getStr("oort_name")); // department name
// sysUser.setUserName(deptJson.getStr("oort_loginid")); // department name
// sysUser.setIdcard(deptJson.getStr("oort_idcard")); //
//        Date updateDate = new Date(deptJson.getLong("oort_tupdate"));
// sysUser.setUpdateTime(updateDate); // Update
//        Date createDate = new Date(deptJson.getLong("oort_tcreate"));
// sysUser.setCreateTime(createDate); // create time
//        sysUser.setDelFlag(deptJson.getInt("oort_tdelete") == 0 ? "0" : "2");
// sysUser.setStatus("0".equals(deptJson.getStr("oort_status")) ? "1" : "0" ); //
// sysUser.setOortIspart(deptJson.getStr("oort_ispart")); // 1: 0:
// sysUser.setOortJobname(deptJson.getStr("oort_jobname")); //
//        sysUser.setPassword("$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2");
// sysUser.setDeptInfo(deptJson.getStr("deptinfo")); //
//        JSONArray deptinfo = deptJson.getJSONArray("deptinfo");
//        for (Object o : deptinfo) {
//            JSONObject dept = (JSONObject) o;
//            String oortUdid = dept.getStr("oort_udid");
//            sysUser.setOortUdid(oortUdid);
//            if(oortUdid != null) {
//                QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
//                queryWrapper.eq("oort_udid", oortUdid);
//                SysDept sysDept = sysDeptMapper.selectOne(queryWrapper);
//                if(sysDept == null) {
// throw new RuntimeException("departmentdata not , etc. departmentdata ");
//                }
//                sysUser.setDeptId(sysDept.getDeptId());
//            }
//        }
//        sysUser.setUserId(deptJson.getStr("oort_uuid"));
//        return sysUser;
//    }
//
//    /**
// * whether is uuid
//     * @param str
//     * @return
//     */
//    public static boolean isUUID(String str) {
// // UUID
//        Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
// Matcher matcher = pattern.matcher(str.toLowerCase()); // UUID can , Convert to
//        return matcher.matches();
//    }
//
//    /**
// * whether is
//     * @param str
//     * @return
//     */
//    public static boolean containsNonDigit(String str) {
//        Pattern pattern = Pattern.compile("[^0-9]");
//        Matcher matcher = pattern.matcher(str);
//        return !matcher.find();
//    }
//}
