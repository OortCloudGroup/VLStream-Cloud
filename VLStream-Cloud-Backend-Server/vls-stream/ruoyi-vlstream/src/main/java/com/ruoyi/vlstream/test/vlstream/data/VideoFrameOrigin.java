package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

@Data @EqualsAndHashCode(callSuper=true)
@TableName("vls_dataset_frame_origin")
public class VideoFrameOrigin extends TenantEntity {
    private Long datasetId;
    private Long videoId;
    private String videoName;
    private Long sampleId;
    private Long timestampMs;
    private Long requestedMs;
    private Long jobId;
}
