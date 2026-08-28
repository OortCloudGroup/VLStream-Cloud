/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;


import javax.servlet.http.HttpServletResponse;

/**
 * datasetserviceinterface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DatasetService {

	/**
	 * service
	 *
	 * @param host service
	 * @param username user
	 * @param password
	 * @param path dataset
	 * @return whether successfully
	 */
	boolean connectToServer(String host, String username, String password, String path);

	/**
	 * Get dataset
	 *
	 * @param host service
	 * @param path dataset
	 * @return
	 */
	Object getDatasetFiles(String host, String path);

	/**
	 * Get
	 *
	 * @param host service
	 * @param path dataset
	 * @param filename
	 * @return
	 */
	String getFileContent(String host, String path, String filename);

	/**
	 *
	 *
	 * @param host service
	 * @param path dataset
	 * @param filename
	 * @param response HTTP object
	 */
	void downloadFile(String host, String path, String filename, HttpServletResponse response);
}
