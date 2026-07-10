# 决策式AI模块 (DecisionAI)

这个文件夹包含了所有与决策式AI相关的页面组件，主要用于事件处理和场景治理。

## 模块结构

```
DecisionAI/
├── index.js                    # 模块统一导出文件
├── EventManagement.vue         # 事件管理页面
├── SceneGovernance.vue         # 场景治理页面
└── README.md                   # 模块说明文档
```

## 页面功能说明

### EventManagement.vue - 事件管理
- 事件创建和管理
- 事件状态跟踪
- 事件处理流程
- 事件统计和分析

### SceneGovernance.vue - 场景治理
- 场景配置管理
- 场景规则设置
- 场景监控和治理
- 场景效果评估

## 路由配置

所有页面的路由配置：
- `/event-management` → `@/views/DecisionAI/EventManagement.vue`
- `/scene-governance` → `@/views/DecisionAI/SceneGovernance.vue`

## 使用方式

可以通过统一导出文件引入组件：

```javascript
import { EventManagement, SceneGovernance } from '@/views/DecisionAI'
```

或者直接引入单个组件：

```javascript
import EventManagement from '@/views/DecisionAI/EventManagement.vue'
``` 