package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** Uses generated CRUD, including tenant audit fields and logical deletion. */
public interface DataSampleMapper extends BaseMapper<AnnotationImage> {
    @Update("UPDATE vls_annotation_image SET is_deleted = 0 WHERE id = #{id} " +
        "AND annotation_id = #{projectId} AND tenant_id = #{tenant}")
    int revive(@Param("id") Long id, @Param("projectId") Long projectId, @Param("tenant") String tenant);
}
