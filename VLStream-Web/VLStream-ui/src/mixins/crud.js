import { mapGetters } from 'vuex';

export default (app, option = {}) => {
  let optionObj = import.meta.glob(`../option/**/**`)[`../option/${option.name}.js`];
  let apiObj = import.meta.glob(`../api/**/**`)[`../api/${option.name}.js`];
  let mixins = {
    data() {
      return {
        data: [],
        form: {},
        params: {},
        option: {},
        api: {},
        loading: false,
        page: {},
      };
    },
    created() {
      optionObj().then(mode => (this.option = mode.default));
      apiObj().then(mode => {
        this.api = mode;
        this.getList();
      });
    },
    computed: {
      ...mapGetters(['userInfo', 'permission', 'roles']),
      ids() {
        const ids = [];
        this.selectionList.forEach(ele => {
          ids.push(ele[this.rowKey]);
        });
        return ids.join(',');
      },
      bindVal() {
        return {
          ref: 'crud',
          option: this.option,
          data: this.data,
          tableLoading: this.loading,
        };
      },
      onEvent() {
        return {
          'size-change': this.sizeChange,
          'current-change': this.currentChange,
          'row-save': this.rowSave,
          'row-update': this.rowUpdate,
          'row-del': this.rowDel,
          'refresh-change': this.refreshChange,
          'search-reset': this.searchChange,
          'search-change': this.searchChange,
        };
      },
      rowKey() {
        return option.rowKey || 'id';
      },
    },
    methods: {
      getList() {
        const callback = () => {
          this.loading = true;
          this.data = [];
          this.api[option.list || 'getList'](
            this.page.currentPage,
            this.page.pageSize,
            this.params
          ).then(res => {
            this.loading = false;
            let data;
            if (option.res) {
              data = option.res(res.data);
            } else {
              data = res.data.data;
            }
            this.page.total = data[option.total || 'total'];
            const result = data[option.data || 'records'];
            this.data = result;
            if (this.listAfter) {
              this.listAfter(data);
            }
          });
        };
        if (this.listBefore) {
          this.listBefore();
        }
        callback();
      },
      rowSave(row, done, loading) {
        const callback = () => {
          delete this.form.params;
          this.api[option.add || 'add'](this.form)
            .then(data => {
              this.getList();
              if (this.addAfter) {
                this.addAfter(data);
              } else {
                this.$message.success('新增成功');
              }
              done();
            })
            .catch(() => {
              loading();
            });
        };
        if (this.addBefore) {
          this.addBefore();
        }
        callback();
      },
      rowUpdate(row, index, done, loading) {
        const callback = () => {
          delete this.form.params;
          this.api[option.update || 'update'](this.form)
            .then(data => {
              this.getList();
              if (this.updateAfter) {
                this.updateAfter(data);
              } else {
                this.$message.success('更新成功');
              }
              done();
            })
            .catch(() => {
              loading();
            });
        };
        if (this.updateBefore) {
          this.updateBefore();
        }
        callback();
      },
      rowDel(row, index) {
        const callback = () => {
          this.api[option.del || 'del'](row[this.rowKey], row).then(data => {
            this.getList();
            if (this.delAfter) {
              this.delAfter(data, row, index);
            } else {
              this.$message.success('删除成功');
            }
          });
        };
        if (this.delBefore) {
          this.delBefore();
          callback();
        } else {
          this.$confirm('确定将选择数据删除?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }).then(() => {
            callback();
          });
        }
      },
      handleDelete() {
        if (this.selectionList.length === 0) {
          this.$message.warning('请选择至少一条数据');
          return;
        }
        this.$confirm('确定将选择数据删除?', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }).then(() => {
          this.api[option.del || 'remove'](this.ids).then(data => {
            this.getList();
            if (this.delMultiAfter) {
              this.delMultiAfter(data, this.ids);
            } else {
              this.$message.success('删除成功');
            }
          });
        });
      },
      searchChange(params, done) {
        if (done) done();
        this.params = params;
        this.page.currentPage = 1;
        this.getList();
      },
      dateChange(date) {
        if (date) {
          this.params.createTime_dategt = date[0];
          this.params.createTime_datelt = date[1];
        } else {
          delete this.params.createTime_dategt;
          delete this.params.createTime_datelt;
        }
        this.page.currentPage = 1;
        this.getList();
      },
      selectionChange(list) {
        this.selectionList = list;
      },
      selectionClear() {
        this.selectionList = [];
        this.$refs.crud.toggleSelection();
      },
      refreshChange() {
        this.getList();
      },
      sizeChange(val) {
        this.page.currentPage = 1;
        this.page.pageSize = val;
        this.getList();
      },
      currentChange(val) {
        this.page.currentPage = val;
        this.getList();
      },
    },
  };
  app.mixins = app.mixins || [];
  app.mixins.push(mixins);
  return app;
};
