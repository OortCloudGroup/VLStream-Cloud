import { loadRemoteScript } from '~@/utils/util'
import { BEAUTIFIER_PATH } from '~@/utils/config'

let beautifierObj

export const beautifierOpts = {
  css: {
    brace_style: 'end-expand',
    break_chained_methods: false,
    comma_first: false,
    e4x: true,
    end_with_newline: true,
    indent_char: ' ',
    indent_empty_lines: true,
    indent_inner_html: true,
    indent_scripts: 'normal',
    indent_size: '2',
    jslint_happy: true,
    keep_array_indentation: false,
    max_preserve_newlines: '-1',
    preserve_newlines: false,
    space_before_conditional: true,
    unescape_strings: false,
    wrap_line_length: '110'
  },
  html: {
    brace_style: 'end-expand',
    break_chained_methods: false,
    comma_first: false,
    e4x: true,
    end_with_newline: true,
    indent_char: ' ',
    indent_empty_lines: true,
    indent_inner_html: true,
    indent_scripts: 'separate',
    indent_size: '2',
    jslint_happy: false,
    keep_array_indentation: false,
    max_preserve_newlines: '-1',
    preserve_newlines: false,
    space_before_conditional: true,
    unescape_strings: false,
    wrap_line_length: '110'
  },
  js: {
    brace_style: 'end-expand',
    break_chained_methods: false,
    comma_first: false,
    e4x: true,
    end_with_newline: true,
    indent_char: ' ',
    indent_empty_lines: true,
    indent_inner_html: true,
    indent_scripts: 'normal',
    indent_size: '2',
    jslint_happy: true,
    keep_array_indentation: false,
    max_preserve_newlines: '-1',
    preserve_newlines: false,
    space_before_conditional: true,
    unescape_strings: false,
    wrap_line_length: '110'
  }
}

export default function loadBeautifier(callback) {
  if (beautifierObj) {
    callback(beautifierObj)
    return
  }

  loadRemoteScript(BEAUTIFIER_PATH, () => {
    // eslint-disable-next-line no-undef
    beautifierObj = beautifier // beautifier为全局对象
    callback(beautifierObj)
  })
}
