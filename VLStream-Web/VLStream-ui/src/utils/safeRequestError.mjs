// Axios errors carry credentials in config, request, response and sometimes message.
// Only fixed diagnostic metadata is allowed in authentication/request logs.
export function logSafeRequestError(label, error, logger = console.error) {
  const status = Number(error?.response?.status)
  const codes = ['ERR_CANCELED', 'ECONNABORTED', 'ETIMEDOUT', 'ERR_NETWORK', 'ERR_BAD_REQUEST', 'ERR_BAD_RESPONSE']
  logger(label, {
    status: Number.isInteger(status) && status >= 100 && status <= 599 ? status : 'network',
    kind: codes.includes(error?.code) ? error.code : 'request'
  })
}
