package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.vlstream.excel.VlsSceneGovernanceExcel;
import org.springblade.vlstream.pojo.entity.SceneGovernance;
import org.springblade.vlstream.pojo.vo.SceneGovernanceVO;

import java.util.List;

/**
 * Scene governance table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsSceneGovernanceMapper extends BaseMapper<SceneGovernance> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsSceneGovernance query parameters
	 * @return List<VlsSceneGovernanceVO>
	 */
	List<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage page, SceneGovernanceVO vlsSceneGovernance);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsSceneGovernanceExcel>
	 */
	List<VlsSceneGovernanceExcel> exportVlsSceneGovernance(@Param("ew") Wrapper<SceneGovernance> queryWrapper);

	/**
	 * Query scene governance information by name
	 *
	 * @param name scene name
	 * @return scene governance information
	 */
	@Select("SELECT * FROM vls_scene_governance WHERE name = #{name} AND is_deleted = 0")
	SceneGovernance selectByName(@Param("name") String name);

	/**
	 * Query scene governance list by status
	 *
	 * @param status scene status
	 * @return scene governance list
	 */
	@Select("SELECT * FROM vls_scene_governance WHERE status = #{status} AND is_deleted = 0 ORDER BY created_at DESC")
	List<SceneGovernance> selectByStatus(@Param("status") String status);

	/**
	 * Get total count of scene governance
	 *
	 * @return total count
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE is_deleted = 0")
	Long getTotalCount();

	/**
	 * Get enabled scene governance count
	 *
	 * @return enabled quantity
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE status = 'enabled' AND is_deleted = 0")
	Long getEnabledCount();

	/**
	 * Get disabled scene governance count
	 *
	 * @return disabled quantity
	 */
	@Select("SELECT COUNT(*) FROM vls_scene_governance WHERE status = 'disabled' AND is_deleted = 0")
	Long getDisabledCount();

}
