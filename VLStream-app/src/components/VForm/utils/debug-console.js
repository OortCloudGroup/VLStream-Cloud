console.log = (function(logFunc) {
  return function() {
    if (process.env.NODE_ENV === 'development') {
      // eslint-disable-next-line prefer-rest-params
      logFunc.call(console, ...arguments)
    }
  }
})(console.log)
