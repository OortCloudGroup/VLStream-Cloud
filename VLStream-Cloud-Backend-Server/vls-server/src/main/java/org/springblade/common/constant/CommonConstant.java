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
package org.springblade.common.constant;

/**
 * Common constants
 *
 * @author Chill
 */
public interface CommonConstant {

	/**
	 * sword system name
	 */
	String SWORD_NAME = "sword";

	/**
	 * saber system name
	 */
	String SABER_NAME = "saber";

	/**
	 * Top-level parent node ID
	 */
	Long TOP_PARENT_ID = 0L;

	/**
	 * Top-level parent node name
	 */
	String TOP_PARENT_NAME = "Top-level";

	/**
	 * Default password
	 */
	String DEFAULT_PASSWORD = "123456";

	/**
	 * Data permission type
	 */
	Integer DATA_SCOPE_CATEGORY = 1;

	/**
	 * Interface permission type
	 */
	Integer API_SCOPE_CATEGORY = 2;


	/**
	 * Dataset path
	 */
	String BASE_DATASETS_PATH = "/data/work/ultralytics_yolov8-main/datasets/";

	/**
	 * yolo working path
	 */
	String BASE_YOLO_PATH = "/data/work/ultralytics_yolov8-main/";

	/**
	 * synset file
	 */
	String SYNSET_TXT = "synset.txt";

}
