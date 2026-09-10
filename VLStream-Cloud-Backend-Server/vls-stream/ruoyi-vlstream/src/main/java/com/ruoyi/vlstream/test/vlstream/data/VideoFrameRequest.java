package com.ruoyi.vlstream.test.vlstream.data;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class VideoFrameRequest {
    @NotNull private Long datasetId;
    @NotNull private Long videoId;
    @NotNull @DecimalMin("0") @DecimalMax("86400") private BigDecimal startSeconds = BigDecimal.ZERO;
    @DecimalMin("0.001") @DecimalMax("86400") private BigDecimal endSeconds;
    @NotNull @DecimalMin("0.1") @DecimalMax("3600") private BigDecimal intervalSeconds = BigDecimal.ONE;
    @Min(1) @Max(1000) private int maxFrames = 100;
}
