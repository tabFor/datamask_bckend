# Presidio敏感列自动识别功能前端修改指南

## 后端接口变更

后端已经实现了使用Presidio自动识别敏感列的功能。主要变更包括：

1. 在Task模型中新增两个字段：
   - `autoDetectColumns`: 布尔值，标记是否自动识别敏感列
   - `detectedColumns`: 字符串(JSON)，存储识别到的敏感列信息

2. 新增的API接口：
   - `POST /api/presidio/detect-sensitive-columns`: 使用Presidio检测指定表的敏感列
     - 请求体：`{"tableName": "表名"}`
     - 响应：
       ```json
       {
         "success": true,
         "tableName": "users",
         "columns": [
           {
             "columnName": "name",
             "tableSchema": null,
             "tableName": "users",
             "dataType": "varchar",
             "sensitiveType": "PERSON",
             "maskingRule": "保留姓氏，名字用*代替",
             "description": "Presidio自动识别: PERSON",
             "enabled": true
           },
           // 其他敏感列...
         ],
         "count": 5
       }
       ```

## 前端修改建议

### 1. 任务创建/编辑表单增加选项

在任务创建/编辑表单中添加自动识别敏感列的选项：

```vue
<template>
  <div class="form-section">
    <h3>敏感数据处理配置</h3>
    
    <el-form-item label="数据处理方式">
      <el-checkbox v-model="form.usePresidio" 
                  @change="handlePresidioChange">
        使用Presidio自动识别敏感数据
      </el-checkbox>
      
      <el-checkbox v-model="form.autoDetectColumns" 
                  :disabled="!form.usePresidio">
        自动识别敏感列
      </el-checkbox>
    </el-form-item>
    
    <!-- 如果需要预览按钮 -->
    <el-button type="primary" 
              v-if="form.sourceTables && form.usePresidio" 
              @click="previewSensitiveColumns" 
              :loading="previewLoading">
      预览敏感列识别结果
    </el-button>
    
    <!-- 显示识别结果 -->
    <div v-if="detectedColumns.length > 0" class="detected-columns">
      <h4>识别出 {{ detectedColumns.length }} 个敏感列：</h4>
      <el-table :data="detectedColumns" border stripe>
        <el-table-column prop="columnName" label="列名"></el-table-column>
        <el-table-column prop="sensitiveType" label="敏感类型"></el-table-column>
        <el-table-column prop="maskingRule" label="脱敏规则"></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      form: {
        // 其他表单字段...
        usePresidio: false,
        autoDetectColumns: false
      },
      detectedColumns: [],
      previewLoading: false
    }
  },
  methods: {
    // 处理Presidio选项改变
    handlePresidioChange(val) {
      if (!val) {
        this.form.autoDetectColumns = false;
        this.detectedColumns = [];
      }
    },
    
    // 预览敏感列识别结果
    previewSensitiveColumns() {
      if (!this.form.sourceTables) {
        this.$message.warning('请先选择源表');
        return;
      }
      
      this.previewLoading = true;
      this.$http.post('/api/presidio/detect-sensitive-columns', {
        tableName: this.form.sourceTables
      }).then(response => {
        if (response.data.success) {
          this.detectedColumns = response.data.columns;
          this.$message.success(`成功识别出${response.data.count}个敏感列`);
        } else {
          this.$message.error(response.data.message || '识别敏感列失败');
        }
      }).catch(error => {
        console.error('识别敏感列请求失败', error);
        this.$message.error('识别敏感列请求失败: ' + (error.response?.data?.message || error.message));
      }).finally(() => {
        this.previewLoading = false;
      });
    },
    
    // 提交表单时
    submitForm() {
      // 确保包含新字段
      const formData = {
        // 其他字段...
        usePresidio: this.form.usePresidio,
        autoDetectColumns: this.form.autoDetectColumns,
        detectedColumns: this.detectedColumns.length > 0 ? JSON.stringify(this.detectedColumns) : null
      };
      
      // 提交表单...
    }
  }
}
</script>
```

### 2. 任务详情页显示识别的敏感列

在任务详情页面中显示识别到的敏感列：

```vue
<template>
  <div class="task-detail">
    <!-- 其他任务详情... -->
    
    <el-divider></el-divider>
    
    <div v-if="task.usePresidio" class="presidio-section">
      <h3>Presidio敏感数据处理</h3>
      <p>
        <el-tag type="success" v-if="task.usePresidio">使用Presidio自动识别</el-tag>
        <el-tag type="info" v-else>使用自定义规则</el-tag>
        
        <el-tag type="warning" v-if="task.autoDetectColumns">自动识别敏感列</el-tag>
      </p>
      
      <div v-if="task.detectedColumns && parsedDetectedColumns.length > 0">
        <h4>识别出的敏感列 ({{ parsedDetectedColumns.length }})</h4>
        <el-table :data="parsedDetectedColumns" border stripe>
          <el-table-column prop="columnName" label="列名"></el-table-column>
          <el-table-column prop="sensitiveType" label="敏感类型"></el-table-column>
          <el-table-column prop="maskingRule" label="脱敏规则"></el-table-column>
          <el-table-column prop="description" label="说明"></el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      task: {}
    }
  },
  computed: {
    parsedDetectedColumns() {
      if (!this.task.detectedColumns) return [];
      
      try {
        return JSON.parse(this.task.detectedColumns);
      } catch (e) {
        console.error('解析敏感列数据失败', e);
        return [];
      }
    }
  },
  // 其他方法...
}
</script>
```

### 3. 任务列表页显示Presidio使用情况

在任务列表中添加Presidio使用情况的标记：

```vue
<el-table-column label="敏感数据处理" width="180">
  <template slot-scope="scope">
    <el-tag type="success" v-if="scope.row.usePresidio">
      Presidio自动识别
    </el-tag>
    <el-tag type="info" v-else>
      自定义规则
    </el-tag>
    
    <el-tag type="warning" size="small" v-if="scope.row.autoDetectColumns">
      自动识别敏感列
    </el-tag>
  </template>
</el-table-column>
```

## 通信和数据模型变更

### 1. 数据模型更新

Task模型新增字段：
```typescript
interface Task {
  // 原有字段...
  usePresidio: boolean;
  autoDetectColumns: boolean;
  detectedColumns: string; // JSON字符串
}
```

敏感列数据结构：
```typescript
interface SensitiveColumn {
  columnName: string;
  tableName: string;
  dataType: string;
  sensitiveType: string;
  maskingRule: string;
  description: string;
  enabled: boolean;
}
```

### 2. API请求示例

预览敏感列：
```javascript
// 预览敏感列
async function previewSensitiveColumns(tableName) {
  try {
    const response = await axios.post('/api/presidio/detect-sensitive-columns', {
      tableName: tableName
    });
    return response.data;
  } catch (error) {
    console.error('预览敏感列失败', error);
    throw error;
  }
}
```

## 总结

1. 后端已实现使用Presidio自动识别敏感列的功能
2. 前端需要添加相应的UI元素和交互
3. 用户流程：
   - 用户创建任务，选择"使用Presidio"和"自动识别敏感列"
   - 可以点击"预览"按钮查看识别结果
   - 提交任务后，系统会在执行任务时自动识别敏感列并进行脱敏处理
   - 用户可以在任务详情页查看识别到的敏感列信息

如有任何问题或需要进一步的说明，请随时联系后端开发组。 