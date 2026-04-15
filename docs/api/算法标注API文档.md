# 算法标注API文档

## 概述

算法标注管理API提供了完整的算法标注数据管理功能，包括创建、查询、更新、删除标注任务，以及标注进度管理、数据导入导出、统计分析等功能。

## 基础信息

- **基础路径**: `/algorithm-annotation`
- **数据格式**: JSON
- **认证方式**: 根据系统配置

## 数据模型

### AlgorithmAnnotation 算法标注实体

```json
{
  "id": 1,
  "annotationName": "人员检测标注任务",
  "annotationType": "object_detection",
  "datasetPath": "/data/person_detection/",
  "totalCount": 1000,
  "annotatedCount": 500,
  "annotationStatus": "partial",
  "progress": 50,
  "annotationRules": "{\"classes\": [\"person\", \"car\"], \"format\": \"yolo\"}",
  "remark": "备注信息",
  "createdBy": 1,
  "createdTime": "2023-12-01T10:00:00",
  "updatedTime": "2023-12-01T15:30:00",
  "deleted": 0
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 标注ID |
| annotationName | String | 标注名称 |
| annotationType | String | 标注类型：`object_detection`、`image_classification`、`instance_segmentation`、`semantic_segmentation` |
| datasetPath | String | 数据集路径 |
| totalCount | Integer | 总数量 |
| annotatedCount | Integer | 已标注数量 |
| annotationStatus | String | 标注状态：`none`、`partial`、`completed` |
| progress | Integer | 标注进度百分比 |
| annotationRules | String | 标注规则（JSON格式） |
| remark | String | 备注 |
| createdBy | Long | 创建人ID |
| createdTime | DateTime | 创建时间 |
| updatedTime | DateTime | 更新时间 |
| deleted | Integer | 是否删除：0-否，1-是 |

## API接口

### 1. 查询接口

#### 1.1 分页查询算法标注列表

```http
GET /algorithm-annotation/page?current=1&size=10&annotationName=人员检测&annotationType=object_detection&annotationStatus=partial
```

**请求参数**：
- `current` (Integer, optional): 当前页，默认1
- `size` (Integer, optional): 每页大小，默认10  
- `annotationName` (String, optional): 标注名称（模糊查询）
- `annotationType` (String, optional): 标注类型
- `annotationStatus` (String, optional): 标注状态

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "annotationName": "人员检测标注任务",
        "annotationType": "object_detection",
        "annotationStatus": "partial",
        "progress": 50,
        "totalCount": 1000,
        "annotatedCount": 500
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

#### 1.2 根据标注类型查询

```http
GET /algorithm-annotation/type/{annotationType}
```

#### 1.3 根据标注状态查询

```http
GET /algorithm-annotation/status/{annotationStatus}
```

#### 1.4 根据ID查询标注详情

```http
GET /algorithm-annotation/{id}
```

### 2. 创建和更新接口

#### 2.1 创建算法标注

```http
POST /algorithm-annotation
```

**请求体**：
```json
{
  "annotationName": "人员检测标注任务",
  "annotationType": "object_detection",
  "datasetPath": "/data/person_detection/",
  "totalCount": 1000,
  "annotationRules": "{\"classes\": [\"person\", \"car\"], \"format\": \"yolo\"}",
  "remark": "备注信息"
}
```

#### 2.2 更新算法标注

```http
PUT /algorithm-annotation/{id}
```

**请求体**：与创建接口相同

### 3. 删除接口

#### 3.1 删除算法标注

```http
DELETE /algorithm-annotation/{id}
```

#### 3.2 批量删除算法标注

```http
DELETE /algorithm-annotation/batch
```

**请求体**：
```json
[1, 2, 3]
```

### 4. 标注任务管理接口

#### 4.1 更新标注进度

```http
PUT /algorithm-annotation/{id}/progress?annotatedCount=500
```

#### 4.2 批量更新标注状态

```http
PUT /algorithm-annotation/batch/status?annotationStatus=completed
```

**请求体**：
```json
[1, 2, 3]
```

#### 4.3 开始标注任务

```http
POST /algorithm-annotation/{id}/start
```

#### 4.4 完成标注任务

```http
POST /algorithm-annotation/{id}/complete
```

#### 4.5 重置标注任务

```http
POST /algorithm-annotation/{id}/reset
```

### 5. 数据导入导出接口

#### 5.1 导出标注数据

```http
POST /algorithm-annotation/{id}/export
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "message": "导出成功",
    "exportPath": "/exports/annotation_1.json",
    "annotationInfo": {
      "id": 1,
      "annotationName": "人员检测标注任务"
    }
  }
}
```

#### 5.2 导入标注数据

```http
POST /algorithm-annotation/{id}/import?dataPath=/data/import/annotation_data.json
```

#### 5.3 验证标注数据

```http
POST /algorithm-annotation/{id}/validate
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "message": "验证成功",
    "validCount": 500,
    "invalidCount": 0,
    "validationDetails": {}
  }
}
```

### 6. 统计分析接口

#### 6.1 获取标注类型统计

```http
GET /algorithm-annotation/statistics/type
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "annotation_type": "object_detection",
      "count": 15
    },
    {
      "annotation_type": "image_classification",
      "count": 8
    }
  ]
}
```

#### 6.2 获取标注状态统计

```http
GET /algorithm-annotation/statistics/status
```

#### 6.3 获取标注进度统计

```http
GET /algorithm-annotation/statistics/progress
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "progress_range": "0-25%",
      "count": 5
    },
    {
      "progress_range": "25-50%",
      "count": 8
    },
    {
      "progress_range": "50-75%",
      "count": 6
    },
    {
      "progress_range": "75-100%",
      "count": 4
    }
  ]
}
```

#### 6.4 获取标注工作量统计

```http
GET /algorithm-annotation/statistics/workload
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total_count": 10000,
    "annotated_count": 6500,
    "overall_progress": 65.00
  }
}
```

## 错误响应

所有API在出错时会返回以下格式：

```json
{
  "code": 400,
  "message": "标注名称已存在",
  "data": null
}
```

常见错误码：
- `400`: 请求参数错误
- `404`: 资源不存在  
- `500`: 服务器内部错误

## 标注类型说明

1. **object_detection**: 物体检测
   - 用于标注图像中的物体位置和类别
   - 输出格式：边界框坐标 + 类别标签

2. **image_classification**: 图像分类
   - 用于标注整张图像的分类标签
   - 输出格式：类别标签

3. **instance_segmentation**: 实例分割
   - 用于标注图像中每个物体实例的像素级分割
   - 输出格式：像素掩码 + 类别标签

4. **semantic_segmentation**: 语义分割
   - 用于标注图像中每个像素的语义类别
   - 输出格式：像素级语义标签

## 标注状态说明

- **none**: 未开始标注
- **partial**: 部分标注完成
- **completed**: 标注完成

## 注意事项

1. 创建标注时，系统会自动计算进度并设置状态
2. 更新标注进度时，系统会自动更新标注状态
3. 标注名称在系统中必须唯一
4. 删除操作为软删除，不会真正删除数据
5. 导入导出功能需要配置相应的文件存储路径 