package com.ruoyi.workflow.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.ReModelJson;
import com.ruoyi.workflow.domain.bo.ReModeJsonBo;
import com.ruoyi.workflow.domain.vo.ReModelJsonVo;
import com.ruoyi.workflow.mapper.ReModelJsonMapper;
import com.ruoyi.workflow.service.IReModeJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程图JSONService业务层处理
 *
 * @author 雷超群
 * @date 2024-11-02
 */
@RequiredArgsConstructor
@Service
public class ReModelJsonServiceImpl extends ServiceImpl<ReModelJsonMapper, ReModelJson> implements IReModeJsonService {

    private final ReModelJsonMapper baseMapper;

    /**
     * 查询流程图JSON
     */
    @Override
    public ReModelJsonVo queryById(String modelId){
        return baseMapper.selectVoById(modelId);
    }

    /**
     * 查询流程图JSON列表
     */
    @Override
    public TableDataInfo<ReModelJsonVo> queryPageList(ReModeJsonBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReModelJson> lqw = buildQueryWrapper(bo);
        Page<ReModelJsonVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询流程图JSON列表
     */
    @Override
    public List<ReModelJsonVo> queryList(ReModeJsonBo bo) {
        LambdaQueryWrapper<ReModelJson> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ReModelJson> buildQueryWrapper(ReModeJsonBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ReModelJson> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), ReModelJson::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getUserId()), ReModelJson::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getJsonContent()), ReModelJson::getJsonContent, bo.getJsonContent());
        return lqw;
    }

    /**
     * 新增流程图JSON
     */
    @Override
    public Boolean insertByBo(ReModeJsonBo bo) {
        ReModelJson add = BeanUtil.toBean(bo, ReModelJson.class);
        validEntityBeforeSave(add);
        boolean flag;
        if (queryById(bo.getModelId()) != null) {
            flag = updateByBo(bo);
        } else {
            flag = baseMapper.insert(add) > 0;
        }
        return flag;
    }

    /**
     * 修改流程图JSON
     */
    @Override
    public Boolean updateByBo(ReModeJsonBo bo) {
        ReModelJson update = BeanUtil.toBean(bo, ReModelJson.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ReModelJson entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除流程图JSON
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
