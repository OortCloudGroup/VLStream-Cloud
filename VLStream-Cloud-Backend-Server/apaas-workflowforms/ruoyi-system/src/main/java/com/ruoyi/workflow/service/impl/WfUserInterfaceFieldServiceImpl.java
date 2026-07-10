package com.ruoyi.workflow.service.impl;

import com.ruoyi.workflow.mapper.WfUserInterfaceFieldMapper;
import com.ruoyi.workflow.service.IWfUserInterfaceFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class WfUserInterfaceFieldServiceImpl implements IWfUserInterfaceFieldService {

    private final WfUserInterfaceFieldMapper wfUserInterfaceFieldMapper;

    /**
     * 获取用户对某接口的字段配置；若返回 null，意味着未配置，应使用默认字段列表
     */
    @Override
    public String getFieldCodes(String userId, String interfacePath) {
        String json = wfUserInterfaceFieldMapper.selectFieldCodes(userId, interfacePath);
        if (json == null) {
            return null;
        }
        return json;
    }

    /**
     * 保存用户配置
     */
    @Override
    public int saveFieldCodes(String userId, String interfacePath, String codes) {
       return wfUserInterfaceFieldMapper.upsert(userId, interfacePath, codes);
    }
}
