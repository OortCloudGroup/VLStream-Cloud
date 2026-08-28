/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

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
// * department task
// */
//@Component
//@Transactional(rollbackFor = Exception.class)
//public class SysJobResourcesSync {
//
//    @Value("${synchronise.JOB-SYNURL}")
//    private String jobSynUrl;
//
//    @Resource
// private SysJobLevelMapper sysJobLevelMapper; // assuming already Service
//    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    //@Scheduled(cron = "0 */2 * * * ?")
//    public void syncDeptData() {
// System.out.println("start etc. data : " + new Date());
// System.out.println("departmentdata interface to : " + jobSynUrl);
//
// // parameter, can need to Set parameter
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("oort_dcode", null);
//        requestBody.put("oort_udid", null);
//        requestBody.put("page", 1);
//        requestBody.put("pagesize", Integer.MAX_VALUE);
// // Query current data after new
//        Long latestUpdateTime = sysJobLevelMapper.selectLatestUpdateTime();
//        if (latestUpdateTime != null) {
// requestBody.put("startDate", latestUpdateTime+1); // Convert to
//        } else {
// requestBody.put("startDate", null); // if latestUpdateTime to null, null
//        }
//        requestBody.put("tag", null);
//        if(latestUpdateTime == null) {
// System.out.println("Query etc. full data");
//        } else {
// System.out.println("Query etc. "+ sdf.format(new Date(latestUpdateTime+1)) + " after data");
//        }
// // POST Get data
//        JSONArray jobLevelList = fetchDataFromSyncAPI(requestBody);
//
// // Process data
//        if (CollectionUtils.isNotEmpty(jobLevelList)) {
// // Query already all data oort_udid
//            List<String> oortUdidList = sysDeptMapper.selectOortUdidList();
//            List<SysJobLevel> sysJobLevelList = new ArrayList<>();
//            for (Object obj : jobLevelList) {
//                JSONObject deptJson = (JSONObject) obj;
// SysJobLevel sysJobLevel = parseDeptJson(deptJson); // Parse JSON data to SysDept object
//                if (sysJobLevel != null) {
//                    sysJobLevelList.add(sysJobLevel);
//                }
//            }
// boolean b = sysJobLevelMapper.insertOrUpdateBatch(sysJobLevelList);// departmentdata
// System.out.println(" etc. successfully, etc. : " + sysJobLevelList.size());
//        } else {
// System.out.println(" etc. data successfully: etc. : 0");
//        }
// System.out.println("finish etc. data : " + sdf.format(new Date()));
//    }
//
//    private JSONArray fetchDataFromSyncAPI(JSONObject requestBody) {
// // POST
//        HttpResponse response = HttpRequest.post(jobSynUrl)
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
