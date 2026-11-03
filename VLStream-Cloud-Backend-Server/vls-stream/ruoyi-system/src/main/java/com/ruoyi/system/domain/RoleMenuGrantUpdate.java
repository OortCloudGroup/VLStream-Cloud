package com.ruoyi.system.domain;

import lombok.Data;

import java.util.List;

@Data
public class RoleMenuGrantUpdate {
    private List<String> menuIds;
    private List<String> expectedMenuIds;
}
