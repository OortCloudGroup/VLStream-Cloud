package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.VlsPublicFile;
import com.ruoyi.system.service.PublicFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
public class PublicFileController {

    public static final String PUBLIC_PATH = "/system/oss/public/";
    public static final String FILE_ID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final PublicFileService files;
    private final String publicBaseUrl;

    public PublicFileController(PublicFileService files, @Value("${VLSTREAM_PUBLIC_FILES_BASE_URL:}") String publicBaseUrl) {
        this.files = files;
        this.publicBaseUrl = publicBaseUrl.trim().replaceAll("/+$", "");
    }

    @SaCheckLogin
    @PostMapping(value = "/system/oss/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<R<UploadReceipt>> upload(@RequestPart(value = "file", required = false) List<MultipartFile> parts,
                                                    HttpServletRequest request) {
        String tenantId = TenantContextHolder.getTenantId();
        LoginUser user = LoginHelper.getLoginUser();
        if (StringUtils.isBlank(tenantId) || user == null || StringUtils.isBlank(user.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail("无法确认当前用户及租户"));
        }
        if (parts == null || parts.size() != 1 || parts.get(0).isEmpty()) {
            return ResponseEntity.badRequest().body(R.fail("请提交一个非空文件"));
        }
        MultipartFile file = parts.get(0);
        if (file.getSize() > PublicFileService.MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(R.fail("单个文件不能超过 50 MiB"));
        }
        String baseUrl = publicBaseUrl.isEmpty()
            ? ServletUriComponentsBuilder.fromContextPath(request).build().toUriString()
            : publicBaseUrl;
        VlsPublicFile stored = files.upload(file, tenantId, user.getUserId());
        return ResponseEntity.ok(R.ok(new UploadReceipt(stored.getFileId(), stored.getOriginalName(),
            Math.toIntExact(stored.getFileSize()), baseUrl + PUBLIC_PATH + stored.getFileId())));
    }

    @RequestMapping(value = PUBLIC_PATH + "{fileId}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public void download(@PathVariable String fileId, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        boolean head = "HEAD".equals(request.getMethod());
        VlsPublicFile file;
        InputStream content = null;
        try {
            file = files.find(fileId);
            if (file == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (head) {
                files.checkExists(file);
            } else {
                content = files.open(file);
            }
        } catch (RuntimeException failure) {
            if (failure instanceof AmazonS3Exception && ((AmazonS3Exception) failure).getStatusCode() == 404) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                log.warn("Public file {} could not be read", fileId, failure);
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            return;
        }
        try (InputStream input = content) {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLengthLong(file.getFileSize());
            FileUtils.setAttachmentResponseHeader(response, file.getOriginalName());
            if (input != null) {
                StreamUtils.copy(input, response.getOutputStream());
            }
        }
    }

    @lombok.Value
    public static class UploadReceipt {
        String fileId;
        String fileName;
        int size;
        String url;
    }
}
