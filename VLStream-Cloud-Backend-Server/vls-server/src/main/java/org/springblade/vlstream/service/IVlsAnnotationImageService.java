package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAnnotationImageExcel;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.vo.AnnotationImageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Annotation Image Info Table Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationImageService extends BaseService<AnnotationImage> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnnotationImage query parameters
	 * @return IPage<VlsAnnotationImageVO>
	 */
	IPage<AnnotationImageVO> selectVlsAnnotationImagePage(IPage<AnnotationImageVO> page, AnnotationImageVO vlsAnnotationImage);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnnotationImageExcel>
	 */
	List<VlsAnnotationImageExcel> exportVlsAnnotationImage(Wrapper<AnnotationImage> queryWrapper);

	/**
	 * Upload image
	 */
	List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId);

	/**
	 * Get image list by dataset ID
	 */
	List<AnnotationImage> getImagesByDataset(Long annotationId);

	/**
	 * Get image details by ID
	 */
	AnnotationImage getImageById(Long id);

	/**
	 * Update image information
	 */
	AnnotationImage updateImage(AnnotationImage image);

	/**
	 * Delete image
	 */
	void deleteImage(Long id);

	/**
	 * Batch delete images
	 */
	void batchDeleteImages(List<Long> ids);

	/**
	 * Get dataset statistics
	 */
	Map<String, Object> getDatasetStats(Long datasetId);

	/**
	 * Save image information to the annotation_image table
	 */
	boolean saveImage(AnnotationImage annotationImage);

	/**
	 * Bulk save image info to annotation_image table
	 */
	boolean batchSaveImages(List<AnnotationImage> annotationImages);

	/**
	 * Get image list by annotation project ID
	 */
	List<AnnotationImage> getImagesByAnnotationId(Long annotationId);

}
