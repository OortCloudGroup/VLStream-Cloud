export const timeExpressionTypeDic = [
  {
    label: 'API',
    value: 1,
  },
  {
    label: 'CRON',
    value: 2,
  },
  {
    label: '固定频率(毫秒)',
    value: 3,
  },
  {
    label: '固定延迟(毫秒)',
    value: 4,
  },
  {
    label: '工作流',
    value: 5,
  },
];

export const executeTypeDic = [
  {
    label: '单机执行',
    value: 1,
  },
  {
    label: '广播执行',
    value: 2,
  },
  {
    label: 'MapReduce',
    value: 3,
  },
];

export const processorTypeDic = [
  {
    label: '内建处理器',
    value: 1,
  },
  {
    label: '外部处理器（动态加载）',
    value: 4,
  },
];

export const dispatchStrategyDic = [
  {
    label: 'HEALTH_FIRST',
    value: 1,
  },
  {
    label: 'RANDOM',
    value: 2,
  },
];

export const enableDic = [
  {
    label: '暂停',
    value: 0,
  },
  {
    label: '启用',
    value: 1,
  },
];

export const logTypeDic = [
  {
    label: 'ONLINE',
    value: 1,
  },
  {
    label: 'LOCAL',
    value: 2,
  },
  {
    label: 'STDOUT',
    value: 3,
  },
  {
    label: 'LOCAL_AND_ONLINE',
    value: 4,
  },
  {
    label: 'NULL',
    value: 999,
  },
];

export const logLevelDic = [
  {
    label: 'DEBUG',
    value: 1,
  },
  {
    label: 'INFO',
    value: 2,
  },
  {
    label: 'WARN',
    value: 3,
  },
  {
    label: 'ERROR',
    value: 4,
  },
  {
    label: 'OFF',
    value: 99,
  },
];

export default {
  height: 'auto',
  calcHeight: 30,
  tip: false,
  searchShow: true,
  searchMenuSpan: 6,
  border: true,
  index: true,
  viewBtn: true,
  selection: true,
  grid: true,
  labelWidth: 180,
  menuWidth: 350,
  dialogWidth: 1200,
  dialogClickModal: false,
  tabs: true,
  column: [
    {
      label: '任务应用',
      prop: 'jobServerId',
      type: 'select',
      dicUrl: '/blade-job/job-server/select',
      props: {
        label: 'jobAppName',
        value: 'id',
      },
      search: true,
      display: false,
    },
    {
      label: '任务ID',
      prop: 'jobId',
      type: 'input',
      search: true,
      width: 80,
      display: false,
    },
    {
      label: '任务名称',
      prop: 'jobName',
      type: 'input',
      search: true,
      width: 200,
      display: false,
    },
    {
      label: '定时信息',
      labelTip: '时间表达式类型',
      prop: 'timeExpressionType',
      type: 'select',
      dicData: timeExpressionTypeDic,
      rules: [
        {
          required: true,
          message: '请选择定时信息',
          trigger: 'blur',
        },
      ],
      width: 120,
      display: false,
    },
    {
      label: '执行类型',
      labelTip: '枚举值',
      prop: 'executeType',
      type: 'select',
      dicData: executeTypeDic,
      rules: [
        {
          required: true,
          message: '请选择执行器类型',
          trigger: 'blur',
        },
      ],
      width: 110,
      display: false,
    },
    {
      label: '处理器类型',
      labelTip: '枚举值',
      prop: 'processorType',
      type: 'select',
      dicData: processorTypeDic,
      rules: [
        {
          required: true,
          message: '请选择处理器类型',
          trigger: 'blur',
        },
      ],
      width: 180,
      display: false,
    },
    {
      label: '任务状态',
      labelTip: '未启用的任务不会被调度',
      prop: 'enable',
      type: 'switch',
      dicData: enableDic,
      slot: true,
      width: 100,
      display: false,
    },
  ],
  group: [
    {
      label: '基础配置',
      prop: 'modelSetting',
      icon: 'el-icon-tickets',
      column: [
        {
          label: '任务应用',
          prop: 'jobServerId',
          type: 'select',
          dicUrl: '/blade-job/job-server/select',
          props: {
            label: 'jobAppName',
            value: 'id',
          },
          editDisabled: true,
          rules: [
            {
              required: true,
              message: '请选择任务应用',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '任务状态',
          labelTip: '未启用的任务不会被调度',
          prop: 'enable',
          type: 'switch',
          value: 1,
          rules: [
            {
              required: true,
              message: '请选择是否开启',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '任务名称',
          prop: 'jobName',
          type: 'input',
          rules: [
            {
              required: true,
              message: '请输入任务名称',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '生命周期',
          labelTip: '预留，用于指定定时调度任务的生效时间范围)',
          prop: 'lifecycle',
          type: 'datetimerange',
          format: 'YYYY-MM-DD HH:mm:ss',
          valueFormat: 'YYYY-MM-DD HH:mm:ss',
          startPlaceholder: '任务开始时间',
          endPlaceholder: '任务结束时间',
        },
        {
          label: '定时类型',
          labelTip: '时间表达式类型',
          prop: 'timeExpressionType',
          type: 'select',
          dicData: timeExpressionTypeDic,
          value: 2,
          rules: [
            {
              required: true,
              message: '请选择定时信息',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '时间表达式',
          labelTip: '填写类型由 定时类型 决定，比如 CRON 需要填写 CRON 表达式',
          prop: 'timeExpression',
          type: 'input',
        },
        {
          label: '执行类型',
          labelTip: '枚举值',
          prop: 'executeType',
          type: 'select',
          dicData: executeTypeDic,
          value: 1,
          rules: [
            {
              required: true,
              message: '请选择执行器类型',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '运行时配置',
          labelTip: '目前支持随机(RANDOM)和健康度优先(HEALTH_FIRST)',
          prop: 'dispatchStrategy',
          type: 'select',
          dicData: dispatchStrategyDic,
          value: 1,
          rules: [
            {
              required: true,
              message: '请选择运行时配置',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '处理器类型',
          labelTip: '枚举值',
          prop: 'processorType',
          type: 'select',
          dicData: processorTypeDic,
          value: 1,
          rules: [
            {
              required: true,
              message: '请选择处理器类型',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '处理器参数',
          labelTip:
            '如Java处理器需要填写全限定类名，如:org.springblade.demo.MapReduceProcessorDemo',
          prop: 'processorInfo',
          type: 'input',
          rules: [
            {
              required: true,
              message: '请输入处理器参数',
              trigger: 'blur',
            },
          ],
        },
        {
          label: '任务参数',
          labelTip: '方法入参 TaskContext 对象的 json 字段',
          prop: 'jobParams',
          type: 'input',
          span: 24,
        },
        {
          label: '任务描述',
          prop: 'jobDescription',
          type: 'textarea',
          minRows: 3,
          span: 24,
        },
      ],
    },
    {
      label: '高级配置',
      prop: 'templateSetting',
      icon: 'el-icon-copy-document',
      column: [
        {
          label: '最大实例数',
          labelTip:
            '该任务同时执行的数量（任务和实例就像是类和对象的关系，任务被调度执行后被称为实例）',
          prop: 'maxInstanceNum',
          type: 'number',
          value: 0,
        },
        {
          label: '单机线程并发数',
          labelTip: '该实例执行过程中每个 Worker 使用的线程数量',
          prop: 'concurrency',
          type: 'number',
          value: 5,
        },
        {
          label: '任务实例运行时间限制',
          labelTip: '0 代表无任何限制，超时会被打断并判定为执行失败',
          prop: 'instanceTimeLimit',
          type: 'number',
          value: 0,
        },
        {
          label: '任务实例重试次数',
          labelTip: '任务实例重试次数，整个任务失败时重试，代价大，不推荐使用',
          prop: 'instanceRetryNum',
          type: 'number',
          value: 0,
        },
        {
          label: '任务作业重试次数',
          labelTip: 'Task 重试次数，每个子 Task 失败后单独重试，代价小，推荐使用',
          prop: 'taskRetryNum',
          type: 'number',
          value: 0,
        },
        {
          label: '最低CPU核心',
          labelTip:
            '最小可用 CPU 核心数，CPU 可用核心数小于该值的 Worker 将不会执行该任务，0 代表无任何限制',
          prop: 'minCpuCores',
          type: 'number',
          value: 0,
        },
        {
          label: '最低内存(GB)',
          labelTip: '可用内存小于该值的Worker 将不会执行该任务，0 代表无任何限制',
          prop: 'minMemorySpace',
          type: 'number',
          value: 0,
        },
        {
          label: '最低磁盘空间(GB)',
          labelTip: '可用磁盘空间小于该值的Worker 将不会执行该任务，0 代表无任何限制',
          prop: 'minDiskSpace',
          type: 'number',
          value: 0,
        },
        {
          label: '执行机器地址',
          labelTip: '设置该参数后只有列表中的机器允许执行该任务，空代表不指定机器',
          prop: 'designatedWorkers',
          type: 'input',
        },
        {
          label: '最大执行机器数量',
          labelTip: '限定调动执行的机器数量，0代表无限制',
          prop: 'maxWorkerCount',
          type: 'number',
          value: 0,
        },
      ],
    },
    {
      label: '其他配置',
      prop: 'codingSetting',
      icon: 'el-icon-printer',
      column: [
        {
          label: '报警人员ID列表',
          labelTip: '接收报警的用户ID列表',
          prop: 'notifyUserIds',
          type: 'input',
        },
        {
          label: '错误阈值',
          labelTip: '错误阈值，0代表不限制',
          prop: 'alertThreshold',
          type: 'number',
          value: 0,
        },
        {
          label: '统计窗口(s)',
          labelTip: '统计的窗口长度(s)，0代表不限制',
          prop: 'statisticWindowLen',
          type: 'number',
          value: 0,
        },
        {
          label: '沉默窗口(s)',
          labelTip: '沉默时间窗口(s)，0代表不限制',
          prop: 'silenceWindowLen',
          type: 'number',
          value: 0,
        },
        {
          label: '日志配置',
          labelTip: '日志配置',
          prop: 'logType',
          type: 'select',
          value: 1,
          dicData: logTypeDic,
        },
        {
          label: '日志级别',
          labelTip: '日志级别',
          prop: 'logLevel',
          type: 'select',
          value: 2,
          dicData: logLevelDic,
        },
        {
          label: '扩展字段',
          labelTip: '供开发者使用，用于功能扩展，powerjob 自身不会使用该字段',
          prop: 'extra',
          type: 'textarea',
          minRows: 3,
          span: 24,
        },
      ],
    },
  ],
};
