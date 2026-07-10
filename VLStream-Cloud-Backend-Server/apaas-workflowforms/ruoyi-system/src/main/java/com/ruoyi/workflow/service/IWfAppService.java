package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.bo.WfAppBo;
import com.ruoyi.workflow.domain.vo.WfAppVo;

import java.util.Collection;
import java.util.List;

/**
 * 应用通用流程Service接口
 *
 * @author 雷超群
 * @date 2025-01-04
 */
public interface IWfAppService extends IService<WfApp> {

    /**
     * 查询应用通用流程
     */
    WfAppVo queryById(String appId);

    /**
     * 查询应用通用流程列表
     */
    List<WfAppVo> queryPageList(WfAppBo bo, PageQuery pageQuery);

    /**
     * 查询应用通用流程列表
     */
    List<WfAppVo> queryList(WfAppBo bo);

    /**
     * 新增应用通用流程
     */
    WfApp insertByBo(WfAppBo bo);

    /**
     * 修改应用通用流程
     */
    Boolean updateByBo(WfAppBo bo);

    /**
     * 校验并批量删除应用通用流程信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
