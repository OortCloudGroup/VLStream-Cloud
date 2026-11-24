package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.system.domain.VlsPublicFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VlsPublicFileMapper extends BaseMapper<VlsPublicFile> {

    // Public links look up only the download metadata for an exact file ID.
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT file_id, original_name, file_size, storage_config_key FROM vls_public_file WHERE file_id = #{fileId}")
    VlsPublicFile findPublishedFile(@Param("fileId") String fileId);
}
