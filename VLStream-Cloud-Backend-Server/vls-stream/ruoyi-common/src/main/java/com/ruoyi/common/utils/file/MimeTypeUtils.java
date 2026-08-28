/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.utils.file;

/**
 *
 *
 * @author ruoyi
 */
public class MimeTypeUtils {
    public static final String IMAGE_PNG = "image/png";

    public static final String IMAGE_JPG = "image/jpg";

    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String IMAGE_BMP = "image/bmp";

    public static final String IMAGE_GIF = "image/gif";

    public static final String[] IMAGE_EXTENSION = {"bmp", "gif", "jpg", "jpeg", "png"};

    public static final String[] FLASH_EXTENSION = {"swf", "flv"};

    public static final String[] MEDIA_EXTENSION = {"swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg",
        "asf", "rm", "rmvb"};

    public static final String[] VIDEO_EXTENSION = {"mp4", "avi", "rmvb"};

    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
        //
        "bmp", "gif", "jpg", "jpeg", "png",
        // word excel powerpoint
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt",
        //
        "rar", "zip", "gz", "bz2",
        //
        "mp4", "avi", "rmvb",
        // pdf
        "pdf"};

}
