package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;


/**
 * 算法仓库表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmRepositoryExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 算法仓库名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法仓库名称")
	private String name;
	/**
	 * 拥有算法数量
	 */
	@ColumnWidth(20)
	@ExcelProperty("拥有算法数量")
	private Integer algorithmCount;
	/**
	 * 仓库类型：basic-基础预置, extended-扩展
	 */
	@ColumnWidth(20)
	@ExcelProperty("仓库类型：basic-基础预置, extended-扩展")
	private String repositoryType;
	/**
	 * 备注
	 */
	@ColumnWidth(20)
	@ExcelProperty("备注")
	private String remark;

}
