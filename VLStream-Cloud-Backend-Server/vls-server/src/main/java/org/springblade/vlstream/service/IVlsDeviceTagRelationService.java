package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsDeviceTagRelationExcel;
import org.springblade.vlstream.pojo.dto.DeviceTagRelationDTO;
import org.springblade.vlstream.pojo.entity.DeviceTagRelation;
import org.springblade.vlstream.pojo.vo.DeviceTagRelationVO;

import java.util.List;
import java.util.Map;

/**
 * Device Tag Association Table Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsDeviceTagRelationService extends BaseService<DeviceTagRelation> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsDeviceTagRelation query parameters
	 * @return IPage<VlsDeviceTagRelationVO>
	 */
	IPage<DeviceTagRelationVO> selectVlsDeviceTagRelationPage(IPage<DeviceTagRelationVO> page, DeviceTagRelationVO vlsDeviceTagRelation);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsDeviceTagRelationExcel>
	 */
	List<VlsDeviceTagRelationExcel> exportVlsDeviceTagRelation(Wrapper<DeviceTagRelation> queryWrapper);

	/**
	 * Set device tags (overwrite original tags)
	 *
	 * @param deviceId device ID
	 * @param tagIds label ID list
	 * @param createdBy creator
	 * @return whether successful
	 */
	boolean setDeviceTags(Long deviceId, List<Long> tagIds, String createdBy);

	/**
	 * Add device tags (append to existing tags)
	 *
	 * @param deviceId device ID
	 * @param tagIds label ID list
	 * @param createdBy creator
	 * @return whether successful
	 */
	boolean addDeviceTags(Long deviceId, List<Long> tagIds, String createdBy);

	/**
	 * Remove device tags
	 *
	 * @param deviceId device ID
	 * @param tagIds label ID list
	 * @return whether successful
	 */
	boolean removeDeviceTags(Long deviceId, List<Long> tagIds);

	/**
	 * Clear all tags of the device
	 *
	 * @param deviceId device ID
	 * @return whether successful
	 */
	boolean clearDeviceTags(Long deviceId);

	/**
	 * Get all labels of device
	 *
	 * @param deviceId device ID
	 * @return label information list
	 */
	List<DeviceTagRelationDTO> getDeviceTags(Long deviceId);

	/**
	 * Get label ID list of device
	 *
	 * @param deviceId device ID
	 * @return label ID list
	 */
	List<Long> getDeviceTagIds(Long deviceId);

	/**
	 * Get device list with specified label
	 *
	 * @param tagId label ID
	 * @return device information list
	 */
	List<Map<String, Object>> getDevicesByTag(Long tagId);

	/**
	 * Query devices by multiple labels (intersection - must contain all labels simultaneously)
	 *
	 * @param tagIds label ID list
	 * @return device ID list
	 */
	List<Long> findDevicesByAllTags(List<Long> tagIds);

	/**
	 * Query devices by multiple labels (union - containing any label)
	 *
	 * @param tagIds label ID list
	 * @return device ID list
	 */
	List<Long> findDevicesByAnyTags(List<Long> tagIds);

	/**
	 * Batch set device labels
	 *
	 * @param deviceTagMap mapping of device ID -> label ID list
	 * @param createdBy creator
	 * @return number of successfully configured devices
	 */
	int batchSetDeviceTags(Map<Long, List<Long>> deviceTagMap, String createdBy);

	/**
	 * Copy device tags to other devices
	 *
	 * @param sourceDeviceId source device ID
	 * @param targetDeviceIds target device ID list
	 * @param createdBy creator
	 * @return whether successful
	 */
	boolean copyDeviceTags(Long sourceDeviceId, List<Long> targetDeviceIds, String createdBy);

	/**
	 * Get device label statistics
	 *
	 * @return statistics
	 */
	List<Map<String, Object>> getDeviceTagStatistics();

	/**
	 * Get label usage statistics
	 *
	 * @return label usage statistics
	 */
	List<Map<String, Object>> getTagUsageStatistics();

	/**
	 * Check if device has the specified tag
	 *
	 * @param deviceId device ID
	 * @param tagId label ID
	 * @return whether exists
	 */
	boolean hasDeviceTag(Long deviceId, Long tagId);

	/**
	 * Get device count of label
	 *
	 * @param tagId label ID
	 * @return device quantity
	 */
	int getTagDeviceCount(Long tagId);

	/**
	 * Verify if the tag ID list is valid
	 *
	 * @param tagIds label ID list
	 * @return Verification results
	 */
	Map<String, Object> validateTagIds(List<Long> tagIds);

	/**
	 * Get detailed information of device label (containing label hierarchy)
	 *
	 * @param deviceId device ID
	 * @return label details
	 */
	Map<String, Object> getDeviceTagDetails(Long deviceId);

	/**
	 * Get device list by tag type
	 *
	 * @param categoryType label type (own/public)
	 * @param level label level (1/2)
	 * @return device information
	 */
	List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level);

	/**
	 * Synchronize tag usage count
	 * Update usage_count field in tag_management table
	 *
	 * @return whether successful
	 */
	boolean syncTagUsageCount();

}
