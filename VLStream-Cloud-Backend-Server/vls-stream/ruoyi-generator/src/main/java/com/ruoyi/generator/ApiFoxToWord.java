/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.generator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
@Slf4j
public class ApiFoxToWord {
    //Apifox导出的json文件
    private final static String apiFoxFile = "C:\\Users\\oort\\Desktop\\流程初始化模版.json";
    //word模板
    private final static String templateFile = "C:\\Users\\oort\\Desktop\\template.docx";
    //文件总标题
    private final static String docxTitle = "统一工单对接文档";
    //表格头颜色
    private final static String tableTitleColor = "cccccc";

    /**
     * 读取模板样式
     * @return
     * @throws Exception
     */
    public static CTStyles getTemplateStype() throws Exception {
        XWPFDocument template = new XWPFDocument(new FileInputStream(templateFile));
        return template.getStyle();
    }

    /**
     * 添加标题
     * @param document
     * @param level
     * @param title
     */
    private static void addTitle(XWPFDocument document, int level, String title) {
        log.info("添加标题：{}", title);
        if(level==0){
            XWPFParagraph paragraph = document.createParagraph();   // 创建段落对象
            paragraph.setStyle(String.valueOf(2));
            paragraph.setAlignment(ParagraphAlignment.CENTER);  // 设置段落对齐方式为居中
            XWPFRun run = paragraph.createRun();
            run.setText(title);
        } else if (level==99){
            XWPFParagraph paragraph = document.createParagraph();   // 创建段落对象
            paragraph.setStyle(String.valueOf(8));
            XWPFRun run = paragraph.createRun();
            run.setText(title);
        } else {
            XWPFParagraph paragraph = document.createParagraph();   // 创建段落对象
            paragraph.setStyle(String.valueOf(level+3));
            XWPFRun run = paragraph.createRun();
            run.setText(title);
        }
    }

    /**
     * 添加正文
     * @param document
     * @param content
     */
    private static void addContent(XWPFDocument document, String content) {
        log.info("添加正文：{}", content);
        XWPFParagraph paragraph = document.createParagraph();   // 创建段落对象
        XWPFRun run = paragraph.createRun();
        run.setText(content);
        paragraph.setStyle("1");
    }

    /**
     * 表格默认样式
     * @param table
     * @return
     */
    private static XWPFTable doTableDefaultStyle(XWPFTable table) {
        //设置单元格边距
        table.setCellMargins(100, 100, 100, 100);
        //设置表格列宽固定，不随内容改变宽度
        table.getCTTbl().getTblPr().addNewTblLayout().setType(STTblLayoutType.FIXED);
        //表格边框线条
        CTTblBorders borders = table.getCTTbl().getTblPr().addNewTblBorders();
        CTBorder hBorder = borders.addNewInsideH();
        hBorder.setVal(STBorder.Enum.forString("single"));
        hBorder.setSz(new BigInteger("10"));
        CTBorder vBorder = borders.addNewInsideV();
        vBorder.setVal(STBorder.Enum.forString("single"));
        vBorder.setSz(new BigInteger("10"));
        CTBorder lBorder = borders.addNewLeft();
        lBorder.setVal(STBorder.Enum.forString("single"));
        lBorder.setSz(new BigInteger("10"));
        CTBorder rBorder = borders.addNewRight();
        rBorder.setVal(STBorder.Enum.forString("single"));
        rBorder.setSz(new BigInteger("10"));
        CTBorder tBorder = borders.addNewTop();
        tBorder.setVal(STBorder.Enum.forString("single"));
        tBorder.setSz(new BigInteger("10"));
        CTBorder bBorder = borders.addNewBottom();
        bBorder.setVal(STBorder.Enum.forString("single"));
        bBorder.setSz(new BigInteger("10"));
        //返回表格对象
        return table;
    }

    /**
     * 表格行默认样式
     * @param row
     * @return
     */
    private static XWPFTableRow doRowDefaultStyle(XWPFTableRow row) {
        //设置表格行的样式
        return row;
    }

    /**
     * 表格单元格默认样式
     * @param cell
     * @param isTitle
     * @param content
     * @return
     */
    private static XWPFTableCell doCellDefaultStyle(XWPFTableCell cell, Integer width, int isTitle, String content) {
        //单元格上下居中
        cell.getCTTc().addNewTcPr().addNewVAlign().setVal(STVerticalJc.CENTER);
        //设置单元格宽度
        cell.getCTTc().addNewTcPr().addNewTcW().setType(STTblWidth.DXA);
        cell.getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(width));
        if (isTitle == 1) {
            cell.getCTTc().addNewTcPr().addNewShd().setFill(tableTitleColor);
        }
        //设置字体
        XWPFParagraph paragraph = cell.getParagraphArray(0);
        XWPFRun run1 = paragraph.createRun();
        run1.setFontSize(10);
        run1.setFontFamily("Calibri");
        if(StrUtil.isNotBlank(content)){
            run1.setText(content);
        }
        return cell;
    }

    private static String doConvertType(JSONObject value) {
        try {
            JSONArray type = value.getJSONArray("type");
            return String.valueOf(type.get(0));
        } catch (Exception e) {

        }
        try {
            String type = value.getStr("type");
            if("null".equals(type)){
                return "string";
            } else {
                return type;
            }
        } catch (Exception e) {

        }
        return "object";
    }

    /**
     * 计算列宽
     * @param tableTitle
     * @return
     */
    private static Integer[] doColumnWidth(String[] tableTitle){
        //计算列宽
        int totalWidth = 8000;
        int firstWidth = 4000;
        int otherWidth = (totalWidth - firstWidth) / (tableTitle.length - 1);
        return new Integer[]{firstWidth, otherWidth};
    }

    /**
     * 创建表格及表格标题
     * @param document
     * @param tableTitle
     * @param columnWidth
     * @return
     */
    private static XWPFTable createTableWithTitle(XWPFDocument document, String[] tableTitle, Integer[] columnWidth) {
        XWPFTable table = doTableDefaultStyle(document.createTable());
        XWPFTableRow row = doRowDefaultStyle(table.getRow(0));
        //添加单元格
        for (int i = 0; i < tableTitle.length; i++) {
            if (i == 0) {
                doCellDefaultStyle(row.getCell(0), columnWidth[0], 1, tableTitle[i]);
            } else {
                doCellDefaultStyle(row.addNewTableCell(), columnWidth[1], 1, tableTitle[i]);
            }
        }
        return table;
    }

    /**
     * 请求消息体数据
     * @param table
     * @param topParamsCode
     * @param root
     */
    public static void doBodyParamsData(XWPFTable table, String topParamsCode, Integer[] columnWidth, JSONObject root) {
        if (null == root) {
            return;
        }
        Set<Map.Entry<String, Object>> properties = root.entrySet();
        if (properties.size() == 0) {
            table.getRow(0).getTableCells();
            //创建表格行
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "");
            //设置单元格合并
            cell1.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            cell2.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            cell3.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            cell4.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            return;
        }
        for(Map.Entry<String, Object> propertie : properties){
            String paramsCode = null;
            if(StrUtil.isBlank(topParamsCode)){
                paramsCode = propertie.getKey();
            } else {
                paramsCode = topParamsCode + "." + propertie.getKey();
            }
            log.info("添加请求体参数：{}", paramsCode);
            JSONObject value = JSONUtil.parseObj(propertie.getValue());
            String type = doConvertType(value);

            String description = value.getStr("description");
            //创建表格行
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            //添加单元格
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, paramsCode);
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, description);
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, type);
            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "N");
            //递归处理
            if(value.containsKey("properties")){
                JSONObject childProperties = value.getJSONObject("properties");
                doBodyParamsData(table, paramsCode, columnWidth, childProperties);
            }
            if(value.containsKey("items")){
                JSONObject childProperties = value.getJSONObject("items").getJSONObject("properties");
                doBodyParamsData(table, paramsCode, columnWidth, childProperties);
            }
        }
    }

    /**
     * 返回消息体数据
     * @param table
     * @param topParamsCode
     * @param root
     */
    private static void doBodyResponseData(XWPFTable table, String topParamsCode, Integer[] columnWidth, JSONObject root) {
        if (null == root || root.entrySet().size() == 0) {
            //创建表格行
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
            //设置单元格合并
            cell1.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            cell2.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            cell3.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            return;
        }
        Set<Map.Entry<String, Object>> properties = root.entrySet();
        for(Map.Entry<String, Object> propertie : properties){
            String paramsCode = null;
            if(StrUtil.isBlank(topParamsCode)){
                paramsCode = propertie.getKey();
            } else {
                paramsCode = topParamsCode + "." + propertie.getKey();
            }
            log.info("添加返回参数：{}", paramsCode);
            JSONObject value = JSONUtil.parseObj(propertie.getValue());
            String type = doConvertType(value);

            String description = value.getStr("description");
            //创建表格行
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            //添加单元格
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, paramsCode);
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, description);
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, type);

            //递归处理
            if(value.containsKey("properties")){
                JSONObject childProperties = value.getJSONObject("properties");
                doBodyResponseData(table, paramsCode, columnWidth, childProperties);
            }
            if(value.containsKey("items")){
                JSONObject childProperties = value.getJSONObject("items").getJSONObject("properties");
                doBodyResponseData(table, paramsCode, columnWidth, childProperties);
            }
        }
    }

    public static void doTitleItems(XWPFDocument document, int level, String topIndex, JSONArray root) {
        if (null == root || root.size() <= 0) {
            return;
        }
        for (int i = 0; i < root.size(); i++) {
            JSONObject apiCollection = JSONUtil.parseObj(root.get(i));
            if(apiCollection.containsKey("api")){   //如果包含“api”说明是接口
                //添加标题
                String title = "（"+(i+1)+"）"+ apiCollection.getStr("name");
                addTitle(document, 99, title);
                //添加正文接口描述
                JSONObject api = apiCollection.getJSONObject("api");
                String path = api.getStr("path");
                addContent(document, "接口地址：" + path);
                String method = api.getStr("method");
                addContent(document, "请求方式：" + method);
                JSONObject requestBody = api.getJSONObject("requestBody");
                String requestType = requestBody.getStr("type");
                addContent(document, "Content-Type：" + requestType);

                //添加接口Header参数
                if (api.containsKey("parameters")) {
                    JSONObject parameters = api.getJSONObject("parameters");
                    if(parameters.containsKey("header")){
                        addContent(document, "请求头参数说明：");
                        //获取header信息
                        JSONArray header = parameters.getJSONArray("header");
                        //创建表格
                        String tableTitle[] = {"参数名", "参数描述", "参数类型", "是否必填"};
                        Integer[] columnWidth = doColumnWidth(tableTitle);
                        XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                        if (header.size() == 0) {
                            //创建表格行
                            XWPFTableRow row = doRowDefaultStyle(table.createRow());
                            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
                            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
                            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
                            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "");
                            //设置单元格合并
                            cell1.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
                            cell2.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                            cell3.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                            cell4.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                        } else {
                            for(Object obj : header){
                                JSONObject headerOne = JSONUtil.parseObj(obj);
                                String paramsCode = headerOne.getStr("name");
                                String description = headerOne.getStr("description");
                                String type = headerOne.getStr("type");
                                String required = headerOne.getStr("required");
                                if("true".equals(required)){
                                    required = "Y";
                                } else {
                                    required = "N";
                                }
                                //创建表格行
                                XWPFTableRow rowHeader = doRowDefaultStyle(table.createRow());
                                //添加单元格
                                XWPFTableCell cellHeader1 = doCellDefaultStyle(rowHeader.getCell(0), columnWidth[0], 0, paramsCode);
                                XWPFTableCell cellHeader2 = doCellDefaultStyle(rowHeader.getCell(1), columnWidth[1],0, description);
                                XWPFTableCell cellHeader3 = doCellDefaultStyle(rowHeader.getCell(2), columnWidth[1],0, type);
                                XWPFTableCell cellHeader4 = doCellDefaultStyle(rowHeader.getCell(3), columnWidth[1],0, required);
                            }
                        }
                        //空一行
                        addContent(document, "");
                    }
                    if(parameters.containsKey("query")){
                        addContent(document, "URI参数说明：");
                        //获取query信息
                        JSONArray query = parameters.getJSONArray("query");
                        //创建表格
                        String tableTitle[] = {"参数名", "参数描述", "参数类型", "是否必填"};
                        Integer[] columnWidth = doColumnWidth(tableTitle);
                        XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                        if (query.size() == 0) {
                            //创建表格行
                            XWPFTableRow row = doRowDefaultStyle(table.createRow());
                            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
                            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
                            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
                            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "");
                            //设置单元格合并
                            cell1.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
                            cell2.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                            cell3.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                            cell4.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                        } else {
                            for(Object obj : query){
                                JSONObject queryOne = JSONUtil.parseObj(obj);
                                String paramsCode = queryOne.getStr("name");
                                String description = queryOne.getStr("description");
                                String type = queryOne.getStr("type");
                                String required = queryOne.getStr("required");
                                if("true".equals(required)){
                                    required = "Y";
                                } else {
                                    required = "N";
                                }
                                //创建表格行
                                XWPFTableRow rowQuery = doRowDefaultStyle(table.createRow());
                                //添加单元格
                                XWPFTableCell cellHeader1 = doCellDefaultStyle(rowQuery.getCell(0), columnWidth[0],0, paramsCode);
                                XWPFTableCell cellHeader2 = doCellDefaultStyle(rowQuery.getCell(1), columnWidth[1],0, description);
                                XWPFTableCell cellHeader3 = doCellDefaultStyle(rowQuery.getCell(2), columnWidth[1],0, type);
                                XWPFTableCell cellHeader4 = doCellDefaultStyle(rowQuery.getCell(3), columnWidth[1],0, required);
                            }
                        }
                        //空一行
                        addContent(document, "");
                    }
                }
                //添加接口请求消息体参数
                if (requestType.indexOf("json") > 0) {
                    addContent(document, "请求体参数说明：");
                    //如果是json请求
                    if(requestBody.containsKey("jsonSchema")){
                        JSONObject jsonSchema = requestBody.getJSONObject("jsonSchema");
                        JSONObject properties = jsonSchema.getJSONObject("properties");
                        if (null != properties) {
                            //创建表格
                            String tableTitle[] = {"参数名", "参数描述", "参数类型", "是否必填"};
                            Integer[] columnWidth = doColumnWidth(tableTitle);
                            XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                            //递归处理参数
                            doBodyParamsData(table, null, columnWidth, properties);
                            //空一行
                            addContent(document, "");
                        }
                    }
                }
                //添加接口返回参数
                if (api.containsKey("responses")) {
                    JSONArray responses = api.getJSONArray("responses");
                    for(Object response : responses){

                        JSONObject responseOne = JSONUtil.parseObj(response);
                        //写返回参数说明
                        addContent(document, "返回参数说明：（"+responseOne.getStr("code")+"）"+responseOne.getStr("name")+"");
                        //创建表格
                        String tableTitle[] = {"参数名", "参数描述", "参数类型"};
                        Integer[] columnWidth = doColumnWidth(tableTitle);
                        XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                        //设置表格默认样式
                        doTableDefaultStyle(table);

                        JSONObject properties = responseOne.getJSONObject("jsonSchema").getJSONObject("properties");
                        //递归处理参数
                        doBodyResponseData(table, null, columnWidth, properties);
                        //空一行
                        addContent(document, "");
                    }
                }
            } else if(apiCollection.containsKey("items")){ //如果包含“items”说明是文件夹
                //设置序号
                String currentIndex = null;
                if(StrUtil.isBlank(topIndex)){
                    currentIndex = (i+1) + "";
                } else {
                    currentIndex = topIndex + "." + (i+1);
                }
                //添加标题
                String title = currentIndex + " " + apiCollection.getStr("name");
                addTitle(document, level, title);
                //如果包含“items”说明是文件夹
                JSONArray items = apiCollection.getJSONArray("items");
                //递归处理数据
                doTitleItems(document, level + 1, currentIndex, items);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String json = IoUtil.read(FileUtil.getInputStream(apiFoxFile)).toString();
        JSONObject apifoxProject = JSONUtil.parseObj(json);
        JSONObject info = apifoxProject.getJSONObject("info");
        JSONArray apiCollections = apifoxProject.getJSONArray("paths");
        //定义层级
        int level= 0;
        //创建word文档
        XWPFDocument document = new XWPFDocument();
        //设置文档的样式为读取的模板样式
        XWPFStyles styles = document.createStyles();
        styles.setStyles(getTemplateStype());
        //设置文章标题
        addTitle(document, level, docxTitle);

        //设置文档内容
        for (int i = 0; i < apiCollections.size(); i++) {
            JSONArray items = JSONUtil.parseObj(apiCollections.get(i)).getJSONArray("items");
            //递归处理数据
            doTitleItems(document, level + 1, null, items);
        }
        //输出文档
        FileOutputStream fos = new FileOutputStream("C:/my_test/oort/Desktop/" + info.getStr("name") + ".docx");
        // 将文档写入输出流
        document.write(fos);
    }
}
