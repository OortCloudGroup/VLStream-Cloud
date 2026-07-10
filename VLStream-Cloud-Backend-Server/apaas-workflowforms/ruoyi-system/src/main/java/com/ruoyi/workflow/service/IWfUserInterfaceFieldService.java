package com.ruoyi.workflow.service;

public interface IWfUserInterfaceFieldService {
    String getFieldCodes(String userId, String interfacePath);

    int saveFieldCodes(String userId, String interfacePath,String codes);
}
