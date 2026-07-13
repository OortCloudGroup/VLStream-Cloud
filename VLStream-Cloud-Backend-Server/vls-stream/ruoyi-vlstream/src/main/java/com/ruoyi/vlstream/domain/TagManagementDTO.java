package com.ruoyi.vlstream.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * VLS tag tree node.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TagManagementDTO extends TagManagement {

    private static final long serialVersionUID = 1L;

    private List<TagManagementDTO> children = new ArrayList<TagManagementDTO>();
}
