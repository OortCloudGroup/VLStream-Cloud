package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 职务(职位)表
 *
 * @author Lion Li
 */

@Data
@TableName("sys_post_view")
@Alias("CommonSysPost")
public class SysPost {

    /**
     * 职务名称(老)
     */
    @TableField("oort_jobname")
    private String oortJobname;



    /**
     * 职务等级（数字越小等级越高）
     */
    @TableField("oort_level")
    private Integer oortLevel;

    /**
     * 创建时间
     */
    @TableField("oort_tcreate")
    private Long oortTcreate;

    /**
     * 修改时间
     */
    @TableField("oort_tupdate")
    private Long oortTupdate;

    /**
     * 是否删除 0否 1是
     */
    @TableField("oort_tdelete")
    private Integer oortTdelete;

    //新---------

    /**
     * 职位ID
     */
    @TableId("post_id")
    private String postId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 职务名称
     */
    @TableField("name")
    private String name;

    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private String tenantId;


    /**
     * 上级职位ID
     */
    @TableField("ppost_id")
    private String  ppostId;

    /**
     * 编码
     */
    @TableField("code")
    private String  code;

    /**
     * 职位类型
     */
    @TableField("type")
    private String  type;


}
