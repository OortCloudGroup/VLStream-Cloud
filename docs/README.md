# 数据库文件目录

本目录包含VLStream项目的数据库相关文件和文档。

## 目录结构

```
database/
├── README.md                      # 本文件
├── scene_governance/              # 场景治理模块
│   ├── README.md                 # 场景治理数据表设计说明
│   └── scene_governance_table.sql # 场景治理数据表SQL脚本
└── ...                           # 其他模块的数据库文件
```

## 模块说明

### 场景治理 (scene_governance)

- **功能**: 管理AI决策场景的配置和执行策略
- **表名**: `scene_governance`
- **文件**: 
  - `scene_governance_table.sql` - 建表SQL脚本
  - `README.md` - 详细设计说明

## 使用指南

1. **执行SQL脚本**: 按顺序执行各模块的SQL文件
2. **查看设计说明**: 每个模块都有详细的README文档
3. **版本控制**: 所有数据库变更应记录在相应的迁移脚本中

## 注意事项

- 建议在测试环境先验证SQL脚本
- 生产环境执行前请备份数据
- 遵循数据库命名规范和约束条件

## 联系方式

如有问题请联系开发团队。 