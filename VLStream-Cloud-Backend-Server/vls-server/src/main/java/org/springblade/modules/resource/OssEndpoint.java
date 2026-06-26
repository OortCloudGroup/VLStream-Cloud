/**
 * Copyright (c) 2018-2099, Chill Zhuang (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.modules.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.oss.QiniuTemplate;
import org.springblade.core.oss.model.BladeFile;
import org.springblade.core.oss.model.OssFile;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Object storage endpoint
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_RESOURCE_NAME + "/oss/endpoint")
@Tag(name = "Object storage endpoint", description = "Object storage endpoint")
public class OssEndpoint {

	private QiniuTemplate qiniuTemplate;

	/**
	 * Create bucket
	 *
	 * @param bucketName bucket name
	 * @return Bucket
	 */
	@SneakyThrows
	@PostMapping("/make-bucket")
	public R makeBucket(@RequestParam String bucketName) {
		qiniuTemplate.makeBucket(bucketName);
		return R.success("Created successfully");
	}

	/**
	 * Create bucket
	 *
	 * @param bucketName bucket name
	 * @return R
	 */
	@SneakyThrows
	@PostMapping("/remove-bucket")
	public R removeBucket(@RequestParam String bucketName) {
		qiniuTemplate.removeBucket(bucketName);
		return R.success("Deleted successfully");
	}

	/**
	 * Copy file
	 *
	 * @param fileName       bucket object name
	 * @param destBucketName destination bucket name
	 * @param destFileName   destination object name
	 * @return R
	 */
	@SneakyThrows
	@PostMapping("/copy-file")
	public R copyFile(@RequestParam String fileName, @RequestParam String destBucketName, String destFileName) {
		qiniuTemplate.copyFile(fileName, destBucketName, destFileName);
		return R.success("Operation successful");
	}

	/**
	 * Get file info
	 *
	 * @param fileName bucket object name
	 * @return InputStream
	 */
	@SneakyThrows
	@GetMapping("/stat-file")
	public R<OssFile> statFile(@RequestParam String fileName) {
		return R.data(qiniuTemplate.statFile(fileName));
	}

	/**
	 * Get file relative path
	 *
	 * @param fileName bucket object name
	 * @return String
	 */
	@SneakyThrows
	@GetMapping("/file-path")
	public R<String> filePath(@RequestParam String fileName) {
		return R.data(qiniuTemplate.filePath(fileName));
	}


	/**
	 * Get file external link
	 *
	 * @param fileName bucket object name
	 * @return String
	 */
	@SneakyThrows
	@GetMapping("/file-link")
	public R<String> fileLink(@RequestParam String fileName) {
		return R.data(qiniuTemplate.fileLink(fileName));
	}

	/**
	 * Upload file
	 *
	 * @param file file
	 * @return ObjectStat
	 */
	@SneakyThrows
	@PostMapping("/put-file")
	public R<BladeFile> putFile(@RequestParam MultipartFile file) {
		BladeFile bladeFile = qiniuTemplate.putFile(file.getOriginalFilename(), file.getInputStream());
		return R.data(bladeFile);
	}

	/**
	 * Upload file
	 *
	 * @param fileName bucket object name
	 * @param file     file
	 * @return ObjectStat
	 */
	@SneakyThrows
	@PostMapping("/put-file-by-name")
	public R<BladeFile> putFile(@RequestParam String fileName, @RequestParam MultipartFile file) {
		BladeFile bladeFile = qiniuTemplate.putFile(fileName, file.getInputStream());
		return R.data(bladeFile);
	}

	/**
	 * Delete file
	 *
	 * @param fileName bucket object name
	 * @return R
	 */
	@SneakyThrows
	@PostMapping("/remove-file")
	public R removeFile(@RequestParam String fileName) {
		qiniuTemplate.removeFile(fileName);
		return R.success("Operation successful");
	}

	/**
	 * Batch delete files
	 *
	 * @param fileNames bucket object name collection
	 * @return R
	 */
	@SneakyThrows
	@PostMapping("/remove-files")
	public R removeFiles(@RequestParam String fileNames) {
		qiniuTemplate.removeFiles(Func.toStrList(fileNames));
		return R.success("Operation successful");
	}

}
