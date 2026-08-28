/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// import { vfApp } from '~@/utils/create-app'

// import {
//   addContainerWidgetSchema
// } from '~@/components/form-designer/widget-panel/widgetsConfig'
// import * as PERegister from '~@/components/form-designer/setting-panel/propertyRegister'
// import * as PEFactory from '~@/components/form-designer/setting-panel/property-editor-factory.jsx'

// import { cardSchema } from '~@/extension/samples/extension-schema'
// import CardWidget from '~@/extension/samples/card/card-widget'
// import CardItem from '~@/extension/samples/card/card-item'
// import { registerCWGenerator } from '~@/utils/sfc-generator'
// import { cardTemplateGenerator } from '~@/extension/samples/extension-sfc-generator'

// // import { alertSchema } from '~@/extension/samples/extension-schema'
// import AlertWidget from '~@/extension/samples/alert/alert-widget'
// import { registerFWGenerator } from '~@/utils/sfc-generator'
// import { alertTemplateGenerator } from '~@/extension/samples/extension-sfc-generator'

export const loadExtension = function() {
  // /**
  // * Load component :
  // * 1. Load componentJson Schema;
  // * 2. full component, component —— and , need to component;
  // * 3. full property component ( property、 property、eventproperty);
  // * 4. component Generate ;
  // * 5. Load .
  //  */
  // addContainerWidgetSchema(cardSchema) // Load componentJson Schema
  // /* -------------------------------------------------- */
  // app.component(CardWidget.name, CardWidget) // component
  // app.component(CardItem.name, CardItem) // component
  // /* -------------------------------------------------- */
  // PERegister.registerCPEditor(app, 'card-folded', 'card-folded-editor',
  //   PEFactory.createBooleanEditor('folded', 'extension.setting.cardFolded'))

  // PERegister.registerCPEditor(app, 'card-showFold', 'card-showFold-editor',
  //   PEFactory.createBooleanEditor('showFold', 'extension.setting.cardShowFold'))

  // PERegister.registerCPEditor(app, 'card-cardWidth', 'card-cardWidth-editor',
  //   PEFactory.createInputTextEditor('cardWidth', 'extension.setting.cardWidth'))

  // let shadowOptions = [
  //   { label: 'never', value: 'never' },
  //   { label: 'hover', value: 'hover' },
  //   { label: 'always', value: 'always' }
  // ]
  // PERegister.registerCPEditor(app, 'card-shadow', 'card-shadow-editor',
  //   PEFactory.createSelectEditor('shadow', 'extension.setting.cardShadow',
  //     { optionItems: shadowOptions }))
  // /* -------------------------------------------------- */
  // registerCWGenerator('card', cardTemplateGenerator) // component Generate
  // /* -------------------------------------------------- */
  // /* componentLoad end */

  // /**
  // * Load fieldcomponent :
  // * 1. Load componentJson Schema;
  // * 2. full fieldcomponent, fieldcomponent and , component;
  // * 3. full property component ( property、 property、eventproperty);
  // * 4. fieldcomponent Generate ;
  // * 5. Load .
  //  */
  // // addCustomWidgetSchema(alertSchema) // Load componentJson Schema
  // /* -------------------------------------------------- */
  // app.component(AlertWidget.name, AlertWidget) // component
  // /* -------------------------------------------------- */
  // PERegister.registerCPEditor(app, 'alert-title', 'alert-title-editor',
  //   PEFactory.createInputTextEditor('title', 'extension.setting.alertTitle'))

  // let typeOptions = [
  //   { label: 'success', value: 'success' },
  //   { label: 'warning', value: 'warning' },
  //   { label: 'info', value: 'info' },
  //   { label: 'error', value: 'error' }
  // ]
  // // PERegister.registerCPEditor(app, 'alert-type', 'alert-type-editor',
  // //     PEFactory.createSelectEditor('type', 'extension.setting.alertType',
  // //         {optionItems: typeOptions}))
  // /* typeproperty already in , , only property ! ! */
  // app.component('AlertTypeEditor',
  //   PEFactory.createSelectEditor('type', 'extension.setting.alertType',
  //     { optionItems: typeOptions }))

  // PERegister.registerCPEditor(app, 'alert-description', 'alert-description-editor',
  //   PEFactory.createInputTextEditor('description', 'extension.setting.description'))

  // PERegister.registerCPEditor(app, 'alert-closable', 'alert-closable-editor',
  //   PEFactory.createBooleanEditor('closable', 'extension.setting.closable'))

  // PERegister.registerCPEditor(app, 'alert-closeText', 'alert-closeText-editor',
  //   PEFactory.createInputTextEditor('closeText', 'extension.setting.closeText'))

  // PERegister.registerCPEditor(app, 'alert-center', 'alert-center-editor',
  //   PEFactory.createBooleanEditor('center', 'extension.setting.center'))

  // PERegister.registerCPEditor(app, 'alert-showIcon', 'alert-showIcon-editor',
  //   PEFactory.createBooleanEditor('showIcon', 'extension.setting.showIcon'))

  // let effectOptions = [
  //   { label: 'light', value: 'light' },
  //   { label: 'dark', value: 'dark' }
  // ]
  // PERegister.registerCPEditor(app, 'alert-effect', 'alert-effect-editor',
  //   PEFactory.createRadioButtonGroupEditor('effect', 'extension.setting.effect',
  //     { optionItems: effectOptions }))

  // PERegister.registerEPEditor(app, 'alert-onClose', 'alert-onClose-editor',
  //   PEFactory.createEventHandlerEditor('onClose', []))
  // /* -------------------------------------------------- */
  // registerFWGenerator('alert', alertTemplateGenerator) // fieldcomponent Generate
  // /* -------------------------------------------------- */
  // /* fieldcomponentLoad end */
}
