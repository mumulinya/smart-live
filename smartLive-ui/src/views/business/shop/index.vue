<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="商铺名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入商铺名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商圈，例如陆家嘴" prop="area">
        <el-input
          v-model="queryParams.area"
          placeholder="请输入商圈，例如陆家嘴"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input
          v-model="queryParams.address"
          placeholder="请输入地址"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销量" prop="sold">
        <el-input
          v-model="queryParams.sold"
          placeholder="请输入销量"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评论数量" prop="comments">
        <el-input
          v-model="queryParams.comments"
          placeholder="请输入评论数量"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评分，1~5分，乘10保存，避免小数" prop="score">
        <el-input
          v-model="queryParams.score"
          placeholder="请输入评分，1~5分，乘10保存，避免小数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="营业时间，例如 10:00-22:00" prop="openHours">
        <el-input
          v-model="queryParams.openHours"
          placeholder="请输入营业时间，例如 10:00-22:00"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['shop:shop:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['shop:shop:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['shop:shop:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['shop:shop:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="shopList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="商铺名称" align="center" prop="name" />
      <el-table-column label="商铺类型的id" align="center" prop="typeId" />
      <el-table-column label="商铺图片，多个图片以','隔开" align="center" prop="images" />
      <el-table-column label="商圈，例如陆家嘴" align="center" prop="area" />
      <el-table-column label="地址" align="center" prop="address" />
      <el-table-column label="经度" align="center" prop="x" />
      <el-table-column label="维度" align="center" prop="y" />
      <el-table-column label="均价，取整数" align="center" prop="avgPrice" />
      <el-table-column label="销量" align="center" prop="sold" />
      <el-table-column label="评论数量" align="center" prop="comments" />
      <el-table-column label="评分，1~5分，乘10保存，避免小数" align="center" prop="score" />
      <el-table-column label="营业时间，例如 10:00-22:00" align="center" prop="openHours" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['shop:shop:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['shop:shop:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改店铺对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="商铺名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商铺名称" />
        </el-form-item>
        <el-form-item label="商铺图片，多个图片以','隔开" prop="images">
          <el-input v-model="form.images" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="商圈，例如陆家嘴" prop="area">
          <el-input v-model="form.area" placeholder="请输入商圈，例如陆家嘴" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="销量" prop="sold">
          <el-input v-model="form.sold" placeholder="请输入销量" />
        </el-form-item>
        <el-form-item label="评论数量" prop="comments">
          <el-input v-model="form.comments" placeholder="请输入评论数量" />
        </el-form-item>
        <el-form-item label="评分，1~5分，乘10保存，避免小数" prop="score">
          <el-input v-model="form.score" placeholder="请输入评分，1~5分，乘10保存，避免小数" />
        </el-form-item>
        <el-form-item label="营业时间，例如 10:00-22:00" prop="openHours">
          <el-input v-model="form.openHours" placeholder="请输入营业时间，例如 10:00-22:00" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listShop, getShop, delShop, addShop, updateShop } from "@/api/shop/shop"

export default {
  name: "Shop",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 店铺表格数据
      shopList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        typeId: null,
        images: null,
        area: null,
        address: null,
        x: null,
        y: null,
        avgPrice: null,
        sold: null,
        comments: null,
        score: null,
        openHours: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "商铺名称不能为空", trigger: "blur" }
        ],
        typeId: [
          { required: true, message: "商铺类型的id不能为空", trigger: "blur" }
        ],
        images: [
          { required: true, message: "商铺图片，多个图片以','隔开不能为空", trigger: "blur" }
        ],
        address: [
          { required: true, message: "地址不能为空", trigger: "blur" }
        ],
        x: [
          { required: true, message: "经度不能为空", trigger: "blur" }
        ],
        y: [
          { required: true, message: "维度不能为空", trigger: "blur" }
        ],
        sold: [
          { required: true, message: "销量不能为空", trigger: "blur" }
        ],
        comments: [
          { required: true, message: "评论数量不能为空", trigger: "blur" }
        ],
        score: [
          { required: true, message: "评分，1~5分，乘10保存，避免小数不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询店铺列表 */
    getList() {
      this.loading = true
      listShop(this.queryParams).then(response => {
        this.shopList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        typeId: null,
        images: null,
        area: null,
        address: null,
        x: null,
        y: null,
        avgPrice: null,
        sold: null,
        comments: null,
        score: null,
        openHours: null,
        createTime: null,
        updateTime: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加店铺"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getShop(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改店铺"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateShop(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addShop(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除店铺编号为"' + ids + '"的数据项？').then(function() {
        return delShop(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('shop/shop/export', {
        ...this.queryParams
      }, `shop_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
