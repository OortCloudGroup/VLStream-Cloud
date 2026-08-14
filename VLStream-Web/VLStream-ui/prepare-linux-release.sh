#!/bin/sh
set -eu

# Keep the committed application source identical to main while correcting
# Windows-tolerated import casing inside the ephemeral Linux build context.
sed -i 's#./components/ChoosePersonOrDept.vue#./components/choosePersonOrDept.vue#' \
  src/components/VForm/components/form-designer/form-widget/field-widget/address-book-widget.vue
sed -i 's#./flow/nodewrap.vue#./flow/nodeWrap.vue#' \
  src/components/processui/flowChart.vue
sed -i 's#@/components/personInfoCard.vue#@/components/PersonInfoCard.vue#' \
  src/views/events/page/eventManagement/components/eventDetailsDialog/index.vue \
  src/views/events/page/myWorkorder/myWorkorderDetails.vue
