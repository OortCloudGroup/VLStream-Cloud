/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import javax.annotation.Resource;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmTrainingExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * algorithmtrainingtask service
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsAlgorithmTrainingServiceImpl extends BaseServiceImpl<VlsAlgorithmTrainingMapper, AlgorithmTraining> implements IVlsAlgorithmTrainingService {

	@Resource
	private VlsAlgorithmTrainingMapper algorithmTrainingMapper;
	@Resource private com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess publicAccess;
	@Override public AlgorithmTraining getById(java.io.Serializable id) {
		if(publicAccess!=null && id!=null)publicAccess.trainingFromHttp(Long.valueOf(id.toString()));
		return super.getById(id);
	}
	@Override public boolean updateById(AlgorithmTraining row) {
		if(publicAccess!=null && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()!=null) {
			if(row.getId()==null)throw new ServiceException("缺少训练记录身份");
			publicAccess.training(row.getId());
			com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess.protectTrainingUpdate(algorithmTrainingMapper.selectAlgorithmTrainingById(row.getId()),row);
		}
		return super.updateById(row);
	}
	@Override public boolean recordQueuedLog(Long trainingId,Long containerRecordId) {
		if(publicAccess==null)throw new ServiceException("缺少可信排队日志上下文");
		String logPath=publicAccess.queuedTrainingLog(trainingId,containerRecordId);
		AlgorithmTraining update=new AlgorithmTraining();update.setId(trainingId);update.setLogPath(logPath);
		return super.updateById(update);
	}

	@Override
	public IPage<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage<AlgorithmTrainingVO> page, AlgorithmTrainingVO vlsAlgorithmTraining) {
		String user=LoginHelper.getUserId();
		if(user==null || user.trim().isEmpty() || publicAccess==null)
			throw new ServiceException("缺少可信训练访问上下文");
		QueryWrapper<AlgorithmTraining> query=new QueryWrapper<>();
		query.eq("vls_algorithm_training.is_deleted",0)
			.apply("CAST(vls_algorithm_training.tenant_id AS BINARY)=CAST({0} AS BINARY)",ModelArtifactObjectStore.tenant())
			.apply("CAST(vls_algorithm_training.create_user AS BINARY)=CAST({0} AS BINARY)",user);
		publicAccess.filter(query,"vls_algorithm_training.id");
		if(vlsAlgorithmTraining!=null) {
			if(vlsAlgorithmTraining.getTaskName()!=null && !vlsAlgorithmTraining.getTaskName().trim().isEmpty())query.like("task_name",vlsAlgorithmTraining.getTaskName().trim());
			if(vlsAlgorithmTraining.getAlgorithmId()!=null)query.eq("algorithm_id",vlsAlgorithmTraining.getAlgorithmId());
			if(vlsAlgorithmTraining.getDatasetId()!=null)query.eq("dataset_id",vlsAlgorithmTraining.getDatasetId());
			if(vlsAlgorithmTraining.getTrainStatus()!=null)query.eq("train_status",vlsAlgorithmTraining.getTrainStatus().getCode());
		}
		query.orderByDesc("create_time","id");
		return page.setRecords(baseMapper.selectVlsAlgorithmTrainingPage(page,vlsAlgorithmTraining,query));
	}

	@Override
	public List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(Wrapper<AlgorithmTraining> queryWrapper) {
		List<VlsAlgorithmTrainingExcel> vlsAlgorithmTrainingList = baseMapper.exportVlsAlgorithmTraining(queryWrapper);
		//vlsAlgorithmTrainingList.forEach(vlsAlgorithmTraining -> {
		//	vlsAlgorithmTraining.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmTrainingEntity.getType()));
		//});
		return vlsAlgorithmTrainingList;
	}

	/**
	 * Query algorithmtrainingtask
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return algorithmtrainingtask
	 */
	@Override
	public AlgorithmTraining selectAlgorithmTrainingById(Long id) {
		if(publicAccess!=null)publicAccess.trainingFromHttp(id);
		return algorithmTrainingMapper.selectAlgorithmTrainingById(id);
	}

	/**
	 * Query algorithmtrainingtask list
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return algorithmtrainingtask
	 */
	@Override
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining) {
		return algorithmTrainingMapper.selectAlgorithmTrainingList(algorithmTraining);
	}

	/**
	 * Add algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	@Override
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return save(algorithmTraining) ? 1 : 0;
	}

	/**
	 * Update algorithmtrainingtask
	 *
	 * @param algorithmTraining algorithmtrainingtask
	 * @return
	 */
	@Override
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return updateById(algorithmTraining) ? 1 : 0;
	}

	/**
	 * Batch delete algorithmtrainingtask
	 *
	 * @param ids need to Delete algorithmtrainingtaskprimary key
	 * @return
	 */
	@Override
	public int deleteAlgorithmTrainingByIds(Long[] ids) {
		if(publicAccess!=null)java.util.Arrays.asList(ids).forEach(publicAccess::trainingFromHttp);
		return algorithmTrainingMapper.deleteAlgorithmTrainingByIds(ids);
	}

	/**
	 * Delete algorithmtrainingtaskinfo
	 *
	 * @param id algorithmtrainingtaskprimary key
	 * @return
	 */
	@Override
	public int deleteAlgorithmTrainingById(Long id) {
		if(publicAccess!=null)publicAccess.trainingFromHttp(id);
		return algorithmTrainingMapper.deleteAlgorithmTrainingById(id);
	}

}
