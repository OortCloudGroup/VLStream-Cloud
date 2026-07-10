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
// * 用户同步定时任务
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
//        System.out.println("开始用户数据同步：" + new Date());
//        System.out.println("用户数据同步接口为：" + userSynUrl);
//
//        // 准备请求参数，这里你可能需要根据具体情况设置请求参数
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("oort_dcode", null);
//        requestBody.put("oort_udid", null);
//        requestBody.put("page", 1);
//        requestBody.put("pagesize", Integer.MAX_VALUE);
//        // 查询当前数据库最后一次更新的时间
//        Date latestUpdateTime = sysUserMapper.selectLatestUpdateTime();
//
//        if (latestUpdateTime != null) {
//            requestBody.put("startDate", latestUpdateTime.getTime()+1000); // 将日期时间转换为时间戳并放入请求体
//        } else {
//            requestBody.put("startDate", null); // 如果latestUpdateTime为null，直接放入null
//        }
//        if(latestUpdateTime == null) {
//            System.out.println("查询用户全量数据");
//        } else {
//            System.out.println("查询用户"+ sdf.format(new Date(latestUpdateTime.getTime()+1000)) + "后的增量数据");
//        }
//        requestBody.put("tag", null);
//
//        // 发送 POST 请求并获取响应数据
//        JSONArray departmentList = fetchDataFromSyncAPI(requestBody);
//
//        // 处理响应数据
//        if (CollectionUtils.isNotEmpty(departmentList)) {
//            List<SysUser> sysUserList = new ArrayList<>();
//            for (Object obj : departmentList) {
//                JSONObject deptJson = (JSONObject) obj;
//                SysUser sysUser = parseUserJson(deptJson); // 解析 JSON 数据为 SysDept 对象
//                if (sysUser != null) {
//                    sysUserList.add(sysUser);
//                }
//                if(sysUser.getNickName().equals("cz")){
//                    System.out.println("sysUser = " + sysUser);
//                }
//            }
//            sysUserMapper.insertOrUpdateBatch(sysUserList); // 批量插入用户数据
//            // 为用户添加角色
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
//            System.out.println("用户数据同步成功，共同步用户数量：" + sysUserList.size());
//        } else {
//            System.out.println("用户数据同步成功：共同步用户数量：0");
//        }
//
//        System.out.println("结束用户数据同步：" + sdf.format(new Date()));
//    }
//
//    private JSONArray fetchDataFromSyncAPI(JSONObject requestBody) {
//        // 发送 POST 请求
//        HttpResponse response = HttpRequest.post(userSynUrl)
//            .header("accept", "application/json")
//            .header("Content-Type", "application/json")
//            .body(requestBody.toString())
//            .execute();
//
//        // 处理响应
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
//        sysUser.setSex(String.valueOf(deptJson.getInt("oort_sex"))); // 性别
//        sysUser.setPhonenumber(deptJson.getStr("oort_phone")); // 手机号
//        sysUser.setNickName(deptJson.getStr("oort_name")); // 部门名称
//        sysUser.setUserName(deptJson.getStr("oort_loginid")); // 部门名称
//        sysUser.setIdcard(deptJson.getStr("oort_idcard")); // 身份证
//        Date updateDate = new Date(deptJson.getLong("oort_tupdate"));
//        sysUser.setUpdateTime(updateDate); // 修改时间
//        Date createDate = new Date(deptJson.getLong("oort_tcreate"));
//        sysUser.setCreateTime(createDate); // 创建时间
//        sysUser.setDelFlag(deptJson.getInt("oort_tdelete") == 0 ? "0" : "2");
//        sysUser.setStatus("0".equals(deptJson.getStr("oort_status")) ? "1" : "0" ); // 状态
//        sysUser.setOortIspart(deptJson.getStr("oort_ispart")); // 1:兼职 0:正职
//        sysUser.setOortJobname(deptJson.getStr("oort_jobname")); // 状态
//        sysUser.setPassword("$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2");
//        sysUser.setDeptInfo(deptJson.getStr("deptinfo")); // 状态
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
//                    throw new RuntimeException("部门数据未同步，请等待部门数据同步");
//                }
//                sysUser.setDeptId(sysDept.getDeptId());
//            }
//        }
//        sysUser.setUserId(deptJson.getStr("oort_uuid"));
//        return sysUser;
//    }
//
//    /**
//     * 是否是uuid
//     * @param str
//     * @return
//     */
//    public static boolean isUUID(String str) {
//        // 使用正则表达式匹配 UUID 格式
//        Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
//        Matcher matcher = pattern.matcher(str.toLowerCase()); // 考虑到 UUID 可能包含大写字母，转换为小写进行匹配
//        return matcher.matches();
//    }
//
//    /**
//     * 是否是纯数字
//     * @param str
//     * @return
//     */
//    public static boolean containsNonDigit(String str) {
//        Pattern pattern = Pattern.compile("[^0-9]");
//        Matcher matcher = pattern.matcher(str);
//        return !matcher.find();
//    }
//}
