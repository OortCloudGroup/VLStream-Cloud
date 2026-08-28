/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

// import { vfApp } from '~@/utils/create-app'

/**
 * : property == property component
 */
const COMMON_PROPERTIES = {
  // field
  'name': 'name-editor',
  'customName': 'customName-editor',
  'label': 'label-editor',
  'labelAlign': 'labelAlign-editor',
  'type': 'type-editor',
  'defaultValue': 'defaultValue-editor',
  'placeholder': 'placeholder-editor',
  'startPlaceholder': 'startPlaceholder-editor',
  'endPlaceholder': 'endPlaceholder-editor',
  'columnWidth': 'columnWidth-editor',
  'autoFullWidth': 'autoFullWidth-editor',
  'size': 'size-editor',
  'color': 'color-editor',
  'showStops': 'showStops-editor',
  'displayStyle': 'displayStyle-editor',
  'buttonStyle': 'buttonStyle-editor',
  'border': 'border-editor',
  'labelWidth': 'labelWidth-editor',
  'labelHidden': 'labelHidden-editor',
  'rows': 'rows-editor',
  'required': 'required-editor',
  'requiredHint': 'requiredHint-editor',
  'validation': 'validation-editor',
  'validationHint': 'validationHint-editor',
  'readonly': 'readonly-editor',
  'disabled': 'disabled-editor',
  'hidden': 'hidden-editor',
  'clearable': 'clearable-editor',
  'editable': 'editable-editor',
  'showPassword': 'showPassword-editor',
  'textContent': 'textContent-editor',
  'textAlign': 'textAlign-editor',
  'fontSize': 'fontSize-editor',
  'preWrap': 'preWrap-editor',
  'htmlContent': 'htmlContent-editor',
  'format': 'date-format-editor',
  'valueFormat': 'date-value-format-editor',
  'datetype': 'date-type-editor',
  'filterable': 'filterable-editor',
  'allowCreate': 'allowCreate-editor',
  'remote': 'remote-editor',
  'automaticDropdown': 'automaticDropdown-editor',
  'checkStrictly': 'checkStrictly-editor',
  'showAllLevels': 'showAllLevels-editor',
  'multiple': 'multiple-editor',
  'multipleLimit': 'multipleLimit-editor',
  'contentPosition': 'contentPosition-editor',
  'optionItems': 'optionItems-editor',
  'uploadURL': 'uploadURL-editor',
  'uploadTip': 'uploadTip-editor',
  'withCredentials': 'withCredentials-editor',
  'multipleSelect': 'multipleSelect-editor',
  'limit': 'limit-editor',
  'fileMaxSize': 'fileMaxSize-editor',
  'fileTypes': 'fileTypes-editor',
  'contentHeight': 'contentHeight-editor',
  'customClass': 'customClass-editor',
  'serialOptions': 'serialOptions-editor',
  'addressSetting': 'addressSetting-editor',
  'buttonText': 'buttonText-editor',
  'linkFormID': 'linkFormIDSelect-editor',
  'moneyType': 'moneyType-editor',
  'decimalLength': 'decimalLength-editor',
  'hypeLinkOptions': 'hypeLinkOptions-editor',
  'numberOptions': 'numberOptions-editor',
  'basicOptions': 'basicOptions-editor',
  'calcOptions': 'calcOptions-editor',
  'linkQuery': 'linkQuery-editor',
  'scanType': 'scanType-editor',
  'height': 'height-editor',
  'fontWeight': 'fontWeight-editor',
  //
  'showBlankRow': 'showBlankRow-editor',
  'showRowNumber': 'showRowNumber-editor',
  'cellWidth': 'cellWidth-editor',
  'cellHeight': 'cellHeight-editor',
  'colHeight': 'colHeight-editor',
  'wordBreak': 'wordBreak-editor',
  'gutter': 'gutter-editor',
  'responsive': 'responsive-editor',
  'span': 'span-editor',
  'offset': 'offset-editor',
  'push': 'push-editor',
  'pull': 'pull-editor'

}

const ADVANCED_PROPERTIES = {
  'min': 'min-editor',
  'max': 'max-editor',
  'precision': 'precision-editor',
  'step': 'step-editor',
  'controlsPosition': 'controlsPosition-editor',
  'minLength': 'minLength-editor',
  'maxLength': 'maxLength-editor',
  'showWordLimit': 'showWordLimit-editor',
  'prefixIcon': 'prefixIcon-editor',
  'suffixIcon': 'suffixIcon-editor',
  'switchWidth': 'switchWidth-editor',
  'activeText': 'activeText-editor',
  'inactiveText': 'inactiveText-editor',
  'activeColor': 'activeColor-editor',
  'inactiveColor': 'inactiveColor-editor',
  'lowThreshold': 'lowThreshold-editor',
  'highThreshold': 'highThreshold-editor',
  'allowHalf': 'allowHalf-editor',
  'showText': 'showText-editor',
  'showScore': 'showScore-editor',
  'range': 'range-editor',
  'vertical': 'vertical-editor',
  'plain': 'plain-editor',
  'round': 'round-editor',
  'circle': 'circle-editor',
  'icon': 'icon-editor',
  'labelIconClass': 'labelIconClass-editor',
  'labelIconPosition': 'labelIconPosition-editor',
  'labelTooltip': 'labelTooltip-editor',
  'appendButton': 'appendButton-editor',
  'appendButtonDisabled': 'appendButtonDisabled-editor',
  'buttonIcon': 'buttonIcon-editor'
}

const EVENT_PROPERTIES = {
  // field
  'onCreated': 'onCreated-editor',
  'onMounted': 'onMounted-editor',
  'onClick': 'onClick-editor',
  'onInput': 'onInput-editor',
  'onChange': 'onChange-editor',
  'onFocus': 'onFocus-editor',
  'onBlur': 'onBlur-editor',
  'onRemoteQuery': 'onRemoteQuery-editor',
  'onBeforeUpload': 'onBeforeUpload-editor',
  'onUploadSuccess': 'onUploadSuccess-editor',
  'onUploadError': 'onUploadError-editor',
  'onFileRemove': 'onFileRemove-editor',
  'onValidate': 'onValidate-editor',
  'onAppendButtonClick': 'onAppendButtonClick-editor',

  //
  'onSubFormRowAdd': 'onSubFormRowAdd-editor',
  'onSubFormRowInsert': 'onSubFormRowInsert-editor',
  'onSubFormRowDelete': 'onSubFormRowDelete-editor',
  'onSubFormRowChange': 'onSubFormRowChange-editor'

}

/**
 * component property
 * property component propEditorNameSet to null, property ! !
 * @param uniquePropName property ( , componentproperty )
 * @param propEditorName property component
 */
export function registerCommonProperty(uniquePropName, propEditorName) {
  COMMON_PROPERTIES[uniquePropName] = propEditorName
}

/**
 * component property
 * property component propEditorNameSet to null, property ! !
 * @param uniquePropName property ( , componentproperty )
 * @param propEditorName property component
 */
export function registerAdvancedProperty(uniquePropName, propEditorName) {
  ADVANCED_PROPERTIES[uniquePropName] = propEditorName
}

/**
 * componenteventproperty
 * property component propEditorNameSet to null, property ! !
 * @param uniquePropName property ( , componentproperty )
 * @param propEditorName property component
 */
export function registerEventProperty(uniquePropName, propEditorName) {
  EVENT_PROPERTIES[uniquePropName] = propEditorName
}

/**
 * Check propertywhether already
 * @param uniquePropName property ( , componentproperty )
 */
export function propertyRegistered(uniquePropName) {
  return !!COMMON_PROPERTIES[uniquePropName] || !!ADVANCED_PROPERTIES[uniquePropName] || !!EVENT_PROPERTIES[uniquePropName]
}

/**
 * property property
 * @param app
 * @param uniquePropName
 * @param propEditorName
 * @param editorComponent
 */
export function registerCPEditor(app, uniquePropName, propEditorName, editorComponent) {
  app.component(propEditorName, editorComponent)
  registerCommonProperty(uniquePropName, propEditorName)
}

/**
 * property property
 * @param app
 * @param uniquePropName
 * @param propEditorName
 * @param editorComponent
 */
export function registerAPEditor(app, uniquePropName, propEditorName, editorComponent) {
  app.component(propEditorName, editorComponent)
  registerAdvancedProperty(uniquePropName, propEditorName)
}

/**
 * eventproperty property
 * @param app
 * @param uniquePropName
 * @param propEditorName
 * @param editorComponent
 */
export function registerEPEditor(app, uniquePropName, propEditorName, editorComponent) {
  app.component(propEditorName, editorComponent)
  registerEventProperty(uniquePropName, propEditorName)
}

export default {
  COMMON_PROPERTIES,
  ADVANCED_PROPERTIES,
  EVENT_PROPERTIES
}

