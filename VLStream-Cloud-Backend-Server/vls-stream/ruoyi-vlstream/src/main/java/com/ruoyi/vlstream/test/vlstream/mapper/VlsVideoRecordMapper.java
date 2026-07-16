package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsVideoRecordExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.VideoRecordVO;

import java.util.List;

/**
 * 视频录制记录表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-25
 */
public interface VlsVideoRecordMapper extends BaseMapper<VideoRecord> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsVideoRecord 查询参数
	 * @return List<VlsVideoRecordVO>
	 */
	List<VideoRecordVO> selectVlsVideoRecordPage(IPage page, VideoRecordVO vlsVideoRecord);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsVideoRecordExcel>
	 */
	List<VlsVideoRecordExcel> exportVlsVideoRecord(@Param("ew") Wrapper<VideoRecord> queryWrapper);

}
