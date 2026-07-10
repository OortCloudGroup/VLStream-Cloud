<template>
  <div class="asdPage flexRowAC">
    <div class="asd_hea_sel flexRowAC">
      <template v-if="selectScope ===1">
        <div
          v-if="props.mode!==2"
          :class="{'bgColorAct' : active === 1,'borColor': active === 1 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=1"
        >
          <svg class="wh5Img" :class="{svgColor:active===1}" viewBox="0 4 14 14" fill="currentColor">
            <path d="M6.99997402,4 C3.12160559,4 0,7.12158827 0,10.9999567 C0,14.8783944 3.12160559,18 6.99997402,18 C10.8784117,18 14,14.8783944 14,10.9999567 C14,7.12158827 10.8784117,4 6.99997402,4 Z M10.8784117,11.5675591 L6.99997402,11.5675591 C6.71620744,11.5675591 6.52702972,11.3783814 6.52702972,11.0946148 L6.52702972,6.83782169 C6.52702972,6.55407243 6.71620744,6.36489471 6.99997402,6.36489471 C7.28379256,6.36489471 7.47297028,6.55407243 7.47297028,6.83782169 L7.47297028,10.6216186 L10.9730006,10.6216186 C11.2567499,10.6216186 11.4459276,10.8107963 11.4459276,11.0946148 C11.4459276,11.3783814 11.1621783,11.5675591 10.8784117,11.5675591 Z" />
          </svg>
          最近使用
        </div>
        <div
          :class="{'bgColorAct' : active === 2,'borColor': active === 2 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=2"
        >
          <svg class="wh5Img" :class="{svgColor:active===2}" viewBox="0 0 14 14" fill="currentColor">
            <polygon points="3.6245099 13.69725 -0.00103960396 13.69725 -0.00103960396 9.78378218 1.29366584 9.78378218 1.29366584 5.87024505 6.47245297 5.87024505 6.47245297 3.91346782 5.17776485 3.91346782 5.17776485 0 8.80331436 0 8.80331436 3.91346782 7.50860891 3.91346782 7.50860891 5.87024505 12.6884183 5.87024505 12.6884183 9.78378218 13.9821015 9.78378218 13.9821015 13.6992946 10.356552 13.6992946 10.356552 9.78378218 11.6522624 9.78378218 11.6522624 6.84764604 7.50860891 6.84764604 7.50860891 9.78378218 8.80331436 9.78378218 8.80331436 13.69725 5.17776485 13.69725 5.17776485 9.78378218 6.47245297 9.78378218 6.47245297 6.84866832 2.3287995 6.84866832 2.3287995 9.78378218 3.6245099 9.78378218" />
          </svg>
          组织架构
        </div>
        <div
          v-if="props.mode!==2"
          :class="{'bgColorAct' : active === 3,'borColor': active === 3 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=3"
        >
          <svg class="wh5Img" :class="{svgColor:active===3}" viewBox="1 0 12 12" fill="currentColor">
            <path d="M8.00455863,8.01185538 C7.91843813,7.94259469 7.86925083,7.71270115 7.86925083,7.63646245 C7.86925083,7.29990744 7.95261971,7.18491738 7.95261971,7.18491738 C7.95128663,7.18626797 9.30212569,6.44524785 9.27616478,4.10281671 C9.23784722,3.16637756 8.67043295,1.5 6.91896865,1.5 L6.81915843,1.5 C4.98704268,1.5 4.50027985,3.16637756 4.46336374,4.10281671 C4.43880428,6.30807706 5.78550737,7.18491738 5.78550737,7.18491738 C5.78550737,7.18491738 5.94959605,7.37328914 5.94959605,7.70993072 C5.94959605,7.78608285 5.81968895,7.94259469 5.73356845,8.01185538 C4.98157363,8.6214014 2.27440937,9.23501647 2.0980324,10.5481991 C1.96263915,11.1050551 1.15870542,11.9888734 3.831671,12.3434189 C4.56864302,12.4418383 5.56812959,12.5 6.91896865,12.5 C13.1181918,12.5 11.8356475,11.2823971 11.661988,10.5690293 C11.4870125,9.25584663 8.75655345,8.6214014 8.00455863,8.01185538 M13.9576402,10.0384751 C13.8140946,8.98292488 11.7645327,8.47179831 11.1520159,7.98141531 C11.0823196,7.92602407 11.0412504,7.74035348 11.0412504,7.67942139 C11.0412504,7.40933933 11.1096136,7.31512748 11.1096136,7.31512748 C11.1096136,7.31512748 12.2088933,6.61007098 12.1884014,4.83561211 C12.1583558,4.0834064 11.6948365,2.74115155 10.2687638,2.74115155 L10.1881123,2.74115155 C9.88047807,2.74115155 9.62204821,2.80073306 9.40053447,2.8976634 C9.70550255,3.93380331 10.0732793,5.83433394 8.52963901,7.10876526 C7.92392427,8.02437425 9.9420391,8.5604693 9.62751727,8.41775765 C11.4528309,9.24888593 11.9450628,9.88743485 12.0776531,10.5468312 C12.1898029,11.0967265 12.252697,11.4347879 12.252697,11.4347879 C12.252697,11.4347879 13.6199603,11.2228156 13.9740644,10.9540841 C13.9877541,10.6604015 14.0328738,10.3431875 13.9576402,10.0384751 M4.70952241,8.19610613 C4.82708997,8.15037676 4.97061843,8.11027482 5.14019326,8.00219352 C5.38766792,7.84424452 5.57908479,7.52013912 5.11829996,7.12121486 C3.59513445,4.89789479 4.15026045,3.41988899 4.38131086,2.86303305 C4.188578,2.78686361 3.96839734,2.7425714 3.71685507,2.7425714 L3.63753671,2.7425714 C2.18008529,2.7425714 1.79313268,4.08617683 1.76442015,4.83838254 C1.74526138,6.61419199 2.81584564,7.32066833 2.81584564,7.32066833 C2.81584564,7.32066833 2.88280736,7.41347766 2.88280736,7.68496225 C2.88280736,7.7473315 2.84313963,7.93156493 2.77477647,7.98697348 C2.17733367,8.47733916 0.181129239,8.98983363 0.0416683817,10.0468037 C-0.0321809279,10.3529186 0.0115544073,10.6701499 0.026645576,10.9637633 C0.353370235,11.2172574 1.39937794,11.4333681 1.39937794,11.4333681 C1.39937794,11.4333681 1.48683152,11.1659872 1.57435346,10.7241213 C1.70427766,10.0716856 2.57657456,9.01343421 4.70952241,8.19610613" />
          </svg>
          我的常用
        </div>
        <div
          v-if="props.mode!==2"
          :class="{'bgColorAct' : active === 4,'borColor': active === 4 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=4"
        >
          <svg class="wh5Img" viewBox="0 1.5 14 14" fill="transparent">
            <path
              d="M10.7536642,1.43223561 L11.0750928,12.6369993 L8.3605321,14.6187298 L5.43251055,12.9044441 L5.28937847,1.75366418 L10.7536642,1.43223561 Z"
              :stroke="active===4?'var(--el-color-primary)':'currentColor'"
              stroke-width="1.92857143"
              transform="translate(8.182236, 8.182236) scale(-1, -1) rotate(45.000000) translate(-8.182236, -8.182236) "
              fill="transparent"
            />
            <circle
              :stroke="active===4?'var(--el-color-primary)':'currentColor'"
              stroke-width="1.28571429"
              fill="transparent"
              cx="10.6300642"
              cy="5.7265501"
              r="1"
            />
          </svg>
          标签
        </div>
        <div
          v-if="props.mode!==2"
          :class="{'bgColorAct' : active === 5,'borColor': active === 5 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=5"
        >
          <svg class="wh5Img" viewBox="0 0 14 14" fill="transparent">
            <rect x="0" y="0" width="14" height="14" />
            <g
              transform="translate(1.000000, 0.000000)"
              :stroke="active===5?'var(--el-color-primary)':'currentColor'"
              stroke-width="2"
            >
              <circle cx="6" cy="6" r="5" />
              <line x1="9" y1="10" x2="11.8284271" y2="12.8284271" stroke-linecap="round" />
            </g>
          </svg>
          用户池搜索
        </div>
      </template>
      <template v-if="selectScope ===2">
        <div
          :class="{'bgColorAct' : active === 5,'borColor': active === 5 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=2"
        >
          <svg class="wh5Img" :class="{svgColor:active===2}" viewBox="0 0 14 14" fill="currentColor">
            <polygon points="3.6245099 13.69725 -0.00103960396 13.69725 -0.00103960396 9.78378218 1.29366584 9.78378218 1.29366584 5.87024505 6.47245297 5.87024505 6.47245297 3.91346782 5.17776485 3.91346782 5.17776485 0 8.80331436 0 8.80331436 3.91346782 7.50860891 3.91346782 7.50860891 5.87024505 12.6884183 5.87024505 12.6884183 9.78378218 13.9821015 9.78378218 13.9821015 13.6992946 10.356552 13.6992946 10.356552 9.78378218 11.6522624 9.78378218 11.6522624 6.84764604 7.50860891 6.84764604 7.50860891 9.78378218 8.80331436 9.78378218 8.80331436 13.69725 5.17776485 13.69725 5.17776485 9.78378218 6.47245297 9.78378218 6.47245297 6.84866832 2.3287995 6.84866832 2.3287995 9.78378218 3.6245099 9.78378218" />
          </svg>
          本部门
        </div>
      </template>
      <template v-if="selectScope ===3">
        <div
          :class="{'bgColorAct' : active === 5,'borColor': active === 5 }"
          class="asd_hea_sel_item flexRowAC"
          @click="active=5"
        >
          <svg class="wh5Img" :class="{svgColor:active===5}" viewBox="0 4 14 14" fill="currentColor">
            <path d="M6.99997402,4 C3.12160559,4 0,7.12158827 0,10.9999567 C0,14.8783944 3.12160559,18 6.99997402,18 C10.8784117,18 14,14.8783944 14,10.9999567 C14,7.12158827 10.8784117,4 6.99997402,4 Z M10.8784117,11.5675591 L6.99997402,11.5675591 C6.71620744,11.5675591 6.52702972,11.3783814 6.52702972,11.0946148 L6.52702972,6.83782169 C6.52702972,6.55407243 6.71620744,6.36489471 6.99997402,6.36489471 C7.28379256,6.36489471 7.47297028,6.55407243 7.47297028,6.83782169 L7.47297028,10.6216186 L10.9730006,10.6216186 C11.2567499,10.6216186 11.4459276,10.8107963 11.4459276,11.0946148 C11.4459276,11.3783814 11.1621783,11.5675591 10.8784117,11.5675591 Z" />
          </svg>
          自定义范围
        </div>
      </template>
    </div>
    <!--选择 -->
    <div v-if="selectScope ===1" class="asd_data_sel">
      <recently-use-person
        v-if="active === 1 && props.mode !==2"
        :is-single="isSingle"
        :user-list="choosePersonList"
        :disalbed-user-list="disalbedUserList"
        @addPerson="addRecentPerson"
      />
      <dept-user-tree
        v-if="active === 2"
        :mode="props.mode"
        :is-single="isSingle"
        :dept-list="chooseDeptList"
        :user-list="choosePersonList"
        :disalbed-dept-list="disalbedDeptList"
        :disalbed-user-list="disalbedUserList"
        @addPerson="addDeptPerson"
      />
      <my-always-use
        v-if="active === 3 && props.mode !==2"
        :is-single="isSingle"
        :user-list="choosePersonList"
        :disalbed-user-list="disalbedUserList"
        @addPerson="addAlwaysPerson"
      />
      <my-tag
        v-if="active === 4 && props.mode !==2"
        :is-single="isSingle"
        :disalbed-user-list="disalbedUserList"
        :user-list="choosePersonList"
        @addPerson="addTagPerson"
      />
      <search-person
        v-if="active === 5 && props.mode !==2"
        :is-single="isSingle"
        :disalbed-user-list="disalbedUserList"
        :user-list="choosePersonList"
        @addPerson="addSearchPerson"
      />
    </div>
    <div v-if="selectScope ===2" class="asd_data_sel">
      <dept-user-tree
        :is-local-dept="true"
        :mode="props.mode"
        :is-single="isSingle"
        :dept-list="chooseDeptList"
        :user-list="choosePersonList"
        :disalbed-dept-list="disalbedDeptList"
        :disalbed-user-list="disalbedUserList"
        @addPerson="addDeptPerson"
      />
    </div>
    <div v-if="selectScope ===3" class="asd_data_sel">
      <scope-person-or-dept
        ref="tabBody5"
        :is-single="isSingle"
        :user-list="choosePersonList"
        :dept-list="chooseDeptList"
        :disalbed-user-list="disalbedUserList"
        :scope-user-list="chooseScopeUserList"
        :scope-dept-list="chooseScopeDeptList"
        @addPerson="addDeptPerson"
      />
    </div>
    <!--选择的人员 -->
    <div
      class="asd_content_sel flexRowAC"
      :class="{'open_select': openSelect , 'close_select': !openSelect && startSelect }"
    >
      <div
        class="asd_content_sel_up flexRowAC"
        @click="openSelect=!openSelect;startSelect=true"
      >
        <svg v-if="openSelect" class="su_img svgColor" viewBox="24 0 12 12" fill="currentColor">
          <g transform="translate(-930.000000, -249.000000)">
            <g transform="translate(960.000000, 255.000000) scale(1, -1) translate(-960.000000, -255.000000) translate(930.000000, 249.000000)">
              <path d="M8,0 L52,0 C56.418278,-8.11624501e-16 60,3.581722 60,8 L60,12 L60,12 L0,12 L0,8 C-5.41083001e-16,3.581722 3.581722,8.11624501e-16 8,0 Z" />
              <path
                d="M30.8,3.06666667 L34.8,8.4 C35.1313708,8.8418278 35.0418278,9.46862915 34.6,9.8 C34.4269038,9.92982213 34.2163702,10 34,10 L26,10 C25.4477153,10 25,9.55228475 25,9 C25,8.78362979 25.0701779,8.57309617 25.2,8.4 L29.2,3.06666667 C29.5313708,2.62483887 30.1581722,2.53529582 30.6,2.86666667 C30.6758057,2.92352092 30.7431458,2.990861 30.8,3.06666667 Z"
                fill="#FFFFFF"
              />
            </g>
          </g>
        </svg>
        <svg v-else class="su_img svgColor" viewBox="24 0 12 12" fill="currentColor">
          <path d="M8,0 L52,0 C56.418278,-8.11624501e-16 60,3.581722 60,8 L60,12 L60,12 L0,12 L0,8 C-5.41083001e-16,3.581722 3.581722,8.11624501e-16 8,0 Z" />
          <path
            d="M30.8,3.06666667 L34.8,8.4 C35.1313708,8.8418278 35.0418278,9.46862915 34.6,9.8 C34.4269038,9.92982213 34.2163702,10 34,10 L26,10 C25.4477153,10 25,9.55228475 25,9 C25,8.78362979 25.0701779,8.57309617 25.2,8.4 L29.2,3.06666667 C29.5313708,2.62483887 30.1581722,2.53529582 30.6,2.86666667 C30.6758057,2.92352092 30.7431458,2.990861 30.8,3.06666667 Z"
            fill="#FFFFFF"
          />
        </svg>
      </div>
      <div class="selPerChLeBox flexRowAC items_container">
        <div
          v-for="(item,index) in choosePersonList"
          :key="index+item.user_id"
          class="carddd_person flexRowAC"
        >
          <oort-img :src="item['photo']" class="carddd_personImg" default-type="user" />
          <span class="selText"> {{ item['user_name'] }} </span>
          <el-icon class="sel_img_del" @click="removeUser(index)">
            <circleCloseFilled />
          </el-icon>
        </div>
        <div
          v-for="(item,index) in chooseDeptList"
          :key="index+item.dept_id"
          class="carddd_person flexRowAC"
        >
          <img
            v-if="item['dept_photo']"
            class="carddd_personImg"
            :src="item['dept_photo']"
          />
          <img v-else class="carddd_personImg" src="@/assets/img/dept_default.png" />
          <span class="selText"> {{ item['dept_name'] }} </span>
          <el-icon class="sel_img_del" @click="removeDept(index)">
            <circleCloseFilled />
          </el-icon>
        </div>
        <div
          v-if="choosePersonList.length ===0 && chooseDeptList.length ===0"
          class="selPerChLe flexRowAC"
        >
          <span style="font-size: 16px;color: #999">暂未选择</span>
        </div>
      </div>
    </div>
    <!--设置我的地址本-->
    <div class="saveBox flexRowAC">
      <div v-if="showAddressBookSettings" class="saveTi flexRowAC" @click="goToMyAddress">
        <oort-svg-icon
          name="setting"
          width="20"
          height="20"
        />
        <span>设置我的地址本</span>
      </div>
      <div class="bt_group">
        <el-button class="common_btn" @click="closeChoose">
          取 消
        </el-button>
        <el-button type="primary" class="common_btn" @click="confirmChoose(true)">
          确 定
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RecentlyUsePerson from './recentlyUsePerson.vue'
import DeptUserTree from './deptUserTree.vue'
import MyAlwaysUse from './myAlwaysUse.vue'
import MyTag from './myTag.vue'
import SearchPerson from './searchPerson.vue'
import ScopePersonOrDept from './scopePersonOrDept.vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { getUserList, getDeptList } from '@/api/system/directory'
import { ElButton, ElIcon, ElMessage } from 'element-plus'
import OortSvgIcon from '@/components/oortSvgIcon/index.vue'
import Config from '@/config'
import { verifyToken } from '@/api/login/indexapaas'

const props = defineProps({
  // 激活的tab栏目
  activeTab: {
    type: Number,
    default: 1
  },
  // 已选的人员list
  userList: {
    type: Array,
    default: null
  },
  // 已选的部门
  deptList: {
    type: Array,
    default: null
  },
  // 选择模式 1, 部门和人都可以选， 2 只选部门  3 只选人
  mode: {
    type: Number,
    default: 2
  },
  // 是否单选
  isSingle: {
    type: Boolean,
    default: null
  },
  // 禁止选择的的部门
  disalbedDeptList: {
    type: Array,
    default: null
  },
  // 禁止选择的的用户
  disalbedUserList: {
    type: Array,
    default: null
  },
  // 选择范围 1 全部 2 本部门 3 自定义（scopeUserList, scopeDeptList）
  selectScope: {
    type: Number,
    default: 1
  },
  // 自定义选择范围的人员
  scopeUserList: {
    type: Array,
    default: null
  },
  // 自定义选择范围的部门
  scopeDeptList: {
    type: Array,
    default: null
  }
})
const emits = defineEmits(['close', 'cancelChoose', 'saveChoose'])
let chooseDeptList = ref<any>([]) // 选择的部门的合集
let choosePersonList = ref<any>([]) // 选择人员的合集
let chooseScopeUserList = ref<any>([]) // 选择的部门的合集
let chooseScopeDeptList = ref<any>([]) // 选择人员的合集
const showAddressBookSettings = false
let active = ref<any>(1) // 激活的tab栏目
if (props.mode === 2) {
  active.value = 2
}
if (props.selectScope === 3 || props.selectScope === 2) {
  active.value = 5
}
let openSelect = ref<any>(false)
let startSelect = ref<any>(false)

const store = useUserStore()

const tabBody5 = ref<any>(null)
const refreshRefDom = () => {
  if (tabBody5.value && tabBody5.value.initData) {
    tabBody5.value.initData()
  }
}
// 处理userList
if (props.userList && props.userList.length > 0) {
  const params = {
    user_id: props.userList,
    accessToken: store.token,
    tenant_id: store.tenantId
  }
  getUserList(params).then((res: any) => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        res.data.list.forEach(item => {
          store.addUser(item)
        })
        choosePersonList.value = res.data.list || []
        refreshRefDom()
      }
    }
  })
}
// 处理deptList
if (props.deptList && props.deptList.length > 0) {
  const params = {
    dept_id: props.deptList,
    accessToken: store.token,
    tenant_id: store.tenantId
  }
  getDeptList(params).then((res: any) => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        chooseDeptList.value = res.data.list || []
        refreshRefDom()
      }
    }
  })
}
// 处理scopeUserList
if (props.scopeUserList && props.scopeUserList.length > 0) {
  const params = {
    user_id: props.scopeUserList,
    accessToken: store.token,
    tenant_id: store.tenantId
  }
  getUserList(params).then((res: any) => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        chooseScopeUserList.value = res.data.list || []
        refreshRefDom()
      }
    }
  })
}
// 处理scopeDeptList
if (props.scopeDeptList && props.scopeDeptList.length > 0) {
  const params = {
    dept_id: props.scopeDeptList,
    accessToken: store.token,
    tenant_id: store.tenantId
  }
  getDeptList(params).then((res: any) => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        chooseScopeDeptList.value = res.data.list || []
        refreshRefDom()
      }
    }
  })
}
// 使用reduce去重
const deleteRepeat = (arr, repeatKey) => {
  let tObj = {}
  return arr.reduce((pre, cur) => {
    if (!tObj[cur[repeatKey]]) {
      tObj[cur[repeatKey]] = true
      pre.push(cur)
    }
    return pre
  }, [])
}

const closeChoose = () => {
  emits('cancelChoose')
  emits('close')
}

// 选择最近联系人
const addRecentPerson = (data: any) => {
  // 去重
  choosePersonList.value = deleteRepeat(data, 'user_id')
}

// 组织
const addDeptPerson = (data: any) => {
  choosePersonList.value = deleteRepeat(data.user, 'user_id')
  chooseDeptList.value = deleteRepeat(data.dept, 'dept_id')
}

// 我的常用
const addAlwaysPerson = (data: any) => {
  choosePersonList.value = deleteRepeat(data, 'user_id')
}

// 标签
const addTagPerson = (data: any) => {
  choosePersonList.value = deleteRepeat(data, 'user_id')
}

// 搜索
const addSearchPerson = (data: any) => {
  choosePersonList.value = deleteRepeat(data, 'user_id')
}

// 移除人员
const removeUser = (index: number) => {
  choosePersonList.value.splice(index, 1)
}

// 移除部门
const removeDept = (index: number) => {
  chooseDeptList.value.splice(index, 1)
}

// 确定
const confirmChoose = (flag: any) => {
  if (flag) {
    emits('saveChoose', { user: choosePersonList.value, dept: chooseDeptList.value })
  } else {
    emits('close')
  }
}

// 跳转到个人地址本
const goToMyAddress = async() => {
  // 校验token
  const params = {
    accessToken: store.token
  }
  let res = await verifyToken(params)
  if (res.code !== 200) {
    ElMessage.warning('用户信息失效， 请重新登录')
    return
  }
  const tempObj = {
    accessToken: store.token,
    fromWhere: 'console_manage'
  }
  let target = '_blank'
  let windowFeatures = ''
  window.sessionStorage.setItem('tempObj', JSON.stringify(tempObj))
  let appendStr = '?accessToken=' + store.token + '&fromWhere=console_manage&tab=myAddress'
  let apk_url = '/personal_center/index.html'
  if (apk_url.indexOf('?') !== -1) {
    appendStr = '&accessToken=' + store.token + '&fromWhere=console_manage&tab=myAddress'
  }
  if (apk_url.includes('http') || apk_url.includes('//')) {
    window.open(apk_url + appendStr, target, windowFeatures)
  } else {
    window.open(Config.URL.slice(0, -1) + Config.frontURLStr + apk_url + appendStr, target, windowFeatures)
  }
}
</script>

<style lang="scss" scoped>
.asdPage {
  width: 100%;
  position: relative;
  flex-direction: column;
  align-items: flex-start;

  .asd_hea_sel {
    width: 100%;
    height: 48px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);

    .borColor {
      border-color: var(--el-color-primary);
    }

    .bgColorAct {
      color: var(--el-color-primary);
      background-color: var(--el-menu-hover-bg-color);
    }

    .asd_hea_sel_item {
      justify-content: center;
      cursor: pointer;
      width: 176px;
      height: 100%;
    }
  }
}

// 图标
.wh5Img {
  width: 20px;
  height: 20px;
  margin-left: 4px;
  margin-right: 4px;
}

// 数据选择
.asd_data_sel {
  width: 100%;
  height: 320px;
  margin-bottom: 116px;
}

// 已选择
.asd_content_sel {
  width: 100%;
  height: 102px;
  flex-direction: column;
  position: absolute;
  bottom: 48px;
  background-color: #fff;
  z-index: 2;

  .asd_content_sel_up {
    justify-content: center;

    .su_img {
      height: 16px;
      width: 80px;
      z-index: 999;
    }
  }

  // 暂未选择
  .selPerChLeBox {
    z-index: 10;
    width: 100%;
    //height: 96px;
    flex-wrap: wrap;
    overflow-y: auto;

    .selPerChLe {
      width: 100%;
      height: 100%;
      justify-content: center;
    }

    .carddd_person {
      border: 1px solid transparent;
      position: relative;
      width: 110px;
      height: 80px;
      flex-direction: column;
      justify-content: center;
    }

    .sel_img_del {
      font-size: 20px;
      display: none;
      cursor: pointer;
      position: absolute;
      right: 4px;
      top: 4px;
    }

    .selText {
      color: #333;
    }

    .carddd_personImg {
      width: 32px;
      height: 32px;
      border-radius: 50%
    }

    .carddd_person:hover {
      background-color: #6586BE14;
      border: 1px solid var(--el-color-primary);

      .sel_img_del {
        display: flex !important;
      }
    }
  }
}

// 确定
.saveBox {
  padding-left: 20px;
  padding-right: 20px;
  justify-content: space-between;
  height: 32px;
  width: 100%;

  .saveTi {
    width: 12.2rem;
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 14px;
  }

  .saveTiImg {
    width: 196px;
    height: 196px;
    margin: 0 4px;
  }

  .bt_group {
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-align: center;
    -ms-flex-align: center;
    align-items: center;
    height: 60px;
    -webkit-box-pack: end;
    -ms-flex-pack: end;
    justify-content: flex-end;
    width: 100%;
  }

  .common_btn {
    border-radius: 2px;
    width: 170px;
    height: 48px;
  }
}

.juitCenter {
  justify-content: center;
  align-items: center;
}

.open_select {
  animation: ping 1s cubic-bezier(0, 0, 0.2, 1) forwards;
}

@keyframes ping {
  0% {
    height: 96px;
  }
  75%, 100% {
    height: 240px;
  }
}

.close_select {
  animation: pong 1s cubic-bezier(0, 0, 0.2, 1) forwards;
}

@keyframes pong {
  0% {
    height: 240px;
  }
  100% {
    height: 96px;
  }
}

.items_container {
  align-content: flex-start; // 解决换行的中间空隙
  height: calc(100% - 16px);
  border-top: 0.5px solid #00000020;
  border-bottom: 0.5px solid #00000020;
}

// 颜色
.svgColor {
  color: var(--el-color-primary);
}
</style>
