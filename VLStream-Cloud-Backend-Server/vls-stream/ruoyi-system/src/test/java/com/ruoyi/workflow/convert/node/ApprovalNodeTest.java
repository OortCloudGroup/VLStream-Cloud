package com.ruoyi.workflow.convert.node;

import cn.hutool.extra.spring.SpringUtil;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.utils.ModelUtils;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ApprovalNode conversion tests.
 */
@Tag("dev")
public class ApprovalNodeTest {

    /**
     * Provides the user service required by ApprovalNode static initialization.
     */
    @BeforeAll
    public static void setUpSpringContext() {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("test-user");

        SysUserServiceImpl sysUserService = mock(SysUserServiceImpl.class);
        when(sysUserService.selectUserById("user-1")).thenReturn(sysUser);

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(SysUserServiceImpl.class)).thenReturn(sysUserService);
        new SpringUtil().setApplicationContext(applicationContext);
    }

    /**
     * Keeps USERS dataType when a stale leader setting is also present.
     */
    @Test
    public void convertUsersNodeKeepsDataTypeWhenPostLeadersRemain() {
        ApprovalNode approvalNode = new ApprovalNode();
        approvalNode.setId("approval_1");
        approvalNode.setPid("start_1");
        approvalNode.setNodeName("Approval");
        approvalNode.setApprovalType(1);
        approvalNode.setUsers(Arrays.asList("user-1"));
        approvalNode.setPostLeaders(Arrays.asList("1"));
        approvalNode.setMulti(com.ruoyi.workflow.convert.enums.ApprovalMultiEnum.SEQUENTIAL);
        approvalNode.setOperations(Arrays.asList("0", "3", "4"));

        EndNode endNode = new EndNode();
        endNode.setId("end");
        endNode.setPid("approval_1");
        endNode.setNodeName("End");
        approvalNode.setChildNode(endNode);

        UserTask userTask = roundTripApprovalTask(approvalNode);

        assertEquals("USERS",
                userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_DATA_TYPE));
    }

    /**
     * Converts the node to XML and parses it back like the save/deploy flow does.
     */
    private UserTask roundTripApprovalTask(ApprovalNode approvalNode) {
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace("https://flowable.org/bpmn20");

        Process process = new Process();
        process.setId("Process_Test");
        process.setName("Test");

        List<FlowElement> elements = approvalNode.convert();
        for (FlowElement element : elements) {
            process.addFlowElement(element);
        }
        bpmnModel.addProcess(process);

        byte[] xmlBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
        BpmnModel parsedModel = ModelUtils.getBpmnModel(new String(xmlBytes, StandardCharsets.UTF_8));
        return (UserTask) parsedModel.getMainProcess().getFlowElement("approval_1");
    }
}
