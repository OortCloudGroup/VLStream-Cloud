/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// import { vfApp } from '~@/utils/create-app'

export function addDirective(app) {
  /**
   * : v-drag="[dragDom, dragHeader]", `<div v-drag="['.drag-container .el-dialog', '.drag-container .el-dialog__header']"></div>`
   */

  app.directive('drag', {
    mounted(el, binding) {
      if (!binding.value) return false

      binding.instance.$nextTick(() => {
        const dragDom = document.querySelector(binding.value[0])
        const dragHeader = document.querySelector(binding.value[1])

        dragHeader.onmouseover = () => (dragHeader.style.cursor = 'move')

        function down(e, type) {
          // , current element
          const disX = type === 'pc' ? e.clientX - dragHeader.offsetLeft : e.touches[0].clientX - dragHeader.offsetLeft
          const disY = type === 'pc' ? e.clientY - dragHeader.offsetTop : e.touches[0].clientY - dragHeader.offsetTop

          // bodycurrent
          const screenWidth = document.body.clientWidth
          // ( to body , method Get )
          const screenHeight = document.documentElement.clientHeight

          //
          const dragDomWidth = dragDom.offsetWidth
          //
          const dragDomheight = dragDom.offsetHeight

          const minDragDomLeft = dragDom.offsetLeft
          const maxDragDomLeft = screenWidth - dragDom.offsetLeft - dragDomWidth

          const minDragDomTop = dragDom.offsetTop
          const maxDragDomTop = screenHeight - dragDom.offsetTop - dragDomheight

          // Get value px Replace
          let styL = getComputedStyle(dragDom).left
          let styT = getComputedStyle(dragDom).top

          // in ie in Get value to component 50% after value to px
          if (styL.includes('%')) {
            styL = +document.body.clientWidth * (+styL.replace(/%/g, '') / 100)
            styT = +document.body.clientHeight * (+styT.replace(/%/g, '') / 100)
          } else {
            styL = +styL.replace(/\px/g, '')
            styT = +styT.replace(/\px/g, '')
          }

          return {
            disX,
            disY,
            minDragDomLeft,
            maxDragDomLeft,
            minDragDomTop,
            maxDragDomTop,
            styL,
            styT
          }
        }

        function move(e, type, obj) {
          let { disX, disY, minDragDomLeft, maxDragDomLeft, minDragDomTop, maxDragDomTop, styL, styT } = obj

          // event ,
          let left = type === 'pc' ? e.clientX - disX : e.touches[0].clientX - disX
          let top = type === 'pc' ? e.clientY - disY : e.touches[0].clientY - disY

          // Process
          if (-left > minDragDomLeft) {
            left = -minDragDomLeft
          } else if (left > maxDragDomLeft) {
            left = maxDragDomLeft
          }

          if (-top > minDragDomTop) {
            top = -minDragDomTop
          } else if (top > maxDragDomTop) {
            top = maxDragDomTop
          }

          // current element
          dragDom.style.cssText += `;left:${left + styL}px;top:${top + styT}px;`
        }

        /**
         * pc
         * onmousedown event
         * onmousemove event
         * onmouseup event
         */
        dragHeader.onmousedown = (e) => {
          const obj = down(e, 'pc')
          document.onmousemove = (e) => {
            move(e, 'pc', obj)
          }
          document.onmouseup = () => {
            document.onmousemove = null
            document.onmouseup = null
          }
        }

        /**
         *
         * ontouchstart , ontouchstart
         * ontouchmove , ontouchmove
         * ontouchend , ontouchend
         */
        dragHeader.ontouchstart = (e) => {
          const obj = down(e, 'app')
          document.ontouchmove = (e) => {
            move(e, 'app', obj)
          }
          document.ontouchend = () => {
            document.ontouchmove = null
            document.ontouchend = null
          }
        }
      })
    }
  })

  // v-dialogDragWidth: dialog
  app.directive('dialogDragWidth', {
    mounted(el, binding) {
      binding.instance.$nextTick(() => {
        const dragDom = binding.value.$el.querySelector('.el-dialog')
        el.onmousedown = (e) => {
          // , current element
          const disX = e.clientX - el.offsetLeft

          document.onmousemove = function(e) {
            e.preventDefault() // event

            // event ,
            const l = e.clientX - disX
            dragDom.style.width = `${l}px`
          }

          document.onmouseup = function() {
            document.onmousemove = null
            document.onmouseup = null
          }
        }
      })
    }
  })
}

