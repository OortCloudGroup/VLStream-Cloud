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
  labelWidth: 100,
  menuWidth: 350,
  dialogClickModal: false,
  column: [
    {
      label: '服务名称',
      prop: 'jobServerName',
      type: 'input',
      span: 24,
      search: true,
      rules: [
        {
          required: true,
          message: '请输入服务名称',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '服务器地址',
      prop: 'jobServerUrl',
      type: 'input',
      span: 24,
      rules: [
        {
          required: true,
          message: '请输入服务器地址',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '应用名称',
      prop: 'jobAppName',
      type: 'input',
      span: 24,
      search: true,
      rules: [
        {
          required: true,
          message: '请输入应用名称',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '应用密码',
      prop: 'jobAppPassword',
      type: 'input',
      span: 24,
      rules: [
        {
          required: true,
          message: '请输入应用密码',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '任务备注',
      prop: 'jobRemark',
      type: 'textarea',
      minRows: 3,
      span: 24,
    },
  ],
};
