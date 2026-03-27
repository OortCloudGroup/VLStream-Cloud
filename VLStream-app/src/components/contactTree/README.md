##  移动端选人组件


### 使用 

```vue
<oort-popup v-model="showContact" position="right" style="width: 100%">
  <Contact is-single :person-list="personList" @editClose="contactClose" />
</oort-popup>
```

---

### 属性

#### personList 

已选人列表，用户传入组件标记已选

#### deptList 

已选部门列表，用户传入组件标记已选


#### isSingle 

是否单选人员

#### isSingleDept 

是否单选部门


#### isChoosePerson 

是否可以勾选人员； 默认true, 如果为false ， 人员不显示


#### isChooseDept 

是否可以勾选部门； 默认false, 如果为true， 出现勾选框

---

### methods 

选择完，通过点击确认返回触发  @editClose 方法  ，

参数 格式  

```vue
{ userList: [] deptList: [] }
```

### example

1. 单选人员

```vue
  <Contact is-single :person-list="personList" @editClose="contactClose" />
```

2. 多选人员

```vue
  <Contact :person-list="checkPersonList" @editClose="checkContactClose" />
```


3. 单选部门 

```vue
  <Contact :person-list="checkPersonList" is-choose-dept is-single-dept :is-choose-person="false" @editClose="checkContactClose" />
```

4. 多选部门

```vue
  <Contact :person-list="checkPersonList" is-choose-dept :is-choose-person="false" @editClose="checkContactClose" />
```

5. 多选部门和人

```vue
  <Contact :person-list="checkPersonList" is-choose-dept @editClose="checkContactClose" />
```


### 待完善  

1. ui待完善

2. 多选部门和人的全选 和全不选待完善
