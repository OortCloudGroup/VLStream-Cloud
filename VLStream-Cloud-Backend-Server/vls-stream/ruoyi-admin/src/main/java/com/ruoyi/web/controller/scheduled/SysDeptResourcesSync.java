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
// * 部门同步定时任务
// */
//@Component
//@Transactional(rollbackFor = Exception.class)
//public class SysDeptResourcesSync {
//
//    @Value("${synchronise.DEPT-SYNURL}")
//    private String deptSynUrl;
//
//    @Resource
//    private SysDeptMapper sysDeptMapper; // 假设已经注入了对应的Service
//    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    @Scheduled(cron = "0 */2 * * * ?")
//    public void syncDeptData() {
//        System.out.println("开始部门数据同步：" + new Date());
//        System.out.println("部门数据同步接口为：" + deptSynUrl);
//
//        // 准备请求参数，这里你可能需要根据具体情况设置请求参数
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("oort_dcode", null);
//        requestBody.put("oort_udid", null);
//        requestBody.put("page", 1);
//        requestBody.put("pagesize", Integer.MAX_VALUE);
//        // 查询当前数据库最后一次更新的时间
//        Date latestUpdateTime = sysDeptMapper.selectLatestUpdateTime();
//        if (latestUpdateTime != null) {
//            requestBody.put("startDate", latestUpdateTime.getTime()+1000); // 将日期时间转换为时间戳并放入请求体
//        } else {
//            requestBody.put("startDate", null); // 如果latestUpdateTime为null，直接放入null
//        }
//        requestBody.put("tag", null);
//        if(latestUpdateTime == null) {
//            System.out.println("查询部门全量数据");
//        } else {
//            System.out.println("查询部门"+ sdf.format(new Date(latestUpdateTime.getTime()+1000)) + "后的增量数据");
//        }
//        // 发送 POST 请求并获取响应数据
//        JSONArray departmentList = fetchDataFromSyncAPI(requestBody);
//
//        // 处理响应数据
//        if (CollectionUtils.isNotEmpty(departmentList)) {
//            // 查询已有的所有数据的oort_udid
//            List<String> oortUdidList = sysDeptMapper.selectOortUdidList();
//            List<SysDept> sysDeptList = new ArrayList<>();
//            List<String> sysDeptUdidList = new ArrayList<>();
//            for (Object obj : departmentList) {
//                JSONObject deptJson = (JSONObject) obj;
//                SysDept sysDept = parseDeptJson(deptJson, sysDeptUdidList); // 解析 JSON 数据为 SysDept 对象
//                if (sysDept != null) {
//                    sysDeptList.add(sysDept);
//                }
//            }
//            boolean b = sysDeptMapper.insertOrUpdateBatch(sysDeptList);// 批量插入部门数据
//            // 插入部门数据后根据父部门code写入父部门id
//            if(b){
//                for (String uuid : sysDeptUdidList) {
//                    //先根据查出数据
//                    SysDept sysDept = sysDeptMapper.selectDeptByUdid(uuid);
//                    String oortPdcode = sysDept.getOortPdcode();
//                    System.out.println("oortPdcode = " + oortPdcode);
//                    Long parentId = sysDeptMapper.selectDeptIdByCode(oortPdcode);
//                    sysDept.setParentId(parentId);
//                    sysDeptMapper.updateById(sysDept);
//                }
//            }
//            System.out.println("部门数据同步成功，共同步部门数量：" + sysDeptList.size());
//        } else {
//            System.out.println("部门数据同步成功：共同步部门数量：0");
//        }
//        System.out.println("结束部门数据同步：" + sdf.format(new Date()));
//    }
//
//    private JSONArray fetchDataFromSyncAPI(JSONObject requestBody) {
//        // 发送 POST 请求
//        HttpResponse response = HttpRequest.post(deptSynUrl)
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
//    private SysDept parseDeptJson(JSONObject deptJson, List<String> sysDeptUdidList) {
//        SysDept sysDept = new SysDept();
//        sysDept.setParentId(0L);
//        sysDept.setOortUdid(deptJson.getStr("oort_udid")); // 部门id
//        sysDeptUdidList.add(deptJson.getStr("oort_udid"));
//        sysDept.setOortDcode(deptJson.getStr("oort_dcode")); // 部门code
//        sysDept.setOortPdcode(deptJson.getStr("oort_pdcode")); // 父部门code
//        sysDept.setDeptName(deptJson.getStr("oort_dname")); // 部门名称
//        sysDept.setOrderNum(deptJson.getInt("oort_dsort")); // 显示顺序
//        Date updateDate = new Date(deptJson.getLong("oort_dupdate"));
//        sysDept.setUpdateTime(updateDate); // 修改时间
//        Date createDate = new Date(deptJson.getLong("oort_dtcreate"));
//        sysDept.setCreateTime(createDate); // 创建时间
//        sysDept.setStatus("0".equals(deptJson.getStr("oort_status")) ? "1" : "0" );
//        return sysDept;
//    }
//}
