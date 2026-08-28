/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

function _broadcast(componentName, eventName, params) {
  this.$children.forEach(function(child) {
    let name = child.$options.componentName
    if (name === componentName) {
      // child.$emit.apply(child, [eventName].concat(params));
      if (!!child.emit$) {
        // eslint-disable-next-line no-useless-call
        child.emit$.call(child, eventName, params)
      }
    } else {
      _broadcast.apply(child, [componentName, eventName].concat([params]))
    }
  })
}

export default {
  data() {
    return {
      vfEvents: {}
    }
  },

  methods: {
    emit$(eventName, data) {
      if (this.vfEvents[eventName]) {
        this.vfEvents[eventName].forEach((fn) => {
          fn(data)
        })
      }
    },

    on$(eventName, fn) {
      this.vfEvents[eventName] = this.vfEvents[eventName] || []
      this.vfEvents[eventName].push(fn)
    },

    off$(eventName, fn) {
      if (this.vfEvents[eventName]) {
        if ((fn === undefined) || (fn === null)) {
          this.vfEvents[eventName].length = 0
          return
        }

        for (let i = 0; i < this.vfEvents[eventName].length; i++) {
          if (this.vfEvents[eventName][i] === fn) {
            this.vfEvents[eventName].splice(i, 1)
            break
          }
        }
      }
    },

    dispatch: function dispatch(componentName, eventName, params) {
      let parent = this.$parent || this.$root
      let name = parent.$options.componentName

      while (parent && (!name || name !== componentName)) {
        parent = parent.$parent

        if (parent) {
          name = parent.$options.componentName
        }
      }
      if (parent) {
        if (!!parent.emit$) {
          // eslint-disable-next-line no-useless-call
          parent.emit$.call(parent, eventName, params)

          if (componentName === 'VFormRender') {
            parent.$emit(eventName, ...params) // Execute $emit, @ eventProcess ! !
          }
        }
      }
    },

    broadcast: function broadcast(componentName, eventName, params) {
      /* Vue3 $childrenproperty, _broadcast method already can ! ! */
      // _broadcast.call(this, componentName, eventName, params);

      if (!!this.widgetRefList) { // FormRenderonly widgetRefListproperty
        Object.keys(this.widgetRefList).forEach(refName => {
          let cmpName = this.widgetRefList[refName].$options.componentName
          if (cmpName === componentName) {
            let foundRef = this.widgetRefList[refName]
            // eslint-disable-next-line no-useless-call
            foundRef.emit$.call(foundRef, eventName, params)
          }
        })
      }

      if (!!this.refList) { // component inject refListproperty
        Object.keys(this.refList).forEach(refName => {
          let cmpName = this.refList[refName].$options.componentName
          if (cmpName === componentName) {
            let foundRef = this.refList[refName]
            // eslint-disable-next-line no-useless-call
            foundRef.emit$.call(foundRef, eventName, params)
          }
        })
      }
    }
  }
}
