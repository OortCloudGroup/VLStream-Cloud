package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;


/**
 * 标签管理表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsTagManagementExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 标签名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签名称")
	private String tagName;
	/**
	 * 标签大类：own-自有标签，public-公共标签
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签大类：own-自有标签，public-公共标签")
	private String categoryType;
	/**
	 * 层级：1-标签类型，2-具体标签
	 */
	@ColumnWidth(20)
	@ExcelProperty("层级：1-标签类型，2-具体标签")
	private Byte level;
	/**
	 * 父级ID，level=1时为NULL，level=2时为标签类型ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("父级ID，level=1时为NULL，level=2时为标签类型ID")
	private Long parentId;
	/**
	 * 排序顺序
	 */
	@ColumnWidth(20)
	@ExcelProperty("排序顺序")
	private Integer sortOrder;
	/**
	 * 标签颜色
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签颜色")
	private String tagColor;
	/**
	 * 标签图标
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签图标")
	private String tagIcon;
	/**
	 * 标签描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签描述")
	private String description;
	/**
	 * 是否启用：1-启用，0-禁用
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否启用：1-启用，0-禁用")
	private Byte isActive;
	/**
	 * 使用次数
	 */
	@ColumnWidth(20)
	@ExcelProperty("使用次数")
	private Integer usageCount;

}
