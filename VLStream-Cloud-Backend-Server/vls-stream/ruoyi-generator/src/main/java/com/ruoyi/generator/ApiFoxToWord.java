/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
    // ApifoxExport json
    private final static String apiFoxFile = "C:\\Users\\oort\\Desktop\\流程初始化模版.json";
    // word
    private final static String templateFile = "C:\\Users\\oort\\Desktop\\template.docx";
    //
    private final static String docxTitle = "统一工单对接文档";
    // table
    private final static String tableTitleColor = "cccccc";

    /**
     *
     * @return
     * @throws Exception
     */
    public static CTStyles getTemplateStype() throws Exception {
        XWPFDocument template = new XWPFDocument(new FileInputStream(templateFile));
        return template.getStyle();
    }

    /**
     *
     * @param document
     * @param level
     * @param title
     */
    private static void addTitle(XWPFDocument document, int level, String title) {
        log.info("添加标题：{}", title);
        if(level==0){
            XWPFParagraph paragraph = document.createParagraph();   // object
            paragraph.setStyle(String.valueOf(2));
            paragraph.setAlignment(ParagraphAlignment.CENTER);  // Set to in
            XWPFRun run = paragraph.createRun();
            run.setText(title);
        } else if (level==99){
            XWPFParagraph paragraph = document.createParagraph();   // object
            paragraph.setStyle(String.valueOf(8));
            XWPFRun run = paragraph.createRun();
            run.setText(title);
        } else {
            XWPFParagraph paragraph = document.createParagraph();   // object
            paragraph.setStyle(String.valueOf(level+3));
            XWPFRun run = paragraph.createRun();
            run.setText(title);
        }
    }

    /**
     *
     * @param document
     * @param content
     */
    private static void addContent(XWPFDocument document, String content) {
        log.info("添加正文：{}", content);
        XWPFParagraph paragraph = document.createParagraph();   // object
        XWPFRun run = paragraph.createRun();
        run.setText(content);
        paragraph.setStyle("1");
    }

    /**
     * table
     * @param table
     * @return
     */
    private static XWPFTable doTableDefaultStyle(XWPFTable table) {
        // Set
        table.setCellMargins(100, 100, 100, 100);
        // Set table ,
        table.getCTTbl().getTblPr().addNewTblLayout().setType(STTblLayoutType.FIXED);
        // table
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
        // tableobject
        return table;
    }

    /**
     * table
     * @param row
     * @return
     */
    private static XWPFTableRow doRowDefaultStyle(XWPFTableRow row) {
        // Set table
        return row;
    }

    /**
     * table
     * @param cell
     * @param isTitle
     * @param content
     * @return
     */
    private static XWPFTableCell doCellDefaultStyle(XWPFTableCell cell, Integer width, int isTitle, String content) {
        // in
        cell.getCTTc().addNewTcPr().addNewVAlign().setVal(STVerticalJc.CENTER);
        // Set
        cell.getCTTc().addNewTcPr().addNewTcW().setType(STTblWidth.DXA);
        cell.getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(width));
        if (isTitle == 1) {
            cell.getCTTc().addNewTcPr().addNewShd().setFill(tableTitleColor);
        }
        // Set
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
     *
     * @param tableTitle
     * @return
     */
    private static Integer[] doColumnWidth(String[] tableTitle){
        //
        int totalWidth = 8000;
        int firstWidth = 4000;
        int otherWidth = (totalWidth - firstWidth) / (tableTitle.length - 1);
        return new Integer[]{firstWidth, otherWidth};
    }

    /**
     * table table
     * @param document
     * @param tableTitle
     * @param columnWidth
     * @return
     */
    private static XWPFTable createTableWithTitle(XWPFDocument document, String[] tableTitle, Integer[] columnWidth) {
        XWPFTable table = doTableDefaultStyle(document.createTable());
        XWPFTableRow row = doRowDefaultStyle(table.getRow(0));
        //
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
     * data
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
            // table
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "");
            // Set
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
            // table
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            //
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, paramsCode);
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, description);
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, type);
            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "N");
            // Process
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
     * data
     * @param table
     * @param topParamsCode
     * @param root
     */
    private static void doBodyResponseData(XWPFTable table, String topParamsCode, Integer[] columnWidth, JSONObject root) {
        if (null == root || root.entrySet().size() == 0) {
            // table
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
            // Set
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
            // table
            XWPFTableRow row = doRowDefaultStyle(table.createRow());
            //
            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, paramsCode);
            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, description);
            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, type);

            // Process
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
            if(apiCollection.containsKey("api")){   // if "api" is interface
                //
                String title = "（"+(i+1)+"）"+ apiCollection.getStr("name");
                addTitle(document, 99, title);
                // interface
                JSONObject api = apiCollection.getJSONObject("api");
                String path = api.getStr("path");
                addContent(document, "接口地址：" + path);
                String method = api.getStr("method");
                addContent(document, "请求方式：" + method);
                JSONObject requestBody = api.getJSONObject("requestBody");
                String requestType = requestBody.getStr("type");
                addContent(document, "Content-Type：" + requestType);

                // interfaceHeaderparameter
                if (api.containsKey("parameters")) {
                    JSONObject parameters = api.getJSONObject("parameters");
                    if(parameters.containsKey("header")){
                        addContent(document, "请求头参数说明：");
                        // Get headerinfo
                        JSONArray header = parameters.getJSONArray("header");
                        // table
                        String tableTitle[] = {"参数名", "参数描述", "参数类型", "是否必填"};
                        Integer[] columnWidth = doColumnWidth(tableTitle);
                        XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                        if (header.size() == 0) {
                            // table
                            XWPFTableRow row = doRowDefaultStyle(table.createRow());
                            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
                            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
                            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
                            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "");
                            // Set
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
                                // table
                                XWPFTableRow rowHeader = doRowDefaultStyle(table.createRow());
                                //
                                XWPFTableCell cellHeader1 = doCellDefaultStyle(rowHeader.getCell(0), columnWidth[0], 0, paramsCode);
                                XWPFTableCell cellHeader2 = doCellDefaultStyle(rowHeader.getCell(1), columnWidth[1],0, description);
                                XWPFTableCell cellHeader3 = doCellDefaultStyle(rowHeader.getCell(2), columnWidth[1],0, type);
                                XWPFTableCell cellHeader4 = doCellDefaultStyle(rowHeader.getCell(3), columnWidth[1],0, required);
                            }
                        }
                        // null / empty
                        addContent(document, "");
                    }
                    if(parameters.containsKey("query")){
                        addContent(document, "URI参数说明：");
                        // Get queryinfo
                        JSONArray query = parameters.getJSONArray("query");
                        // table
                        String tableTitle[] = {"参数名", "参数描述", "参数类型", "是否必填"};
                        Integer[] columnWidth = doColumnWidth(tableTitle);
                        XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                        if (query.size() == 0) {
                            // table
                            XWPFTableRow row = doRowDefaultStyle(table.createRow());
                            XWPFTableCell cell1 = doCellDefaultStyle(row.getCell(0), columnWidth[0],0, "暂无数据");
                            XWPFTableCell cell2 = doCellDefaultStyle(row.getCell(1), columnWidth[1],0, "");
                            XWPFTableCell cell3 = doCellDefaultStyle(row.getCell(2), columnWidth[1],0, "");
                            XWPFTableCell cell4 = doCellDefaultStyle(row.getCell(3), columnWidth[1],0, "");
                            // Set
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
                                // table
                                XWPFTableRow rowQuery = doRowDefaultStyle(table.createRow());
                                //
                                XWPFTableCell cellHeader1 = doCellDefaultStyle(rowQuery.getCell(0), columnWidth[0],0, paramsCode);
                                XWPFTableCell cellHeader2 = doCellDefaultStyle(rowQuery.getCell(1), columnWidth[1],0, description);
                                XWPFTableCell cellHeader3 = doCellDefaultStyle(rowQuery.getCell(2), columnWidth[1],0, type);
                                XWPFTableCell cellHeader4 = doCellDefaultStyle(rowQuery.getCell(3), columnWidth[1],0, required);
                            }
                        }
                        // null / empty
                        addContent(document, "");
                    }
                }
                // interface parameter
                if (requestType.indexOf("json") > 0) {
                    addContent(document, "请求体参数说明：");
                    // if is json
                    if(requestBody.containsKey("jsonSchema")){
                        JSONObject jsonSchema = requestBody.getJSONObject("jsonSchema");
                        JSONObject properties = jsonSchema.getJSONObject("properties");
                        if (null != properties) {
                            // table
                            String tableTitle[] = {"参数名", "参数描述", "参数类型", "是否必填"};
                            Integer[] columnWidth = doColumnWidth(tableTitle);
                            XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                            // Process parameter
                            doBodyParamsData(table, null, columnWidth, properties);
                            // null / empty
                            addContent(document, "");
                        }
                    }
                }
                // interface parameter
                if (api.containsKey("responses")) {
                    JSONArray responses = api.getJSONArray("responses");
                    for(Object response : responses){

                        JSONObject responseOne = JSONUtil.parseObj(response);
                        // parameter
                        addContent(document, "返回参数说明：（"+responseOne.getStr("code")+"）"+responseOne.getStr("name")+"");
                        // table
                        String tableTitle[] = {"参数名", "参数描述", "参数类型"};
                        Integer[] columnWidth = doColumnWidth(tableTitle);
                        XWPFTable table = createTableWithTitle(document, tableTitle, columnWidth);
                        // Set table
                        doTableDefaultStyle(table);

                        JSONObject properties = responseOne.getJSONObject("jsonSchema").getJSONObject("properties");
                        // Process parameter
                        doBodyResponseData(table, null, columnWidth, properties);
                        // null / empty
                        addContent(document, "");
                    }
                }
            } else if(apiCollection.containsKey("items")){ // if "items" is
                // Set
                String currentIndex = null;
                if(StrUtil.isBlank(topIndex)){
                    currentIndex = (i+1) + "";
                } else {
                    currentIndex = topIndex + "." + (i+1);
                }
                //
                String title = currentIndex + " " + apiCollection.getStr("name");
                addTitle(document, level, title);
                // if "items" is
                JSONArray items = apiCollection.getJSONArray("items");
                // Process data
                doTitleItems(document, level + 1, currentIndex, items);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String json = IoUtil.read(FileUtil.getInputStream(apiFoxFile)).toString();
        JSONObject apifoxProject = JSONUtil.parseObj(json);
        JSONObject info = apifoxProject.getJSONObject("info");
        JSONArray apiCollections = apifoxProject.getJSONArray("paths");
        // layer
        int level= 0;
        // word
        XWPFDocument document = new XWPFDocument();
        // Set to
        XWPFStyles styles = document.createStyles();
        styles.setStyles(getTemplateStype());
        // Set
        addTitle(document, level, docxTitle);

        // Set
        for (int i = 0; i < apiCollections.size(); i++) {
            JSONArray items = JSONUtil.parseObj(apiCollections.get(i)).getJSONArray("items");
            // Process data
            doTitleItems(document, level + 1, null, items);
        }
        //
        FileOutputStream fos = new FileOutputStream("C:/my_test/oort/Desktop/" + info.getStr("name") + ".docx");
        //
        document.write(fos);
    }
}
