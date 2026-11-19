/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.common.enums.YesNoEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AlgorithmFormService {
    private final IVlsAlgorithmService algorithms;
    private final AlgorithmCoverService covers;
    private final LlmReviewManagementService reviews;

    @Transactional(rollbackFor = Exception.class)
    public Long create(Algorithm algorithm, MultipartFile file, AlgorithmLlmReviewConfig review) throws IOException {
        algorithm.setId(null);
        algorithm.setTenantId(TenantContextHolder.getTenantId());
        if (algorithm.getIsSystem() != YesNoEnum.NO) {
            algorithm.setIsSystem(YesNoEnum.YES);
            algorithm.setPtModelFilePath(null);
            algorithm.setOnnxModelFilePath(null);
        }
        covers.validate(algorithm);
        if (!algorithms.createAlgorithm(algorithm)) {
            throw new ServiceException("算法创建失败，同一仓库下名称可能已存在");
        }
        if (review != null && Boolean.TRUE.equals(review.getEnabled())) {
            review.setId(null);
            review.setTenantId(algorithm.getTenantId());
            reviews.saveAlgorithmConfig(algorithm.getId(), review);
        }
        if (file != null) {
            Algorithm cover = covers.upload(algorithm.getId(), file);
            if (!algorithms.updateById(cover)) throw new ServiceException("算法封面保存失败");
        }
        return algorithm.getId();
    }
}
