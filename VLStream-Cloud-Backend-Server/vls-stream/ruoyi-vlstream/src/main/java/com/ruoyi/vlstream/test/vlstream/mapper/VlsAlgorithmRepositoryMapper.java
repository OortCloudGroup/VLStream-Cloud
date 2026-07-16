package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmRepositoryExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.List;

/**
 * 算法仓库表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmRepositoryMapper extends BaseMapper<AlgorithmRepository> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmRepository 查询参数
	 * @return List<VlsAlgorithmRepositoryVO>
	 */
	List<AlgorithmRepositoryVO> selectVlsAlgorithmRepositoryPage(IPage page, AlgorithmRepositoryVO vlsAlgorithmRepository);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmRepositoryExcel>
	 */
	List<VlsAlgorithmRepositoryExcel> exportVlsAlgorithmRepository(@Param("ew") Wrapper<AlgorithmRepository> queryWrapper);

	/**
	 * 分页查询算法仓库列表
	 */
	@Select("SELECT r.*, " +
		"(SELECT COUNT(*) FROM algorithm a WHERE a.repository_id = r.id AND a.is_deleted = 0) as algorithm_count " +
		"FROM vls_algorithm_repository r " +
		"WHERE r.is_deleted = 0 " +
		"AND (#{name} IS NULL OR r.name LIKE CONCAT('%', #{name}, '%')) " +
		"AND (#{repositoryType} IS NULL OR r.repository_type = #{repositoryType}) " +
		"AND (#{status} IS NULL OR r.status = #{status}) " +
		"ORDER BY r.id ASC")
	IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page,
													@Param("name") String name,
													@Param("repositoryType") String repositoryType,
													@Param("status") String status);

	/**
	 * 查询所有启用的算法仓库
	 */
	@Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND status = 'enabled' ORDER BY id")
	List<AlgorithmRepository> selectEnabledRepositories();

	/**
	 * 根据类型查询算法仓库
	 */
	@Select("SELECT * FROM vls_algorithm_repository WHERE is_deleted = 0 AND repository_type = #{repositoryType} ORDER BY id")
	List<AlgorithmRepository> selectByRepositoryType(@Param("repositoryType") String repositoryType);

	/**
	 * 统计算法仓库数量
	 */
	@Select("SELECT COUNT(*) FROM vls_algorithm_repository WHERE is_deleted = 0")
	Long countRepositories();

}
