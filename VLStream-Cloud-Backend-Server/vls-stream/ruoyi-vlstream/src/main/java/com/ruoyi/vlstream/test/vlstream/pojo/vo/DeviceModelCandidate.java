package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import lombok.Data;

/** Public selection data; remote artifact paths remain on the server. */
@Data
public class DeviceModelCandidate {
    private String id;
    private String modelName;
    private Integer version;
    private String algorithmName;
    private String category;
    private String imageUrl;
    private String description;
    private Boolean deployable;
}
