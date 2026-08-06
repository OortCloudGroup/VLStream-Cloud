-- Persist Hi3519DV500 SVP ACL OM artifacts for training tasks and published models.
ALTER TABLE `vls_algorithm_training`
    ADD COLUMN `om_model_output_path` varchar(500) NULL DEFAULT NULL COMMENT 'Hi3519DV500 OM模型输出路径'
    AFTER `int8_rknn_model_output_path`;

ALTER TABLE `vls_algorithm_model`
    ADD COLUMN `om_model_output_path` varchar(500) NULL DEFAULT NULL COMMENT 'Hi3519DV500 OM模型输出路径'
    AFTER `int8_rknn_model_output_path`;
