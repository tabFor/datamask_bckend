# 角色权限重构总结

## 重构内容

我们对项目进行了角色权限系统的重构，主要变更如下：

1. 用户模型（User）添加了角色字段（Role），支持三种角色：
   - 管理员（ADMIN）：系统管理员，拥有所有权限
   - 数据分析师（DATA_ANALYST）：可以查看和分析脱敏后的数据
   - 数据操作员（DATA_OPERATOR）：可以执行数据脱敏操作

2. 登录接口（/login）现在会返回用户角色信息，前端可以据此进行权限控制

3. JWT令牌中包含了用户角色信息，后端可以用它来验证用户权限

4. 添加了Swagger API文档支持，方便前端开发人员查看和测试API

5. 为数据分析师角色添加了特定的数据查看和下载接口

## 前端调用示例

### 登录接口

```javascript
// 登录请求
fetch('/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'admin',
    password: 'admin123'
  })
})
.then(response => response.json())
.then(data => {
  if (data.status === 'success') {
    // 存储token
    localStorage.setItem('token', data.token);
    
    // 存储用户角色
    localStorage.setItem('userRole', data.role);
    
    // 根据角色显示不同的功能菜单
    displayMenuByRole(data.role);
  } else {
    // 登录失败处理
    showErrorMessage(data.message);
  }
});

// 根据角色显示菜单
function displayMenuByRole(role) {
  const allMenus = document.querySelectorAll('.menu-item');
  
  // 隐藏所有菜单
  allMenus.forEach(menu => menu.style.display = 'none');
  
  // 根据角色显示对应菜单
  switch(role) {
    case 'ADMIN':
      // 管理员显示所有菜单
      allMenus.forEach(menu => menu.style.display = 'block');
      break;
    case 'DATA_ANALYST':
      // 数据分析师只显示数据查看和分析菜单
      document.querySelectorAll('.menu-item.data-view, .menu-item.data-analysis')
        .forEach(menu => menu.style.display = 'block');
      break;
    case 'DATA_OPERATOR':
      // 数据操作员只显示数据脱敏操作菜单
      document.querySelectorAll('.menu-item.data-operation')
        .forEach(menu => menu.style.display = 'block');
      break;
  }
}
```

### 发送带权限的请求

```javascript
// 获取保存的token
const token = localStorage.getItem('token');

// 发送带认证的请求
fetch('/api/some-protected-endpoint', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => {
  // 处理响应数据
});
```

## 数据分析师专用接口

数据分析师可以使用以下接口来查看和下载脱敏后的数据：

### 1. 获取脱敏数据文件列表

```javascript
// 获取所有可用的脱敏数据文件
fetch('/api/masked-data/files', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(files => {
  // 处理文件列表
  console.log(files);
});
```

### 2. 预览脱敏数据

```javascript
// 预览脱敏数据（支持分页）
fetch(`/api/masked-data/preview?filePath=masked_data/file.csv&page=0&size=10`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => {
  // 处理数据预览结果
  console.log(data.headers); // 表头
  console.log(data.data);    // 数据
  console.log(data.total);   // 总记录数
});
```

### 3. 下载脱敏数据文件

```javascript
// 下载脱敏数据文件
function downloadMaskedFile(filePath) {
  const downloadUrl = `/api/masked-data/download?filePath=${encodeURIComponent(filePath)}`;
  
  // 创建一个隐藏的a标签并触发下载
  const a = document.createElement('a');
  a.style.display = 'none';
  a.href = downloadUrl;
  
  // 添加授权头（需要使用Blob或其他方式，这里简化处理）
  // 实际使用时可能需要其他下载方式来支持添加Authorization头
  a.download = filePath.split('/').pop();
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}
```

### 4. 查询脱敏数据库表

```javascript
// 查询脱敏数据库表（支持分页和条件查询）
const tableName = 'customer_info';
const page = 0;
const size = 10;
const conditions = JSON.stringify({ age: 35 }); // 可选的查询条件

fetch(`/api/masked-data/db-query?tableName=${tableName}&page=${page}&size=${size}&conditions=${encodeURIComponent(conditions)}`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(result => {
  // 处理查询结果
  console.log(result.data);    // 当前页数据
  console.log(result.total);   // 总记录数
  console.log(result.columns); // 表结构信息
});
```

### 5. 获取可用的脱敏数据表列表

```javascript
// 获取所有可查询的脱敏表
fetch('/api/masked-data/db-tables', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(tables => {
  // 处理表列表
  console.log(tables);
});
```

## 注意事项

1. 所有需要权限控制的API请求都需要在请求头中添加Bearer Token
2. 前端应该根据用户角色限制用户访问不同的功能模块
3. 对于特定角色才能访问的页面，建议在路由守卫中进行权限检查
4. 请使用项目根目录下的README.md查看更详细的API使用指南
5. API文档可以通过 http://localhost:8081/swagger-ui.html 访问

## 系统默认用户

系统初始化了三个不同角色的用户，便于测试：

| 用户名 | 密码 | 角色 |
|-------|------|------|
| admin | admin123 | 管理员 |
| analyst | analyst123 | 数据分析师 |
| operator | operator123 | 数据操作员 | 