package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnalysisRequestExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnalysisRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnalysisRequestVO;

import java.util.List;

/**
 * 智能分析请求表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnalysisRequestMapper extends BaseMapper<AnalysisRequest> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnalysisRequest 查询参数
	 * @return List<VlsAnalysisRequestVO>
	 */
	List<AnalysisRequestVO> selectVlsAnalysisRequestPage(IPage page, AnalysisRequestVO vlsAnalysisRequest);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnalysisRequestExcel>
	 */
	List<VlsAnalysisRequestExcel> exportVlsAnalysisRequest(@Param("ew") Wrapper<AnalysisRequest> queryWrapper);

}
