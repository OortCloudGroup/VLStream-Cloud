ALTER TABLE vls_algorithm_training
    ADD COLUMN onnx_conversion_status VARCHAR(20) NULL COMMENT 'ONNX转换状态：converting/completed/failed' AFTER onnx_model_output_path,
    ADD COLUMN onnx_conversion_error TEXT NULL COMMENT 'ONNX转换失败原因' AFTER onnx_conversion_status,
    ADD COLUMN om_conversion_status VARCHAR(20) NULL COMMENT 'OM转换状态：converting/completed/failed' AFTER om_model_output_path,
    ADD COLUMN om_conversion_error TEXT NULL COMMENT 'OM转换失败原因' AFTER om_conversion_status;
