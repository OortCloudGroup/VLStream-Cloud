/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils.poi;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.ruoyi.common.convert.ExcelBigNumberConvert;
import com.ruoyi.common.excel.CellMergeStrategy;
import com.ruoyi.common.excel.DefaultExcelListener;
import com.ruoyi.common.excel.ExcelListener;
import com.ruoyi.common.excel.ExcelResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ExcelrelatedProcess
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelUtil {

    /**
     * Import ( data )
     *
     * @param is
     * @return Convert aftercollection
     */
    public static <T> List<T> importExcel(InputStream is, Class<T> clazz) {
        return EasyExcel.read(is).head(clazz).autoCloseStream(false).sheet().doReadSync();
    }


    /**
     * Validate listener Import
     *
     * @param is
     * @param clazz object
     * @param isValidate whether Validator to is
     * @return Convert aftercollection
     */
    public static <T> ExcelResult<T> importExcel(InputStream is, Class<T> clazz, boolean isValidate) {
        DefaultExcelListener<T> listener = new DefaultExcelListener<>(isValidate);
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * Customlistener Import Custom
     *
     * @param is
     * @param clazz object
     * @param listener Customlistener
     * @return Convert aftercollection
     */
    public static <T> ExcelResult<T> importExcel(InputStream is, Class<T> clazz, ExcelListener<T> listener) {
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * Export excel
     *
     * @param list Export dataset
     * @param sheetName
     * @param clazz
     * @param response
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            exportExcel(list, sheetName, clazz, false, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常");
        }
    }

    /**
     * Export excel
     *
     * @param list Export dataset
     * @param sheetName
     * @param clazz
     * @param merge whether
     * @param response
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, HttpServletResponse response) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            exportExcel(list, sheetName, clazz, merge, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常");
        }
    }

    /**
     * Export excel
     *
     * @param list Export dataset
     * @param sheetName
     * @param clazz
     * @param os
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, OutputStream os) {
        exportExcel(list, sheetName, clazz, false, os);
    }

    /**
     * Export excel
     *
     * @param list Export dataset
     * @param sheetName
     * @param clazz
     * @param merge whether
     * @param os
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, OutputStream os) {
        ExcelWriterSheetBuilder builder = EasyExcel.write(os, clazz)
            .autoCloseStream(false)
            //
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
            // value Convert
            .registerConverter(new ExcelBigNumberConvert())
            .sheet(sheetName);
        if (merge) {
            // Process
            builder.registerWriteHandler(new CellMergeStrategy(list, true));
        }
        builder.doWrite(list);
    }

    /**
     * data Export to {.property}
     *
     * @param filename
     * @param templatePath resource
     * : excel/temp.xlsx
     * : resource
     * @param data need to data
     * @param response
     */
    public static void exportTemplate(List<Object> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            resetResponse(filename, response);
            ServletOutputStream os = response.getOutputStream();
            exportTemplate(data, templatePath, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常");
        }
    }

    /**
     * data Export to {.property}
     *
     * @param templatePath resource
     * : excel/temp.xlsx
     * : resource
     * @param data need to data
     * @param os
     */
    public static void exportTemplate(List<Object> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = EasyExcel.write(os)
            .withTemplate(templateResource.getStream())
            .autoCloseStream(false)
            // value Convert
            .registerConverter(new ExcelBigNumberConvert())
            .build();
        WriteSheet writeSheet = EasyExcel.writerSheet().build();
        if (CollUtil.isEmpty(data)) {
            throw new IllegalArgumentException("数据为空");
        }
        // dataExport to {.property}
        for (Object d : data) {
            excelWriter.fill(d, writeSheet);
        }
        excelWriter.finish();
    }

    /**
     * data Export to {key.property}
     *
     * @param filename
     * @param templatePath resource
     * : excel/temp.xlsx
     * : resource
     * @param data need to data
     * @param response
     */
    public static void exportTemplateMultiList(Map<String, Object> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            resetResponse(filename, response);
            ServletOutputStream os = response.getOutputStream();
            exportTemplateMultiList(data, templatePath, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常");
        }
    }

    /**
     * data Export to {key.property}
     *
     * @param templatePath resource
     * : excel/temp.xlsx
     * : resource
     * @param data need to data
     * @param os
     */
    public static void exportTemplateMultiList(Map<String, Object> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = EasyExcel.write(os)
            .withTemplate(templateResource.getStream())
            .autoCloseStream(false)
            // value Convert
            .registerConverter(new ExcelBigNumberConvert())
            .build();
        WriteSheet writeSheet = EasyExcel.writerSheet().build();
        if (CollUtil.isEmpty(data)) {
            throw new IllegalArgumentException("数据为空");
        }
        for (Map.Entry<String, Object> map : data.entrySet()) {
            // Set after data
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
            if (map.getValue() instanceof Collection) {
                // Export FillWrapper
                excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
            } else {
                excelWriter.fill(map.getValue(), writeSheet);
            }
        }
        excelWriter.finish();
    }

    /**
     *
     */
    private static void resetResponse(String sheetName, HttpServletResponse response) throws UnsupportedEncodingException {
        String filename = encodingFilename(sheetName);
        FileUtils.setAttachmentResponseHeader(response, filename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
    }

    /**
     * Parse Export value 0= ,1= ,2= not
     *
     * @param propertyValue parameter value
     * @param converterExp
     * @param separator
     * @return Parse after value
     */
    public static String convertByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(StringUtils.SEPARATOR);
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(propertyValue, separator)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[0].equals(value)) {
                        propertyString.append(itemArray[1] + separator);
                        break;
                    }
                }
            } else {
                if (itemArray[0].equals(propertyValue)) {
                    return itemArray[1];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * Parse value =0, =1, not =2
     *
     * @param propertyValue parameter value
     * @param converterExp
     * @param separator
     * @return Parse after value
     */
    public static String reverseByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(StringUtils.SEPARATOR);
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(propertyValue, separator)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[1].equals(value)) {
                        propertyString.append(itemArray[0] + separator);
                        break;
                    }
                }
            } else {
                if (itemArray[1].equals(propertyValue)) {
                    return itemArray[0];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     *
     */
    public static String encodingFilename(String filename) {
        return IdUtil.fastSimpleUUID() + "_" + filename + ".xlsx";
    }

}
