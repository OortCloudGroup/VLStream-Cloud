/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.workflow.convert.ProcessModel;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Objects;

/**
 * JSON转BPMN.xml
 */
@RestController
@CrossOrigin
@RequestMapping("/model")
public class ConvertController {

    /**
     * json转为bpmn并下载
     */
    @PostMapping("/download")
    @SaCheckPermission("model:downloadXml")
    public void downloadXml(@RequestBody ProcessModel processModel) throws IOException {
        BpmnModel bpmnModel = processModel.toBpmnModel();
        byte[] xmlBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
        String str = new String(xmlBytes);
        System.out.println("==================xmlBytes================= " + str);
        if (Objects.nonNull(xmlBytes)) {
            String fileName = processModel.getName().replaceAll(" ", "_") + ".bpmn20.xml";
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
            if (Objects.nonNull(response)) {
                response.setContentType("application/xml");
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
                response.setHeader("Content-Disposition", "attachment; filename=file.bpmn20.xml; filename*=" + URLEncoder.encode(fileName, "UTF-8"));
                ServletOutputStream servletOutputStream = response.getOutputStream();
                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(xmlBytes));
                byte[] buffer = new byte[8096];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    servletOutputStream.write(buffer, 0, count);
                }
                // 刷新并关闭流
                servletOutputStream.flush();
                servletOutputStream.close();
            }
        }
    }
}
