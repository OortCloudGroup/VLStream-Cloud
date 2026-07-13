//package com.ruoyi.web.controller.scheduled;
//
//import cn.hutool.http.HttpRequest;
//import cn.hutool.http.HttpResponse;
//import cn.hutool.json.JSONArray;
//import cn.hutool.json.JSONObject;
//import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
//import com.ruoyi.common.core.domain.entity.SysJobLevel;
//import com.ruoyi.system.mapper.SysJobLevelMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.Resource;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//**
// * 部门同步定时任务
// */
//@Component
//@Transactional(rollbackFor = Exception.class)
//public class SysJobResourcesSync {
//
//    @Value("${synchronise.JOB-SYNURL}")
//    private String jobSynUrl;
//
//    @Resource
//    private SysJobLevelMapper sysJobLevelMapper; // 假设已经注入了对应的Service
//    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    //@Scheduled(cron = "0 */2 * * * ?")
//    public void syncDeptData() {
//        System.out.println("开始职务等级数据同步：" + new Date());
//        System.out.println("部门数据同步接口为：" + jobSynUrl);
//
//        // 准备请求参数，这里你可能需要根据具体情况设置请求参数
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("oort_dcode", null);
//        requestBody.put("oort_udid", null);
//        requestBody.put("page", 1);
//        requestBody.put("pagesize", Integer.MAX_VALUE);
//        // 查询当前数据库最后一次更新的时间
//        Long latestUpdateTime = sysJobLevelMapper.selectLatestUpdateTime();
//        if (latestUpdateTime != null) {
//            requestBody.put("startDate", latestUpdateTime+1); // 将日期时间转换为时间戳并放入请求体
//        } else {
//            requestBody.put("startDate", null); // 如果latestUpdateTime为null，直接放入null
//        }
//        requestBody.put("tag", null);
//        if(latestUpdateTime == null) {
//            System.out.println("查询职务等级全量数据");
//        } else {
//            System.out.println("查询职务等级"+ sdf.format(new Date(latestUpdateTime+1)) + "后的增量数据");
//        }
//        // 发送 POST 请求并获取响应数据
//        JSONArray jobLevelList = fetchDataFromSyncAPI(requestBody);
//
//        // 处理响应数据
//        if (CollectionUtils.isNotEmpty(jobLevelList)) {
//            // 查询已有的所有数据的oort_udid
//            List<String> oortUdidList = sysDeptMapper.selectOortUdidList();
//            List<SysJobLevel> sysJobLevelList = new ArrayList<>();
//            for (Object obj : jobLevelList) {
//                JSONObject deptJson = (JSONObject) obj;
//                SysJobLevel sysJobLevel = parseDeptJson(deptJson); // 解析 JSON 数据为 SysDept 对象
//                if (sysJobLevel != null) {
//                    sysJobLevelList.add(sysJobLevel);
//                }
//            }
//            boolean b = sysJobLevelMapper.insertOrUpdateBatch(sysJobLevelList);// 批量插入部门数据
//            System.out.println("职务等级据同步成功，共同步职务等级数量：" + sysJobLevelList.size());
//        } else {
//            System.out.println("职务等级数据同步成功：共同步职务等级数量：0");
//        }
//        System.out.println("结束职务等级数据同步：" + sdf.format(new Date()));
//    }
//
//    private JSONArray fetchDataFromSyncAPI(JSONObject requestBody) {
//        // 发送 POST 请求
//        HttpResponse response = HttpRequest.post(jobSynUrl)
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
//    private SysJobLevel parseDeptJson(JSONObject deptJson) {
//        String oortJobname = deptJson.getStr("oort_jobname");
//        Integer oortLevel = deptJson.getInt("oort_level");
//        Long oortTcreate = deptJson.getLong("oort_tcreate");
//        Long oortTupdate = deptJson.getLong("oort_tupdate");
//        Integer oortTdelete = deptJson.getInt("oort_tdelete");
//        SysJobLevel sysJobLevel = new SysJobLevel();
//        sysJobLevel.setOortJobname(oortJobname);
//        sysJobLevel.setOortLevel(oortLevel);
//        sysJobLevel.setOortTcreate(oortTcreate);
//        sysJobLevel.setOortTupdate(oortTupdate);
//        sysJobLevel.setOortTdelete(oortTdelete);
//        return sysJobLevel;
//    }
//}
