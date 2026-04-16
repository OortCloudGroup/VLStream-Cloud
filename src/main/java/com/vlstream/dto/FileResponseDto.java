package com.vlstream.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * File Storage Response Class
 * </p>
 *
 * @author 刘鑫
 * @since 2025-04-07
 */
@Data
@Schema(description = "File storage response class")
public class FileResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "File access URL", example = "File access URL:http://example.com/files/2024/04/01/audio.mp3")
    private String url;

    @Schema(description =  "File MD5 value", example = "File MD5 value:d41d8cd98f00b204e9800998ecf8427e")
    private String md5;

    @Schema(description =  "File storage path", example = "File storage path:/files/2024/04/01/audio.mp3")
    private String path;

    @Schema(description =  "File domain", example = "File domain:http://example.com")
    private String domain;

    @Schema(description =  "File scene", example = "File scene:audio")
    private String scene;

    @Schema(description =  "File size", example = "File size:1024000")
    private Long size;

    @Schema(description =  "File modification time", example = "File modification time:1711968000")
    private Long mtime;

    @Schema(description =  "File scene list", example = "File scene list:audio,video,image")
    private String scenes;

    @Schema(description =  "Return message", example = "Return message:200")
    private String retmsg;

    @Schema(description =  "Return code", example = "Return code:200")
    private Integer retcode;

    @Schema(description =  "Source file path", example = "Source file path:/upload/2024/04/01/audio.mp3")
    private String src;

    @Schema(description =  "File duration (seconds)", example = "File duration (seconds):300")
    private Integer duration;
}
