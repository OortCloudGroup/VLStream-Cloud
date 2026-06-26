package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.excel.VlsVideoRecordExcel;
import org.springblade.vlstream.pojo.entity.VideoRecord;
import org.springblade.vlstream.pojo.vo.VideoRecordVO;

import java.util.List;

/**
 * Video recording record table Mapper interface
 *
 * @author Oort
 * @since 2025-12-25
 */
public interface VlsVideoRecordMapper extends BaseMapper<VideoRecord> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsVideoRecord query parameters
	 * @return List<VlsVideoRecordVO>
	 */
	List<VideoRecordVO> selectVlsVideoRecordPage(IPage page, VideoRecordVO vlsVideoRecord);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsVideoRecordExcel>
	 */
	List<VlsVideoRecordExcel> exportVlsVideoRecord(@Param("ew") Wrapper<VideoRecord> queryWrapper);

}
