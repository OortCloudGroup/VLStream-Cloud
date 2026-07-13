package com.ruoyi.workflow.domain.bo;

import lombok.Data;

import java.util.List;
@Data
public class WfUserInterfaceFieldBo {
    private String userId;
    private String interfacePath;
    private List<String> fieldCodes;
}
