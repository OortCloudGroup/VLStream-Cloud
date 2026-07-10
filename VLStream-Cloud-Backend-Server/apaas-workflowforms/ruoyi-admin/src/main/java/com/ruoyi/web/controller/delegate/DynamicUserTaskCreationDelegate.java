package com.ruoyi.web.controller.delegate;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.system.service.impl.SysDeptServiceImpl;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import io.micrometer.core.instrument.util.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.applet.AppletContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DynamicUserTaskCreationDelegate implements JavaDelegate, ApplicationContextAware {
    private static ApplicationContext applicationContext;
    public DynamicUserTaskCreationDelegate() {
        // 无参构造函数
    }

    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        this.applicationContext = arg0;
    }

    @Override
    public void execute(DelegateExecution execution) {
        SysUserServiceImpl sysUserServiceImpl = (SysUserServiceImpl)applicationContext.getBean("sysUserServiceImpl");
        TaskService taskService = (TaskService)applicationContext.getBean(TaskService.class);
        RuntimeService runtimeService = (RuntimeService)applicationContext.getBean(RuntimeService.class);

        // 发起人
        String initiator = (String) execution.getVariable("initiator");
        List<SysUser> leaders = sysUserServiceImpl.getLeaders(initiator);

//        // 将部门层级列表存储到流程变量
//        execution.setVariable("leaders", leaders);
        // 初始化 complete 变量为 1
        execution.setVariable("complete", "1");

        // 设置初始审批人（第一个部门领导）
        if (!leaders.isEmpty()) {
            execution.setVariable("currentAssignee", initiator);
        }
//        if (!leaders.isEmpty()) {
//            execution.setVariable("currentAssignee", leaders.get(0));
//        }
    }
}
