export const generateCode = function(formJson, codeType = 'vue') {
  let formJsonStr = JSON.stringify(formJson)

  if (codeType === 'html') {
    return `<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no" />
	<title>VmForm Demo</title>
  <!-- <link rel="stylesheet" href="//unpkg.com/element-plus/dist/index.css"> -->
	<link rel="stylesheet" href="https://vmform3.ks3-cn-beijing.ksyuncs.com/vmform3/designer.style.css?t=20220129">
	<link rel="stylesheet" href="https://fastly.jsdelivr.net/npm/vant@4/lib/index.css">
	<style type="text/css">
	</style>
</head>
<body>

  <div id="app">
    <!-- <vm-form-designer></vm-form-designer> -->
	  <vm-form-render :form-json="formJson" :form-data="formData" :option-data="optionData" ref="vmFormRef">
    </vm-form-render>
	  <van-button type="primary" @click="submitForm">Submit</van-button>
  </div>

<script src="https://lf26-cdn-tos.bytecdntp.com/cdn/expire-1-M/vue/3.2.30/vue.global.min.js"></script>
<script src="https://fastly.jsdelivr.net/npm/vant@4/lib/vant.min.js"></script>
<!-- <script src="//unpkg.com/element-plus"></script> -->
<script src="https://vmform3.ks3-cn-beijing.ksyuncs.com/designer.umd.js?t=20220129"></script>
<script>
  const { createApp } = Vue;
	const app = createApp({
      data() {
        return {
          formJson: ${formJsonStr},
          formData: {},
          optionData: {}
        }
      },
      methods: {
        submitForm() {
          this.$refs.vmFormRef.getFormData().then( (formData) => {
            // Form Validation OK
            alert( JSON.stringify(formData) )
          }).catch( function(error) {
            // Form Validation Failed
            alert(error)
          })
        }
      }
	});
	app.use(vant)

  // 使用设计器需要引用element-plus，渲染器不需要
  //app.use(ElementPlus) 
	app.use(VmFormDesigner)
	app.mount("#app");
</script>
</body>
</html>`
  } else {
    return `<template>
  <div>
    <!-- <vm-form-designer></vm-form-designer> -->
    <vm-form-render :form-json="formJson" :form-data="formData" :option-data="optionData" ref="vmFormRef">
    </vm-form-render>
    <van-button type="primary" @click="submitForm">Submit</-button>
  </div>
</template>

<script setup>
  import { ref, reactive } from 'vue'
  const formJson = reactive(${formJsonStr})
  const formData = reactive({})
  const optionData = reactive({})
  const vmFormRef = ref(null)

  const submitForm = () => {
    vmFormRef.value.getFormData().then(formData => {
      // Form Validation OK
      alert( JSON.stringify(formData) )
    }).catch(error => {
      // Form Validation failed
    })
  }
</script>`
  }
}
