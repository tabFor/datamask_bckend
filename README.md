# 数据脱敏平台

本项目是一个数据脱敏平台，支持不同角色的用户访问不同功能。

## 系统角色说明

系统支持三种角色：

| 角色名称 | 角色描述 | 
|---------|--------|
| 管理员 (ADMIN) | 系统管理员，拥有所有权限 |
| 数据分析师 (DATA_ANALYST) | 可以查看和分析脱敏后的数据 |
| 数据操作员 (DATA_OPERATOR) | 可以执行数据脱敏操作 |

## API文档

启动项目后，可以通过以下地址访问Swagger API文档：
```
http://localhost:8081/swagger-ui.html
```

## 前端接口使用指南

### 用户登录

**POST** `/login`

请求体：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

成功响应：
```json
{
  "message": "登录成功",
  "status": "success",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "role": "ADMIN",
  "username": "admin"
}
```

失败响应：
```json
{
  "message": "登录失败",
  "status": "failure"
}
```

### 检查登录状态

**GET** `/api/check-login`

请求头：
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

成功响应：
```json
{
  "isLoggedIn": true,
  "username": "admin",
  "role": "ADMIN"
}
```

未登录或Token无效：
```json
{
  "isLoggedIn": false
}
```

### 数据分析师专用接口

#### 1. 获取脱敏数据文件列表

**GET** `/api/masked-data/files`

请求头：
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

成功响应：
```json
[
  {
    "path": "masked_data/sample1.csv",
    "name": "sample1.csv",
    "size": "2.50 MB",
    "sizeBytes": 2621440,
    "lastModified": "2025-04-01T10:30:15.000+0000",
    "type": "csv"
  },
  {
    "path": "masked_data/sample2.json",
    "name": "sample2.json",
    "size": "1.75 MB",
    "sizeBytes": 1835008,
    "lastModified": "2025-04-01T11:45:32.000+0000",
    "type": "json"
  }
]
```

#### 2. 预览脱敏数据

**GET** `/api/masked-data/preview?filePath={filePath}&page={page}&size={size}`

参数：
- `filePath`: 文件路径
- `page`: 页码（从0开始，默认0）
- `size`: 每页记录数（默认10）

请求头：
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

CSV文件响应示例：
```json
{
  "data": [
    {
      "name": "张**",
      "age": "28",
      "phone": "138****8001",
      "email": "z****@example.com"
    },
    ...
  ],
  "headers": ["name", "age", "phone", "email"],
  "total": 100,
  "page": 0,
  "size": 10,
  "pages": 10,
  "filePath": "masked_data/sample1.csv",
  "fileName": "sample1.csv",
  "fileType": "csv"
}
```

#### 3. 下载脱敏数据文件

**GET** `/api/masked-data/download?filePath={filePath}`

参数：
- `filePath`: 文件路径

请求头：
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

成功响应：
直接返回文件内容，通过设置的HTTP头`Content-Disposition: attachment`触发浏览器下载。

#### 4. 查询脱敏数据库表

**GET** `/api/masked-data/db-query?tableName={tableName}&page={page}&size={size}&conditions={conditions}`

参数：
- `tableName`: 表名
- `page`: 页码（从0开始，默认0）
- `size`: 每页记录数（默认10）
- `conditions`: 查询条件，JSON格式（可选）

请求头：
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

成功响应：
```json
{
  "data": [
    {
      "id": 1,
      "name": "张**",
      "gender": "男",
      "age": 28,
      "phone": "138****8001"
    },
    ...
  ],
  "columns": [
    {
      "Field": "id",
      "Type": "int(11)",
      "Null": "NO",
      "Key": "PRI",
      "Default": null,
      "Extra": "auto_increment"
    },
    ...
  ],
  "total": 100,
  "page": 0,
  "size": 10,
  "pages": 10,
  "tableName": "customer_info"
}
```

#### 5. 获取可用的脱敏数据库表

**GET** `/api/masked-data/db-tables`

请求头：
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

成功响应：
```json
[
  "customer_info",
  "financial_records",
  "medical_records",
  "employee_data",
  "online_transactions"
]
```

## 前端开发指南

### 1. 登录流程

1. 用户输入用户名和密码，调用登录接口
2. 登录成功后获取token和角色信息
3. 将token存储在localStorage或sessionStorage中
4. 根据角色信息展示对应的功能菜单

### 2. 权限控制

前端可以根据用户角色实现以下权限控制：

- **管理员(ADMIN)**: 显示所有功能模块
- **数据分析师(DATA_ANALYST)**: 仅显示数据查看和分析相关模块
- **数据操作员(DATA_OPERATOR)**: 仅显示数据脱敏操作相关模块

### 3. API请求认证

对于需要认证的API请求，前端需要在请求头中添加token：

```javascript
// 示例代码 (使用axios)
const token = localStorage.getItem('token');

axios.get('/api/some-protected-endpoint', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => {
  // 处理响应
})
.catch(error => {
  // 处理错误
});
```

### 4. 默认账号

系统初始化了三个默认账号，分别对应不同角色：

| 用户名 | 密码 | 角色 |
|-------|------|-----|
| admin | admin123 | 管理员 |
| analyst | analyst123 | 数据分析师 |
| operator | operator123 | 数据操作员 |

## 注意事项

1. 生产环境中请务必修改密码和JWT密钥
2. 对于敏感操作，前端应同时进行权限判断，后端也会进行权限验证
3. Token有效期为1天，过期后需要重新登录
4. 数据分析师可以使用专门的接口查看和下载脱敏后的数据，但不能执行脱敏操作
5. 数据操作员可以执行脱敏操作，但不能查看脱敏后的数据 