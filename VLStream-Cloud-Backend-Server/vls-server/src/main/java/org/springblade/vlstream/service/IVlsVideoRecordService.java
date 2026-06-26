package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsVideoRecordExcel;
import org.springblade.vlstream.pojo.entity.VideoRecord;
import org.springblade.vlstream.pojo.vo.VideoRecordVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Video Recording Record Table Service Class
 *
 * @author Oort
 * @since 2025-12-25
 */
public interface IVlsVideoRecordService extends BaseService<VideoRecord> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsVideoRecord query parameters
	 * @return IPage<VlsVideoRecordVO>
	 */
	IPage<VideoRecordVO> selectVlsVideoRecordPage(IPage<VideoRecordVO> page, VideoRecordVO vlsVideoRecord);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsVideoRecordExcel>
	 */
	List<VlsVideoRecordExcel> exportVlsVideoRecord(Wrapper<VideoRecord> queryWrapper);

	/**
	 * Query playback records by time range
	 *
	 * @param deviceId device ID
	 * @param startTime start time
	 * @param endTime end time
	 * @return playback record list
	 */
	List<VideoRecord> listPlaybackRecords(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

	/**
	 * Query the recording records of a device for a specific day
	 *
	 * @param deviceId device ID
	 * @param recordDate date
	 * @return recording list
	 */
	List<VideoRecord> listDayRecords(Long deviceId, LocalDate recordDate);

	/**
	 * Query the list of dates with recording records for the device in the specified year
	 *
	 * @param deviceId device ID
	 * @param year year (no limit if empty)
	 * @return date list
	 */
	List<LocalDate> listRecordDates(Long deviceId, Integer year);

}
