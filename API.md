# 数据脱敏系统API接口文档

## 目录

- [1. 用户认证](#1-用户认证)
  - [1.1 用户登录](#11-用户登录)
  - [1.2 检查登录状态](#12-检查登录状态)
- [2. 数据库管理](#2-数据库管理)
  - [2.1 测试数据库连接](#21-测试数据库连接)
  - [2.2 获取数据库表列表](#22-获取数据库表列表)
  - [2.3 获取表的列信息](#23-获取表的列信息)
- [3. 脱敏规则管理](#3-脱敏规则管理)
  - [3.1 获取所有脱敏规则](#31-获取所有脱敏规则)
  - [3.2 获取指定脱敏规则](#32-获取指定脱敏规则)
  - [3.3 创建脱敏规则](#33-创建脱敏规则)
  - [3.4 更新脱敏规则](#34-更新脱敏规则)
  - [3.5 删除脱敏规则](#35-删除脱敏规则)
- [4. 脱敏任务管理](#4-脱敏任务管理)
  - [4.1 获取任务列表](#41-获取任务列表)
  - [4.2 获取任务详情](#42-获取任务详情)
  - [4.3 创建脱敏任务](#43-创建脱敏任务)
  - [4.4 执行脱敏任务](#44-执行脱敏任务)
  - [4.5 删除脱敏任务](#45-删除脱敏任务)
- [5. 脱敏数据查询](#5-脱敏数据查询)
  - [5.1 获取脱敏后的用户数据](#51-获取脱敏后的用户数据)
  - [5.2 获取脱敏后的订单数据](#52-获取脱敏后的订单数据)

## 1. 用户认证

### 1.1 用户登录

- **接口URL**: `/login`
- **请求方式**: POST
- **接口描述**: 用户登录接口，验证用户名和密码

**请求参数**:

| 参数名   | 类型   | 是否必须 | 描述     |
| -------- | ------ | -------- | -------- |
| username | String | 是       | 用户名   |
| password | String | 是       | 用户密码 |

**请求示例**:

```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应参数**:

| 参数名  | 类型   | 描述                       |
| ------- | ------ | -------------------------- |
| message | String | 登录结果消息               |
| status  | String | 登录状态：success/failure  |
| token   | String | JWT令牌，登录成功时才返回  |

**响应示例**:

```json
{
  "message": "登录成功",
  "status": "success",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 1.2 检查登录状态

- **接口URL**: `/api/check-login`
- **请求方式**: GET
- **接口描述**: 检查用户是否已登录

**请求头**:

| 参数名        | 类型   | 是否必须 | 描述                            |
| ------------- | ------ | -------- | ------------------------------- |
| Authorization | String | 是       | Bearer token (JWT令牌)          |

**响应参数**:

| 参数名     | 类型    | 描述           |
| ---------- | ------- | -------------- |
| isLoggedIn | Boolean | 是否已登录     |
| username   | String  | 登录用户的用户名，仅在已登录时返回 |

**响应示例**:

```json
{
  "isLoggedIn": true,
  "username": "admin"
}
```

## 2. 数据库管理

### 2.1 测试数据库连接

- **接口URL**: `/api/database/test-connection`
- **请求方式**: POST
- **接口描述**: 测试数据库连接是否成功

**请求参数**:

| 参数名   | 类型   | 是否必须 | 描述         |
| -------- | ------ | -------- | ------------ |
| dbType   | String | 是       | 数据库类型，支持mysql/postgresql/oracle/sqlserver |
| host     | String | 是       | 数据库主机地址 |
| port     | String | 是       | 数据库端口   |
| dbName   | String | 是       | 数据库名称   |
| username | String | 是       | 数据库用户名 |
| password | String | 是       | 数据库密码   |

**请求示例**:

```json
{
  "dbType": "mysql",
  "host": "localhost",
  "port": "3306",
  "dbName": "test_db",
  "username": "root",
  "password": "123456"
}
```

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | String  | 成功时返回"连接成功" |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": "连接成功"
}
```

### 2.2 获取数据库表列表

- **接口URL**: `/api/database/tables`
- **请求方式**: GET
- **接口描述**: 获取指定数据库中的表列表

**请求参数**:

| 参数名   | 类型   | 是否必须 | 描述         |
| -------- | ------ | -------- | ------------ |
| dbType   | String | 是       | 数据库类型   |
| host     | String | 是       | 数据库主机地址 |
| port     | String | 是       | 数据库端口   |
| dbName   | String | 是       | 数据库名称   |
| username | String | 是       | 数据库用户名 |
| password | String | 是       | 数据库密码   |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 返回数据       |
| data.tables | Array | 表名列表      |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "tables": ["users", "orders", "products"]
  }
}
```

### 2.3 获取表的列信息

- **接口URL**: `/api/database/columns`
- **请求方式**: GET
- **接口描述**: 获取指定表的列信息

**请求参数**:

| 参数名   | 类型   | 是否必须 | 描述         |
| -------- | ------ | -------- | ------------ |
| dbType   | String | 是       | 数据库类型   |
| host     | String | 是       | 数据库主机地址 |
| port     | String | 是       | 数据库端口   |
| dbName   | String | 是       | 数据库名称   |
| username | String | 是       | 数据库用户名 |
| password | String | 是       | 数据库密码   |
| tableName | String | 是      | 表名         |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 返回数据       |
| data.columns | Array | 列名列表     |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "columns": ["id", "username", "email", "phone", "address"]
  }
}
```

## 3. 脱敏规则管理

### 3.1 获取所有脱敏规则

- **接口URL**: `/api/rules`
- **请求方式**: GET
- **接口描述**: 获取所有可用的脱敏规则

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 返回数据       |
| data.rules | Array | 脱敏规则列表   |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "rules": [
      {
        "id": "phone_mask",
        "name": "手机号码脱敏",
        "description": "保留前3位和后4位，中间用*替代",
        "type": "PHONE",
        "pattern": "KEEP_PREFIX_SUFFIX",
        "prefixLength": 3,
        "suffixLength": 4,
        "replacementChar": "*"
      },
      {
        "id": "id_card_mask",
        "name": "身份证号脱敏",
        "description": "保留前6位和后4位，中间用*替代",
        "type": "ID_CARD",
        "pattern": "KEEP_PREFIX_SUFFIX",
        "prefixLength": 6,
        "suffixLength": 4,
        "replacementChar": "*"
      }
    ]
  }
}
```

### 3.2 获取指定脱敏规则

- **接口URL**: `/api/rules/{id}`
- **请求方式**: GET
- **接口描述**: 根据ID获取指定的脱敏规则

**路径参数**:

| 参数名 | 类型   | 是否必须 | 描述     |
| ------ | ------ | -------- | -------- |
| id     | String | 是       | 规则ID   |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 脱敏规则详情   |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": "phone_mask",
    "name": "手机号码脱敏",
    "description": "保留前3位和后4位，中间用*替代",
    "type": "PHONE",
    "pattern": "KEEP_PREFIX_SUFFIX",
    "prefixLength": 3,
    "suffixLength": 4,
    "replacementChar": "*"
  }
}
```

### 3.3 创建脱敏规则

- **接口URL**: `/api/rules`
- **请求方式**: POST
- **接口描述**: 创建新的脱敏规则

**请求参数**:

| 参数名          | 类型   | 是否必须 | 描述         |
| --------------- | ------ | -------- | ------------ |
| name            | String | 是       | 规则名称     |
| description     | String | 否       | 规则描述     |
| type            | String | 是       | 规则类型     |
| pattern         | String | 是       | 脱敏模式     |
| prefixLength    | Integer| 否       | 保留前缀长度 |
| suffixLength    | Integer| 否       | 保留后缀长度 |
| replacementChar | String | 否       | 替换字符     |

**请求示例**:

```json
{
  "name": "银行卡号脱敏",
  "description": "仅显示后4位，其余用*替代",
  "type": "BANK_CARD",
  "pattern": "KEEP_SUFFIX",
  "prefixLength": 0,
  "suffixLength": 4,
  "replacementChar": "*"
}
```

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 创建的脱敏规则 |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": "bank_card_mask",
    "name": "银行卡号脱敏",
    "description": "仅显示后4位，其余用*替代",
    "type": "BANK_CARD",
    "pattern": "KEEP_SUFFIX",
    "prefixLength": 0,
    "suffixLength": 4,
    "replacementChar": "*"
  }
}
```

### 3.4 更新脱敏规则

- **接口URL**: `/api/rules/{id}`
- **请求方式**: PUT
- **接口描述**: 更新指定的脱敏规则

**路径参数**:

| 参数名 | 类型   | 是否必须 | 描述     |
| ------ | ------ | -------- | -------- |
| id     | String | 是       | 规则ID   |

**请求参数**:

| 参数名          | 类型   | 是否必须 | 描述         |
| --------------- | ------ | -------- | ------------ |
| name            | String | 否       | 规则名称     |
| description     | String | 否       | 规则描述     |
| type            | String | 否       | 规则类型     |
| pattern         | String | 否       | 脱敏模式     |
| prefixLength    | Integer| 否       | 保留前缀长度 |
| suffixLength    | Integer| 否       | 保留后缀长度 |
| replacementChar | String | 否       | 替换字符     |

**请求示例**:

```json
{
  "name": "银行卡号脱敏规则",
  "description": "仅显示后6位，其余用#替代",
  "suffixLength": 6,
  "replacementChar": "#"
}
```

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 更新后的脱敏规则 |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": "bank_card_mask",
    "name": "银行卡号脱敏规则",
    "description": "仅显示后6位，其余用#替代",
    "type": "BANK_CARD",
    "pattern": "KEEP_SUFFIX",
    "prefixLength": 0,
    "suffixLength": 6,
    "replacementChar": "#"
  }
}
```

### 3.5 删除脱敏规则

- **接口URL**: `/api/rules/{id}`
- **请求方式**: DELETE
- **接口描述**: 删除指定的脱敏规则

**路径参数**:

| 参数名 | 类型   | 是否必须 | 描述     |
| ------ | ------ | -------- | -------- |
| id     | String | 是       | 规则ID   |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | String  | 成功时返回"脱敏规则删除成功" |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": "脱敏规则删除成功"
}
```

## 4. 脱敏任务管理

### 4.1 获取任务列表

- **接口URL**: `/api/tasks`
- **请求方式**: GET
- **接口描述**: 获取脱敏任务列表，支持分页和关键词搜索

**请求参数**:

| 参数名   | 类型    | 是否必须 | 描述         | 默认值 |
| -------- | ------- | -------- | ------------ | ------ |
| page     | Integer | 否       | 页码         | 1      |
| pageSize | Integer | 否       | 每页记录数   | 10     |
| keyword  | String  | 否       | 搜索关键词   | 无     |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 返回数据       |
| data.tasks | Array | 任务列表      |
| data.total | Integer | 总记录数    |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "tasks": [
      {
        "id": 1,
        "taskName": "用户数据脱敏任务",
        "status": "已完成",
        "progress": 100,
        "createTime": "2023-03-01 10:00:00",
        "executeTime": "2023-03-01 10:05:30",
        "sourceDatabase": "test_db",
        "sourceTables": "users",
        "taskDescription": "用户表敏感信息脱敏"
      },
      {
        "id": 2,
        "taskName": "订单数据脱敏任务",
        "status": "进行中",
        "progress": 60,
        "createTime": "2023-03-02 14:30:00",
        "sourceDatabase": "test_db",
        "sourceTables": "orders",
        "taskDescription": "订单表敏感信息脱敏"
      }
    ],
    "total": 2
  }
}
```

### 4.2 获取任务详情

- **接口URL**: `/api/tasks/{id}`
- **请求方式**: GET
- **接口描述**: 获取指定任务的详细信息

**路径参数**:

| 参数名 | 类型    | 是否必须 | 描述     |
| ------ | ------- | -------- | -------- |
| id     | Integer | 是       | 任务ID   |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 任务详情       |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "taskName": "用户数据脱敏任务",
    "status": "已完成",
    "progress": 100,
    "createTime": "2023-03-01 10:00:00",
    "executeTime": "2023-03-01 10:05:30",
    "sourceDatabase": "test_db",
    "sourceTables": "users",
    "taskDescription": "用户表敏感信息脱敏"
  }
}
```

### 4.3 创建脱敏任务

- **接口URL**: `/api/tasks`
- **请求方式**: POST
- **接口描述**: 创建新的脱敏任务

**请求参数**:

| 参数名          | 类型   | 是否必须 | 描述         |
| --------------- | ------ | -------- | ------------ |
| taskName        | String | 是       | 任务名称     |
| taskDescription | String | 否       | 任务描述     |
| sourceDatabase  | String | 是       | 源数据库     |
| sourceTables    | String | 是       | 源表名       |
| maskingRules    | Array  | 是       | 脱敏规则配置 |

**请求示例**:

```json
{
  "taskName": "用户数据脱敏任务",
  "taskDescription": "用户表敏感信息脱敏",
  "sourceDatabase": "test_db",
  "sourceTables": "users",
  "maskingRules": [
    {
      "columnName": "phone",
      "ruleId": "phone_mask"
    },
    {
      "columnName": "idCard",
      "ruleId": "id_card_mask"
    }
  ]
}
```

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 创建的任务信息 |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 3,
    "taskName": "用户数据脱敏任务",
    "status": "待执行",
    "progress": 0,
    "createTime": "2023-03-03 15:20:00",
    "sourceDatabase": "test_db",
    "sourceTables": "users",
    "taskDescription": "用户表敏感信息脱敏"
  }
}
```

### 4.4 执行脱敏任务

- **接口URL**: `/api/tasks/{id}/execute`
- **请求方式**: POST
- **接口描述**: 执行指定的脱敏任务

**路径参数**:

| 参数名 | 类型    | 是否必须 | 描述     |
| ------ | ------- | -------- | -------- |
| id     | Integer | 是       | 任务ID   |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Object  | 任务执行状态   |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 3,
    "taskName": "用户数据脱敏任务",
    "status": "进行中",
    "progress": 0,
    "createTime": "2023-03-03 15:20:00",
    "executeTime": "2023-03-03 15:25:10",
    "sourceDatabase": "test_db",
    "sourceTables": "users",
    "taskDescription": "用户表敏感信息脱敏"
  }
}
```

### 4.5 删除脱敏任务

- **接口URL**: `/api/tasks/{id}`
- **请求方式**: DELETE
- **接口描述**: 删除指定的脱敏任务

**路径参数**:

| 参数名 | 类型    | 是否必须 | 描述     |
| ------ | ------- | -------- | -------- |
| id     | Integer | 是       | 任务ID   |

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | String  | 成功时返回"任务删除成功" |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": "任务删除成功"
}
```

## 5. 脱敏数据查询

### 5.1 获取脱敏后的用户数据

- **接口URL**: `/api/masked-data/users`
- **请求方式**: GET
- **接口描述**: 获取经过脱敏处理的用户数据

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Array   | 脱敏后的用户数据列表 |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "username": "z***san",
      "email": "z****@example.com",
      "phone": "138****8001",
      "idCard": "320123******0101",
      "address": "北京市朝阳区******",
      "createTime": "2023-01-01 10:00:00",
      "updateTime": "2023-01-10 15:30:00",
      "status": 1
    },
    {
      "id": 2,
      "username": "l**i",
      "email": "l****@example.com",
      "phone": "139****9002",
      "idCard": "320123******0202",
      "address": "上海市浦东新区******",
      "createTime": "2023-02-01 09:30:00",
      "updateTime": "2023-02-15 14:20:00",
      "status": 1
    }
  ]
}
```

### 5.2 获取脱敏后的订单数据

- **接口URL**: `/api/masked-data/orders`
- **请求方式**: GET
- **接口描述**: 获取经过脱敏处理的订单数据

**响应参数**:

| 参数名  | 类型    | 描述           |
| ------- | ------- | -------------- |
| success | Boolean | 是否成功       |
| data    | Array   | 脱敏后的订单数据列表 |
| message | String  | 失败时返回错误信息 |

**响应示例**:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "orderNo": "ORD20230101001",
      "receiverName": "张*",
      "receiverPhone": "138****8001",
      "receiverAddress": "北京市朝阳区******",
      "createTime": "2023-01-05 14:30:00",
      "status": "已完成",
      "amount": 299.50
    },
    {
      "id": 2,
      "orderNo": "ORD20230215002",
      "receiverName": "李*",
      "receiverPhone": "139****9002",
      "receiverAddress": "上海市浦东新区******",
      "createTime": "2023-02-15 10:20:00",
      "status": "已发货",
      "amount": 599.90
    }
  ]
}
``` 