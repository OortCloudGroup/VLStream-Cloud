package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.excel.VlsEventManagementExcel;
import org.springblade.vlstream.pojo.entity.EventManagement;
import org.springblade.vlstream.pojo.vo.EventManagementVO;

import java.util.List;

/**
 * Event management table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsEventManagementMapper extends BaseMapper<EventManagement> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsEventManagement query parameters
	 * @return List<VlsEventManagementVO>
	 */
	List<EventManagementVO> selectVlsEventManagementPage(IPage page, EventManagementVO vlsEventManagement);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsEventManagementExcel>
	 */
	List<VlsEventManagementExcel> exportVlsEventManagement(@Param("ew") Wrapper<EventManagement> queryWrapper);

}
