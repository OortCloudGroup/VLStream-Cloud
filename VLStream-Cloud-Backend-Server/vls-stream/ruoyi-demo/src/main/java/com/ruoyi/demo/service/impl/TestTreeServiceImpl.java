/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.demo.domain.TestTree;
import com.ruoyi.demo.domain.bo.TestTreeBo;
import com.ruoyi.demo.domain.vo.TestTreeVo;
import com.ruoyi.demo.mapper.TestTreeMapper;
import com.ruoyi.demo.service.ITestTreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service layer Process
 *
 * @author Lion Li
 * @date 2021-07-26
 */
// @DS("slave") // from Query
@RequiredArgsConstructor
@Service
public class TestTreeServiceImpl implements ITestTreeService {

    private final TestTreeMapper baseMapper;

    @Override
    public TestTreeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    // @DS("slave") // from Query
    @Override
    public List<TestTreeVo> queryList(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<TestTree> buildQueryWrapper(TestTreeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<TestTree> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getTreeName()), TestTree::getTreeName, bo.getTreeName());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            TestTree::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        return lqw;
    }

    @Override
    public Boolean insertByBo(TestTreeBo bo) {
        TestTree add = BeanUtil.toBean(bo, TestTree.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(TestTreeBo bo) {
        TestTree update = BeanUtil.toBean(bo, TestTree.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     *
     * @param entity data
     */
    private void validEntityBeforeSave(TestTree entity) {
        // TODO dataValidate ,
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO Validate ,Check whether need to Validate
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
