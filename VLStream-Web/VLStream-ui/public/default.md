# VLStream Cloud API文档


**简介**:VLStream Cloud API文档


**HOST**:192.168.60.75:32557


**联系人**:VLStream Team


**Version**:1.0.0


**接口路径**:/v2/api-docs


[TOC]






# WebRTC流控制器


## getActiveStreams


**接口地址**:`/api/webrtc/active`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,string»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## checkStreamStatus


**接口地址**:`/api/webrtc/check/{deviceId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|deviceId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getWebRTCConfig


**接口地址**:`/api/webrtc/config`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## refreshWebRTCConnection


**接口地址**:`/api/webrtc/refresh`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## startWebRTCStream


**接口地址**:`/api/webrtc/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": "",
  "mode": "",
  "quality": "",
  "rtspUrl": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StreamRequest|StreamRequest|
|&emsp;&emsp;deviceId|||false|string||
|&emsp;&emsp;mode|||false|string||
|&emsp;&emsp;quality|||false|string||
|&emsp;&emsp;rtspUrl|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getWebRTCStatus


**接口地址**:`/api/webrtc/status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## stopWebRTCStream


**接口地址**:`/api/webrtc/stop`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StopStreamRequest|StopStreamRequest|
|&emsp;&emsp;deviceId|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## stopAllStreams


**接口地址**:`/api/webrtc/stop-all`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## validateRtspStream


**接口地址**:`/api/webrtc/validate`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 事件管理


## 创建事件


**接口地址**:`/api/event-management`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "createdBy": 0,
  "createdTime": "",
  "deleted": 0,
  "eventData": "",
  "eventDesc": "",
  "eventLevel": "",
  "eventStatus": "",
  "eventType": "",
  "executor": "",
  "executorIds": "",
  "feedbackImg": "",
  "feedbackInfo": "",
  "feedbackStatus": 0,
  "handleResult": "",
  "id": 0,
  "reportDevice": "",
  "reportImg": "",
  "reportLocation": "",
  "reportTime": "",
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|eventManagement|Event management record|body|true|EventManagement|EventManagement|
|&emsp;&emsp;createdBy|创建人||false|integer(int64)||
|&emsp;&emsp;createdTime|创建时间||false|string(date-time)||
|&emsp;&emsp;deleted|逻辑删除: 0-normal,1-deleted||false|integer(int32)||
|&emsp;&emsp;eventData|事件数据||false|string||
|&emsp;&emsp;eventDesc|事件描述||false|string||
|&emsp;&emsp;eventLevel|事件级别：low-低,medium-中,high-高,urgent-紧急||false|string||
|&emsp;&emsp;eventStatus|事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭||false|string||
|&emsp;&emsp;eventType|事件类型||false|string||
|&emsp;&emsp;executor|执行人||false|string||
|&emsp;&emsp;executorIds|执行人ID列表||false|string||
|&emsp;&emsp;feedbackImg|反馈图片||false|string||
|&emsp;&emsp;feedbackInfo|反馈信息||false|string||
|&emsp;&emsp;feedbackStatus|反馈状态||false|integer(int32)||
|&emsp;&emsp;handleResult|处理结果||false|string||
|&emsp;&emsp;id|Primary key||false|integer(int64)||
|&emsp;&emsp;reportDevice|上报设备||false|string||
|&emsp;&emsp;reportImg|上报图片||false|string||
|&emsp;&emsp;reportLocation|上报位置||false|string||
|&emsp;&emsp;reportTime|上报时间||false|string(date-time)||
|&emsp;&emsp;updatedTime|更新时间||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新事件


**接口地址**:`/api/event-management`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "createdBy": 0,
  "createdTime": "",
  "deleted": 0,
  "eventData": "",
  "eventDesc": "",
  "eventLevel": "",
  "eventStatus": "",
  "eventType": "",
  "executor": "",
  "executorIds": "",
  "feedbackImg": "",
  "feedbackInfo": "",
  "feedbackStatus": 0,
  "handleResult": "",
  "id": 0,
  "reportDevice": "",
  "reportImg": "",
  "reportLocation": "",
  "reportTime": "",
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|eventManagement|Event management record|body|true|EventManagement|EventManagement|
|&emsp;&emsp;createdBy|创建人||false|integer(int64)||
|&emsp;&emsp;createdTime|创建时间||false|string(date-time)||
|&emsp;&emsp;deleted|逻辑删除: 0-normal,1-deleted||false|integer(int32)||
|&emsp;&emsp;eventData|事件数据||false|string||
|&emsp;&emsp;eventDesc|事件描述||false|string||
|&emsp;&emsp;eventLevel|事件级别：low-低,medium-中,high-高,urgent-紧急||false|string||
|&emsp;&emsp;eventStatus|事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭||false|string||
|&emsp;&emsp;eventType|事件类型||false|string||
|&emsp;&emsp;executor|执行人||false|string||
|&emsp;&emsp;executorIds|执行人ID列表||false|string||
|&emsp;&emsp;feedbackImg|反馈图片||false|string||
|&emsp;&emsp;feedbackInfo|反馈信息||false|string||
|&emsp;&emsp;feedbackStatus|反馈状态||false|integer(int32)||
|&emsp;&emsp;handleResult|处理结果||false|string||
|&emsp;&emsp;id|Primary key||false|integer(int64)||
|&emsp;&emsp;reportDevice|上报设备||false|string||
|&emsp;&emsp;reportImg|上报图片||false|string||
|&emsp;&emsp;reportLocation|上报位置||false|string||
|&emsp;&emsp;reportTime|上报时间||false|string(date-time)||
|&emsp;&emsp;updatedTime|更新时间||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量保存事件


**接口地址**:`/api/event-management/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询


**接口地址**:`/api/event-management/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|current|Current page|query|false|integer(int64)||
|endTime|Report time to|query|false|string(date-time)||
|eventLevel|Event level|query|false|string||
|eventStatus|Event status|query|false|string||
|eventType|Event type|query|false|string||
|keyword|Search keyword|query|false|string||
|size|Page size|query|false|integer(int64)||
|startTime|Report time from|query|false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«EventManagement»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«EventManagement»|IPage«EventManagement»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|EventManagement|
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|integer||
|&emsp;&emsp;&emsp;&emsp;createdTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;deleted|逻辑删除: 0-normal,1-deleted|integer||
|&emsp;&emsp;&emsp;&emsp;eventData|事件数据|string||
|&emsp;&emsp;&emsp;&emsp;eventDesc|事件描述|string||
|&emsp;&emsp;&emsp;&emsp;eventLevel|事件级别：low-低,medium-中,high-高,urgent-紧急|string||
|&emsp;&emsp;&emsp;&emsp;eventStatus|事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭|string||
|&emsp;&emsp;&emsp;&emsp;eventType|事件类型|string||
|&emsp;&emsp;&emsp;&emsp;executor|执行人|string||
|&emsp;&emsp;&emsp;&emsp;executorIds|执行人ID列表|string||
|&emsp;&emsp;&emsp;&emsp;feedbackImg|反馈图片|string||
|&emsp;&emsp;&emsp;&emsp;feedbackInfo|反馈信息|string||
|&emsp;&emsp;&emsp;&emsp;feedbackStatus|反馈状态|integer||
|&emsp;&emsp;&emsp;&emsp;handleResult|处理结果|string||
|&emsp;&emsp;&emsp;&emsp;id|Primary key|integer||
|&emsp;&emsp;&emsp;&emsp;reportDevice|上报设备|string||
|&emsp;&emsp;&emsp;&emsp;reportImg|上报图片|string||
|&emsp;&emsp;&emsp;&emsp;reportLocation|上报位置|string||
|&emsp;&emsp;&emsp;&emsp;reportTime|上报时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedTime|更新时间|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"createdBy": 0,
				"createdTime": "",
				"deleted": 0,
				"eventData": "",
				"eventDesc": "",
				"eventLevel": "",
				"eventStatus": "",
				"eventType": "",
				"executor": "",
				"executorIds": "",
				"feedbackImg": "",
				"feedbackInfo": "",
				"feedbackStatus": 0,
				"handleResult": "",
				"id": 0,
				"reportDevice": "",
				"reportImg": "",
				"reportLocation": "",
				"reportTime": "",
				"updatedTime": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据id查询详情


**接口地址**:`/api/event-management/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|Primary id|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«EventManagement»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||EventManagement|EventManagement|
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|逻辑删除: 0-normal,1-deleted|integer(int32)||
|&emsp;&emsp;eventData|事件数据|string||
|&emsp;&emsp;eventDesc|事件描述|string||
|&emsp;&emsp;eventLevel|事件级别：low-低,medium-中,high-高,urgent-紧急|string||
|&emsp;&emsp;eventStatus|事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭|string||
|&emsp;&emsp;eventType|事件类型|string||
|&emsp;&emsp;executor|执行人|string||
|&emsp;&emsp;executorIds|执行人ID列表|string||
|&emsp;&emsp;feedbackImg|反馈图片|string||
|&emsp;&emsp;feedbackInfo|反馈信息|string||
|&emsp;&emsp;feedbackStatus|反馈状态|integer(int32)||
|&emsp;&emsp;handleResult|处理结果|string||
|&emsp;&emsp;id|Primary key|integer(int64)||
|&emsp;&emsp;reportDevice|上报设备|string||
|&emsp;&emsp;reportImg|上报图片|string||
|&emsp;&emsp;reportLocation|上报位置|string||
|&emsp;&emsp;reportTime|上报时间|string(date-time)||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"createdBy": 0,
		"createdTime": "",
		"deleted": 0,
		"eventData": "",
		"eventDesc": "",
		"eventLevel": "",
		"eventStatus": "",
		"eventType": "",
		"executor": "",
		"executorIds": "",
		"feedbackImg": "",
		"feedbackInfo": "",
		"feedbackStatus": 0,
		"handleResult": "",
		"id": 0,
		"reportDevice": "",
		"reportImg": "",
		"reportLocation": "",
		"reportTime": "",
		"updatedTime": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除事件


**接口地址**:`/api/event-management/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|Primary id|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新事件状态


**接口地址**:`/api/event-management/{id}/status`


**请求方式**:`PATCH`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|Primary id|path|true|integer(int64)||
|status|Status|query|true|string||
|executor|Executor|query|false|string||
|handleResult|Handle result|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 图片上传管理


## uploadImage


**接口地址**:`/api/image/upload`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|files||formData|false|array|file|
|fileNames||formData|false|array|string|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 场景治理


## 新增场景治理信息


**接口地址**:`/api/scene-governance`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "cameras": "",
  "createdAt": "",
  "createdBy": "",
  "deleted": 0,
  "description": "",
  "devices": "",
  "endTime": "",
  "executeType": "",
  "id": 0,
  "intervalNum": 0,
  "location": "",
  "name": "",
  "rules": "",
  "selectedDays": "",
  "startTime": "",
  "status": "",
  "updatedAt": "",
  "updatedBy": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sceneGovernance|场景治理|body|true|SceneGovernance对象|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法||false|string||
|&emsp;&emsp;cameras|摄像头||false|string||
|&emsp;&emsp;createdAt|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除||false|integer(int32)||
|&emsp;&emsp;description|场景描述/备注||false|string||
|&emsp;&emsp;devices|关联设备||false|string||
|&emsp;&emsp;endTime|结束时间||false|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月||false|string||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量||false|integer(int32)||
|&emsp;&emsp;location|区划地点||false|string||
|&emsp;&emsp;name|场景名称||false|string||
|&emsp;&emsp;rules|治理规则||false|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）||false|string||
|&emsp;&emsp;startTime|开始时间||false|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用||false|string||
|&emsp;&emsp;updatedAt|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除场景治理信息


**接口地址**:`/api/scene-governance/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|场景ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量禁用场景治理


**接口地址**:`/api/scene-governance/batch/disable`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|场景ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量启用场景治理


**接口地址**:`/api/scene-governance/batch/enable`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|场景ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 检查场景名称是否存在


**接口地址**:`/api/scene-governance/check-name`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|name|场景名称|query|true|string||
|id|场景ID（编辑时排除自己）|query|false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据执行类型查询场景治理列表


**接口地址**:`/api/scene-governance/execute-type/{executeType}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|executeType|执行类型|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«SceneGovernance对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法|string||
|&emsp;&emsp;cameras|摄像头|string||
|&emsp;&emsp;createdAt|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|场景描述/备注|string||
|&emsp;&emsp;devices|关联设备|string||
|&emsp;&emsp;endTime|结束时间|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量|integer(int32)||
|&emsp;&emsp;location|区划地点|string||
|&emsp;&emsp;name|场景名称|string||
|&emsp;&emsp;rules|治理规则|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）|string||
|&emsp;&emsp;startTime|开始时间|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用|string||
|&emsp;&emsp;updatedAt|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"algorithm": "",
			"cameras": "",
			"createdAt": "",
			"createdBy": "",
			"deleted": 0,
			"description": "",
			"devices": "",
			"endTime": "",
			"executeType": "",
			"id": 0,
			"intervalNum": 0,
			"location": "",
			"name": "",
			"rules": "",
			"selectedDays": "",
			"startTime": "",
			"status": "",
			"updatedAt": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取所有执行类型列表


**接口地址**:`/api/scene-governance/execute-types`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«string»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 导出场景治理信息


**接口地址**:`/api/scene-governance/export`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sceneIds|sceneIds|query|false|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«SceneGovernance对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法|string||
|&emsp;&emsp;cameras|摄像头|string||
|&emsp;&emsp;createdAt|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|场景描述/备注|string||
|&emsp;&emsp;devices|关联设备|string||
|&emsp;&emsp;endTime|结束时间|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量|integer(int32)||
|&emsp;&emsp;location|区划地点|string||
|&emsp;&emsp;name|场景名称|string||
|&emsp;&emsp;rules|治理规则|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）|string||
|&emsp;&emsp;startTime|开始时间|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用|string||
|&emsp;&emsp;updatedAt|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"algorithm": "",
			"cameras": "",
			"createdAt": "",
			"createdBy": "",
			"deleted": 0,
			"description": "",
			"devices": "",
			"endTime": "",
			"executeType": "",
			"id": 0,
			"intervalNum": 0,
			"location": "",
			"name": "",
			"rules": "",
			"selectedDays": "",
			"startTime": "",
			"status": "",
			"updatedAt": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量导入场景治理


**接口地址**:`/api/scene-governance/import`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[
  {
    "algorithm": "",
    "cameras": "",
    "createdAt": "",
    "createdBy": "",
    "deleted": 0,
    "description": "",
    "devices": "",
    "endTime": "",
    "executeType": "",
    "id": 0,
    "intervalNum": 0,
    "location": "",
    "name": "",
    "rules": "",
    "selectedDays": "",
    "startTime": "",
    "status": "",
    "updatedAt": "",
    "updatedBy": ""
  }
]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sceneGovernanceList|场景治理|body|true|array|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法||false|string||
|&emsp;&emsp;cameras|摄像头||false|string||
|&emsp;&emsp;createdAt|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除||false|integer(int32)||
|&emsp;&emsp;description|场景描述/备注||false|string||
|&emsp;&emsp;devices|关联设备||false|string||
|&emsp;&emsp;endTime|结束时间||false|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月||false|string||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量||false|integer(int32)||
|&emsp;&emsp;location|区划地点||false|string||
|&emsp;&emsp;name|场景名称||false|string||
|&emsp;&emsp;rules|治理规则||false|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）||false|string||
|&emsp;&emsp;startTime|开始时间||false|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用||false|string||
|&emsp;&emsp;updatedAt|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据名称查询场景治理信息


**接口地址**:`/api/scene-governance/name/{name}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|name|场景名称|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«SceneGovernance对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||SceneGovernance对象|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法|string||
|&emsp;&emsp;cameras|摄像头|string||
|&emsp;&emsp;createdAt|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|场景描述/备注|string||
|&emsp;&emsp;devices|关联设备|string||
|&emsp;&emsp;endTime|结束时间|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量|integer(int32)||
|&emsp;&emsp;location|区划地点|string||
|&emsp;&emsp;name|场景名称|string||
|&emsp;&emsp;rules|治理规则|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）|string||
|&emsp;&emsp;startTime|开始时间|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用|string||
|&emsp;&emsp;updatedAt|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"algorithm": "",
		"cameras": "",
		"createdAt": "",
		"createdBy": "",
		"deleted": 0,
		"description": "",
		"devices": "",
		"endTime": "",
		"executeType": "",
		"id": 0,
		"intervalNum": 0,
		"location": "",
		"name": "",
		"rules": "",
		"selectedDays": "",
		"startTime": "",
		"status": "",
		"updatedAt": "",
		"updatedBy": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询场景治理信息


**接口地址**:`/api/scene-governance/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|current|当前页|query|false|integer(int64)||
|endDate|结束日期|query|false|string||
|name|场景名称|query|false|string||
|size|每页大小|query|false|integer(int64)||
|startDate|开始日期|query|false|string||
|status|场景状态|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«SceneGovernance对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«SceneGovernance对象»|IPage«SceneGovernance对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|SceneGovernance对象|
|&emsp;&emsp;&emsp;&emsp;algorithm|AI算法|string||
|&emsp;&emsp;&emsp;&emsp;cameras|摄像头|string||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer||
|&emsp;&emsp;&emsp;&emsp;description|场景描述/备注|string||
|&emsp;&emsp;&emsp;&emsp;devices|关联设备|string||
|&emsp;&emsp;&emsp;&emsp;endTime|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月|string||
|&emsp;&emsp;&emsp;&emsp;id|主键ID|integer||
|&emsp;&emsp;&emsp;&emsp;intervalNum|间隔数量|integer||
|&emsp;&emsp;&emsp;&emsp;location|区划地点|string||
|&emsp;&emsp;&emsp;&emsp;name|场景名称|string||
|&emsp;&emsp;&emsp;&emsp;rules|治理规则|string||
|&emsp;&emsp;&emsp;&emsp;selectedDays|选择的天数（JSON数组）|string||
|&emsp;&emsp;&emsp;&emsp;startTime|开始时间|string||
|&emsp;&emsp;&emsp;&emsp;status|状态: enabled-启用, disabled-禁用|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"algorithm": "",
				"cameras": "",
				"createdAt": "",
				"createdBy": "",
				"deleted": 0,
				"description": "",
				"devices": "",
				"endTime": "",
				"executeType": "",
				"id": 0,
				"intervalNum": 0,
				"location": "",
				"name": "",
				"rules": "",
				"selectedDays": "",
				"startTime": "",
				"status": "",
				"updatedAt": "",
				"updatedBy": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取场景治理统计信息


**接口地址**:`/api/scene-governance/statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据状态查询场景治理列表


**接口地址**:`/api/scene-governance/status/{status}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|status|场景状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«SceneGovernance对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法|string||
|&emsp;&emsp;cameras|摄像头|string||
|&emsp;&emsp;createdAt|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|场景描述/备注|string||
|&emsp;&emsp;devices|关联设备|string||
|&emsp;&emsp;endTime|结束时间|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量|integer(int32)||
|&emsp;&emsp;location|区划地点|string||
|&emsp;&emsp;name|场景名称|string||
|&emsp;&emsp;rules|治理规则|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）|string||
|&emsp;&emsp;startTime|开始时间|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用|string||
|&emsp;&emsp;updatedAt|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"algorithm": "",
			"cameras": "",
			"createdAt": "",
			"createdBy": "",
			"deleted": 0,
			"description": "",
			"devices": "",
			"endTime": "",
			"executeType": "",
			"id": 0,
			"intervalNum": 0,
			"location": "",
			"name": "",
			"rules": "",
			"selectedDays": "",
			"startTime": "",
			"status": "",
			"updatedAt": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量更新场景治理状态


**接口地址**:`/api/scene-governance/status/{status}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|场景ID列表|body|true|array|integer|
|status|场景状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 验证场景治理配置


**接口地址**:`/api/scene-governance/validate`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "cameras": "",
  "createdAt": "",
  "createdBy": "",
  "deleted": 0,
  "description": "",
  "devices": "",
  "endTime": "",
  "executeType": "",
  "id": 0,
  "intervalNum": 0,
  "location": "",
  "name": "",
  "rules": "",
  "selectedDays": "",
  "startTime": "",
  "status": "",
  "updatedAt": "",
  "updatedBy": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sceneGovernance|场景治理|body|true|SceneGovernance对象|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法||false|string||
|&emsp;&emsp;cameras|摄像头||false|string||
|&emsp;&emsp;createdAt|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除||false|integer(int32)||
|&emsp;&emsp;description|场景描述/备注||false|string||
|&emsp;&emsp;devices|关联设备||false|string||
|&emsp;&emsp;endTime|结束时间||false|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月||false|string||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量||false|integer(int32)||
|&emsp;&emsp;location|区划地点||false|string||
|&emsp;&emsp;name|场景名称||false|string||
|&emsp;&emsp;rules|治理规则||false|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）||false|string||
|&emsp;&emsp;startTime|开始时间||false|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用||false|string||
|&emsp;&emsp;updatedAt|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据ID查询场景治理信息


**接口地址**:`/api/scene-governance/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«SceneGovernance对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||SceneGovernance对象|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法|string||
|&emsp;&emsp;cameras|摄像头|string||
|&emsp;&emsp;createdAt|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|场景描述/备注|string||
|&emsp;&emsp;devices|关联设备|string||
|&emsp;&emsp;endTime|结束时间|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量|integer(int32)||
|&emsp;&emsp;location|区划地点|string||
|&emsp;&emsp;name|场景名称|string||
|&emsp;&emsp;rules|治理规则|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）|string||
|&emsp;&emsp;startTime|开始时间|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用|string||
|&emsp;&emsp;updatedAt|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"algorithm": "",
		"cameras": "",
		"createdAt": "",
		"createdBy": "",
		"deleted": 0,
		"description": "",
		"devices": "",
		"endTime": "",
		"executeType": "",
		"id": 0,
		"intervalNum": 0,
		"location": "",
		"name": "",
		"rules": "",
		"selectedDays": "",
		"startTime": "",
		"status": "",
		"updatedAt": "",
		"updatedBy": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新场景治理信息


**接口地址**:`/api/scene-governance/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "cameras": "",
  "createdAt": "",
  "createdBy": "",
  "deleted": 0,
  "description": "",
  "devices": "",
  "endTime": "",
  "executeType": "",
  "id": 0,
  "intervalNum": 0,
  "location": "",
  "name": "",
  "rules": "",
  "selectedDays": "",
  "startTime": "",
  "status": "",
  "updatedAt": "",
  "updatedBy": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||
|sceneGovernance|场景治理|body|true|SceneGovernance对象|SceneGovernance对象|
|&emsp;&emsp;algorithm|AI算法||false|string||
|&emsp;&emsp;cameras|摄像头||false|string||
|&emsp;&emsp;createdAt|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除||false|integer(int32)||
|&emsp;&emsp;description|场景描述/备注||false|string||
|&emsp;&emsp;devices|关联设备||false|string||
|&emsp;&emsp;endTime|结束时间||false|string(date-time)||
|&emsp;&emsp;executeType|执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月||false|string||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;intervalNum|间隔数量||false|integer(int32)||
|&emsp;&emsp;location|区划地点||false|string||
|&emsp;&emsp;name|场景名称||false|string||
|&emsp;&emsp;rules|治理规则||false|string||
|&emsp;&emsp;selectedDays|选择的天数（JSON数组）||false|string||
|&emsp;&emsp;startTime|开始时间||false|string(date-time)||
|&emsp;&emsp;status|状态: enabled-启用, disabled-禁用||false|string||
|&emsp;&emsp;updatedAt|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除场景治理信息


**接口地址**:`/api/scene-governance/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 复制场景治理


**接口地址**:`/api/scene-governance/{id}/copy`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|源场景ID|path|true|integer(int64)||
|name|新场景名称|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 禁用场景治理


**接口地址**:`/api/scene-governance/{id}/disable`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 启用场景治理


**接口地址**:`/api/scene-governance/{id}/enable`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 执行场景治理


**接口地址**:`/api/scene-governance/{id}/execute`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取场景治理执行历史


**接口地址**:`/api/scene-governance/{id}/execute-history`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新场景治理状态


**接口地址**:`/api/scene-governance/{id}/status/{status}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|场景ID|path|true|integer(int64)||
|status|场景状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 数据集管理


## 连接远程服务器


**接口地址**:`/api/dataset/connect`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>连接到远程服务器并验证连接状态</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|host|服务器地址|query|true|string||
|password|密码|query|true|string||
|path|数据集路径|query|true|string||
|username|用户名|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 下载文件


**接口地址**:`/api/dataset/download`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>从远程服务器下载指定文件</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|filename|文件名|query|true|string||
|host|服务器地址|query|true|string||
|path|数据集路径|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## 获取文件内容


**接口地址**:`/api/dataset/file-content`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取远程服务器指定文件的内容</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|filename|文件名|query|true|string||
|host|服务器地址|query|true|string||
|path|数据集路径|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取文件列表


**接口地址**:`/api/dataset/files`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取远程服务器指定路径下的文件列表</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|host|服务器地址|query|true|string||
|path|数据集路径|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«object»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 时间策略管理


## 保存时间策略


**接口地址**:`/api/time-strategy`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>保存或更新时间策略</p>



**请求示例**:


```javascript
{
  "createTime": "",
  "dailyTimes": [],
  "deviceId": "",
  "id": 0,
  "strategyType": "",
  "updateTime": "",
  "weeklyTimes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|timeStrategy|timeStrategy|body|true|TimeStrategy|TimeStrategy|
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;dailyTimes|||false|array|integer|
|&emsp;&emsp;deviceId|||false|string||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;strategyType|||false|string||
|&emsp;&emsp;updateTime|||false|string(date-time)||
|&emsp;&emsp;weeklyTimes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 调试接口


**接口地址**:`/api/time-strategy/debug/{deviceId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>调试时间策略数据</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|deviceId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取时间策略


**接口地址**:`/api/time-strategy/{deviceId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据设备ID获取时间策略</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|deviceId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«TimeStrategy»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||TimeStrategy|TimeStrategy|
|&emsp;&emsp;createTime||string(date-time)||
|&emsp;&emsp;dailyTimes||array|integer|
|&emsp;&emsp;deviceId||string||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;strategyType||string||
|&emsp;&emsp;updateTime||string(date-time)||
|&emsp;&emsp;weeklyTimes||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"createTime": "",
		"dailyTimes": [],
		"deviceId": "",
		"id": 0,
		"strategyType": "",
		"updateTime": "",
		"weeklyTimes": {}
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除时间策略


**接口地址**:`/api/time-strategy/{deviceId}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据设备ID删除时间策略</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|deviceId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 标注图片管理


## batchDeleteImages


**接口地址**:`/api/annotation-images/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getImagesByDataset


**接口地址**:`/api/annotation-images/dataset/{annotationId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getImageFile


**接口地址**:`/api/annotation-images/dataset/{datasetId}/image/{imageName}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|datasetId|datasetId|path|true|integer(int64)||
|imageName|imageName|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getDatasetStats


**接口地址**:`/api/annotation-images/dataset/{datasetId}/stats`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|datasetId|datasetId|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## batchSaveImages


**接口地址**:`/api/annotation-images/images/batch`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[
  {
    "annotationId": 0,
    "createTime": "",
    "deleted": true,
    "fileSize": 0,
    "id": 0,
    "imageName": "",
    "importTime": "",
    "isImported": true,
    "lastModified": "",
    "localPath": "",
    "originalName": "",
    "updateTime": ""
  }
]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationImages|annotationImages|body|true|array|AnnotationImage|
|&emsp;&emsp;annotationId|||false|integer(int64)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;deleted|||false|boolean||
|&emsp;&emsp;fileSize|||false|integer(int64)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;imageName|||false|string||
|&emsp;&emsp;importTime|||false|string(date-time)||
|&emsp;&emsp;isImported|||false|boolean||
|&emsp;&emsp;lastModified|||false|string(date-time)||
|&emsp;&emsp;localPath|||false|string||
|&emsp;&emsp;originalName|||false|string||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## uploadImages


**接口地址**:`/api/annotation-images/upload`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|query|true|integer(int64)||
|files||formData|false|array|file|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getImageById


**接口地址**:`/api/annotation-images/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|id|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## updateImage


**接口地址**:`/api/annotation-images/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "annotationId": 0,
  "createTime": "",
  "deleted": true,
  "fileSize": 0,
  "id": 0,
  "imageName": "",
  "importTime": "",
  "isImported": true,
  "lastModified": "",
  "localPath": "",
  "originalName": "",
  "updateTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|id|path|true|integer(int64)||
|image|image|body|true|AnnotationImage|AnnotationImage|
|&emsp;&emsp;annotationId|||false|integer(int64)||
|&emsp;&emsp;createTime|||false|string(date-time)||
|&emsp;&emsp;deleted|||false|boolean||
|&emsp;&emsp;fileSize|||false|integer(int64)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;imageName|||false|string||
|&emsp;&emsp;importTime|||false|string(date-time)||
|&emsp;&emsp;isImported|||false|boolean||
|&emsp;&emsp;lastModified|||false|string(date-time)||
|&emsp;&emsp;localPath|||false|string||
|&emsp;&emsp;originalName|||false|string||
|&emsp;&emsp;updateTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## deleteImage


**接口地址**:`/api/annotation-images/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|id|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


暂无


**响应示例**:
```javascript

```


# 标注标签管理


## batchDeleteAnnotationInstances


**接口地址**:`/api/annotation/instances/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|instanceIds|instanceIds|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## deleteAnnotationInstancesByImage


**接口地址**:`/api/annotation/instances/by-image`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|params|params|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## deleteAnnotationInstance


**接口地址**:`/api/annotation/instances/{instanceId}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|instanceId|instanceId|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## updateLabel


**接口地址**:`/api/annotation/labels/{labelId}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|labelId|labelId|path|true|integer(int64)||
|requestBody|requestBody|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AnnotationLabel»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AnnotationLabel|AnnotationLabel|
|&emsp;&emsp;annotationId||integer(int64)||
|&emsp;&emsp;color||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;description||string||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;sortOrder||integer(int32)||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;usageCount||integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"annotationId": 0,
		"color": "",
		"createdBy": 0,
		"createdTime": "",
		"deleted": 0,
		"description": "",
		"id": 0,
		"name": "",
		"sortOrder": 0,
		"updatedTime": "",
		"usageCount": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## deleteLabel


**接口地址**:`/api/annotation/labels/{labelId}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|labelId|labelId|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getAnnotationInstances


**接口地址**:`/api/annotation/{annotationId}/instances`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||
|imageName|imageName|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AnnotationInstance»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AnnotationInstance|
|&emsp;&emsp;annotationData||string||
|&emsp;&emsp;annotationId||integer(int64)||
|&emsp;&emsp;annotationType||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;imageId||integer(int64)||
|&emsp;&emsp;labelId||integer(int64)||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;verified||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"annotationData": "",
			"annotationId": 0,
			"annotationType": "",
			"confidence": 0,
			"createdBy": 0,
			"createdTime": "",
			"deleted": 0,
			"id": 0,
			"imageId": 0,
			"labelId": 0,
			"updatedTime": "",
			"verified": true
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## saveAnnotationInstance


**接口地址**:`/api/annotation/{annotationId}/instances`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||
|requestBody|requestBody|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AnnotationInstance»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AnnotationInstance|AnnotationInstance|
|&emsp;&emsp;annotationData||string||
|&emsp;&emsp;annotationId||integer(int64)||
|&emsp;&emsp;annotationType||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;imageId||integer(int64)||
|&emsp;&emsp;labelId||integer(int64)||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;verified||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"annotationData": "",
		"annotationId": 0,
		"annotationType": "",
		"confidence": 0,
		"createdBy": 0,
		"createdTime": "",
		"deleted": 0,
		"id": 0,
		"imageId": 0,
		"labelId": 0,
		"updatedTime": "",
		"verified": true
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getAllAnnotationInstances


**接口地址**:`/api/annotation/{annotationId}/instances/all`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AnnotationInstance»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AnnotationInstance|
|&emsp;&emsp;annotationData||string||
|&emsp;&emsp;annotationId||integer(int64)||
|&emsp;&emsp;annotationType||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;imageId||integer(int64)||
|&emsp;&emsp;labelId||integer(int64)||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;verified||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"annotationData": "",
			"annotationId": 0,
			"annotationType": "",
			"confidence": 0,
			"createdBy": 0,
			"createdTime": "",
			"deleted": 0,
			"id": 0,
			"imageId": 0,
			"labelId": 0,
			"updatedTime": "",
			"verified": true
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## batchSaveAnnotationInstances


**接口地址**:`/api/annotation/{annotationId}/instances/batch`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||
|requestBody|requestBody|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getLabels


**接口地址**:`/api/annotation/{annotationId}/labels`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||
|keyword|keyword|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AnnotationLabel»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AnnotationLabel|
|&emsp;&emsp;annotationId||integer(int64)||
|&emsp;&emsp;color||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;description||string||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;sortOrder||integer(int32)||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;usageCount||integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"annotationId": 0,
			"color": "",
			"createdBy": 0,
			"createdTime": "",
			"deleted": 0,
			"description": "",
			"id": 0,
			"name": "",
			"sortOrder": 0,
			"updatedTime": "",
			"usageCount": 0
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## createLabel


**接口地址**:`/api/annotation/{annotationId}/labels`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||
|requestBody|requestBody|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AnnotationLabel»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AnnotationLabel|AnnotationLabel|
|&emsp;&emsp;annotationId||integer(int64)||
|&emsp;&emsp;color||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;description||string||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;sortOrder||integer(int32)||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;usageCount||integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"annotationId": 0,
		"color": "",
		"createdBy": 0,
		"createdTime": "",
		"deleted": 0,
		"description": "",
		"id": 0,
		"name": "",
		"sortOrder": 0,
		"updatedTime": "",
		"usageCount": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## updateLabelSort


**接口地址**:`/api/annotation/{annotationId}/labels/sort`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationId|annotationId|path|true|integer(int64)||
|requestBody|requestBody|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 标签管理


## 创建标签


**接口地址**:`/api/tag-management`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "categoryType": "",
  "children": [
    {
      "categoryType": "",
      "children": [],
      "createTime": "",
      "createdBy": "",
      "deleted": 0,
      "description": "",
      "id": 0,
      "isActive": 0,
      "level": 0,
      "parentId": 0,
      "parentName": "",
      "sortOrder": 0,
      "tagColor": "",
      "tagIcon": "",
      "tagName": "",
      "updateTime": "",
      "updatedBy": "",
      "usageCount": 0
    }
  ],
  "createTime": "",
  "createdBy": "",
  "deleted": 0,
  "description": "",
  "id": 0,
  "isActive": 0,
  "level": 0,
  "parentId": 0,
  "parentName": "",
  "sortOrder": 0,
  "tagColor": "",
  "tagIcon": "",
  "tagName": "",
  "updateTime": "",
  "updatedBy": "",
  "usageCount": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagManagement|标签管理|body|true|TagManagement对象|TagManagement对象|
|&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)||false|string||
|&emsp;&emsp;children|子标签列表||false|array|TagManagement对象|
|&emsp;&emsp;createTime|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)||false|integer(int32)||
|&emsp;&emsp;description|标签描述||false|string||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)||false|integer(int32)||
|&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)||false|integer(int32)||
|&emsp;&emsp;parentId|父级标签ID||false|integer(int64)||
|&emsp;&emsp;parentName|父标签名称||false|string||
|&emsp;&emsp;sortOrder|排序序号||false|integer(int32)||
|&emsp;&emsp;tagColor|标签颜色(十六进制)||false|string||
|&emsp;&emsp;tagIcon|标签图标||false|string||
|&emsp;&emsp;tagName|标签名称||false|string||
|&emsp;&emsp;updateTime|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||
|&emsp;&emsp;usageCount|使用次数||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«TagManagement对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||TagManagement对象|TagManagement对象|
|&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)|string||
|&emsp;&emsp;children|子标签列表|array|TagManagement对象|
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)|integer(int32)||
|&emsp;&emsp;description|标签描述|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)|integer(int32)||
|&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)|integer(int32)||
|&emsp;&emsp;parentId|父级标签ID|integer(int64)||
|&emsp;&emsp;parentName|父标签名称|string||
|&emsp;&emsp;sortOrder|排序序号|integer(int32)||
|&emsp;&emsp;tagColor|标签颜色(十六进制)|string||
|&emsp;&emsp;tagIcon|标签图标|string||
|&emsp;&emsp;tagName|标签名称|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;usageCount|使用次数|integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"categoryType": "",
		"children": [],
		"createTime": "",
		"createdBy": "",
		"deleted": 0,
		"description": "",
		"id": 0,
		"isActive": 0,
		"level": 0,
		"parentId": 0,
		"parentName": "",
		"sortOrder": 0,
		"tagColor": "",
		"tagIcon": "",
		"tagName": "",
		"updateTime": "",
		"updatedBy": "",
		"usageCount": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除标签


**接口地址**:`/api/tag-management/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagIds|tagIds|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 检查标签名称是否重复


**接口地址**:`/api/tag-management/check-name`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagName|标签名称|query|true|string||
|excludeId|排除的ID|query|false|integer(int64)||
|parentId|父级ID|query|false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询标签信息


**接口地址**:`/api/tag-management/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|categoryType|标签大类|query|false|string||
|current|当前页|query|false|integer(int64)||
|keyword|搜索关键字|query|false|string||
|level|标签层级|query|false|integer(int32)||
|parentId|父级ID|query|false|integer(int64)||
|size|每页大小|query|false|integer(int64)||
|tagId|标签ID|query|false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«TagManagement对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«TagManagement对象»|IPage«TagManagement对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|TagManagement对象|
|&emsp;&emsp;&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)|string||
|&emsp;&emsp;&emsp;&emsp;children|子标签列表|array|TagManagement对象|
|&emsp;&emsp;&emsp;&emsp;createTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)|integer||
|&emsp;&emsp;&emsp;&emsp;description|标签描述|string||
|&emsp;&emsp;&emsp;&emsp;id|主键ID|integer||
|&emsp;&emsp;&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)|integer||
|&emsp;&emsp;&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)|integer||
|&emsp;&emsp;&emsp;&emsp;parentId|父级标签ID|integer||
|&emsp;&emsp;&emsp;&emsp;parentName|父标签名称|string||
|&emsp;&emsp;&emsp;&emsp;sortOrder|排序序号|integer||
|&emsp;&emsp;&emsp;&emsp;tagColor|标签颜色(十六进制)|string||
|&emsp;&emsp;&emsp;&emsp;tagIcon|标签图标|string||
|&emsp;&emsp;&emsp;&emsp;tagName|标签名称|string||
|&emsp;&emsp;&emsp;&emsp;updateTime|更新时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;&emsp;&emsp;usageCount|使用次数|integer||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"categoryType": "",
				"children": [],
				"createTime": "",
				"createdBy": "",
				"deleted": 0,
				"description": "",
				"id": 0,
				"isActive": 0,
				"level": 0,
				"parentId": 0,
				"parentName": "",
				"sortOrder": 0,
				"tagColor": "",
				"tagIcon": "",
				"tagName": "",
				"updateTime": "",
				"updatedBy": "",
				"usageCount": 0
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标签树形结构（用于左侧导航）


**接口地址**:`/api/tag-management/tree`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据类型获取标签树


**接口地址**:`/api/tag-management/tree/{tagType}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagType|标签类型(own-自有, public-公共)|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«TagManagement对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|TagManagement对象|
|&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)|string||
|&emsp;&emsp;children|子标签列表|array|TagManagement对象|
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)|integer(int32)||
|&emsp;&emsp;description|标签描述|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)|integer(int32)||
|&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)|integer(int32)||
|&emsp;&emsp;parentId|父级标签ID|integer(int64)||
|&emsp;&emsp;parentName|父标签名称|string||
|&emsp;&emsp;sortOrder|排序序号|integer(int32)||
|&emsp;&emsp;tagColor|标签颜色(十六进制)|string||
|&emsp;&emsp;tagIcon|标签图标|string||
|&emsp;&emsp;tagName|标签名称|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;usageCount|使用次数|integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"categoryType": "",
			"children": [],
			"createTime": "",
			"createdBy": "",
			"deleted": 0,
			"description": "",
			"id": 0,
			"isActive": 0,
			"level": 0,
			"parentId": 0,
			"parentName": "",
			"sortOrder": 0,
			"tagColor": "",
			"tagIcon": "",
			"tagName": "",
			"updateTime": "",
			"updatedBy": "",
			"usageCount": 0
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新标签


**接口地址**:`/api/tag-management/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "categoryType": "",
  "children": [
    {
      "categoryType": "",
      "children": [],
      "createTime": "",
      "createdBy": "",
      "deleted": 0,
      "description": "",
      "id": 0,
      "isActive": 0,
      "level": 0,
      "parentId": 0,
      "parentName": "",
      "sortOrder": 0,
      "tagColor": "",
      "tagIcon": "",
      "tagName": "",
      "updateTime": "",
      "updatedBy": "",
      "usageCount": 0
    }
  ],
  "createTime": "",
  "createdBy": "",
  "deleted": 0,
  "description": "",
  "id": 0,
  "isActive": 0,
  "level": 0,
  "parentId": 0,
  "parentName": "",
  "sortOrder": 0,
  "tagColor": "",
  "tagIcon": "",
  "tagName": "",
  "updateTime": "",
  "updatedBy": "",
  "usageCount": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标签ID|path|true|integer(int64)||
|tagManagement|标签管理|body|true|TagManagement对象|TagManagement对象|
|&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)||false|string||
|&emsp;&emsp;children|子标签列表||false|array|TagManagement对象|
|&emsp;&emsp;createTime|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)||false|integer(int32)||
|&emsp;&emsp;description|标签描述||false|string||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)||false|integer(int32)||
|&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)||false|integer(int32)||
|&emsp;&emsp;parentId|父级标签ID||false|integer(int64)||
|&emsp;&emsp;parentName|父标签名称||false|string||
|&emsp;&emsp;sortOrder|排序序号||false|integer(int32)||
|&emsp;&emsp;tagColor|标签颜色(十六进制)||false|string||
|&emsp;&emsp;tagIcon|标签图标||false|string||
|&emsp;&emsp;tagName|标签名称||false|string||
|&emsp;&emsp;updateTime|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||
|&emsp;&emsp;usageCount|使用次数||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«TagManagement对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||TagManagement对象|TagManagement对象|
|&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)|string||
|&emsp;&emsp;children|子标签列表|array|TagManagement对象|
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)|integer(int32)||
|&emsp;&emsp;description|标签描述|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)|integer(int32)||
|&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)|integer(int32)||
|&emsp;&emsp;parentId|父级标签ID|integer(int64)||
|&emsp;&emsp;parentName|父标签名称|string||
|&emsp;&emsp;sortOrder|排序序号|integer(int32)||
|&emsp;&emsp;tagColor|标签颜色(十六进制)|string||
|&emsp;&emsp;tagIcon|标签图标|string||
|&emsp;&emsp;tagName|标签名称|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;usageCount|使用次数|integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"categoryType": "",
		"children": [],
		"createTime": "",
		"createdBy": "",
		"deleted": 0,
		"description": "",
		"id": 0,
		"isActive": 0,
		"level": 0,
		"parentId": 0,
		"parentName": "",
		"sortOrder": 0,
		"tagColor": "",
		"tagIcon": "",
		"tagName": "",
		"updateTime": "",
		"updatedBy": "",
		"usageCount": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除标签


**接口地址**:`/api/tag-management/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标签ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 移动标签


**接口地址**:`/api/tag-management/{id}/move`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标签ID|path|true|integer(int64)||
|targetParentId|目标父级ID|query|false|integer(int64)||
|targetPosition|目标位置|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标签使用统计


**接口地址**:`/api/tag-management/{id}/stats`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标签ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«TagManagement对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||TagManagement对象|TagManagement对象|
|&emsp;&emsp;categoryType|标签大类(own-自有, public-公共)|string||
|&emsp;&emsp;children|子标签列表|array|TagManagement对象|
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除, 1-已删除)|integer(int32)||
|&emsp;&emsp;description|标签描述|string||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;isActive|是否启用(0-禁用, 1-启用)|integer(int32)||
|&emsp;&emsp;level|层级(0-类型级, 1-父级标签, 2-子级标签)|integer(int32)||
|&emsp;&emsp;parentId|父级标签ID|integer(int64)||
|&emsp;&emsp;parentName|父标签名称|string||
|&emsp;&emsp;sortOrder|排序序号|integer(int32)||
|&emsp;&emsp;tagColor|标签颜色(十六进制)|string||
|&emsp;&emsp;tagIcon|标签图标|string||
|&emsp;&emsp;tagName|标签名称|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;usageCount|使用次数|integer(int32)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"categoryType": "",
		"children": [],
		"createTime": "",
		"createdBy": "",
		"deleted": 0,
		"description": "",
		"id": 0,
		"isActive": 0,
		"level": 0,
		"parentId": 0,
		"parentName": "",
		"sortOrder": 0,
		"tagColor": "",
		"tagIcon": "",
		"tagName": "",
		"updateTime": "",
		"updatedBy": "",
		"usageCount": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 启用-禁用标签


**接口地址**:`/api/tag-management/{id}/toggle-status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标签ID|path|true|integer(int64)||
|isActive|是否启用|query|true|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 模型下载管理


## downloadModel


**接口地址**:`/api/models/download/{modelName}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|modelName|modelName|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Resource|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|description||string||
|file||file|file|
|filename||string||
|inputStream||InputStream|InputStream|
|open||boolean||
|readable||boolean||
|uri||string(uri)|string(uri)|
|url||string(url)|string(url)|


**响应示例**:
```javascript
{
	"description": "",
	"file": "",
	"filename": "",
	"inputStream": {},
	"open": true,
	"readable": true,
	"uri": "",
	"url": ""
}
```


## checkModelExists


**接口地址**:`/api/models/exists/{modelName}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|modelName|modelName|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getModelList


**接口地址**:`/api/models/list`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


# 流转换控制器


## getActiveStreams


**接口地址**:`/api/stream/active`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## checkStreamStatus


**接口地址**:`/api/stream/check/{deviceId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|deviceId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getStreamConfig


**接口地址**:`/api/stream/config`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## startStream


**接口地址**:`/api/stream/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": "",
  "mode": "",
  "quality": "",
  "rtspUrl": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StreamRequest|StreamRequest|
|&emsp;&emsp;deviceId|||false|string||
|&emsp;&emsp;mode|||false|string||
|&emsp;&emsp;quality|||false|string||
|&emsp;&emsp;rtspUrl|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## startHLSStream


**接口地址**:`/api/stream/start-hls`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": "",
  "mode": "",
  "quality": "",
  "rtspUrl": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StreamRequest|StreamRequest|
|&emsp;&emsp;deviceId|||false|string||
|&emsp;&emsp;mode|||false|string||
|&emsp;&emsp;quality|||false|string||
|&emsp;&emsp;rtspUrl|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## startWebRTCStream


**接口地址**:`/api/stream/start-webrtc`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": "",
  "mode": "",
  "quality": "",
  "rtspUrl": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StreamRequest|StreamRequest|
|&emsp;&emsp;deviceId|||false|string||
|&emsp;&emsp;mode|||false|string||
|&emsp;&emsp;quality|||false|string||
|&emsp;&emsp;rtspUrl|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## stopStream


**接口地址**:`/api/stream/stop`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StopStreamRequest|StopStreamRequest|
|&emsp;&emsp;deviceId|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## stopAllStreams


**接口地址**:`/api/stream/stop-all`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## stopHLSStream


**接口地址**:`/api/stream/stop-hls`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StopStreamRequest|StopStreamRequest|
|&emsp;&emsp;deviceId|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## stopWebRTCStream


**接口地址**:`/api/stream/stop-webrtc`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "deviceId": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|StopStreamRequest|StopStreamRequest|
|&emsp;&emsp;deviceId|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 用户同步控制器


## health


**接口地址**:`/api/user/health`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## syncUser


**接口地址**:`/api/user/sync`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accessToken": "",
  "client": "",
  "loginIP": "",
  "loginId": "",
  "loginTime": "",
  "loginType": 0,
  "tenantId": "",
  "userId": "",
  "userName": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|UserSyncRequest|UserSyncRequest|
|&emsp;&emsp;accessToken|||false|string||
|&emsp;&emsp;client|||false|string||
|&emsp;&emsp;loginIP|||false|string||
|&emsp;&emsp;loginId|||false|string||
|&emsp;&emsp;loginTime|||false|string||
|&emsp;&emsp;loginType|||false|integer(int32)||
|&emsp;&emsp;tenantId|||false|string||
|&emsp;&emsp;userId|||false|string||
|&emsp;&emsp;userName|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getUserInfo


**接口地址**:`/api/user/{userId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|userId|userId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## updateUser


**接口地址**:`/api/user/{userId}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accessToken": "",
  "client": "",
  "createdAt": "",
  "form": 0,
  "id": 0,
  "loginId": "",
  "loginIp": "",
  "loginTime": "",
  "loginType": 0,
  "photo": "",
  "status": 0,
  "tenantId": "",
  "tokenExpireTime": "",
  "updatedAt": "",
  "userId": "",
  "userName": "",
  "userNameFpy": "",
  "userNamePy": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|userData|userData|body|true|LocalUser|LocalUser|
|&emsp;&emsp;accessToken|||false|string||
|&emsp;&emsp;client|||false|string||
|&emsp;&emsp;createdAt|||false|string(date-time)||
|&emsp;&emsp;form|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;loginId|||false|string||
|&emsp;&emsp;loginIp|||false|string||
|&emsp;&emsp;loginTime|||false|string(date-time)||
|&emsp;&emsp;loginType|||false|integer(int32)||
|&emsp;&emsp;photo|||false|string||
|&emsp;&emsp;status|||false|integer(int32)||
|&emsp;&emsp;tenantId|||false|string||
|&emsp;&emsp;tokenExpireTime|||false|string(date-time)||
|&emsp;&emsp;updatedAt|||false|string(date-time)||
|&emsp;&emsp;userId|||false|string||
|&emsp;&emsp;userName|||false|string||
|&emsp;&emsp;userNameFpy|||false|string||
|&emsp;&emsp;userNamePy|||false|string||
|userId|userId|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


# 算法仓库管理


## 创建算法仓库


**接口地址**:`/api/algorithm-repository`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>新增算法仓库</p>



**请求示例**:


```javascript
{
  "algorithmCount": 0,
  "createdBy": "",
  "createdTime": "",
  "deleted": 0,
  "id": 0,
  "name": "",
  "remark": "",
  "repositoryType": "",
  "status": "",
  "updatedBy": "",
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|repository|repository|body|true|AlgorithmRepository|AlgorithmRepository|
|&emsp;&emsp;algorithmCount|||false|integer(int32)||
|&emsp;&emsp;createdBy|||false|string||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;deleted|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;repositoryType|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;updatedBy|||false|string||
|&emsp;&emsp;updatedTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除算法仓库


**接口地址**:`/api/algorithm-repository/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID列表批量删除算法仓库</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量更新仓库状态


**接口地址**:`/api/algorithm-repository/batch/status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量启用或禁用算法仓库</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|
|status|新状态|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 统计算法仓库数量


**接口地址**:`/api/algorithm-repository/count`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取算法仓库总数</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«long»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||integer(int64)|integer(int64)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 查询所有启用的算法仓库


**接口地址**:`/api/algorithm-repository/enabled`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取状态为启用的所有算法仓库</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmRepository»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmRepository|
|&emsp;&emsp;algorithmCount||integer(int32)||
|&emsp;&emsp;createdBy||string||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;repositoryType||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;updatedTime||string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"algorithmCount": 0,
			"createdBy": "",
			"createdTime": "",
			"deleted": 0,
			"id": 0,
			"name": "",
			"remark": "",
			"repositoryType": "",
			"status": "",
			"updatedBy": "",
			"updatedTime": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询算法仓库列表


**接口地址**:`/api/algorithm-repository/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据条件分页查询算法仓库</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|current|当前页|query|false|integer(int32)||
|name|仓库名称|query|false|string||
|repositoryType|仓库类型|query|false|string||
|size|每页大小|query|false|integer(int32)||
|status|状态|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«AlgorithmRepository»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«AlgorithmRepository»|IPage«AlgorithmRepository»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|AlgorithmRepository|
|&emsp;&emsp;&emsp;&emsp;algorithmCount||integer||
|&emsp;&emsp;&emsp;&emsp;createdBy||string||
|&emsp;&emsp;&emsp;&emsp;createdTime||string||
|&emsp;&emsp;&emsp;&emsp;deleted||integer||
|&emsp;&emsp;&emsp;&emsp;id||integer||
|&emsp;&emsp;&emsp;&emsp;name||string||
|&emsp;&emsp;&emsp;&emsp;remark||string||
|&emsp;&emsp;&emsp;&emsp;repositoryType||string||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;&emsp;&emsp;updatedTime||string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"algorithmCount": 0,
				"createdBy": "",
				"createdTime": "",
				"deleted": 0,
				"id": 0,
				"name": "",
				"remark": "",
				"repositoryType": "",
				"status": "",
				"updatedBy": "",
				"updatedTime": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据类型查询算法仓库


**接口地址**:`/api/algorithm-repository/type/{repositoryType}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据仓库类型获取算法仓库列表</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|repositoryType|仓库类型|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmRepository»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmRepository|
|&emsp;&emsp;algorithmCount||integer(int32)||
|&emsp;&emsp;createdBy||string||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;repositoryType||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;updatedTime||string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"algorithmCount": 0,
			"createdBy": "",
			"createdTime": "",
			"deleted": 0,
			"id": 0,
			"name": "",
			"remark": "",
			"repositoryType": "",
			"status": "",
			"updatedBy": "",
			"updatedTime": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 查询算法仓库详情


**接口地址**:`/api/algorithm-repository/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID获取算法仓库详细信息</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|仓库ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmRepository»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmRepository|AlgorithmRepository|
|&emsp;&emsp;algorithmCount||integer(int32)||
|&emsp;&emsp;createdBy||string||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;repositoryType||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;updatedTime||string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"algorithmCount": 0,
		"createdBy": "",
		"createdTime": "",
		"deleted": 0,
		"id": 0,
		"name": "",
		"remark": "",
		"repositoryType": "",
		"status": "",
		"updatedBy": "",
		"updatedTime": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新算法仓库


**接口地址**:`/api/algorithm-repository/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID更新算法仓库信息</p>



**请求示例**:


```javascript
{
  "algorithmCount": 0,
  "createdBy": "",
  "createdTime": "",
  "deleted": 0,
  "id": 0,
  "name": "",
  "remark": "",
  "repositoryType": "",
  "status": "",
  "updatedBy": "",
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|仓库ID|path|true|integer(int64)||
|repository|repository|body|true|AlgorithmRepository|AlgorithmRepository|
|&emsp;&emsp;algorithmCount|||false|integer(int32)||
|&emsp;&emsp;createdBy|||false|string||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;deleted|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;repositoryType|||false|string||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;updatedBy|||false|string||
|&emsp;&emsp;updatedTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除算法仓库


**接口地址**:`/api/algorithm-repository/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID删除算法仓库（软删除）</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|仓库ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 刷新仓库算法数量


**接口地址**:`/api/algorithm-repository/{id}/refresh-count`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>重新计算并更新仓库的算法数量</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|仓库ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新仓库状态


**接口地址**:`/api/algorithm-repository/{id}/status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>启用或禁用算法仓库</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|仓库ID|path|true|integer(int64)||
|status|新状态|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 算法标注管理


## 创建算法标注


**接口地址**:`/api/algorithm-annotation`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>新增算法标注</p>



**请求示例**:


```javascript
{
  "annotatedCount": 0,
  "annotationName": "",
  "annotationRules": "",
  "annotationStatus": "",
  "annotationType": "",
  "createdBy": 0,
  "createdTime": "",
  "datasetPath": "",
  "deleted": 0,
  "id": 0,
  "progress": 0,
  "remark": "",
  "totalCount": 0,
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotation|annotation|body|true|AlgorithmAnnotation|AlgorithmAnnotation|
|&emsp;&emsp;annotatedCount|||false|integer(int32)||
|&emsp;&emsp;annotationName|||false|string||
|&emsp;&emsp;annotationRules|||false|string||
|&emsp;&emsp;annotationStatus|||false|string||
|&emsp;&emsp;annotationType|||false|string||
|&emsp;&emsp;createdBy|||false|integer(int64)||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;datasetPath|||false|string||
|&emsp;&emsp;deleted|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;progress|||false|integer(int32)||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;totalCount|||false|integer(int32)||
|&emsp;&emsp;updatedTime|||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除算法标注


**接口地址**:`/api/algorithm-annotation/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID列表批量删除标注</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量更新标注状态


**接口地址**:`/api/algorithm-annotation/batch/status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量更新标注的状态</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationStatus|标注状态|query|true|string||
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询算法标注列表


**接口地址**:`/api/algorithm-annotation/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据条件分页查询算法标注</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationName|标注名称|query|false|string||
|annotationStatus|标注状态|query|false|string||
|annotationType|标注类型|query|false|string||
|current|当前页|query|false|integer(int32)||
|size|每页大小|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«AlgorithmAnnotation»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«AlgorithmAnnotation»|IPage«AlgorithmAnnotation»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|AlgorithmAnnotation|
|&emsp;&emsp;&emsp;&emsp;annotatedCount||integer||
|&emsp;&emsp;&emsp;&emsp;annotationName||string||
|&emsp;&emsp;&emsp;&emsp;annotationRules||string||
|&emsp;&emsp;&emsp;&emsp;annotationStatus||string||
|&emsp;&emsp;&emsp;&emsp;annotationType||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdTime||string||
|&emsp;&emsp;&emsp;&emsp;datasetPath||string||
|&emsp;&emsp;&emsp;&emsp;deleted||integer||
|&emsp;&emsp;&emsp;&emsp;id||integer||
|&emsp;&emsp;&emsp;&emsp;progress||integer||
|&emsp;&emsp;&emsp;&emsp;remark||string||
|&emsp;&emsp;&emsp;&emsp;totalCount||integer||
|&emsp;&emsp;&emsp;&emsp;updatedTime||string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"annotatedCount": 0,
				"annotationName": "",
				"annotationRules": "",
				"annotationStatus": "",
				"annotationType": "",
				"createdBy": 0,
				"createdTime": "",
				"datasetPath": "",
				"deleted": 0,
				"id": 0,
				"progress": 0,
				"remark": "",
				"totalCount": 0,
				"updatedTime": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标注进度统计


**接口地址**:`/api/algorithm-annotation/statistics/progress`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取各进度区间的标注数量统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标注状态统计


**接口地址**:`/api/algorithm-annotation/statistics/status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取各状态的标注数量统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标注类型统计


**接口地址**:`/api/algorithm-annotation/statistics/type`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取各类型的标注数量统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标注工作量统计


**接口地址**:`/api/algorithm-annotation/statistics/workload`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取标注工作量的总体统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据标注状态查询标注列表


**接口地址**:`/api/algorithm-annotation/status/{annotationStatus}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定状态的所有标注</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationStatus|标注状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmAnnotation»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmAnnotation|
|&emsp;&emsp;annotatedCount||integer(int32)||
|&emsp;&emsp;annotationName||string||
|&emsp;&emsp;annotationRules||string||
|&emsp;&emsp;annotationStatus||string||
|&emsp;&emsp;annotationType||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;datasetPath||string||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;progress||integer(int32)||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;totalCount||integer(int32)||
|&emsp;&emsp;updatedTime||string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"annotatedCount": 0,
			"annotationName": "",
			"annotationRules": "",
			"annotationStatus": "",
			"annotationType": "",
			"createdBy": 0,
			"createdTime": "",
			"datasetPath": "",
			"deleted": 0,
			"id": 0,
			"progress": 0,
			"remark": "",
			"totalCount": 0,
			"updatedTime": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据标注类型查询标注列表


**接口地址**:`/api/algorithm-annotation/type/{annotationType}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定类型的所有标注</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotationType|标注类型|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmAnnotation»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmAnnotation|
|&emsp;&emsp;annotatedCount||integer(int32)||
|&emsp;&emsp;annotationName||string||
|&emsp;&emsp;annotationRules||string||
|&emsp;&emsp;annotationStatus||string||
|&emsp;&emsp;annotationType||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;datasetPath||string||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;progress||integer(int32)||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;totalCount||integer(int32)||
|&emsp;&emsp;updatedTime||string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"annotatedCount": 0,
			"annotationName": "",
			"annotationRules": "",
			"annotationStatus": "",
			"annotationType": "",
			"createdBy": 0,
			"createdTime": "",
			"datasetPath": "",
			"deleted": 0,
			"id": 0,
			"progress": 0,
			"remark": "",
			"totalCount": 0,
			"updatedTime": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 查询标注详情


**接口地址**:`/api/algorithm-annotation/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID获取标注详细信息</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmAnnotation»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmAnnotation|AlgorithmAnnotation|
|&emsp;&emsp;annotatedCount||integer(int32)||
|&emsp;&emsp;annotationName||string||
|&emsp;&emsp;annotationRules||string||
|&emsp;&emsp;annotationStatus||string||
|&emsp;&emsp;annotationType||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;datasetPath||string||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;progress||integer(int32)||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;totalCount||integer(int32)||
|&emsp;&emsp;updatedTime||string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"annotatedCount": 0,
		"annotationName": "",
		"annotationRules": "",
		"annotationStatus": "",
		"annotationType": "",
		"createdBy": 0,
		"createdTime": "",
		"datasetPath": "",
		"deleted": 0,
		"id": 0,
		"progress": 0,
		"remark": "",
		"totalCount": 0,
		"updatedTime": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新算法标注


**接口地址**:`/api/algorithm-annotation/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID更新标注信息</p>



**请求示例**:


```javascript
{
  "annotatedCount": 0,
  "annotationName": "",
  "annotationRules": "",
  "annotationStatus": "",
  "annotationType": "",
  "createdBy": 0,
  "createdTime": "",
  "datasetPath": "",
  "deleted": 0,
  "id": 0,
  "progress": 0,
  "remark": "",
  "totalCount": 0,
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotation|annotation|body|true|AlgorithmAnnotation|AlgorithmAnnotation|
|&emsp;&emsp;annotatedCount|||false|integer(int32)||
|&emsp;&emsp;annotationName|||false|string||
|&emsp;&emsp;annotationRules|||false|string||
|&emsp;&emsp;annotationStatus|||false|string||
|&emsp;&emsp;annotationType|||false|string||
|&emsp;&emsp;createdBy|||false|integer(int64)||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;datasetPath|||false|string||
|&emsp;&emsp;deleted|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;progress|||false|integer(int32)||
|&emsp;&emsp;remark|||false|string||
|&emsp;&emsp;totalCount|||false|integer(int32)||
|&emsp;&emsp;updatedTime|||false|string(date-time)||
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除算法标注


**接口地址**:`/api/algorithm-annotation/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID删除标注（软删除）</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 完成标注任务


**接口地址**:`/api/algorithm-annotation/{id}/complete`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>完成指定的标注任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 导出标注数据


**接口地址**:`/api/algorithm-annotation/{id}/export`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>导出指定标注的数据</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 导入标注数据


**接口地址**:`/api/algorithm-annotation/{id}/import`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>导入标注数据</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|dataPath|数据路径|query|true|string||
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新标注进度


**接口地址**:`/api/algorithm-annotation/{id}/progress`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新标注的进度信息</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|annotatedCount|已标注数量|query|true|integer(int32)||
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 重置标注任务


**接口地址**:`/api/algorithm-annotation/{id}/reset`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>重置指定的标注任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 保存标注数据到数据集


**接口地址**:`/api/algorithm-annotation/{id}/save-dataset`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>将标注数据保存到数据集文件并更新数据库路径</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 开始标注任务


**接口地址**:`/api/algorithm-annotation/{id}/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>开始指定的标注任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 验证标注数据


**接口地址**:`/api/algorithm-annotation/{id}/validate`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>验证指定标注的数据质量</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|标注ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 算法模型管理


## 根据算法ID查询模型列表


**接口地址**:`/api/algorithm-model/algorithm/{algorithmId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithmId|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmModel对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"accuracy": 0,
			"algorithmId": 0,
			"algorithmName": "",
			"createdBy": 0,
			"createdByName": "",
			"createdTime": "",
			"deleted": 0,
			"deployCount": 0,
			"description": "",
			"downloadCount": 0,
			"formattedAccuracy": "",
			"id": 0,
			"modelFormat": "",
			"modelFormatDesc": "",
			"modelName": "",
			"modelPath": "",
			"modelSize": "",
			"publishTime": "",
			"status": "",
			"statusDesc": "",
			"trainingId": 0,
			"trainingTaskName": "",
			"updatedTime": "",
			"version": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取算法下最新版本的模型


**接口地址**:`/api/algorithm-model/algorithm/{algorithmId}/latest`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithmId|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmModel对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmModel对象|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"algorithmId": 0,
		"algorithmName": "",
		"createdBy": 0,
		"createdByName": "",
		"createdTime": "",
		"deleted": 0,
		"deployCount": 0,
		"description": "",
		"downloadCount": 0,
		"formattedAccuracy": "",
		"id": 0,
		"modelFormat": "",
		"modelFormatDesc": "",
		"modelName": "",
		"modelPath": "",
		"modelSize": "",
		"publishTime": "",
		"status": "",
		"statusDesc": "",
		"trainingId": 0,
		"trainingTaskName": "",
		"updatedTime": "",
		"version": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据算法ID和版本查询模型


**接口地址**:`/api/algorithm-model/algorithm/{algorithmId}/version/{version}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithmId|算法ID|path|true|integer(int64)||
|version|版本|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmModel对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmModel对象|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"algorithmId": 0,
		"algorithmName": "",
		"createdBy": 0,
		"createdByName": "",
		"createdTime": "",
		"deleted": 0,
		"deployCount": 0,
		"description": "",
		"downloadCount": 0,
		"formattedAccuracy": "",
		"id": 0,
		"modelFormat": "",
		"modelFormatDesc": "",
		"modelName": "",
		"modelPath": "",
		"modelSize": "",
		"publishTime": "",
		"status": "",
		"statusDesc": "",
		"trainingId": 0,
		"trainingTaskName": "",
		"updatedTime": "",
		"version": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除算法模型


**接口地址**:`/api/algorithm-model/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量发布模型


**接口地址**:`/api/algorithm-model/batch-publish`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 检查模型名称和版本是否存在


**接口地址**:`/api/algorithm-model/check-name-version`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|modelName|模型名称|query|true|string||
|version|模型版本|query|true|string||
|excludeId|排除的ID|query|false|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据创建人查询模型数量


**接口地址**:`/api/algorithm-model/count/creator/{createdBy}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createdBy|创建人ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«long»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||integer(int64)|integer(int64)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 创建算法模型


**接口地址**:`/api/algorithm-model/create`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accuracy": 0.95,
  "algorithmId": 1,
  "description": "基于YOLOv5的目标检测模型",
  "modelFormat": "ONNX",
  "modelName": "目标检测模型v1.0",
  "modelPath": "/models/detection/v1.0/model.onnx",
  "modelSize": "100MB",
  "status": "draft",
  "trainingId": 1,
  "version": "v1.0.0"
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createDTO|算法模型创建参数|body|true|AlgorithmModelCreateDTO|AlgorithmModelCreateDTO|
|&emsp;&emsp;accuracy|模型准确率||false|number||
|&emsp;&emsp;algorithmId|算法ID||true|integer(int64)||
|&emsp;&emsp;description|模型描述||false|string||
|&emsp;&emsp;modelFormat|模型格式||false|string||
|&emsp;&emsp;modelName|模型名称||true|string||
|&emsp;&emsp;modelPath|模型文件路径||true|string||
|&emsp;&emsp;modelSize|模型大小||false|string||
|&emsp;&emsp;status|状态||false|string||
|&emsp;&emsp;trainingId|训练任务ID||false|integer(int64)||
|&emsp;&emsp;version|模型版本||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmModel对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmModel对象|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"algorithmId": 0,
		"algorithmName": "",
		"createdBy": 0,
		"createdByName": "",
		"createdTime": "",
		"deleted": 0,
		"deployCount": 0,
		"description": "",
		"downloadCount": 0,
		"formattedAccuracy": "",
		"id": 0,
		"modelFormat": "",
		"modelFormatDesc": "",
		"modelName": "",
		"modelPath": "",
		"modelSize": "",
		"publishTime": "",
		"status": "",
		"statusDesc": "",
		"trainingId": 0,
		"trainingTaskName": "",
		"updatedTime": "",
		"version": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 部署模型


**接口地址**:`/api/algorithm-model/deploy/{id}`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|模型ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 下载模型


**接口地址**:`/api/algorithm-model/download/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|模型ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询算法模型


**接口地址**:`/api/algorithm-model/page`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithmId": 0,
  "createdBy": 0,
  "createdTimeBegin": "",
  "createdTimeEnd": "",
  "current": 1,
  "maxAccuracy": 0,
  "minAccuracy": 0,
  "modelFormat": "",
  "modelName": "",
  "order": "desc",
  "orderBy": "created_time",
  "publishTimeBegin": "",
  "publishTimeEnd": "",
  "size": 10,
  "status": "",
  "trainingId": 0,
  "version": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|queryDTO|算法模型查询参数|body|true|AlgorithmModelQueryDTO|AlgorithmModelQueryDTO|
|&emsp;&emsp;algorithmId|算法ID||false|integer(int64)||
|&emsp;&emsp;createdBy|创建人||false|integer(int64)||
|&emsp;&emsp;createdTimeBegin|创建时间-开始范围||false|string(date-time)||
|&emsp;&emsp;createdTimeEnd|创建时间-结束范围||false|string(date-time)||
|&emsp;&emsp;current|当前页码||false|integer(int32)||
|&emsp;&emsp;maxAccuracy|最大准确率||false|number||
|&emsp;&emsp;minAccuracy|最小准确率||false|number||
|&emsp;&emsp;modelFormat|模型格式||false|string||
|&emsp;&emsp;modelName|模型名称（模糊搜索）||false|string||
|&emsp;&emsp;order|排序方式||false|string||
|&emsp;&emsp;orderBy|排序字段||false|string||
|&emsp;&emsp;publishTimeBegin|发布时间-开始范围||false|string(date-time)||
|&emsp;&emsp;publishTimeEnd|发布时间-结束范围||false|string(date-time)||
|&emsp;&emsp;size|每页大小||false|integer(int32)||
|&emsp;&emsp;status|模型状态||false|string||
|&emsp;&emsp;trainingId|训练任务ID||false|integer(int64)||
|&emsp;&emsp;version|模型版本||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«AlgorithmModel对象»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«AlgorithmModel对象»|IPage«AlgorithmModel对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|AlgorithmModel对象|
|&emsp;&emsp;&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;&emsp;&emsp;algorithmId|算法ID|integer||
|&emsp;&emsp;&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|integer||
|&emsp;&emsp;&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;&emsp;&emsp;createdTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除：0-否，1-是|integer||
|&emsp;&emsp;&emsp;&emsp;deployCount|部署次数|integer||
|&emsp;&emsp;&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;&emsp;&emsp;downloadCount|下载次数|integer||
|&emsp;&emsp;&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;&emsp;&emsp;id|模型ID|integer||
|&emsp;&emsp;&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;&emsp;&emsp;publishTime|发布时间|string||
|&emsp;&emsp;&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;&emsp;&emsp;trainingId|训练任务ID|integer||
|&emsp;&emsp;&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;&emsp;&emsp;updatedTime|更新时间|string||
|&emsp;&emsp;&emsp;&emsp;version|模型版本|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"accuracy": 0,
				"algorithmId": 0,
				"algorithmName": "",
				"createdBy": 0,
				"createdByName": "",
				"createdTime": "",
				"deleted": 0,
				"deployCount": 0,
				"description": "",
				"downloadCount": 0,
				"formattedAccuracy": "",
				"id": 0,
				"modelFormat": "",
				"modelFormatDesc": "",
				"modelName": "",
				"modelPath": "",
				"modelSize": "",
				"publishTime": "",
				"status": "",
				"statusDesc": "",
				"trainingId": 0,
				"trainingTaskName": "",
				"updatedTime": "",
				"version": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 查询热门模型


**接口地址**:`/api/algorithm-model/popular`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|limit|限制数量|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmModel对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"accuracy": 0,
			"algorithmId": 0,
			"algorithmName": "",
			"createdBy": 0,
			"createdByName": "",
			"createdTime": "",
			"deleted": 0,
			"deployCount": 0,
			"description": "",
			"downloadCount": 0,
			"formattedAccuracy": "",
			"id": 0,
			"modelFormat": "",
			"modelFormatDesc": "",
			"modelName": "",
			"modelPath": "",
			"modelSize": "",
			"publishTime": "",
			"status": "",
			"statusDesc": "",
			"trainingId": 0,
			"trainingTaskName": "",
			"updatedTime": "",
			"version": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 发布模型


**接口地址**:`/api/algorithm-model/publish/{id}`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|模型ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取模型统计信息


**接口地址**:`/api/algorithm-model/statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmModelStatisticsDTO»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmModelStatisticsDTO|AlgorithmModelStatisticsDTO|
|&emsp;&emsp;avgAccuracy|平均准确率|number||
|&emsp;&emsp;draftCount|草稿状态模型数|integer(int64)||
|&emsp;&emsp;maxAccuracy|最高准确率|number||
|&emsp;&emsp;minAccuracy|最低准确率|number||
|&emsp;&emsp;monthlyNewCount|本月新增模型数|integer(int64)||
|&emsp;&emsp;mostPopularModelDownloadCount|最受欢迎的模型下载次数|integer(int64)||
|&emsp;&emsp;mostPopularModelName|最受欢迎的模型名称|string||
|&emsp;&emsp;onnxFormatCount|ONNX格式模型数|integer(int64)||
|&emsp;&emsp;publishedCount|已发布模型数|integer(int64)||
|&emsp;&emsp;pytorchFormatCount|PyTorch格式模型数|integer(int64)||
|&emsp;&emsp;tensorflowFormatCount|TensorFlow格式模型数|integer(int64)||
|&emsp;&emsp;testingCount|测试状态模型数|integer(int64)||
|&emsp;&emsp;totalCount|模型总数|integer(int64)||
|&emsp;&emsp;totalDeployCount|总部署次数|integer(int64)||
|&emsp;&emsp;totalDownloadCount|总下载次数|integer(int64)||
|&emsp;&emsp;weeklyNewCount|本周新增模型数|integer(int64)||
|&emsp;&emsp;yearlyNewCount|本年新增模型数|integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"avgAccuracy": 0,
		"draftCount": 0,
		"maxAccuracy": 0,
		"minAccuracy": 0,
		"monthlyNewCount": 0,
		"mostPopularModelDownloadCount": 0,
		"mostPopularModelName": "",
		"onnxFormatCount": 0,
		"publishedCount": 0,
		"pytorchFormatCount": 0,
		"tensorflowFormatCount": 0,
		"testingCount": 0,
		"totalCount": 0,
		"totalDeployCount": 0,
		"totalDownloadCount": 0,
		"weeklyNewCount": 0,
		"yearlyNewCount": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据状态查询模型列表


**接口地址**:`/api/algorithm-model/status/{status}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|status|状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmModel对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"accuracy": 0,
			"algorithmId": 0,
			"algorithmName": "",
			"createdBy": 0,
			"createdByName": "",
			"createdTime": "",
			"deleted": 0,
			"deployCount": 0,
			"description": "",
			"downloadCount": 0,
			"formattedAccuracy": "",
			"id": 0,
			"modelFormat": "",
			"modelFormatDesc": "",
			"modelName": "",
			"modelPath": "",
			"modelSize": "",
			"publishTime": "",
			"status": "",
			"statusDesc": "",
			"trainingId": 0,
			"trainingTaskName": "",
			"updatedTime": "",
			"version": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取算法模型的总大小


**接口地址**:`/api/algorithm-model/total-size`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«long»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||integer(int64)|integer(int64)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据训练任务ID查询模型列表


**接口地址**:`/api/algorithm-model/training/{trainingId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|trainingId|训练任务ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«AlgorithmModel对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"accuracy": 0,
			"algorithmId": 0,
			"algorithmName": "",
			"createdBy": 0,
			"createdByName": "",
			"createdTime": "",
			"deleted": 0,
			"deployCount": 0,
			"description": "",
			"downloadCount": 0,
			"formattedAccuracy": "",
			"id": 0,
			"modelFormat": "",
			"modelFormatDesc": "",
			"modelName": "",
			"modelPath": "",
			"modelSize": "",
			"publishTime": "",
			"status": "",
			"statusDesc": "",
			"trainingId": 0,
			"trainingTaskName": "",
			"updatedTime": "",
			"version": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 撤销发布模型


**接口地址**:`/api/algorithm-model/unpublish/{id}`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|模型ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新算法模型


**接口地址**:`/api/algorithm-model/update`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accuracy": 0.95,
  "description": "基于YOLOv5的目标检测模型",
  "id": 1,
  "modelFormat": "ONNX",
  "modelName": "目标检测模型v1.0",
  "modelPath": "/models/detection/v1.0/model.onnx",
  "modelSize": "100MB",
  "status": "draft",
  "version": "v1.0.0"
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|updateDTO|算法模型更新参数|body|true|AlgorithmModelUpdateDTO|AlgorithmModelUpdateDTO|
|&emsp;&emsp;accuracy|模型准确率||false|number||
|&emsp;&emsp;description|模型描述||false|string||
|&emsp;&emsp;id|模型ID||true|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式||false|string||
|&emsp;&emsp;modelName|模型名称||false|string||
|&emsp;&emsp;modelPath|模型文件路径||false|string||
|&emsp;&emsp;modelSize|模型大小||false|string||
|&emsp;&emsp;status|状态||false|string||
|&emsp;&emsp;version|模型版本||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmModel对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmModel对象|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"algorithmId": 0,
		"algorithmName": "",
		"createdBy": 0,
		"createdByName": "",
		"createdTime": "",
		"deleted": 0,
		"deployCount": 0,
		"description": "",
		"downloadCount": 0,
		"formattedAccuracy": "",
		"id": 0,
		"modelFormat": "",
		"modelFormatDesc": "",
		"modelName": "",
		"modelPath": "",
		"modelSize": "",
		"publishTime": "",
		"status": "",
		"statusDesc": "",
		"trainingId": 0,
		"trainingTaskName": "",
		"updatedTime": "",
		"version": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据ID查询算法模型详情


**接口地址**:`/api/algorithm-model/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|模型ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmModel对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmModel对象|AlgorithmModel对象|
|&emsp;&emsp;accuracy|模型准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;deployCount|部署次数|integer(int32)||
|&emsp;&emsp;description|模型描述|string||
|&emsp;&emsp;downloadCount|下载次数|integer(int32)||
|&emsp;&emsp;formattedAccuracy||string||
|&emsp;&emsp;id|模型ID|integer(int64)||
|&emsp;&emsp;modelFormat|模型格式：ONNX,PyTorch,TensorFlow|string||
|&emsp;&emsp;modelFormatDesc||string||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;modelPath|模型文件路径|string||
|&emsp;&emsp;modelSize|模型大小|string||
|&emsp;&emsp;publishTime|发布时间|string(date-time)||
|&emsp;&emsp;status|状态：draft-草稿,testing-测试中,published-已发布|string||
|&emsp;&emsp;statusDesc|状态描述|string||
|&emsp;&emsp;trainingId|训练任务ID|integer(int64)||
|&emsp;&emsp;trainingTaskName|训练任务名称|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|&emsp;&emsp;version|模型版本|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"algorithmId": 0,
		"algorithmName": "",
		"createdBy": 0,
		"createdByName": "",
		"createdTime": "",
		"deleted": 0,
		"deployCount": 0,
		"description": "",
		"downloadCount": 0,
		"formattedAccuracy": "",
		"id": 0,
		"modelFormat": "",
		"modelFormatDesc": "",
		"modelName": "",
		"modelPath": "",
		"modelSize": "",
		"publishTime": "",
		"status": "",
		"statusDesc": "",
		"trainingId": 0,
		"trainingTaskName": "",
		"updatedTime": "",
		"version": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除算法模型


**接口地址**:`/api/algorithm-model/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|模型ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 算法管理


## 创建算法


**接口地址**:`/api/algorithm`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>新增算法</p>



**请求示例**:


```javascript
{
  "accuracy": 0,
  "category": "",
  "categoryName": "",
  "configParams": "",
  "createdBy": "",
  "createdTime": "",
  "deleted": 0,
  "deployCount": 0,
  "deployStatus": "",
  "description": "",
  "gpuRequired": 0,
  "id": 0,
  "imageUrl": "",
  "inputFormat": "",
  "lastDeployTime": "",
  "memoryUsage": 0,
  "modelFilePath": "",
  "name": "",
  "outputFormat": "",
  "processingSpeed": 0,
  "repositoryId": 0,
  "updatedBy": "",
  "updatedTime": "",
  "version": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithm|algorithm|body|true|Algorithm|Algorithm|
|&emsp;&emsp;accuracy|||false|number||
|&emsp;&emsp;category|||false|string||
|&emsp;&emsp;categoryName|||false|string||
|&emsp;&emsp;configParams|||false|string||
|&emsp;&emsp;createdBy|||false|string||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;deleted|||false|integer(int32)||
|&emsp;&emsp;deployCount|||false|integer(int32)||
|&emsp;&emsp;deployStatus|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;gpuRequired|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;imageUrl|||false|string||
|&emsp;&emsp;inputFormat|||false|string||
|&emsp;&emsp;lastDeployTime|||false|string(date-time)||
|&emsp;&emsp;memoryUsage|||false|integer(int32)||
|&emsp;&emsp;modelFilePath|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;outputFormat|||false|string||
|&emsp;&emsp;processingSpeed|||false|integer(int32)||
|&emsp;&emsp;repositoryId|||false|integer(int64)||
|&emsp;&emsp;updatedBy|||false|string||
|&emsp;&emsp;updatedTime|||false|string(date-time)||
|&emsp;&emsp;version|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除算法


**接口地址**:`/api/algorithm/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID列表批量删除算法</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量更新部署状态


**接口地址**:`/api/algorithm/batch/deploy-status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量更新算法的部署状态</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deployStatus|部署状态|query|true|string||
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据分类查询算法列表


**接口地址**:`/api/algorithm/category/{category}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定分类的所有算法</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|category|算法类型|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Algorithm»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|Algorithm|
|&emsp;&emsp;accuracy||number||
|&emsp;&emsp;category||string||
|&emsp;&emsp;categoryName||string||
|&emsp;&emsp;configParams||string||
|&emsp;&emsp;createdBy||string||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;deployCount||integer(int32)||
|&emsp;&emsp;deployStatus||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;gpuRequired||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;imageUrl||string||
|&emsp;&emsp;inputFormat||string||
|&emsp;&emsp;lastDeployTime||string(date-time)||
|&emsp;&emsp;memoryUsage||integer(int32)||
|&emsp;&emsp;modelFilePath||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;outputFormat||string||
|&emsp;&emsp;processingSpeed||integer(int32)||
|&emsp;&emsp;repositoryId||integer(int64)||
|&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;version||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"accuracy": 0,
			"category": "",
			"categoryName": "",
			"configParams": "",
			"createdBy": "",
			"createdTime": "",
			"deleted": 0,
			"deployCount": 0,
			"deployStatus": "",
			"description": "",
			"gpuRequired": 0,
			"id": 0,
			"imageUrl": "",
			"inputFormat": "",
			"lastDeployTime": "",
			"memoryUsage": 0,
			"modelFilePath": "",
			"name": "",
			"outputFormat": "",
			"processingSpeed": 0,
			"repositoryId": 0,
			"updatedBy": "",
			"updatedTime": "",
			"version": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 统计某仓库下的算法数量


**接口地址**:`/api/algorithm/count/repository/{repositoryId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定仓库的算法数量</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|repositoryId|仓库ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«long»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||integer(int64)|integer(int64)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询算法列表


**接口地址**:`/api/algorithm/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据条件分页查询算法</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|category|算法类型|query|false|string||
|current|当前页|query|false|integer(int32)||
|deployStatus|部署状态|query|false|string||
|name|算法名称|query|false|string||
|repositoryId|仓库ID|query|false|integer(int64)||
|size|每页大小|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«Algorithm»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«Algorithm»|IPage«Algorithm»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|Algorithm|
|&emsp;&emsp;&emsp;&emsp;accuracy||number||
|&emsp;&emsp;&emsp;&emsp;category||string||
|&emsp;&emsp;&emsp;&emsp;categoryName||string||
|&emsp;&emsp;&emsp;&emsp;configParams||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||string||
|&emsp;&emsp;&emsp;&emsp;createdTime||string||
|&emsp;&emsp;&emsp;&emsp;deleted||integer||
|&emsp;&emsp;&emsp;&emsp;deployCount||integer||
|&emsp;&emsp;&emsp;&emsp;deployStatus||string||
|&emsp;&emsp;&emsp;&emsp;description||string||
|&emsp;&emsp;&emsp;&emsp;gpuRequired||integer||
|&emsp;&emsp;&emsp;&emsp;id||integer||
|&emsp;&emsp;&emsp;&emsp;imageUrl||string||
|&emsp;&emsp;&emsp;&emsp;inputFormat||string||
|&emsp;&emsp;&emsp;&emsp;lastDeployTime||string||
|&emsp;&emsp;&emsp;&emsp;memoryUsage||integer||
|&emsp;&emsp;&emsp;&emsp;modelFilePath||string||
|&emsp;&emsp;&emsp;&emsp;name||string||
|&emsp;&emsp;&emsp;&emsp;outputFormat||string||
|&emsp;&emsp;&emsp;&emsp;processingSpeed||integer||
|&emsp;&emsp;&emsp;&emsp;repositoryId||integer||
|&emsp;&emsp;&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;&emsp;&emsp;updatedTime||string||
|&emsp;&emsp;&emsp;&emsp;version||string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"accuracy": 0,
				"category": "",
				"categoryName": "",
				"configParams": "",
				"createdBy": "",
				"createdTime": "",
				"deleted": 0,
				"deployCount": 0,
				"deployStatus": "",
				"description": "",
				"gpuRequired": 0,
				"id": 0,
				"imageUrl": "",
				"inputFormat": "",
				"lastDeployTime": "",
				"memoryUsage": 0,
				"modelFilePath": "",
				"name": "",
				"outputFormat": "",
				"processingSpeed": 0,
				"repositoryId": 0,
				"updatedBy": "",
				"updatedTime": "",
				"version": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据仓库ID查询算法列表


**接口地址**:`/api/algorithm/repository/{repositoryId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定仓库下的所有算法</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|repositoryId|仓库ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Algorithm»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|Algorithm|
|&emsp;&emsp;accuracy||number||
|&emsp;&emsp;category||string||
|&emsp;&emsp;categoryName||string||
|&emsp;&emsp;configParams||string||
|&emsp;&emsp;createdBy||string||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;deployCount||integer(int32)||
|&emsp;&emsp;deployStatus||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;gpuRequired||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;imageUrl||string||
|&emsp;&emsp;inputFormat||string||
|&emsp;&emsp;lastDeployTime||string(date-time)||
|&emsp;&emsp;memoryUsage||integer(int32)||
|&emsp;&emsp;modelFilePath||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;outputFormat||string||
|&emsp;&emsp;processingSpeed||integer(int32)||
|&emsp;&emsp;repositoryId||integer(int64)||
|&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;version||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"accuracy": 0,
			"category": "",
			"categoryName": "",
			"configParams": "",
			"createdBy": "",
			"createdTime": "",
			"deleted": 0,
			"deployCount": 0,
			"deployStatus": "",
			"description": "",
			"gpuRequired": 0,
			"id": 0,
			"imageUrl": "",
			"inputFormat": "",
			"lastDeployTime": "",
			"memoryUsage": 0,
			"modelFilePath": "",
			"name": "",
			"outputFormat": "",
			"processingSpeed": 0,
			"repositoryId": 0,
			"updatedBy": "",
			"updatedTime": "",
			"version": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取算法分类统计


**接口地址**:`/api/algorithm/statistics/category`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取各分类的算法数量统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取部署状态统计


**接口地址**:`/api/algorithm/statistics/deploy-status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取各部署状态的算法数量统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取算法类型统计


**接口地址**:`/api/algorithm/statistics/type`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取各类型的算法数量统计</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 算法评估


**接口地址**:`/api/algorithm/{algorithmId}/evaluate`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>对算法进行性能评估</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithmId|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 查询算法详情


**接口地址**:`/api/algorithm/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID获取算法详细信息</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Algorithm»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||Algorithm|Algorithm|
|&emsp;&emsp;accuracy||number||
|&emsp;&emsp;category||string||
|&emsp;&emsp;categoryName||string||
|&emsp;&emsp;configParams||string||
|&emsp;&emsp;createdBy||string||
|&emsp;&emsp;createdTime||string(date-time)||
|&emsp;&emsp;deleted||integer(int32)||
|&emsp;&emsp;deployCount||integer(int32)||
|&emsp;&emsp;deployStatus||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;gpuRequired||integer(int32)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;imageUrl||string||
|&emsp;&emsp;inputFormat||string||
|&emsp;&emsp;lastDeployTime||string(date-time)||
|&emsp;&emsp;memoryUsage||integer(int32)||
|&emsp;&emsp;modelFilePath||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;outputFormat||string||
|&emsp;&emsp;processingSpeed||integer(int32)||
|&emsp;&emsp;repositoryId||integer(int64)||
|&emsp;&emsp;updatedBy||string||
|&emsp;&emsp;updatedTime||string(date-time)||
|&emsp;&emsp;version||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"category": "",
		"categoryName": "",
		"configParams": "",
		"createdBy": "",
		"createdTime": "",
		"deleted": 0,
		"deployCount": 0,
		"deployStatus": "",
		"description": "",
		"gpuRequired": 0,
		"id": 0,
		"imageUrl": "",
		"inputFormat": "",
		"lastDeployTime": "",
		"memoryUsage": 0,
		"modelFilePath": "",
		"name": "",
		"outputFormat": "",
		"processingSpeed": 0,
		"repositoryId": 0,
		"updatedBy": "",
		"updatedTime": "",
		"version": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新算法


**接口地址**:`/api/algorithm/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID更新算法信息</p>



**请求示例**:


```javascript
{
  "accuracy": 0,
  "category": "",
  "categoryName": "",
  "configParams": "",
  "createdBy": "",
  "createdTime": "",
  "deleted": 0,
  "deployCount": 0,
  "deployStatus": "",
  "description": "",
  "gpuRequired": 0,
  "id": 0,
  "imageUrl": "",
  "inputFormat": "",
  "lastDeployTime": "",
  "memoryUsage": 0,
  "modelFilePath": "",
  "name": "",
  "outputFormat": "",
  "processingSpeed": 0,
  "repositoryId": 0,
  "updatedBy": "",
  "updatedTime": "",
  "version": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithm|algorithm|body|true|Algorithm|Algorithm|
|&emsp;&emsp;accuracy|||false|number||
|&emsp;&emsp;category|||false|string||
|&emsp;&emsp;categoryName|||false|string||
|&emsp;&emsp;configParams|||false|string||
|&emsp;&emsp;createdBy|||false|string||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;deleted|||false|integer(int32)||
|&emsp;&emsp;deployCount|||false|integer(int32)||
|&emsp;&emsp;deployStatus|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;gpuRequired|||false|integer(int32)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;imageUrl|||false|string||
|&emsp;&emsp;inputFormat|||false|string||
|&emsp;&emsp;lastDeployTime|||false|string(date-time)||
|&emsp;&emsp;memoryUsage|||false|integer(int32)||
|&emsp;&emsp;modelFilePath|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;outputFormat|||false|string||
|&emsp;&emsp;processingSpeed|||false|integer(int32)||
|&emsp;&emsp;repositoryId|||false|integer(int64)||
|&emsp;&emsp;updatedBy|||false|string||
|&emsp;&emsp;updatedTime|||false|string(date-time)||
|&emsp;&emsp;version|||false|string||
|id|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除算法


**接口地址**:`/api/algorithm/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID删除算法（软删除）</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 部署算法到设备


**接口地址**:`/api/algorithm/{id}/deploy`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>将算法部署到指定设备</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithmId|算法ID|path|true|integer(int64)||
|deviceIds|deviceIds|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新部署状态


**接口地址**:`/api/algorithm/{id}/deploy-status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新算法的部署状态</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deployStatus|部署状态|query|true|string||
|id|算法ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 算法训练管理


## 创建训练任务


**接口地址**:`/api/algorithm-training`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>新增训练任务</p>



**请求示例**:


```javascript
{
  "accuracy": 0,
  "algorithmId": 0,
  "algorithmName": "",
  "configParams": "",
  "createdBy": 0,
  "createdByName": "",
  "createdTime": "",
  "datasetId": 0,
  "datasetName": "",
  "deleted": 0,
  "durationMinutes": 0,
  "endTime": "",
  "epochCurrent": 0,
  "epochTotal": 0,
  "errorMessage": "",
  "estimatedTime": "",
  "gpuUsage": "",
  "id": 0,
  "logPath": "",
  "lossValue": 0,
  "mapValue": 0,
  "modelOutputPath": "",
  "precisionValue": 0,
  "progress": 0,
  "recallValue": 0,
  "startTime": "",
  "status": 0,
  "taskName": "",
  "trainStatus": "",
  "trainStatusDesc": "",
  "trainType": "",
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|training|算法训练任务|body|true|AlgorithmTraining对象Req|AlgorithmTraining对象Req|
|&emsp;&emsp;accuracy|准确率||false|number||
|&emsp;&emsp;algorithmId|算法ID||false|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称||false|string||
|&emsp;&emsp;configParams|训练参数||false|string||
|&emsp;&emsp;createdBy|创建人||false|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名||false|string||
|&emsp;&emsp;createdTime|创建时间||false|string(date-time)||
|&emsp;&emsp;datasetId|数据集ID||false|integer(int64)||
|&emsp;&emsp;datasetName|数据集名称||false|string||
|&emsp;&emsp;deleted|是否删除：0-否，1-是||false|integer(int32)||
|&emsp;&emsp;durationMinutes|训练时长（分钟）||false|integer(int64)||
|&emsp;&emsp;endTime|结束时间||false|string(date-time)||
|&emsp;&emsp;epochCurrent|当前轮次||false|integer(int32)||
|&emsp;&emsp;epochTotal|总轮次||false|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息||false|string||
|&emsp;&emsp;estimatedTime|预计时间||false|string||
|&emsp;&emsp;gpuUsage|GPU使用率||false|string||
|&emsp;&emsp;id|训练任务ID||false|integer(int64)||
|&emsp;&emsp;logPath|日志路径||false|string||
|&emsp;&emsp;lossValue|损失值||false|number||
|&emsp;&emsp;mapValue|mAP值||false|number||
|&emsp;&emsp;modelOutputPath|模型输出路径||false|string||
|&emsp;&emsp;precisionValue|精确率||false|number||
|&emsp;&emsp;progress|训练进度百分比||false|integer(int32)||
|&emsp;&emsp;recallValue|召回率||false|number||
|&emsp;&emsp;startTime|开始时间||false|string(date-time)||
|&emsp;&emsp;status|状态：0-等待,1-训练中,2-完成,3-停止,4-失败||false|integer(int32)||
|&emsp;&emsp;taskName|任务名称||false|string||
|&emsp;&emsp;trainStatus|训练状态：pending-等待,training-训练中,completed-完成,failed-失败||false|string||
|&emsp;&emsp;trainStatusDesc|训练状态描述||false|string||
|&emsp;&emsp;trainType|训练类型||false|string||
|&emsp;&emsp;updatedTime|更新时间||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除训练任务


**接口地址**:`/api/algorithm-training/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID列表批量删除训练任务</p>



**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 检查远程服务器文件


**接口地址**:`/api/algorithm-training/check-remote-files`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>检查远程服务器上的模型文件</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 诊断conda环境


**接口地址**:`/api/algorithm-training/diagnose-conda`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>检查远程服务器上的conda安装情况</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 下载模型文件


**接口地址**:`/api/algorithm-training/download-model`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>从远程服务器下载训练好的模型文件</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|path|path|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## 分页查询训练任务列表


**接口地址**:`/api/algorithm-training/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据条件分页查询训练任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|current|当前页|query|false|integer(int32)||
|size|每页大小|query|false|integer(int32)||
|taskName|任务名称|query|false|string||
|trainType|训练类型|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«AlgorithmTraining对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«AlgorithmTraining对象»|IPage«AlgorithmTraining对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|AlgorithmTraining对象Res|
|&emsp;&emsp;&emsp;&emsp;accuracy|准确率|number||
|&emsp;&emsp;&emsp;&emsp;algorithmId|算法ID|integer||
|&emsp;&emsp;&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;&emsp;&emsp;baseModel||string||
|&emsp;&emsp;&emsp;&emsp;configParams|训练参数|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|integer||
|&emsp;&emsp;&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;&emsp;&emsp;createdTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;datasetId|数据集ID|integer||
|&emsp;&emsp;&emsp;&emsp;datasetName|数据集名称|string||
|&emsp;&emsp;&emsp;&emsp;datasetPath||string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除：0-否，1-是|integer||
|&emsp;&emsp;&emsp;&emsp;durationMinutes|训练时长（分钟）|integer||
|&emsp;&emsp;&emsp;&emsp;endTime|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;epochCurrent|当前轮次|integer||
|&emsp;&emsp;&emsp;&emsp;epochTotal|总轮次|integer||
|&emsp;&emsp;&emsp;&emsp;errorMessage|错误信息|string||
|&emsp;&emsp;&emsp;&emsp;estimatedTime|预计时间|string||
|&emsp;&emsp;&emsp;&emsp;gpuUsage|GPU使用率|string||
|&emsp;&emsp;&emsp;&emsp;id|训练任务ID|integer||
|&emsp;&emsp;&emsp;&emsp;logPath|日志路径|string||
|&emsp;&emsp;&emsp;&emsp;lossValue|损失值|number||
|&emsp;&emsp;&emsp;&emsp;mapValue|mAP值|number||
|&emsp;&emsp;&emsp;&emsp;modelOutputPath|模型输出路径|string||
|&emsp;&emsp;&emsp;&emsp;precisionValue|精确率|number||
|&emsp;&emsp;&emsp;&emsp;progress|训练进度百分比|integer||
|&emsp;&emsp;&emsp;&emsp;recallValue|召回率|number||
|&emsp;&emsp;&emsp;&emsp;startTime|开始时间|string||
|&emsp;&emsp;&emsp;&emsp;status|状态：0-等待,1-训练中,2-完成,3-停止,4-失败|integer||
|&emsp;&emsp;&emsp;&emsp;taskName|任务名称|string||
|&emsp;&emsp;&emsp;&emsp;trainParams||string||
|&emsp;&emsp;&emsp;&emsp;trainStatus|训练状态：pending-等待,training-训练中,completed-完成,failed-失败|string||
|&emsp;&emsp;&emsp;&emsp;trainStatusDesc|训练状态描述|string||
|&emsp;&emsp;&emsp;&emsp;trainType|训练类型|string||
|&emsp;&emsp;&emsp;&emsp;updatedTime|更新时间|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"accuracy": 0,
				"algorithmId": 0,
				"algorithmName": "",
				"baseModel": "",
				"configParams": "",
				"createdBy": 0,
				"createdByName": "",
				"createdTime": "",
				"datasetId": 0,
				"datasetName": "",
				"datasetPath": "",
				"deleted": 0,
				"durationMinutes": 0,
				"endTime": "",
				"epochCurrent": 0,
				"epochTotal": 0,
				"errorMessage": "",
				"estimatedTime": "",
				"gpuUsage": "",
				"id": 0,
				"logPath": "",
				"lossValue": 0,
				"mapValue": 0,
				"modelOutputPath": "",
				"precisionValue": 0,
				"progress": 0,
				"recallValue": 0,
				"startTime": "",
				"status": 0,
				"taskName": "",
				"trainParams": "",
				"trainStatus": "",
				"trainStatusDesc": "",
				"trainType": "",
				"updatedTime": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 查询训练任务详情


**接口地址**:`/api/algorithm-training/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID获取训练任务详细信息</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«AlgorithmTraining对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||AlgorithmTraining对象Res|AlgorithmTraining对象Res|
|&emsp;&emsp;accuracy|准确率|number||
|&emsp;&emsp;algorithmId|算法ID|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称|string||
|&emsp;&emsp;baseModel||string||
|&emsp;&emsp;configParams|训练参数|string||
|&emsp;&emsp;createdBy|创建人|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名|string||
|&emsp;&emsp;createdTime|创建时间|string(date-time)||
|&emsp;&emsp;datasetId|数据集ID|integer(int64)||
|&emsp;&emsp;datasetName|数据集名称|string||
|&emsp;&emsp;datasetPath||string||
|&emsp;&emsp;deleted|是否删除：0-否，1-是|integer(int32)||
|&emsp;&emsp;durationMinutes|训练时长（分钟）|integer(int64)||
|&emsp;&emsp;endTime|结束时间|string(date-time)||
|&emsp;&emsp;epochCurrent|当前轮次|integer(int32)||
|&emsp;&emsp;epochTotal|总轮次|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息|string||
|&emsp;&emsp;estimatedTime|预计时间|string||
|&emsp;&emsp;gpuUsage|GPU使用率|string||
|&emsp;&emsp;id|训练任务ID|integer(int64)||
|&emsp;&emsp;logPath|日志路径|string||
|&emsp;&emsp;lossValue|损失值|number||
|&emsp;&emsp;mapValue|mAP值|number||
|&emsp;&emsp;modelOutputPath|模型输出路径|string||
|&emsp;&emsp;precisionValue|精确率|number||
|&emsp;&emsp;progress|训练进度百分比|integer(int32)||
|&emsp;&emsp;recallValue|召回率|number||
|&emsp;&emsp;startTime|开始时间|string(date-time)||
|&emsp;&emsp;status|状态：0-等待,1-训练中,2-完成,3-停止,4-失败|integer(int32)||
|&emsp;&emsp;taskName|任务名称|string||
|&emsp;&emsp;trainParams||string||
|&emsp;&emsp;trainStatus|训练状态：pending-等待,training-训练中,completed-完成,failed-失败|string||
|&emsp;&emsp;trainStatusDesc|训练状态描述|string||
|&emsp;&emsp;trainType|训练类型|string||
|&emsp;&emsp;updatedTime|更新时间|string(date-time)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"accuracy": 0,
		"algorithmId": 0,
		"algorithmName": "",
		"baseModel": "",
		"configParams": "",
		"createdBy": 0,
		"createdByName": "",
		"createdTime": "",
		"datasetId": 0,
		"datasetName": "",
		"datasetPath": "",
		"deleted": 0,
		"durationMinutes": 0,
		"endTime": "",
		"epochCurrent": 0,
		"epochTotal": 0,
		"errorMessage": "",
		"estimatedTime": "",
		"gpuUsage": "",
		"id": 0,
		"logPath": "",
		"lossValue": 0,
		"mapValue": 0,
		"modelOutputPath": "",
		"precisionValue": 0,
		"progress": 0,
		"recallValue": 0,
		"startTime": "",
		"status": 0,
		"taskName": "",
		"trainParams": "",
		"trainStatus": "",
		"trainStatusDesc": "",
		"trainType": "",
		"updatedTime": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新训练任务


**接口地址**:`/api/algorithm-training/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID更新训练任务信息</p>



**请求示例**:


```javascript
{
  "accuracy": 0,
  "algorithmId": 0,
  "algorithmName": "",
  "configParams": "",
  "createdBy": 0,
  "createdByName": "",
  "createdTime": "",
  "datasetId": 0,
  "datasetName": "",
  "deleted": 0,
  "durationMinutes": 0,
  "endTime": "",
  "epochCurrent": 0,
  "epochTotal": 0,
  "errorMessage": "",
  "estimatedTime": "",
  "gpuUsage": "",
  "id": 0,
  "logPath": "",
  "lossValue": 0,
  "mapValue": 0,
  "modelOutputPath": "",
  "precisionValue": 0,
  "progress": 0,
  "recallValue": 0,
  "startTime": "",
  "status": 0,
  "taskName": "",
  "trainStatus": "",
  "trainStatusDesc": "",
  "trainType": "",
  "updatedTime": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||
|training|算法训练任务|body|true|AlgorithmTraining对象Req|AlgorithmTraining对象Req|
|&emsp;&emsp;accuracy|准确率||false|number||
|&emsp;&emsp;algorithmId|算法ID||false|integer(int64)||
|&emsp;&emsp;algorithmName|算法名称||false|string||
|&emsp;&emsp;configParams|训练参数||false|string||
|&emsp;&emsp;createdBy|创建人||false|integer(int64)||
|&emsp;&emsp;createdByName|创建人姓名||false|string||
|&emsp;&emsp;createdTime|创建时间||false|string(date-time)||
|&emsp;&emsp;datasetId|数据集ID||false|integer(int64)||
|&emsp;&emsp;datasetName|数据集名称||false|string||
|&emsp;&emsp;deleted|是否删除：0-否，1-是||false|integer(int32)||
|&emsp;&emsp;durationMinutes|训练时长（分钟）||false|integer(int64)||
|&emsp;&emsp;endTime|结束时间||false|string(date-time)||
|&emsp;&emsp;epochCurrent|当前轮次||false|integer(int32)||
|&emsp;&emsp;epochTotal|总轮次||false|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息||false|string||
|&emsp;&emsp;estimatedTime|预计时间||false|string||
|&emsp;&emsp;gpuUsage|GPU使用率||false|string||
|&emsp;&emsp;id|训练任务ID||false|integer(int64)||
|&emsp;&emsp;logPath|日志路径||false|string||
|&emsp;&emsp;lossValue|损失值||false|number||
|&emsp;&emsp;mapValue|mAP值||false|number||
|&emsp;&emsp;modelOutputPath|模型输出路径||false|string||
|&emsp;&emsp;precisionValue|精确率||false|number||
|&emsp;&emsp;progress|训练进度百分比||false|integer(int32)||
|&emsp;&emsp;recallValue|召回率||false|number||
|&emsp;&emsp;startTime|开始时间||false|string(date-time)||
|&emsp;&emsp;status|状态：0-等待,1-训练中,2-完成,3-停止,4-失败||false|integer(int32)||
|&emsp;&emsp;taskName|任务名称||false|string||
|&emsp;&emsp;trainStatus|训练状态：pending-等待,training-训练中,completed-完成,failed-失败||false|string||
|&emsp;&emsp;trainStatusDesc|训练状态描述||false|string||
|&emsp;&emsp;trainType|训练类型||false|string||
|&emsp;&emsp;updatedTime|更新时间||false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除训练任务


**接口地址**:`/api/algorithm-training/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID删除训练任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取训练日志


**接口地址**:`/api/algorithm-training/{id}/logs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定训练任务的日志</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 开始训练任务


**接口地址**:`/api/algorithm-training/{id}/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>开始指定的训练任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||
|batchSize|批次大小|query|false|integer(int32)||
|datasetPath|数据集路径|query|false|string||
|epochs|训练轮数|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取训练状态


**接口地址**:`/api/algorithm-training/{id}/status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定训练任务的状态</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新训练状态


**接口地址**:`/api/algorithm-training/{id}/status`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新指定训练任务的状态</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||
|statusUpdate|statusUpdate|body|true|||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 停止训练任务


**接口地址**:`/api/algorithm-training/{id}/stop`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>停止指定的训练任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|训练任务ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 视频录制记录管理


## 清理过期录制记录


**接口地址**:`/api/video-record/cleanup`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据设备ID查询录制记录


**接口地址**:`/api/video-record/device/{deviceId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||
|currentPage|当前页|query|false|integer(int32)||
|date|日期|query|false|string(date)||
|pageSize|页面大小|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«VideoRecord对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|VideoRecord对象|
|&emsp;&emsp;bitrate|比特率(kbps)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;duration|视频时长(秒)|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;fileSize|文件大小(字节)|integer(int64)||
|&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;frameRate|帧率|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;qualityText||string||
|&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string(date)||
|&emsp;&emsp;recordEndTime|录制结束时间|string(date-time)||
|&emsp;&emsp;recordStartTime|录制开始时间|string(date-time)||
|&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"bitrate": 0,
			"createTime": "",
			"createdBy": "",
			"deleted": 0,
			"deviceId": 0,
			"deviceName": "",
			"duration": 0,
			"errorMessage": "",
			"fileName": "",
			"filePath": "",
			"fileSize": 0,
			"format": "",
			"formattedDuration": "",
			"formattedFileSize": "",
			"frameRate": 0,
			"id": 0,
			"quality": "",
			"qualityText": "",
			"recordDate": "",
			"recordEndTime": "",
			"recordStartTime": "",
			"recordStatus": "",
			"recordStatusText": "",
			"resolution": "",
			"thumbnailPath": "",
			"updateTime": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据设备ID和日期查询录制记录


**接口地址**:`/api/video-record/device/{deviceId}/date/{date}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|date|日期|path|true|string(date)||
|deviceId|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«VideoRecord对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|VideoRecord对象|
|&emsp;&emsp;bitrate|比特率(kbps)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;duration|视频时长(秒)|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;fileSize|文件大小(字节)|integer(int64)||
|&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;frameRate|帧率|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;qualityText||string||
|&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string(date)||
|&emsp;&emsp;recordEndTime|录制结束时间|string(date-time)||
|&emsp;&emsp;recordStartTime|录制开始时间|string(date-time)||
|&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"bitrate": 0,
			"createTime": "",
			"createdBy": "",
			"deleted": 0,
			"deviceId": 0,
			"deviceName": "",
			"duration": 0,
			"errorMessage": "",
			"fileName": "",
			"filePath": "",
			"fileSize": 0,
			"format": "",
			"formattedDuration": "",
			"formattedFileSize": "",
			"frameRate": 0,
			"id": 0,
			"quality": "",
			"qualityText": "",
			"recordDate": "",
			"recordEndTime": "",
			"recordStartTime": "",
			"recordStatus": "",
			"recordStatusText": "",
			"resolution": "",
			"thumbnailPath": "",
			"updateTime": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据设备ID和时间范围查询录制记录


**接口地址**:`/api/video-record/device/{deviceId}/time-range`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||
|endTime|结束时间|query|true|string(date-time)||
|startTime|开始时间|query|true|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«VideoRecord对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|VideoRecord对象|
|&emsp;&emsp;bitrate|比特率(kbps)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;duration|视频时长(秒)|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;fileSize|文件大小(字节)|integer(int64)||
|&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;frameRate|帧率|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;qualityText||string||
|&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string(date)||
|&emsp;&emsp;recordEndTime|录制结束时间|string(date-time)||
|&emsp;&emsp;recordStartTime|录制开始时间|string(date-time)||
|&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"bitrate": 0,
			"createTime": "",
			"createdBy": "",
			"deleted": 0,
			"deviceId": 0,
			"deviceName": "",
			"duration": 0,
			"errorMessage": "",
			"fileName": "",
			"filePath": "",
			"fileSize": 0,
			"format": "",
			"formattedDuration": "",
			"formattedFileSize": "",
			"frameRate": 0,
			"id": 0,
			"quality": "",
			"qualityText": "",
			"recordDate": "",
			"recordEndTime": "",
			"recordStartTime": "",
			"recordStatus": "",
			"recordStatusText": "",
			"resolution": "",
			"thumbnailPath": "",
			"updateTime": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取录制文件


**接口地址**:`/api/video-record/file/**`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Resource|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|description||string||
|file||file|file|
|filename||string||
|inputStream||InputStream|InputStream|
|open||boolean||
|readable||boolean||
|uri||string(uri)|string(uri)|
|url||string(url)|string(url)|


**响应示例**:
```javascript
{
	"description": "",
	"file": "",
	"filename": "",
	"inputStream": {},
	"open": true,
	"readable": true,
	"uri": "",
	"url": ""
}
```


## 分页查询视频录制记录


**接口地址**:`/api/video-record/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|current|当前页|query|false|integer(int32)||
|deviceId|设备ID|query|false|integer(int64)||
|deviceName|设备名称|query|false|string||
|endDate|结束日期|query|false|string(date)||
|endTime|结束时间|query|false|string(date-time)||
|quality|录制质量|query|false|string||
|recordStatus|录制状态|query|false|string||
|size|每页大小|query|false|integer(int32)||
|startDate|开始日期|query|false|string(date)||
|startTime|开始时间|query|false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«VideoRecord对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«VideoRecord对象»|IPage«VideoRecord对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|VideoRecord对象|
|&emsp;&emsp;&emsp;&emsp;bitrate|比特率(kbps)|integer||
|&emsp;&emsp;&emsp;&emsp;createTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer||
|&emsp;&emsp;&emsp;&emsp;deviceId|设备ID|integer||
|&emsp;&emsp;&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;&emsp;&emsp;duration|视频时长(秒)|integer||
|&emsp;&emsp;&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;&emsp;&emsp;fileSize|文件大小(字节)|integer||
|&emsp;&emsp;&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;&emsp;&emsp;frameRate|帧率|integer||
|&emsp;&emsp;&emsp;&emsp;id|主键ID|integer||
|&emsp;&emsp;&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;&emsp;&emsp;qualityText||string||
|&emsp;&emsp;&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string||
|&emsp;&emsp;&emsp;&emsp;recordEndTime|录制结束时间|string||
|&emsp;&emsp;&emsp;&emsp;recordStartTime|录制开始时间|string||
|&emsp;&emsp;&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;&emsp;&emsp;updateTime|更新时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"bitrate": 0,
				"createTime": "",
				"createdBy": "",
				"deleted": 0,
				"deviceId": 0,
				"deviceName": "",
				"duration": 0,
				"errorMessage": "",
				"fileName": "",
				"filePath": "",
				"fileSize": 0,
				"format": "",
				"formattedDuration": "",
				"formattedFileSize": "",
				"frameRate": 0,
				"id": 0,
				"quality": "",
				"qualityText": "",
				"recordDate": "",
				"recordEndTime": "",
				"recordStartTime": "",
				"recordStatus": "",
				"recordStatusText": "",
				"resolution": "",
				"thumbnailPath": "",
				"updateTime": "",
				"updatedBy": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取正在录制的视频记录


**接口地址**:`/api/video-record/recording`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«VideoRecord对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|VideoRecord对象|
|&emsp;&emsp;bitrate|比特率(kbps)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;duration|视频时长(秒)|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;fileSize|文件大小(字节)|integer(int64)||
|&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;frameRate|帧率|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;qualityText||string||
|&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string(date)||
|&emsp;&emsp;recordEndTime|录制结束时间|string(date-time)||
|&emsp;&emsp;recordStartTime|录制开始时间|string(date-time)||
|&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"bitrate": 0,
			"createTime": "",
			"createdBy": "",
			"deleted": 0,
			"deviceId": 0,
			"deviceName": "",
			"duration": 0,
			"errorMessage": "",
			"fileName": "",
			"filePath": "",
			"fileSize": 0,
			"format": "",
			"formattedDuration": "",
			"formattedFileSize": "",
			"frameRate": 0,
			"id": 0,
			"quality": "",
			"qualityText": "",
			"recordDate": "",
			"recordEndTime": "",
			"recordStartTime": "",
			"recordStatus": "",
			"recordStatusText": "",
			"resolution": "",
			"thumbnailPath": "",
			"updateTime": "",
			"updatedBy": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 开始录制视频


**接口地址**:`/api/video-record/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|query|true|integer(int64)||
|deviceName|设备名称|query|true|string||
|duration|录制时长(秒)|query|false|integer(int32)||
|quality|录制质量|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«VideoRecord对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||VideoRecord对象|VideoRecord对象|
|&emsp;&emsp;bitrate|比特率(kbps)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;duration|视频时长(秒)|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;fileSize|文件大小(字节)|integer(int64)||
|&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;frameRate|帧率|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;qualityText||string||
|&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string(date)||
|&emsp;&emsp;recordEndTime|录制结束时间|string(date-time)||
|&emsp;&emsp;recordStartTime|录制开始时间|string(date-time)||
|&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"bitrate": 0,
		"createTime": "",
		"createdBy": "",
		"deleted": 0,
		"deviceId": 0,
		"deviceName": "",
		"duration": 0,
		"errorMessage": "",
		"fileName": "",
		"filePath": "",
		"fileSize": 0,
		"format": "",
		"formattedDuration": "",
		"formattedFileSize": "",
		"frameRate": 0,
		"id": 0,
		"quality": "",
		"qualityText": "",
		"recordDate": "",
		"recordEndTime": "",
		"recordStartTime": "",
		"recordStatus": "",
		"recordStatusText": "",
		"resolution": "",
		"thumbnailPath": "",
		"updateTime": "",
		"updatedBy": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备录制统计信息


**接口地址**:`/api/video-record/statistics/device`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|query|false|integer(int64)||
|endDate|结束日期|query|false|string(date)||
|startDate|开始日期|query|false|string(date)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 停止录制视频


**接口地址**:`/api/video-record/stop/{id}`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|录制记录ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取录制文件缩略图


**接口地址**:`/api/video-record/thumbnail/**`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Resource|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|description||string||
|file||file|file|
|filename||string||
|inputStream||InputStream|InputStream|
|open||boolean||
|readable||boolean||
|uri||string(uri)|string(uri)|
|url||string(url)|string(url)|


**响应示例**:
```javascript
{
	"description": "",
	"file": "",
	"filename": "",
	"inputStream": {},
	"open": true,
	"readable": true,
	"uri": "",
	"url": ""
}
```


## 根据ID查询视频录制记录


**接口地址**:`/api/video-record/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|录制记录ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«VideoRecord对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||VideoRecord对象|VideoRecord对象|
|&emsp;&emsp;bitrate|比特率(kbps)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;duration|视频时长(秒)|integer(int32)||
|&emsp;&emsp;errorMessage|错误信息(录制失败时)|string||
|&emsp;&emsp;fileName|视频文件名|string||
|&emsp;&emsp;filePath|视频文件路径|string||
|&emsp;&emsp;fileSize|文件大小(字节)|integer(int64)||
|&emsp;&emsp;format|视频格式|string||
|&emsp;&emsp;formattedDuration||string||
|&emsp;&emsp;formattedFileSize||string||
|&emsp;&emsp;frameRate|帧率|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;quality|录制质量：high-高清，medium-中等，low-低清|string||
|&emsp;&emsp;qualityText||string||
|&emsp;&emsp;recordDate|录制日期(用于按日期分组)|string(date)||
|&emsp;&emsp;recordEndTime|录制结束时间|string(date-time)||
|&emsp;&emsp;recordStartTime|录制开始时间|string(date-time)||
|&emsp;&emsp;recordStatus|录制状态：recording-录制中，completed-已完成，failed-失败，deleted-已删除|string||
|&emsp;&emsp;recordStatusText||string||
|&emsp;&emsp;resolution|视频分辨率(如1920x1080)|string||
|&emsp;&emsp;thumbnailPath|缩略图路径|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"bitrate": 0,
		"createTime": "",
		"createdBy": "",
		"deleted": 0,
		"deviceId": 0,
		"deviceName": "",
		"duration": 0,
		"errorMessage": "",
		"fileName": "",
		"filePath": "",
		"fileSize": 0,
		"format": "",
		"formattedDuration": "",
		"formattedFileSize": "",
		"frameRate": 0,
		"id": 0,
		"quality": "",
		"qualityText": "",
		"recordDate": "",
		"recordEndTime": "",
		"recordStartTime": "",
		"recordStatus": "",
		"recordStatusText": "",
		"resolution": "",
		"thumbnailPath": "",
		"updateTime": "",
		"updatedBy": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除录制记录


**接口地址**:`/api/video-record/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|录制记录ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 视频文件管理


## 获取视频文件


**接口地址**:`/api/recordings/**`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|filePath|文件路径|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Resource|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|description||string||
|file||file|file|
|filename||string||
|inputStream||InputStream|InputStream|
|open||boolean||
|readable||boolean||
|uri||string(uri)|string(uri)|
|url||string(url)|string(url)|


**响应示例**:
```javascript
{
	"description": "",
	"file": "",
	"filename": "",
	"inputStream": {},
	"open": true,
	"readable": true,
	"uri": "",
	"url": ""
}
```


# 视频汇聚管理


## 创建汇聚配置


**接口地址**:`/api/video-aggregation`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "aggregationName": "",
  "aggregationType": 0,
  "createTime": "",
  "creator": "",
  "deleted": 0,
  "description": "",
  "enabled": 0,
  "id": 0,
  "layout": "",
  "outputBitRate": 0,
  "outputFrameRate": 0,
  "outputResolution": "",
  "outputStreamUrl": "",
  "sourceStreamIds": "",
  "status": 0,
  "switchInterval": 0,
  "switchStrategy": 0,
  "updateTime": "",
  "updater": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|videoAggregation|视频汇聚配置|body|true|VideoAggregation对象|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称||false|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)||false|integer(int32)||
|&emsp;&emsp;createTime|创建时间||false|string(date-time)||
|&emsp;&emsp;creator|创建人||false|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)||false|integer(int32)||
|&emsp;&emsp;description|描述||false|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)||false|integer(int32)||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)||false|string||
|&emsp;&emsp;outputBitRate|输出比特率||false|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率||false|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率||false|string||
|&emsp;&emsp;outputStreamUrl|输出流地址||false|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)||false|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)||false|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)||false|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)||false|integer(int32)||
|&emsp;&emsp;updateTime|更新时间||false|string(date-time)||
|&emsp;&emsp;updater|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«VideoAggregation对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||VideoAggregation对象|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)|integer(int32)||
|&emsp;&emsp;description|描述|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)|string||
|&emsp;&emsp;outputBitRate|输出比特率|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率|string||
|&emsp;&emsp;outputStreamUrl|输出流地址|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)|integer(int32)||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updater|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"aggregationName": "",
		"aggregationType": 0,
		"createTime": "",
		"creator": "",
		"deleted": 0,
		"description": "",
		"enabled": 0,
		"id": 0,
		"layout": "",
		"outputBitRate": 0,
		"outputFrameRate": 0,
		"outputResolution": "",
		"outputStreamUrl": "",
		"sourceStreamIds": "",
		"status": 0,
		"switchInterval": 0,
		"switchStrategy": 0,
		"updateTime": "",
		"updater": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量启动汇聚


**接口地址**:`/api/video-aggregation/batch/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量停止汇聚


**接口地址**:`/api/video-aggregation/batch/stop`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询视频汇聚配置


**接口地址**:`/api/video-aggregation/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|aggregationName|汇聚名称|query|false|string||
|aggregationType|汇聚类型|query|false|integer(int32)||
|current|页码|query|false|integer(int32)||
|size|每页数量|query|false|integer(int32)||
|status|状态|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«VideoAggregation对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«VideoAggregation对象»|IPage«VideoAggregation对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|VideoAggregation对象|
|&emsp;&emsp;&emsp;&emsp;aggregationName|汇聚名称|string||
|&emsp;&emsp;&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)|integer||
|&emsp;&emsp;&emsp;&emsp;createTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)|integer||
|&emsp;&emsp;&emsp;&emsp;description|描述|string||
|&emsp;&emsp;&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)|integer||
|&emsp;&emsp;&emsp;&emsp;id|主键ID|integer||
|&emsp;&emsp;&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)|string||
|&emsp;&emsp;&emsp;&emsp;outputBitRate|输出比特率|integer||
|&emsp;&emsp;&emsp;&emsp;outputFrameRate|输出帧率|integer||
|&emsp;&emsp;&emsp;&emsp;outputResolution|输出分辨率|string||
|&emsp;&emsp;&emsp;&emsp;outputStreamUrl|输出流地址|string||
|&emsp;&emsp;&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)|string||
|&emsp;&emsp;&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)|integer||
|&emsp;&emsp;&emsp;&emsp;switchInterval|切换间隔(秒)|integer||
|&emsp;&emsp;&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)|integer||
|&emsp;&emsp;&emsp;&emsp;updateTime|更新时间|string||
|&emsp;&emsp;&emsp;&emsp;updater|更新人|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"aggregationName": "",
				"aggregationType": 0,
				"createTime": "",
				"creator": "",
				"deleted": 0,
				"description": "",
				"enabled": 0,
				"id": 0,
				"layout": "",
				"outputBitRate": 0,
				"outputFrameRate": 0,
				"outputResolution": "",
				"outputStreamUrl": "",
				"sourceStreamIds": "",
				"status": 0,
				"switchInterval": 0,
				"switchStrategy": 0,
				"updateTime": "",
				"updater": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取运行中的汇聚列表


**接口地址**:`/api/video-aggregation/running`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«VideoAggregation对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)|integer(int32)||
|&emsp;&emsp;description|描述|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)|string||
|&emsp;&emsp;outputBitRate|输出比特率|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率|string||
|&emsp;&emsp;outputStreamUrl|输出流地址|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)|integer(int32)||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updater|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"aggregationName": "",
			"aggregationType": 0,
			"createTime": "",
			"creator": "",
			"deleted": 0,
			"description": "",
			"enabled": 0,
			"id": 0,
			"layout": "",
			"outputBitRate": 0,
			"outputFrameRate": 0,
			"outputResolution": "",
			"outputStreamUrl": "",
			"sourceStreamIds": "",
			"status": 0,
			"switchInterval": 0,
			"switchStrategy": 0,
			"updateTime": "",
			"updater": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取状态统计


**接口地址**:`/api/video-aggregation/statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 验证汇聚配置


**接口地址**:`/api/video-aggregation/validate`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "aggregationName": "",
  "aggregationType": 0,
  "createTime": "",
  "creator": "",
  "deleted": 0,
  "description": "",
  "enabled": 0,
  "id": 0,
  "layout": "",
  "outputBitRate": 0,
  "outputFrameRate": 0,
  "outputResolution": "",
  "outputStreamUrl": "",
  "sourceStreamIds": "",
  "status": 0,
  "switchInterval": 0,
  "switchStrategy": 0,
  "updateTime": "",
  "updater": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|videoAggregation|视频汇聚配置|body|true|VideoAggregation对象|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称||false|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)||false|integer(int32)||
|&emsp;&emsp;createTime|创建时间||false|string(date-time)||
|&emsp;&emsp;creator|创建人||false|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)||false|integer(int32)||
|&emsp;&emsp;description|描述||false|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)||false|integer(int32)||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)||false|string||
|&emsp;&emsp;outputBitRate|输出比特率||false|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率||false|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率||false|string||
|&emsp;&emsp;outputStreamUrl|输出流地址||false|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)||false|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)||false|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)||false|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)||false|integer(int32)||
|&emsp;&emsp;updateTime|更新时间||false|string(date-time)||
|&emsp;&emsp;updater|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据ID查询汇聚配置


**接口地址**:`/api/video-aggregation/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«VideoAggregation对象»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||VideoAggregation对象|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)|integer(int32)||
|&emsp;&emsp;description|描述|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)|string||
|&emsp;&emsp;outputBitRate|输出比特率|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率|string||
|&emsp;&emsp;outputStreamUrl|输出流地址|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)|integer(int32)||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updater|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"aggregationName": "",
		"aggregationType": 0,
		"createTime": "",
		"creator": "",
		"deleted": 0,
		"description": "",
		"enabled": 0,
		"id": 0,
		"layout": "",
		"outputBitRate": 0,
		"outputFrameRate": 0,
		"outputResolution": "",
		"outputStreamUrl": "",
		"sourceStreamIds": "",
		"status": 0,
		"switchInterval": 0,
		"switchStrategy": 0,
		"updateTime": "",
		"updater": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新汇聚配置


**接口地址**:`/api/video-aggregation/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "aggregationName": "",
  "aggregationType": 0,
  "createTime": "",
  "creator": "",
  "deleted": 0,
  "description": "",
  "enabled": 0,
  "id": 0,
  "layout": "",
  "outputBitRate": 0,
  "outputFrameRate": 0,
  "outputResolution": "",
  "outputStreamUrl": "",
  "sourceStreamIds": "",
  "status": 0,
  "switchInterval": 0,
  "switchStrategy": 0,
  "updateTime": "",
  "updater": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||
|videoAggregation|视频汇聚配置|body|true|VideoAggregation对象|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称||false|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)||false|integer(int32)||
|&emsp;&emsp;createTime|创建时间||false|string(date-time)||
|&emsp;&emsp;creator|创建人||false|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)||false|integer(int32)||
|&emsp;&emsp;description|描述||false|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)||false|integer(int32)||
|&emsp;&emsp;id|主键ID||false|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)||false|string||
|&emsp;&emsp;outputBitRate|输出比特率||false|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率||false|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率||false|string||
|&emsp;&emsp;outputStreamUrl|输出流地址||false|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)||false|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)||false|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)||false|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)||false|integer(int32)||
|&emsp;&emsp;updateTime|更新时间||false|string(date-time)||
|&emsp;&emsp;updater|更新人||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«VideoAggregation对象»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||VideoAggregation对象|VideoAggregation对象|
|&emsp;&emsp;aggregationName|汇聚名称|string||
|&emsp;&emsp;aggregationType|汇聚类型(1-画面分割,2-画中画,3-轮播,4-智能切换)|integer(int32)||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除(0-未删除,1-已删除)|integer(int32)||
|&emsp;&emsp;description|描述|string||
|&emsp;&emsp;enabled|是否启用(0-禁用,1-启用)|integer(int32)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;layout|画面布局(1x1,2x2,3x3,4x4,自定义)|string||
|&emsp;&emsp;outputBitRate|输出比特率|integer(int32)||
|&emsp;&emsp;outputFrameRate|输出帧率|integer(int32)||
|&emsp;&emsp;outputResolution|输出分辨率|string||
|&emsp;&emsp;outputStreamUrl|输出流地址|string||
|&emsp;&emsp;sourceStreamIds|源流ID列表(JSON格式)|string||
|&emsp;&emsp;status|状态(0-停止,1-运行,2-异常)|integer(int32)||
|&emsp;&emsp;switchInterval|切换间隔(秒)|integer(int32)||
|&emsp;&emsp;switchStrategy|切换策略(1-手动,2-自动,3-定时)|integer(int32)||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updater|更新人|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"aggregationName": "",
		"aggregationType": 0,
		"createTime": "",
		"creator": "",
		"deleted": 0,
		"description": "",
		"enabled": 0,
		"id": 0,
		"layout": "",
		"outputBitRate": 0,
		"outputFrameRate": 0,
		"outputResolution": "",
		"outputStreamUrl": "",
		"sourceStreamIds": "",
		"status": 0,
		"switchInterval": 0,
		"switchStrategy": 0,
		"updateTime": "",
		"updater": ""
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除汇聚配置


**接口地址**:`/api/video-aggregation/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 重启视频汇聚


**接口地址**:`/api/video-aggregation/{id}/restart`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 启动视频汇聚


**接口地址**:`/api/video-aggregation/{id}/start`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取汇聚状态


**接口地址**:`/api/video-aggregation/{id}/status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 停止视频汇聚


**接口地址**:`/api/video-aggregation/{id}/stop`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 切换画面


**接口地址**:`/api/video-aggregation/{id}/switch`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|汇聚配置ID|path|true|integer(int64)||
|targetStreamId|目标流ID|query|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Void»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 设备标签关联管理


## 获取设备标签的完整信息


**接口地址**:`/api/device-tag/device/{deviceId}/full-info`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 检查设备是否有指定标签


**接口地址**:`/api/device-tag/device/{deviceId}/has-tag/{tagId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||
|tagId|标签ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«boolean»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||boolean||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": true,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备标签的详细信息


**接口地址**:`/api/device-tag/device/{deviceId}/tag-details`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备的标签ID列表


**接口地址**:`/api/device-tag/device/{deviceId}/tag-ids`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«long»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备的所有标签


**接口地址**:`/api/device-tag/device/{deviceId}/tags`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«DeviceTagRelation对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|DeviceTagRelation对象|
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;tagId|标签ID|integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"createTime": "",
			"createdBy": "",
			"deviceId": 0,
			"id": 0,
			"tagId": 0
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 添加设备标签


**接口地址**:`/api/device-tag/device/{deviceId}/tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 设置设备标签


**接口地址**:`/api/device-tag/device/{deviceId}/tags`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 移除设备标签


**接口地址**:`/api/device-tag/device/{deviceId}/tags`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 清除设备的所有标签


**接口地址**:`/api/device-tag/device/{deviceId}/tags/all`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 复制设备标签到其他设备


**接口地址**:`/api/device-tag/device/{sourceDeviceId}/copy-tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sourceDeviceId|源设备ID|path|true|integer(int64)||
|targetDeviceIds|目标设备ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量设置设备标签


**接口地址**:`/api/device-tag/devices/batch-tags`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceTagMap|设备标签映射|body|true|||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据多个标签查询设备（交集）


**接口地址**:`/api/device-tag/devices/by-all-tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«long»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据多个标签查询设备（并集）


**接口地址**:`/api/device-tag/devices/by-any-tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«long»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备标签统计信息


**接口地址**:`/api/device-tag/statistics/device-tags`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标签使用统计


**接口地址**:`/api/device-tag/statistics/tag-usage`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 同步标签使用计数


**接口地址**:`/api/device-tag/sync-usage-count`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取标签的设备数量


**接口地址**:`/api/device-tag/tag/{tagId}/device-count`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagId|标签ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«int»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||integer(int32)|integer(int32)|
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": 0,
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取带有指定标签的设备列表


**接口地址**:`/api/device-tag/tag/{tagId}/devices`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagId|标签ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据标签批量获取设备标签关联信息


**接口地址**:`/api/device-tag/tags/device-relations`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 验证标签ID列表是否有效


**接口地址**:`/api/device-tag/validate-tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 设备管理


## 新增设备信息


**接口地址**:`/api/device`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "address": "",
  "algorithmId": "",
  "brand": "",
  "createTime": "",
  "createdBy": "",
  "creator": "",
  "deleted": 0,
  "description": "",
  "deviceId": "",
  "deviceName": "",
  "deviceType": "",
  "heightPosition": "",
  "id": 0,
  "imagePath": "",
  "ipAddress": "",
  "latitude": "",
  "location": "",
  "longitude": "",
  "manufacturer": "",
  "model": "",
  "password": "",
  "port": 0,
  "position": "",
  "region": "",
  "remark": "",
  "rtspUrl": "",
  "status": "",
  "streamPath": "",
  "streamUrl": "",
  "tag": "",
  "updateTime": "",
  "updatedBy": "",
  "username": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceInfo|设备信息|body|true|DeviceInfo对象|DeviceInfo对象|
|&emsp;&emsp;address|详细地址||false|string||
|&emsp;&emsp;algorithmId|视频流路径||false|string||
|&emsp;&emsp;brand|设备品牌 (海康威视、大华、宇视等)||false|string||
|&emsp;&emsp;createTime|创建时间||false|string(date-time)||
|&emsp;&emsp;createdBy|创建人||false|string||
|&emsp;&emsp;creator|创建人||false|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除||false|integer(int32)||
|&emsp;&emsp;description|设备描述信息||false|string||
|&emsp;&emsp;deviceId|设备编号，唯一标识||false|string||
|&emsp;&emsp;deviceName|设备名称||false|string||
|&emsp;&emsp;deviceType|设备类型 (球机监控、云台、枪机等)||false|string||
|&emsp;&emsp;heightPosition|高度位置(高空/地面/地下/其他)||false|string||
|&emsp;&emsp;id|设备ID，主键||false|integer(int64)||
|&emsp;&emsp;imagePath|设备图像路径||false|string||
|&emsp;&emsp;ipAddress|IP地址 (支持IPv4和IPv6)||false|string||
|&emsp;&emsp;latitude|纬度||false|string||
|&emsp;&emsp;location|位置描述||false|string||
|&emsp;&emsp;longitude|经度||false|string||
|&emsp;&emsp;manufacturer|厂商||false|string||
|&emsp;&emsp;model|设备型号||false|string||
|&emsp;&emsp;password|登录密码||false|string||
|&emsp;&emsp;port|端口号||false|integer(int32)||
|&emsp;&emsp;position|设备位置/安装地点||false|string||
|&emsp;&emsp;region|区划选择(JSON格式)||false|string||
|&emsp;&emsp;remark|备注信息||false|string||
|&emsp;&emsp;rtspUrl|RTSP地址||false|string||
|&emsp;&emsp;status|设备状态||false|string||
|&emsp;&emsp;streamPath|视频流路径||false|string||
|&emsp;&emsp;streamUrl|视频流地址 (RTSP/HTTP等)||false|string||
|&emsp;&emsp;tag|设备标签||false|string||
|&emsp;&emsp;updateTime|更新时间||false|string(date-time)||
|&emsp;&emsp;updatedBy|更新人||false|string||
|&emsp;&emsp;username|登录用户名||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量删除设备信息


**接口地址**:`/api/device/batch`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|设备ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量刷新设备状态


**接口地址**:`/api/device/batch/refresh`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|设备ID列表|body|true|||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据品牌查询设备列表


**接口地址**:`/api/device/brand/{brand}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|brand|设备品牌|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«DeviceInfo对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|DeviceInfo对象|
|&emsp;&emsp;address|详细地址|string||
|&emsp;&emsp;algorithmId|视频流路径|string||
|&emsp;&emsp;brand|设备品牌 (海康威视、大华、宇视等)|string||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|设备描述信息|string||
|&emsp;&emsp;deviceId|设备编号，唯一标识|string||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;deviceType|设备类型 (球机监控、云台、枪机等)|string||
|&emsp;&emsp;heightPosition|高度位置(高空/地面/地下/其他)|string||
|&emsp;&emsp;id|设备ID，主键|integer(int64)||
|&emsp;&emsp;imagePath|设备图像路径|string||
|&emsp;&emsp;ipAddress|IP地址 (支持IPv4和IPv6)|string||
|&emsp;&emsp;latitude|纬度|string||
|&emsp;&emsp;location|位置描述|string||
|&emsp;&emsp;longitude|经度|string||
|&emsp;&emsp;manufacturer|厂商|string||
|&emsp;&emsp;model|设备型号|string||
|&emsp;&emsp;password|登录密码|string||
|&emsp;&emsp;port|端口号|integer(int32)||
|&emsp;&emsp;position|设备位置/安装地点|string||
|&emsp;&emsp;region|区划选择(JSON格式)|string||
|&emsp;&emsp;remark|备注信息|string||
|&emsp;&emsp;rtspUrl|RTSP地址|string||
|&emsp;&emsp;status|设备状态|string||
|&emsp;&emsp;streamPath|视频流路径|string||
|&emsp;&emsp;streamUrl|视频流地址 (RTSP/HTTP等)|string||
|&emsp;&emsp;tag|设备标签|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;username|登录用户名|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"address": "",
			"algorithmId": "",
			"brand": "",
			"createTime": "",
			"createdBy": "",
			"creator": "",
			"deleted": 0,
			"description": "",
			"deviceId": "",
			"deviceName": "",
			"deviceType": "",
			"heightPosition": "",
			"id": 0,
			"imagePath": "",
			"ipAddress": "",
			"latitude": "",
			"location": "",
			"longitude": "",
			"manufacturer": "",
			"model": "",
			"password": "",
			"port": 0,
			"position": "",
			"region": "",
			"remark": "",
			"rtspUrl": "",
			"status": "",
			"streamPath": "",
			"streamUrl": "",
			"tag": "",
			"updateTime": "",
			"updatedBy": "",
			"username": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取所有设备品牌列表


**接口地址**:`/api/device/brands`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«string»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据设备编号查询设备信息


**接口地址**:`/api/device/deviceId/{deviceId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceId|设备编号|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 导出设备列表


**接口地址**:`/api/device/export`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceIds|deviceIds|query|false|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«DeviceInfo对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|DeviceInfo对象|
|&emsp;&emsp;address|详细地址|string||
|&emsp;&emsp;algorithmId|视频流路径|string||
|&emsp;&emsp;brand|设备品牌 (海康威视、大华、宇视等)|string||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|设备描述信息|string||
|&emsp;&emsp;deviceId|设备编号，唯一标识|string||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;deviceType|设备类型 (球机监控、云台、枪机等)|string||
|&emsp;&emsp;heightPosition|高度位置(高空/地面/地下/其他)|string||
|&emsp;&emsp;id|设备ID，主键|integer(int64)||
|&emsp;&emsp;imagePath|设备图像路径|string||
|&emsp;&emsp;ipAddress|IP地址 (支持IPv4和IPv6)|string||
|&emsp;&emsp;latitude|纬度|string||
|&emsp;&emsp;location|位置描述|string||
|&emsp;&emsp;longitude|经度|string||
|&emsp;&emsp;manufacturer|厂商|string||
|&emsp;&emsp;model|设备型号|string||
|&emsp;&emsp;password|登录密码|string||
|&emsp;&emsp;port|端口号|integer(int32)||
|&emsp;&emsp;position|设备位置/安装地点|string||
|&emsp;&emsp;region|区划选择(JSON格式)|string||
|&emsp;&emsp;remark|备注信息|string||
|&emsp;&emsp;rtspUrl|RTSP地址|string||
|&emsp;&emsp;status|设备状态|string||
|&emsp;&emsp;streamPath|视频流路径|string||
|&emsp;&emsp;streamUrl|视频流地址 (RTSP/HTTP等)|string||
|&emsp;&emsp;tag|设备标签|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;username|登录用户名|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"address": "",
			"algorithmId": "",
			"brand": "",
			"createTime": "",
			"createdBy": "",
			"creator": "",
			"deleted": 0,
			"description": "",
			"deviceId": "",
			"deviceName": "",
			"deviceType": "",
			"heightPosition": "",
			"id": 0,
			"imagePath": "",
			"ipAddress": "",
			"latitude": "",
			"location": "",
			"longitude": "",
			"manufacturer": "",
			"model": "",
			"password": "",
			"port": 0,
			"position": "",
			"region": "",
			"remark": "",
			"rtspUrl": "",
			"status": "",
			"streamPath": "",
			"streamUrl": "",
			"tag": "",
			"updateTime": "",
			"updatedBy": "",
			"username": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备分组统计


**接口地址**:`/api/device/group-statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 导入设备列表


**接口地址**:`/api/device/import`


**请求方式**:`POST`


**请求数据类型**:`multipart/form-data`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|file|file|body|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 分页查询设备信息


**接口地址**:`/api/device/page`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|current|当前页|query|false|integer(int64)||
|deviceType|设备类型|query|false|string||
|keyword|搜索关键字|query|false|string||
|size|每页大小|query|false|integer(int64)||
|status|设备状态|query|false|string||
|tag|设备标签|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«IPage«DeviceInfo对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||IPage«DeviceInfo对象»|IPage«DeviceInfo对象»|
|&emsp;&emsp;current||integer(int64)||
|&emsp;&emsp;pages||integer(int64)||
|&emsp;&emsp;records||array|DeviceInfo对象|
|&emsp;&emsp;&emsp;&emsp;address|详细地址|string||
|&emsp;&emsp;&emsp;&emsp;algorithmId|视频流路径|string||
|&emsp;&emsp;&emsp;&emsp;brand|设备品牌 (海康威视、大华、宇视等)|string||
|&emsp;&emsp;&emsp;&emsp;createTime|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer||
|&emsp;&emsp;&emsp;&emsp;description|设备描述信息|string||
|&emsp;&emsp;&emsp;&emsp;deviceId|设备编号，唯一标识|string||
|&emsp;&emsp;&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;&emsp;&emsp;deviceType|设备类型 (球机监控、云台、枪机等)|string||
|&emsp;&emsp;&emsp;&emsp;heightPosition|高度位置(高空/地面/地下/其他)|string||
|&emsp;&emsp;&emsp;&emsp;id|设备ID，主键|integer||
|&emsp;&emsp;&emsp;&emsp;imagePath|设备图像路径|string||
|&emsp;&emsp;&emsp;&emsp;ipAddress|IP地址 (支持IPv4和IPv6)|string||
|&emsp;&emsp;&emsp;&emsp;latitude|纬度|string||
|&emsp;&emsp;&emsp;&emsp;location|位置描述|string||
|&emsp;&emsp;&emsp;&emsp;longitude|经度|string||
|&emsp;&emsp;&emsp;&emsp;manufacturer|厂商|string||
|&emsp;&emsp;&emsp;&emsp;model|设备型号|string||
|&emsp;&emsp;&emsp;&emsp;password|登录密码|string||
|&emsp;&emsp;&emsp;&emsp;port|端口号|integer||
|&emsp;&emsp;&emsp;&emsp;position|设备位置/安装地点|string||
|&emsp;&emsp;&emsp;&emsp;region|区划选择(JSON格式)|string||
|&emsp;&emsp;&emsp;&emsp;remark|备注信息|string||
|&emsp;&emsp;&emsp;&emsp;rtspUrl|RTSP地址|string||
|&emsp;&emsp;&emsp;&emsp;status|设备状态|string||
|&emsp;&emsp;&emsp;&emsp;streamPath|视频流路径|string||
|&emsp;&emsp;&emsp;&emsp;streamUrl|视频流地址 (RTSP/HTTP等)|string||
|&emsp;&emsp;&emsp;&emsp;tag|设备标签|string||
|&emsp;&emsp;&emsp;&emsp;updateTime|更新时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;&emsp;&emsp;username|登录用户名|string||
|&emsp;&emsp;size||integer(int64)||
|&emsp;&emsp;total||integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"current": 0,
		"pages": 0,
		"records": [
			{
				"address": "",
				"algorithmId": "",
				"brand": "",
				"createTime": "",
				"createdBy": "",
				"creator": "",
				"deleted": 0,
				"description": "",
				"deviceId": "",
				"deviceName": "",
				"deviceType": "",
				"heightPosition": "",
				"id": 0,
				"imagePath": "",
				"ipAddress": "",
				"latitude": "",
				"location": "",
				"longitude": "",
				"manufacturer": "",
				"model": "",
				"password": "",
				"port": 0,
				"position": "",
				"region": "",
				"remark": "",
				"rtspUrl": "",
				"status": "",
				"streamPath": "",
				"streamUrl": "",
				"tag": "",
				"updateTime": "",
				"updatedBy": "",
				"username": ""
			}
		],
		"size": 0,
		"total": 0
	},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备统计信息


**接口地址**:`/api/device/statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据状态查询设备列表


**接口地址**:`/api/device/status/{status}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|status|设备状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«DeviceInfo对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|DeviceInfo对象|
|&emsp;&emsp;address|详细地址|string||
|&emsp;&emsp;algorithmId|视频流路径|string||
|&emsp;&emsp;brand|设备品牌 (海康威视、大华、宇视等)|string||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|设备描述信息|string||
|&emsp;&emsp;deviceId|设备编号，唯一标识|string||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;deviceType|设备类型 (球机监控、云台、枪机等)|string||
|&emsp;&emsp;heightPosition|高度位置(高空/地面/地下/其他)|string||
|&emsp;&emsp;id|设备ID，主键|integer(int64)||
|&emsp;&emsp;imagePath|设备图像路径|string||
|&emsp;&emsp;ipAddress|IP地址 (支持IPv4和IPv6)|string||
|&emsp;&emsp;latitude|纬度|string||
|&emsp;&emsp;location|位置描述|string||
|&emsp;&emsp;longitude|经度|string||
|&emsp;&emsp;manufacturer|厂商|string||
|&emsp;&emsp;model|设备型号|string||
|&emsp;&emsp;password|登录密码|string||
|&emsp;&emsp;port|端口号|integer(int32)||
|&emsp;&emsp;position|设备位置/安装地点|string||
|&emsp;&emsp;region|区划选择(JSON格式)|string||
|&emsp;&emsp;remark|备注信息|string||
|&emsp;&emsp;rtspUrl|RTSP地址|string||
|&emsp;&emsp;status|设备状态|string||
|&emsp;&emsp;streamPath|视频流路径|string||
|&emsp;&emsp;streamUrl|视频流地址 (RTSP/HTTP等)|string||
|&emsp;&emsp;tag|设备标签|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;username|登录用户名|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"address": "",
			"algorithmId": "",
			"brand": "",
			"createTime": "",
			"createdBy": "",
			"creator": "",
			"deleted": 0,
			"description": "",
			"deviceId": "",
			"deviceName": "",
			"deviceType": "",
			"heightPosition": "",
			"id": 0,
			"imagePath": "",
			"ipAddress": "",
			"latitude": "",
			"location": "",
			"longitude": "",
			"manufacturer": "",
			"model": "",
			"password": "",
			"port": 0,
			"position": "",
			"region": "",
			"remark": "",
			"rtspUrl": "",
			"status": "",
			"streamPath": "",
			"streamUrl": "",
			"tag": "",
			"updateTime": "",
			"updatedBy": "",
			"username": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 批量更新设备状态


**接口地址**:`/api/device/status/{status}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|设备ID列表|body|true|array|integer|
|status|设备状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取所有设备类型列表


**接口地址**:`/api/device/tags`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«string»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备树形结构


**接口地址**:`/api/device/tree`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«Map«string,object»»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备类型统计


**接口地址**:`/api/device/type-statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据设备类型查询设备列表


**接口地址**:`/api/device/type/{deviceType}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|deviceType|设备类型|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«DeviceInfo对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|DeviceInfo对象|
|&emsp;&emsp;address|详细地址|string||
|&emsp;&emsp;algorithmId|视频流路径|string||
|&emsp;&emsp;brand|设备品牌 (海康威视、大华、宇视等)|string||
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;creator|创建人|string||
|&emsp;&emsp;deleted|是否删除：0-未删除，1-已删除|integer(int32)||
|&emsp;&emsp;description|设备描述信息|string||
|&emsp;&emsp;deviceId|设备编号，唯一标识|string||
|&emsp;&emsp;deviceName|设备名称|string||
|&emsp;&emsp;deviceType|设备类型 (球机监控、云台、枪机等)|string||
|&emsp;&emsp;heightPosition|高度位置(高空/地面/地下/其他)|string||
|&emsp;&emsp;id|设备ID，主键|integer(int64)||
|&emsp;&emsp;imagePath|设备图像路径|string||
|&emsp;&emsp;ipAddress|IP地址 (支持IPv4和IPv6)|string||
|&emsp;&emsp;latitude|纬度|string||
|&emsp;&emsp;location|位置描述|string||
|&emsp;&emsp;longitude|经度|string||
|&emsp;&emsp;manufacturer|厂商|string||
|&emsp;&emsp;model|设备型号|string||
|&emsp;&emsp;password|登录密码|string||
|&emsp;&emsp;port|端口号|integer(int32)||
|&emsp;&emsp;position|设备位置/安装地点|string||
|&emsp;&emsp;region|区划选择(JSON格式)|string||
|&emsp;&emsp;remark|备注信息|string||
|&emsp;&emsp;rtspUrl|RTSP地址|string||
|&emsp;&emsp;status|设备状态|string||
|&emsp;&emsp;streamPath|视频流路径|string||
|&emsp;&emsp;streamUrl|视频流地址 (RTSP/HTTP等)|string||
|&emsp;&emsp;tag|设备标签|string||
|&emsp;&emsp;updateTime|更新时间|string(date-time)||
|&emsp;&emsp;updatedBy|更新人|string||
|&emsp;&emsp;username|登录用户名|string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"address": "",
			"algorithmId": "",
			"brand": "",
			"createTime": "",
			"createdBy": "",
			"creator": "",
			"deleted": 0,
			"description": "",
			"deviceId": "",
			"deviceName": "",
			"deviceType": "",
			"heightPosition": "",
			"id": 0,
			"imagePath": "",
			"ipAddress": "",
			"latitude": "",
			"location": "",
			"longitude": "",
			"manufacturer": "",
			"model": "",
			"password": "",
			"port": 0,
			"position": "",
			"region": "",
			"remark": "",
			"rtspUrl": "",
			"status": "",
			"streamPath": "",
			"streamUrl": "",
			"tag": "",
			"updateTime": "",
			"updatedBy": "",
			"username": ""
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 摄像头算法下发


**接口地址**:`/api/device/{algorithmId}/algorithms`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|algorithmId|算法id|path|true|integer(int64)||
|deviceIds|deviceIds|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 根据ID查询设备信息


**接口地址**:`/api/device/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新设备信息


**接口地址**:`/api/device/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|requestData|requestData|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 删除设备信息


**接口地址**:`/api/device/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备配置


**接口地址**:`/api/device/{id}/config`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新设备配置


**接口地址**:`/api/device/{id}/config`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|config|config|body|true|object||
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## PTZ控制 - 移动


**接口地址**:`/api/device/{id}/ptz/move`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|params|params|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## PTZ控制 - 停止


**接口地址**:`/api/device/{id}/ptz/stop`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## PTZ控制 - 缩放


**接口地址**:`/api/device/{id}/ptz/zoom`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|params|params|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 刷新设备状态


**接口地址**:`/api/device/{id}/refresh`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 更新设备状态


**接口地址**:`/api/device/{id}/status/{status}`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|status|设备状态|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备视频流信息


**接口地址**:`/api/device/{id}/stream`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备标签详细信息


**接口地址**:`/api/device/{id}/tag-details`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 获取设备标签


**接口地址**:`/api/device/{id}/tags`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«List«DeviceTagRelation对象»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||array|DeviceTagRelation对象|
|&emsp;&emsp;createTime|创建时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人|string||
|&emsp;&emsp;deviceId|设备ID|integer(int64)||
|&emsp;&emsp;id|主键ID|integer(int64)||
|&emsp;&emsp;tagId|标签ID|integer(int64)||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": [
		{
			"createTime": "",
			"createdBy": "",
			"deviceId": 0,
			"id": 0,
			"tagId": 0
		}
	],
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 添加设备标签


**接口地址**:`/api/device/{id}/tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 设置设备标签


**接口地址**:`/api/device/{id}/tags`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 移除设备标签


**接口地址**:`/api/device/{id}/tags`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||
|tagIds|标签ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 清除设备的所有标签


**接口地址**:`/api/device/{id}/tags/all`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 测试设备连接


**接口地址**:`/api/device/{id}/test`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|设备ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«Map«string,object»»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## 复制设备标签到其他设备


**接口地址**:`/api/device/{sourceId}/copy-tags`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
[]
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sourceId|源设备ID|path|true|integer(int64)||
|targetDeviceIds|目标设备ID列表|body|true|array|integer|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 远程服务器管理


## add


**接口地址**:`/api/remote-servers`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "condaEnv": "",
  "createdTime": "",
  "id": 0,
  "password": "",
  "serverIp": "",
  "serverName": "",
  "serverPort": 0,
  "status": 0,
  "updatedTime": "",
  "username": "",
  "workDir": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|remoteServer|remoteServer|body|true|RemoteServer|RemoteServer|
|&emsp;&emsp;condaEnv|||false|string||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;serverIp|||false|string||
|&emsp;&emsp;serverName|||false|string||
|&emsp;&emsp;serverPort|||false|integer(int32)||
|&emsp;&emsp;status|||false|integer(int32)||
|&emsp;&emsp;updatedTime|||false|string(date-time)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;workDir|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## edit


**接口地址**:`/api/remote-servers`


**请求方式**:`PUT`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "condaEnv": "",
  "createdTime": "",
  "id": 0,
  "password": "",
  "serverIp": "",
  "serverName": "",
  "serverPort": 0,
  "status": 0,
  "updatedTime": "",
  "username": "",
  "workDir": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|remoteServer|remoteServer|body|true|RemoteServer|RemoteServer|
|&emsp;&emsp;condaEnv|||false|string||
|&emsp;&emsp;createdTime|||false|string(date-time)||
|&emsp;&emsp;id|||false|integer(int64)||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;serverIp|||false|string||
|&emsp;&emsp;serverName|||false|string||
|&emsp;&emsp;serverPort|||false|integer(int32)||
|&emsp;&emsp;status|||false|integer(int32)||
|&emsp;&emsp;updatedTime|||false|string(date-time)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;workDir|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## list


**接口地址**:`/api/remote-servers/list`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|condaEnv||query|false|string||
|createdTime||query|false|string(date-time)||
|id||query|false|integer(int64)||
|password||query|false|string||
|serverIp||query|false|string||
|serverName||query|false|string||
|serverPort||query|false|integer(int32)||
|status||query|false|integer(int32)||
|updatedTime||query|false|string(date-time)||
|username||query|false|string||
|workDir||query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|TableDataInfo|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|msg||string||
|rows||array||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|
|total||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"msg": "",
	"rows": [],
	"success": true,
	"timestamp": 0,
	"total": 0
}
```


## remove


**接口地址**:`/api/remote-servers/{ids}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|ids|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## getInfo


**接口地址**:`/api/remote-servers/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|id|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


## testConnection


**接口地址**:`/api/remote-servers/{id}/test`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|id|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|message||string||
|success||boolean||
|timestamp||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"message": "",
	"success": true,
	"timestamp": 0
}
```


# 鉴权管理


## getTenantId


**接口地址**:`/api/auth/getTenantId`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "phrase": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|TenantRequest|TenantRequest|
|&emsp;&emsp;phrase|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getTenantList


**接口地址**:`/api/auth/getTenantList`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## getUserInfo


**接口地址**:`/api/auth/getUserInfo`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accessToken": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|VerifyTokenRequest|VerifyTokenRequest|
|&emsp;&emsp;accessToken|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## health


**接口地址**:`/api/auth/health`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## login


**接口地址**:`/api/auth/login`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "userInfo": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|LoginRequest|LoginRequest|
|&emsp;&emsp;userInfo|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## logout


**接口地址**:`/api/auth/logout`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accessToken": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|VerifyTokenRequest|VerifyTokenRequest|
|&emsp;&emsp;accessToken|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## verifyAuth


**接口地址**:`/api/auth/verifyAuth`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```


## verifyToken


**接口地址**:`/api/auth/verifyToken`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "accessToken": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|request|request|body|true|VerifyTokenRequest|VerifyTokenRequest|
|&emsp;&emsp;accessToken|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```