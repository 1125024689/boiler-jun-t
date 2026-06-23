# Transaction/Order 模块接口文档

## Base URL

```bash
BASE_URL="http://127.0.0.1:8080"
```

## 统一返回格式

```json
{
  "code": 1,          // 1=成功，0=失败
  "msg": null,        // 失败时的错误信息
  "data": {}          // 返回数据
}
```

## 状态枚举

### 帖子状态（postStatus）

| 状态 | 说明 |
|------|------|
| `AVAILABLE` | 可预约 |
| `BOOKED` | 已预约（锁定） |
| `SOLD` | 已售出 |
| `OFFLINE` | 已下架 |

### 交易状态（transactionStatus）

| 状态 | 说明 |
|------|------|
| `PENDING` | 待交易 |
| `ONGOING` | 交易中 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |

### 订单状态（orderStatus）

| 状态 | 说明 |
|------|------|
| `CREATED` | 已创建 |
| `CONFIRMED` | 已确认 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |

### 预约状态（bookingStatus）

| 状态 | 说明 |
|------|------|
| `BOOKED` | 已预约 |
| `CANCELLED` | 已取消 |

---

## 1. 买家预约帖子

### 接口说明

买家对某个帖子发起预约，系统创建交易记录和关联订单，并将帖子状态锁定为 `BOOKED`。

### 请求

```http
POST /transaction/book
Content-Type: application/json
```

```json
{
  "buyerId": "cb967d33836948b6895c5b8d693e3f33",
  "postId": "post001"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `buyerId` | String | 是 | 买家用户ID |
| `postId` | String | 是 | 帖子ID |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "transactionId": "330860f6185645f980faccffb46a7442",
    "buyerId": "cb967d33836948b6895c5b8d693e3f33",
    "sellerId": "046f8a5da7dc41e08c941994b8e10550",
    "postId": "post001",
    "postTitle": "测试锅炉帖子",
    "transactionAmount": null,
    "transactionTime": null,
    "transactionStatus": "PENDING",
    "bookingStatus": "BOOKED",
    "logisticsInfo": null,
    "orderId": "a1b2c3d4e5f6...",
    "orderStatus": "CREATED",
    "orderCreateTime": "2026-06-20",
    "orderUpdateTime": "2026-06-20"
  }
}
```

### 业务规则

- 买家必须是 `BUYER` 类型用户
- 帖子状态必须为 `AVAILABLE`
- 同一买家不能重复预约同一帖子
- 预约成功后帖子状态变为 `BOOKED`，其他买家无法预约

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 买家ID或帖子ID为空 | `买家ID和帖子ID不能为空` |
| 买家不存在 | `买家不存在` |
| 用户不是买家 | `当前用户不是买家` |
| 帖子不存在 | `帖子不存在` |
| 帖子不可预约 | `该帖子当前不可预约` |
| 重复预约 | `您已预约该帖子，请勿重复预约` |

---

## 2. 买家取消预约

### 接口说明

买家取消已发起的预约，交易和订单状态变为 `CANCELLED`，帖子恢复为 `AVAILABLE`。

### 请求

```http
PUT /transaction/cancel-booking
Content-Type: application/json
```

```json
{
  "transactionId": "330860f6185645f980faccffb46a7442",
  "buyerId": "cb967d33836948b6895c5b8d693e3f33"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `transactionId` | String | 是 | 交易ID |
| `buyerId` | String | 是 | 买家用户ID |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "transactionId": "330860f6185645f980faccffb46a7442",
    "transactionStatus": "CANCELLED",
    "bookingStatus": "CANCELLED",
    "orderStatus": "CANCELLED",
    "..."
  }
}
```

### 业务规则

- 只有买家本人可以取消预约
- 取消后帖子状态恢复为 `AVAILABLE`
- 取消后交易和订单状态变为 `CANCELLED`

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 交易ID或买家ID为空 | `交易ID和买家ID不能为空` |
| 交易不存在 | `交易不存在` |
| 非买家本人操作 | `只有买家本人可以取消预约` |
| 交易状态不允许取消 | `当前交易状态不允许取消预约` |

---

## 3. 卖家完成交易

### 接口说明

卖家确认交易完成，帖子状态变为 `SOLD`，卖家信用分 +2（上限100），完成交易数 +1。

### 请求

```http
PUT /transaction/complete
Content-Type: application/json
```

```json
{
  "transactionId": "89c4b1abc97949488b7c296d55a612f4",
  "sellerId": "046f8a5da7dc41e08c941994b8e10550"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `transactionId` | String | 是 | 交易ID |
| `sellerId` | String | 是 | 卖家用户ID |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "transactionId": "89c4b1abc97949488b7c296d55a612f4",
    "transactionStatus": "COMPLETED",
    "transactionTime": "2026-06-20",
    "orderStatus": "COMPLETED",
    "..."
  }
}
```

### 业务规则

- 只有该交易的卖家可以完成交易
- 交易状态必须为 `PENDING` 或 `ONGOING`
- 完成后帖子状态变为 `SOLD`
- 买卖双方的交易行为信用分各 +2（组件上限20）
- 总信用分 = 各组件之和（上限100）
- 卖家完成交易数 +1

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 交易ID或卖家ID为空 | `交易ID和卖家ID不能为空` |
| 交易不存在 | `交易不存在` |
| 非卖家本人操作 | `只有该交易的卖家可以完成交易` |
| 交易状态不允许完成 | `当前交易状态不允许完成交易` |

---

## 4. 卖家更新物流信息

### 接口说明

卖家为交易填写或更新物流信息。

### 请求

```http
PUT /transaction/logistics
Content-Type: application/json
```

```json
{
  "transactionId": "89c4b1abc97949488b7c296d55a612f4",
  "sellerId": "046f8a5da7dc41e08c941994b8e10550",
  "logisticsInfo": "SF1234567890"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `transactionId` | String | 是 | 交易ID |
| `sellerId` | String | 是 | 卖家用户ID |
| `logisticsInfo` | String | 是 | 物流信息（物流单号等） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "transactionId": "89c4b1abc97949488b7c296d55a612f4",
    "logisticsInfo": "SF1234567890",
    "..."
  }
}
```

### 业务规则

- 只有该交易的卖家可以更新物流信息
- 物流信息不能为空
- 已取消的交易不能更新物流信息
- 若交易状态为 `PENDING`，更新物流信息后状态流转为 `ONGOING`

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 交易ID或卖家ID为空 | `交易ID和卖家ID不能为空` |
| 物流信息为空 | `物流信息不能为空` |
| 交易不存在 | `交易不存在` |
| 非卖家本人操作 | `只有该交易的卖家可以更新物流信息` |

---

## 5. 查询交易详情

### 接口说明

根据交易ID查询交易详情（含订单信息）。

### 请求

```http
GET /transaction/{transactionId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `transactionId` | String | 是 | 交易ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "transactionId": "89c4b1abc97949488b7c296d55a612f4",
    "buyerId": "cb967d33836948b6895c5b8d693e3f33",
    "sellerId": "046f8a5da7dc41e08c941994b8e10550",
    "postId": "post001",
    "postTitle": "测试锅炉帖子",
    "transactionAmount": 50000.00,
    "transactionTime": "2026-06-20",
    "transactionStatus": "COMPLETED",
    "bookingStatus": "BOOKED",
    "logisticsInfo": "SF1234567890",
    "orderId": "a1b2c3d4e5f6...",
    "orderStatus": "COMPLETED",
    "orderCreateTime": "2026-06-20",
    "orderUpdateTime": "2026-06-20"
  }
}
```

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 交易不存在 | `交易不存在` |

---

## 6. 买家查询交易列表

### 接口说明

查询某个买家的所有交易记录。

### 请求

```http
GET /transaction/buyer/{buyerId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `buyerId` | String | 是 | 买家用户ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "transactionId": "89c4b1abc97949488b7c296d55a612f4",
      "buyerId": "cb967d33836948b6895c5b8d693e3f33",
      "sellerId": "046f8a5da7dc41e08c941994b8e10550",
      "postId": "post001",
      "postTitle": "测试锅炉帖子",
      "transactionStatus": "COMPLETED",
      "bookingStatus": "BOOKED",
      "orderStatus": "COMPLETED",
      "..."
    }
  ]
}
```

---

## 7. 卖家查询交易列表

### 接口说明

查询某个卖家的所有交易记录。

### 请求

```http
GET /transaction/seller/{sellerId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sellerId` | String | 是 | 卖家用户ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "transactionId": "89c4b1abc97949488b7c296d55a612f4",
      "buyerId": "cb967d33836948b6895c5b8d693e3f33",
      "sellerId": "046f8a5da7dc41e08c941994b8e10550",
      "postId": "post001",
      "postTitle": "测试锅炉帖子",
      "transactionStatus": "COMPLETED",
      "bookingStatus": "BOOKED",
      "orderStatus": "COMPLETED",
      "..."
    }
  ]
}
```

---

## 帖子状态流转图

```
AVAILABLE（可预约）
      │
      │ 买家预约
      ▼
BOOKED（已预约/锁定）
      │
      ├─ 买家取消预约 ──> AVAILABLE（恢复可预约）
      │
      └─ 卖家完成交易 ──> SOLD（已售出）
```

## 交易状态流转图

```
PENDING（待交易）
      │
      ├─ 买家取消预约 ──> CANCELLED（已取消）
      │
      ├─ 卖家更新物流 ──> ONGOING（交易中）
      │
      └─ 卖家完成交易 ──> COMPLETED（已完成）

ONGOING（交易中）
      │
      ├─ 买家取消预约 ──> CANCELLED（已取消）
      │
      └─ 卖家完成交易 ──> COMPLETED（已完成）
```

## curl 测试命令

```bash
# 设置 Base URL
BASE_URL="http://127.0.0.1:8080"

# 1. 买家预约帖子
curl -X POST "$BASE_URL/transaction/book" \
  -H "Content-Type: application/json" \
  -d '{
    "buyerId": "cb967d33836948b6895c5b8d693e3f33",
    "postId": "post001"
  }'

# 2. 买家取消预约
curl -X PUT "$BASE_URL/transaction/cancel-booking" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "330860f6185645f980faccffb46a7442",
    "buyerId": "cb967d33836948b6895c5b8d693e3f33"
  }'

# 3. 卖家完成交易
curl -X PUT "$BASE_URL/transaction/complete" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "89c4b1abc97949488b7c296d55a612f4",
    "sellerId": "046f8a5da7dc41e08c941994b8e10550"
  }'

# 4. 卖家更新物流信息
curl -X PUT "$BASE_URL/transaction/logistics" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "89c4b1abc97949488b7c296d55a612f4",
    "sellerId": "046f8a5da7dc41e08c941994b8e10550",
    "logisticsInfo": "SF1234567890"
  }'

# 5. 查询交易详情
curl -X GET "$BASE_URL/transaction/89c4b1abc97949488b7c296d55a612f4"

# 6. 买家查询交易列表
curl -X GET "$BASE_URL/transaction/buyer/cb967d33836948b6895c5b8d693e3f33"

# 7. 卖家查询交易列表
curl -X GET "$BASE_URL/transaction/seller/046f8a5da7dc41e08c941994b8e10550"
```

---

# Review 模块接口文档

## 评分规则

| 评分 | 等级 | 信用分变化 |
|------|------|-----------|
| 4-5 星 | 好评 | +3 |
| 1-2 星 | 差评 | -5 |

- 评分范围：1-5 分
- 信用分组件：互评信用分上限30，下限0
- 总信用分 = 各组件之和（上限100）
- 买家评价卖家后自动更新卖家好评率

## 信用分组件

| 组件 | 字段 | 初始值 | 上限 |
|------|------|--------|------|
| 押金支付 | depositPaymentScore | 0 | 20 |
| 信息完整度 | infoCompletenessScore | 0 | 20 |
| 互评信用 | mutualRatingScore | 15 | 30 |
| 交易行为 | transactionBehaviorScore | 10 | 20 |
| 社区行为 | communityConductScore | 10 | 10 |

---

## 8. 创建评价（双向）

### 接口说明

买家或卖家对已完成的交易进行评价。买家评价卖家，卖家评价买家，双方互评。

### 请求

```http
POST /review/create
Content-Type: application/json
```

```json
{
  "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
  "reviewerType": "BUYER",
  "postId": "post001",
  "orderId": "d7b6929800c6481597c8f40992afc2f7",
  "rating": 5,
  "content": "锅炉质量很好，卖家服务态度不错"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reviewerId` | String | 是 | 评价人用户ID |
| `reviewerType` | String | 是 | 评价人类型：`BUYER` 或 `SELLER` |
| `postId` | String | 是 | 帖子ID |
| `orderId` | String | 是 | 订单ID |
| `rating` | Integer | 是 | 评分（1-5） |
| `content` | String | 否 | 评价内容 |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "reviewId": "82b339a50ce84d6dbf96501e6f767cee",
    "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
    "reviewerName": "test_buyer_001",
    "reviewerType": "BUYER",
    "revieweeId": "046f8a5da7dc41e08c941994b8e10550",
    "revieweeName": "test_seller_001",
    "postId": "post001",
    "postTitle": "二手10吨燃气蒸汽锅炉",
    "orderId": "d7b6929800c6481597c8f40992afc2f7",
    "rating": 5,
    "content": "锅炉质量很好，卖家服务态度不错",
    "reviewTime": "2026-06-21"
  }
}
```

### 业务规则

- 评价人类型必须与用户类型匹配
- 帖子必须存在
- 订单必须为 `COMPLETED` 状态
- 买家评价时：被评价方为卖家，需校验评价人是该订单买家
- 卖家评价时：被评价方为买家，需校验评价人是该订单卖家
- 同一评价人不能对同一订单重复评价
- 评分必须在 1-5 之间
- 好评(4-5星)：被评价人互评信用分 +3（上限30）
- 差评(1-2星)：被评价人互评信用分 -5（下限0）
- 买家评价卖家后自动更新卖家好评率

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 评价人ID、类型、帖子ID、订单ID或评分为空 | `评价人ID、评价人类型、帖子ID、订单ID和评分不能为空` |
| 评分超出范围 | `评分必须在1-5之间` |
| 用户不存在 | `用户不存在` |
| 评价人类型与用户类型不匹配 | `评价人类型与用户类型不匹配` |
| 帖子不存在 | `帖子不存在` |
| 订单不存在 | `订单不存在` |
| 订单未完成 | `订单未完成，不能评价` |
| 交易不存在 | `交易不存在` |
| 非该订单买家/卖家 | `只有该订单的买家/卖家可以评价` |
| 不支持的评价人类型 | `不支持的评价人类型: xxx` |
| 重复评价 | `您已对此订单进行过评价` |

---

## 9. 查询评价详情

### 接口说明

根据评价ID查询评价详情。

### 请求

```http
GET /review/{reviewId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reviewId` | String | 是 | 评价ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "reviewId": "82b339a50ce84d6dbf96501e6f767cee",
    "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
    "reviewerName": "test_buyer_001",
    "reviewerType": "BUYER",
    "revieweeId": "046f8a5da7dc41e08c941994b8e10550",
    "revieweeName": "test_seller_001",
    "postId": "post001",
    "postTitle": "二手10吨燃气蒸汽锅炉",
    "orderId": "d7b6929800c6481597c8f40992afc2f7",
    "rating": 5,
    "content": "锅炉质量很好，卖家服务态度不错",
    "reviewTime": "2026-06-21"
  }
}
```

### 错误场景

| 场景 | 返回 msg |
|------|---------|
| 评价不存在 | `评价不存在` |

---

## 10. 查询帖子评价列表

### 接口说明

查询某个帖子的所有评价，按评价时间倒序排列。

### 请求

```http
GET /review/post/{postId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | String | 是 | 帖子ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "reviewId": "82b339a50ce84d6dbf96501e6f767cee",
      "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
      "reviewerName": "test_buyer_001",
      "reviewerType": "BUYER",
      "revieweeId": "046f8a5da7dc41e08c941994b8e10550",
      "revieweeName": "test_seller_001",
      "postId": "post001",
      "postTitle": "二手10吨燃气蒸汽锅炉",
      "orderId": "d7b6929800c6481597c8f40992afc2f7",
      "rating": 5,
      "content": "锅炉质量很好，卖家服务态度不错",
      "reviewTime": "2026-06-21"
    }
  ]
}
```

---

## 11. 查询评价人发表的评价列表

### 接口说明

查询某个评价人发表的所有评价，按评价时间倒序排列。

### 请求

```http
GET /review/reviewer/{reviewerId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reviewerId` | String | 是 | 评价人用户ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "reviewId": "82b339a50ce84d6dbf96501e6f767cee",
      "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
      "reviewerName": "test_buyer_001",
      "reviewerType": "BUYER",
      "revieweeId": "046f8a5da7dc41e08c941994b8e10550",
      "revieweeName": "test_seller_001",
      "postId": "post001",
      "postTitle": "二手10吨燃气蒸汽锅炉",
      "orderId": "d7b6929800c6481597c8f40992afc2f7",
      "rating": 5,
      "content": "锅炉质量很好，卖家服务态度不错",
      "reviewTime": "2026-06-21"
    }
  ]
}
```

---

## 12. 查询被评价人收到的评价列表

### 接口说明

查询某个被评价人收到的所有评价，按评价时间倒序排列。可用于卖家查看自己收到的评价。

### 请求

```http
GET /review/reviewee/{revieweeId}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `revieweeId` | String | 是 | 被评价人用户ID（路径参数） |

### 响应

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "reviewId": "82b339a50ce84d6dbf96501e6f767cee",
      "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
      "reviewerName": "test_buyer_001",
      "reviewerType": "BUYER",
      "revieweeId": "046f8a5da7dc41e08c941994b8e10550",
      "revieweeName": "test_seller_001",
      "postId": "post001",
      "postTitle": "二手10吨燃气蒸汽锅炉",
      "orderId": "d7b6929800c6481597c8f40992afc2f7",
      "rating": 5,
      "content": "锅炉质量很好，卖家服务态度不错",
      "reviewTime": "2026-06-21"
    }
  ]
}
```

---

## Review 模块 curl 测试命令

```bash
# 8. 创建评价（买家评价卖家）
curl -X POST "$BASE_URL/review/create" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewerId": "cb967d33836948b6895c5b8d693e3f33",
    "reviewerType": "BUYER",
    "postId": "post001",
    "orderId": "d7b6929800c6481597c8f40992afc2f7",
    "rating": 5,
    "content": "锅炉质量很好，卖家服务态度不错"
  }'

# 8b. 创建评价（卖家评价买家）
curl -X POST "$BASE_URL/review/create" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewerId": "046f8a5da7dc41e08c941994b8e10550",
    "reviewerType": "SELLER",
    "postId": "post001",
    "orderId": "d7b6929800c6481597c8f40992afc2f7",
    "rating": 4,
    "content": "买家付款及时，交易顺畅"
  }'

# 9. 查询评价详情
curl -X GET "$BASE_URL/review/82b339a50ce84d6dbf96501e6f767cee"

# 10. 查询帖子评价列表
curl -X GET "$BASE_URL/review/post/post001"

# 11. 查询评价人发表的评价列表
curl -X GET "$BASE_URL/review/reviewer/cb967d33836948b6895c5b8d693e3f33"

# 12. 查询被评价人收到的评价列表（卖家查看收到的评价）
curl -X GET "$BASE_URL/review/reviewee/046f8a5da7dc41e08c941994b8e10550"
```
