package com.vlstream.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 文件存储响应类
 * </p>
 *
 * @author 刘鑫
 * @since 2025-04-07
 */
@Data
@Schema(description = "文件存储响应类")
public class FileResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "文件访问URL", example = "文件访问URL:http://example.com/files/2024/04/01/audio.mp3")
    private String url;

    @Schema(description =  "文件MD5值", example = "文件MD5值:d41d8cd98f00b204e9800998ecf8427e")
    private String md5;

    @Schema(description =  "文件存储路径", example = "文件存储路径:/files/2024/04/01/audio.mp3")
    private String path;

    @Schema(description =  "文件域名", example = "文件域名:http://example.com")
    private String domain;

    @Schema(description =  "文件场景", example = "文件场景:audio")
    private String scene;

    @Schema(description =  "文件大小", example = "文件大小:1024000")
    private Long size;

    @Schema(description =  "文件修改时间", example = "文件修改时间:1711968000")
    private Long mtime;

    @Schema(description =  "文件场景列表", example = "文件场景列表:audio,video,image")
    private String scenes;

    @Schema(description =  "返回消息", example = "返回消息:200")
    private String retmsg;

    @Schema(description =  "返回码", example = "返回码:200")
    private Integer retcode;

    @Schema(description =  "源文件路径", example = "源文件路径:/upload/2024/04/01/audio.mp3")
    private String src;

    @Schema(description =  "文件时长(秒)", example = "文件时长(秒):300")
    private Integer duration;
}
