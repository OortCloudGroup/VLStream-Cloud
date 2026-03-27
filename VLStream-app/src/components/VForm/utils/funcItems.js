import { getDateString } from './index'
export const funcItems = [
  {
    label: '函数公式',
    id: '0',
    children: [
      {
        label: '数字处理',
        desc: ' 常规的数学运算公式，加减乘除',
        demo: 'SUM(1,2,3) = 6, SUM([5, 6, 7]) = 18',
        children: [
          {
            label: 'ADD',
            id: '1',
            type: 'opr',
            desc: ' 获取两个数字相加的值',
            demo: 'ADD(1,2) = 3'
          },
          {
            label: 'SUB',
            id: '2',
            type: 'opr',
            desc: ' 获取两个数字相减的值',
            demo: 'SUB(10, 1) = 9'
          },
          {
            label: 'MUL',
            id: '3',
            type: 'opr',
            desc: '获取两个数字相乘的值',
            demo: 'SUB(1,2) = 2'
          },
          {
            label: 'DIV',
            id: '4',
            type: 'opr',
            desc: '获取两个数字相除的值',
            demo: 'SUB(6,3) = 2'
          },
          {
            label: 'SUM',
            id: '5',
            type: 'opr',
            desc: '获取合集中数值的总和',
            demo: 'SUB(1,2,3) = 6， SUB(10, 1) = 9'
          },
          {
            label: 'MAX',
            id: '6',
            type: 'opr',
            desc: '获取合集中数值的最大值',
            demo: 'MAX(1,2,3) = 3， MAX(10, 1) = 10'
          },
          {
            label: 'MIN',
            id: '7',
            type: 'opr',
            desc: '获取合集中数值的最小值',
            demo: 'MIN(1,2,3) = 1'
          },
          {
            label: 'ABS',
            id: '8',
            type: 'opr',
            desc: '获取数值的绝对值',
            demo: 'ABS(-1) = 1， ABS(1) = 1'
          },
          {
            label: 'AVG',
            id: '9',
            type: 'opr',
            desc: '获取合集中数值的平均值',
            demo: 'AVG(1,2,3) = 2'
          },
          {
            label: 'MOD',
            id: '10',
            type: 'opr',
            desc: '取余数',
            demo: 'MOD(5,2) = 1， MOD(5,3) = 2'
          },
          {
            label: 'POW',
            id: '11',
            type: 'opr',
            desc: '求指定数的指定次幂',
            demo: 'POW(2,3) = 8， POW(2,4) = 16'
          },
          {
            label: 'RAND',
            id: '12',
            type: 'opr',
            desc: '获取一个大于等于0且小于1的随机数',
            demo: 'RAND()'
          },
          {
            label: 'SQRT',
            id: '13',
            type: 'opr',
            desc: '求指定数的平方根',
            demo: 'SQRT(9) = 3'
          }
        ]
      },
      {
        label: '时间处理',
        children: [
          {
            label: 'NOW',
            id: '1',
            type: 'opr',
            desc: '获取当前时间',
            demo: 'NOW() = "2024-03-15 12:08:31"'
          },
          {
            label: 'TODAY',
            id: '2',
            type: 'opr',
            desc: '获取今天的日期',
            demo: 'TODAY() = "2024-03-15"'
          },
          {
            label: 'COUNT',
            id: '3',
            type: 'opr',
            desc: '计算天数',
            demo: 'COUNT(开始日期,结束日期) = 2,COUNT("2024-03-15","2024-03-16") = 2, 还有第三个参数单位 day, month, year： COUNT(开始日期,结束日期,单位) = 2'
          },
          {
            label: 'COUNT2',
            id: '4',
            type: 'opr',
            desc: '计算天数2',
            demo: 'COUNT(日期范围) = 2， COUNT(["2024-03-15","2024-03-16"]) = 2, 还有第三个参数单位 day, month, year： COUNT(日期范围,单位) = 2'
          }
        ]
      }
    ]
  }
]

export const calcRules = (rules, widgetList) => {
  //    操作的替换
  funcItems[0].children.forEach(itt => {
    let hsgs = itt
    hsgs.children.forEach(item => {
      // rules字符串的  /\[\[(.+?)\]\]/g， 匹配并替换
      let reg = new RegExp(`\\[\\[${item.id}.${item.label}\\]\\]`, 'g')
      rules = rules.replace(reg, item.label)
    })
  })
  // 表单的替换
  widgetList.forEach(item => {
    // rules字符串的  /\[\[(.+?)\]\]/g， 匹配并替换
    let reg = new RegExp(`\\[\\[${item.id}.${item.options.label}\\]\\]`, 'g')
    // 先简单的替换为浮点型数字，后面再优化
    // 处理数值还是 时间 还是字符串
    if (item.type === 'm-date') {
      rules = rules.replace(reg, '"' + item.options.defaultValue + '"')
    }
    if (item.type === 'm-date-range') {
      rules = rules.replace(reg, '"' + item.options.defaultValue + '"')
    }
    if (item.type.startsWith('m-number') || item.type.startsWith('m-text') || item.type.startsWith('m-money')) {
      let replaceNum = parseFloat(item.options.defaultValue)
      rules = rules.replace(reg, isNaN(replaceNum) ? 0 : replaceNum)
    }
  })
  return evalRULES(rules)
}

const evalRULES = (rules) => {
  window.ADD = (a, b) => {
    return a + b
  }
  window.SUB = (a, b) => {
    return a - b
  }
  window.MUL = (a, b) => {
    return a * b
  }
  window.DIV = (a, b) => {
    return a / b
  }
  window.SUM = (...args) => {
    return args.reduce((a, b) => a + b)
  }
  window.MAX = (...args) => {
    return Math.max(...args)
  }
  window.MIN = (...args) => {
    return Math.min(...args)
  }
  window.ABS = (a) => {
    return Math.abs(a)
  }
  window.AVG = (...args) => {
    return args.reduce((a, b) => a + b) / args.length
  }
  window.MOD = (a, b) => {
    return a % b
  }
  window.POW = (a, b) => {
    return Math.pow(a, b)
  }
  window.RAND = () => {
    return Math.random(0, 1)
  }
  window.SQRT = (a) => {
    return Math.sqrt(a)
  }

  window.NOW = () => {
    return getDateString(new Date(), 'yyyy-MM-dd hh:mm:ss')
  }

  window.TODAY = () => {
    return getDateString(new Date(), 'yyyy-MM-dd')
  }
  window.COUNT = (a, b, unit = 'day') => {
    if (!a || !b || b === 'null' || a === 'null') return 0
    // 计算a, b 两个日期的差值 默认以天为单位
    let date1 = new Date(a).getTime()
    let date2 = new Date(b).getTime()
    let diff = date2 - date1
    if (unit === 'day') {
      return diff / (1000 * 60 * 60 * 24)
    } else if (unit === 'month') {
      return diff / (1000 * 60 * 60 * 24 * 30)
    } else if (unit === 'year') {
      return diff / (1000 * 60 * 60 * 24 * 365)
    }
  }

  window.COUNT2 = (a, unit = 'day') => {
    if (!a || a === 'null') return 0

    let arr = a.split(',')
    let date1 = new Date(arr[0]).getTime()
    let date2 = new Date(arr[1]).getTime()
    let diff = date2 - date1
    if (unit === 'day') {
      return diff / (1000 * 60 * 60 * 24)
    } else if (unit === 'month') {
      return diff / (1000 * 60 * 60 * 24 * 30)
    } else if (unit === 'year') {
      return diff / (1000 * 60 * 60 * 24 * 365)
    }
  }

  // TODO 暂时先用eval来优化解析rules
  try {
    // eslint-disable-next-line no-eval
    return eval(rules)
  } catch (error) {
    console.log('calc-method:', error)
    return '公式配置错误'
  }

//   return rules
}

