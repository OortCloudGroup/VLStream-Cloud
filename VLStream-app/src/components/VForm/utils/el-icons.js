import {
  Edit,
  Minus,
  More,
  Plus,
  InfoFilled,
  Search,
  CirclePlus,
  Delete,
  ArrowDown,
  ArrowUp,
  ArrowLeftBold,
  ArrowRightBold,
  Bottom,
  Top,
  Back,
  Right,
  BottomLeft,
  TopRight
} from '@element-plus/icons-vue'

export function registerIcon(app) {
  app.component('ElIconEdit', Edit)
  app.component('ElIconMinus', Minus)
  app.component('ElIconPlus', Plus)
  app.component('ElIconMore', More)
  app.component('ElIconInfo', InfoFilled)
  app.component('ElIconSearch', Search)
  app.component('ElIconCirclePlusOutline', CirclePlus)
  app.component('ElIconDelete', Delete)
  app.component('ElIconArrowDown', ArrowDown)
  app.component('ElIconArrowUp', ArrowUp)
  app.component('ElIconBottom', Bottom)
  app.component('ElIconTop', Top)
  app.component('ElIconBack', Back)
  app.component('ElIconRight', Right)
  app.component('ElIconBottomLeft', BottomLeft)
  app.component('ElIconTopRight', TopRight)
  app.component('ElIconArrowLeftBold', ArrowLeftBold)
  app.component('ElIconArrowRightBold', ArrowRightBold)
}
