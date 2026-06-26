package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.pojo.dto.CameraApplyQueryDTO;
import org.springblade.vlstream.pojo.entity.CameraApplyRecord;
import org.springblade.vlstream.pojo.vo.CameraApplyRecordVO;

import java.util.List;

/**
 * Camera application approval record Mapper interface
 */
public interface VlsCameraApplyRecordMapper extends BaseMapper<CameraApplyRecord> {

	List<CameraApplyRecordVO> selectCameraApplyRecordPage(IPage<CameraApplyRecordVO> page, @Param("query") CameraApplyQueryDTO query);
}
