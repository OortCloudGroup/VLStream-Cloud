package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.data.DatasetVersion;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DatasetVersionMapper extends BaseMapper<DatasetVersion> {
    @Select("SELECT * FROM vls_algorithm_annotation WHERE id = #{id} AND tenant_id = #{tenant} " +
        "AND is_deleted = 0 FOR UPDATE")
    AlgorithmAnnotation lockProject(@Param("id") Long id, @Param("tenant") String tenant);

    @Update("UPDATE vls_annotation_label SET is_deleted = 0 WHERE id = #{id} " +
        "AND annotation_id = #{projectId} AND tenant_id = #{tenant}")
    int reviveLabel(@Param("id") Long id, @Param("projectId") Long projectId, @Param("tenant") String tenant);

    @Update("UPDATE vls_annotation_instance SET is_deleted = 0 WHERE id = #{id} " +
        "AND annotation_id = #{projectId} AND tenant_id = #{tenant}")
    int reviveInstance(@Param("id") Long id, @Param("projectId") Long projectId, @Param("tenant") String tenant);
}
