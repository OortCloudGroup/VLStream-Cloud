const deviceId = value => {
    const id = String(value ?? '')
    if (!/^[1-9]\d*$/.test(id)) throw new Error('Invalid ONVIF device ID')
    return id
}
export function createOnvifDevicePresetApi(request) {
    const path = id => `/onvif/service/device/${deviceId(id)}/presets`
    return {
        list:id => request({url:path(id),method:'get',silentError:true}),
        create:(id,name) => request({url:path(id),method:'post',data:{name},silentError:true}),
        goTo:(id,token) => request({url:path(id) + '/goto',method:'post',data:{token},silentError:true}),
        remove:(id,token) => request({url:path(id),method:'delete',data:{token},silentError:true}),
    }
}

export function createOnvifPresetController({api, onChange = () => {}, beforeAction = () => {}}) {
    let revision = 0, requestRevision = 0, context = null, writing = false
    let state = {rows:[],ready:false,loading:false,busy:false,error:''}
    const publish = values => { state = {...state,...values}; onChange(state) }
    const current = (version,id) => version === revision && context?.id === id
    const errorText = (error, write = false) => {
        const status = Number(error?.response?.data?.code || error?.data?.code || error?.response?.status || error?.status || error?.code)
        if (status === 404) return '预置点服务尚未部署'
        if (status === 401 || status === 403) return '没有预置点操作权限'
        return write ? '操作结果未确认，请刷新列表后检查' : '读取预置点失败，请重试'
    }
    async function load() {
        if (!context?.canRead) return
        const id=context.id, version=revision, read=++requestRevision
        publish({loading:true,error:''})
        try {
            const response=await api.list(id)
            if (!current(version,id) || read!==requestRevision) return
            if (!Array.isArray(response?.data)) throw Error('Invalid preset response')
            const rows=response.data.filter(row=>typeof row?.token==='string' && row.token.trim() && row.token.length<=256)
                .map(row=>({token:row.token,name:typeof row.name==='string'?row.name:row.token}))
            publish({rows,ready:true})
        } catch(error) {
            if (current(version,id) && read===requestRevision) publish({rows:[],ready:false,error:errorText(error)})
        } finally { if (current(version,id) && read===requestRevision) publish({loading:false}) }
    }
    function open(value) {
        revision++; requestRevision++
        context = value ? {...value,id:deviceId(value.id)} : null
        publish({rows:[],ready:false,loading:false,busy:writing,error:''})
        return load()
    }
    async function action(kind,value) {
        if (!['create','goTo','remove'].includes(kind) || !context?.[kind] || !state.ready || writing) return false
        if (typeof value!=='string' || !value.trim() || value.length>(kind==='create'?128:256)) return false
        if (kind!=='create' && !state.rows.some(row=>row.token===value)) return false
        const id=context.id, version=revision
        writing=true; publish({busy:true,error:''})
        let errorMessage='', success=false
        try {
            await beforeAction()
            if (!current(version,id)) return false
            await api[kind](id,value)
            success=true
        } catch(error) { errorMessage=errorText(error,true) }
        finally {
            writing=false; publish({busy:false})
            if (current(version,id)) {
                await load()
                if(errorMessage) publish({error:errorMessage})
            }
        }
        return success && current(version,id)
    }
    return {open,load,action,close:()=>open(null),snapshot:()=>state}
}
