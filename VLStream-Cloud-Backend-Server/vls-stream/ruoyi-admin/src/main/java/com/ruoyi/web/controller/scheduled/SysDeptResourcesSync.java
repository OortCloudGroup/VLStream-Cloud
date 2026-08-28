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
//import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
//import com.ruoyi.common.core.domain.entity.SysDept;
//import com.ruoyi.system.mapper.SysDeptMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.Resource;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
// * department task
// */
//@Component
//@Transactional(rollbackFor = Exception.class)
//public class SysDeptResourcesSync {
//
//    @Value("${synchronise.DEPT-SYNURL}")
//    private String deptSynUrl;
//
//    @Resource
// private SysDeptMapper sysDeptMapper; // assuming already Service
//    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    @Scheduled(cron = "0 */2 * * * ?")
//    public void syncDeptData() {
// System.out.println("startdepartmentdata : " + new Date());
// System.out.println("departmentdata interface to : " + deptSynUrl);
//
// // parameter, can need to Set parameter
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("oort_dcode", null);
//        requestBody.put("oort_udid", null);
//        requestBody.put("page", 1);
//        requestBody.put("pagesize", Integer.MAX_VALUE);
// // Query current data after new
//        Date latestUpdateTime = sysDeptMapper.selectLatestUpdateTime();
//        if (latestUpdateTime != null) {
// requestBody.put("startDate", latestUpdateTime.getTime()+1000); // Convert to
//        } else {
// requestBody.put("startDate", null); // if latestUpdateTime to null, null
//        }
//        requestBody.put("tag", null);
//        if(latestUpdateTime == null) {
// System.out.println("Query department full data");
//        } else {
// System.out.println("Query department"+ sdf.format(new Date(latestUpdateTime.getTime()+1000)) + " after data");
//        }
// // POST Get data
//        JSONArray departmentList = fetchDataFromSyncAPI(requestBody);
//
// // Process data
//        if (CollectionUtils.isNotEmpty(departmentList)) {
// // Query already all data oort_udid
//            List<String> oortUdidList = sysDeptMapper.selectOortUdidList();
//            List<SysDept> sysDeptList = new ArrayList<>();
//            List<String> sysDeptUdidList = new ArrayList<>();
//            for (Object obj : departmentList) {
//                JSONObject deptJson = (JSONObject) obj;
// SysDept sysDept = parseDeptJson(deptJson, sysDeptUdidList); // Parse JSON data to SysDept object
//                if (sysDept != null) {
//                    sysDeptList.add(sysDept);
//                }
//            }
// boolean b = sysDeptMapper.insertOrUpdateBatch(sysDeptList);// departmentdata
// // departmentdata after departmentcode department ID
//            if(b){
//                for (String uuid : sysDeptUdidList) {
// // data
//                    SysDept sysDept = sysDeptMapper.selectDeptByUdid(uuid);
//                    String oortPdcode = sysDept.getOortPdcode();
//                    System.out.println("oortPdcode = " + oortPdcode);
//                    Long parentId = sysDeptMapper.selectDeptIdByCode(oortPdcode);
//                    sysDept.setParentId(parentId);
//                    sysDeptMapper.updateById(sysDept);
//                }
//            }
// System.out.println("departmentdata successfully, department : " + sysDeptList.size());
//        } else {
// System.out.println("departmentdata successfully: department : 0");
//        }
// System.out.println("finishdepartmentdata : " + sdf.format(new Date()));
//    }
//
//    private JSONArray fetchDataFromSyncAPI(JSONObject requestBody) {
// // POST
//        HttpResponse response = HttpRequest.post(deptSynUrl)
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
//    private SysDept parseDeptJson(JSONObject deptJson, List<String> sysDeptUdidList) {
//        SysDept sysDept = new SysDept();
//        sysDept.setParentId(0L);
// sysDept.setOortUdid(deptJson.getStr("oort_udid")); // department ID
//        sysDeptUdidList.add(deptJson.getStr("oort_udid"));
// sysDept.setOortDcode(deptJson.getStr("oort_dcode")); // departmentcode
// sysDept.setOortPdcode(deptJson.getStr("oort_pdcode")); // departmentcode
// sysDept.setDeptName(deptJson.getStr("oort_dname")); // department name
// sysDept.setOrderNum(deptJson.getInt("oort_dsort")); //
//        Date updateDate = new Date(deptJson.getLong("oort_dupdate"));
// sysDept.setUpdateTime(updateDate); // Update
//        Date createDate = new Date(deptJson.getLong("oort_dtcreate"));
// sysDept.setCreateTime(createDate); // create time
//        sysDept.setStatus("0".equals(deptJson.getStr("oort_status")) ? "1" : "0" );
//        return sysDept;
//    }
//}
