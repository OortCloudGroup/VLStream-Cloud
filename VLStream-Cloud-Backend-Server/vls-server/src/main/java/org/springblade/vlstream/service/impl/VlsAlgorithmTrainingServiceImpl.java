package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsAlgorithmTrainingExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmTrainingMapper;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.vo.AlgorithmTrainingVO;
import org.springblade.vlstream.service.IVlsAlgorithmTrainingService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Algorithm training task table service implementation class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsAlgorithmTrainingServiceImpl extends BaseServiceImpl<VlsAlgorithmTrainingMapper, AlgorithmTraining> implements IVlsAlgorithmTrainingService {

	@Resource
	private VlsAlgorithmTrainingMapper algorithmTrainingMapper;

	@Override
	public IPage<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage<AlgorithmTrainingVO> page, AlgorithmTrainingVO vlsAlgorithmTraining) {
		return page.setRecords(baseMapper.selectVlsAlgorithmTrainingPage(page, vlsAlgorithmTraining));
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
	 * Query Algorithm Training Tasks
	 *
	 * @param id algorithm training task primary key
	 * @return algorithm training task
	 */
	@Override
	public AlgorithmTraining selectAlgorithmTrainingById(Long id) {
		return algorithmTrainingMapper.selectAlgorithmTrainingById(id);
	}

	/**
	 * Query Algorithm Training Task List
	 *
	 * @param algorithmTraining algorithm training task
	 * @return algorithm training task
	 */
	@Override
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining) {
		return algorithmTrainingMapper.selectAlgorithmTrainingList(algorithmTraining);
	}

	/**
	 * Add algorithm training task
	 *
	 * @param algorithmTraining algorithm training task
	 * @return result
	 */
	@Override
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return algorithmTrainingMapper.insertAlgorithmTraining(algorithmTraining);
	}

	/**
	 * Modify algorithm training task
	 *
	 * @param algorithmTraining algorithm training task
	 * @return result
	 */
	@Override
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining) {
		return algorithmTrainingMapper.updateAlgorithmTraining(algorithmTraining);
	}

	/**
	 * Batch delete algorithm training tasks
	 *
	 * @param ids primary keys of the algorithm training tasks to be deleted
	 * @return result
	 */
	@Override
	public int deleteAlgorithmTrainingByIds(Long[] ids) {
		return algorithmTrainingMapper.deleteAlgorithmTrainingByIds(ids);
	}

	/**
	 * Delete algorithm training task information
	 *
	 * @param id algorithm training task primary key
	 * @return result
	 */
	@Override
	public int deleteAlgorithmTrainingById(Long id) {
		return algorithmTrainingMapper.deleteAlgorithmTrainingById(id);
	}

}
