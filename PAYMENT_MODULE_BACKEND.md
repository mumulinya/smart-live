# 支付模块后端设计文档 (WeChat Pay)

本文档旨在设计一套通用的支付系统，统一处理 **钱包充值**、**订单支付** 等多种业务场景的微信支付对接。后端开发者应按照此文档实现通用支付接口。

## 1. 核心设计思想

采用 **策略模式** + **统一支付网关** 的设计：
1.  **统一入口**: 前端只需要调用一个 `create` 接口，传入 `bizType` (业务类型) 和 `bizId` (业务ID)。
2.  **支付记录**: 所有发起支付的请求，先在 `payment_record` 表落库，生成全局唯一的 `pay_sn` (支付流水号)。
3.  **调用三方**: 后端使用 `pay_sn` 调用微信统一下单接口。
4.  **统一回调**: 微信回调只通知支付模块，支付模块根据 `pay_sn` 找到对应的业务回调处理逻辑（如：充值成功加余额，订单支付成功更新订单状态）。

---

## 2. 数据库设计

### 支付流水表 (`payment_record`)

```sql
CREATE TABLE `payment_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pay_sn` varchar(64) NOT NULL COMMENT '支付流水号(传给微信的out_trade_no)',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型: recharge(充值), order(订单)',
  `biz_id` varchar(64) NOT NULL COMMENT '业务ID(充值单号/订单号)',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_method` varchar(16) NOT NULL DEFAULT 'wechat' COMMENT 'wechat/alipay',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0:待支付 1:支付成功 2:支付失败',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '第三方支付单号(微信返回)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_sn` (`pay_sn`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
```

---

## 3. API 接口定义

建议前缀: `/app-dev-api/app/pay`

### 3.1 统一下单接口

前端在收银台页面选择“微信支付”后调用此接口。

- **URL**: `/app/pay/unified`
- **Method**: `POST`
- **Body**:

```json
{
  "bizType": "recharge",  // 业务类型: recharge | order
  "bizId": "R20260217001", // 业务ID: 充值单号 或 订单号
  "payMethod": "wechat",   // 支付方式
  "appType": "h5"          // 终端类型: app | h5 | miniapp
}
```

- **Response**:

```json
{
  "success": true,
  "code": 200,
  "data": {
    "paySn": "P202602171000001",
    // 针对 H5 支付，返回 mwebUrl
    "mwebUrl": "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?...",
    // 针对 App 支付，返回 SDK 参数
    "payParams": {
      "appid": "wx8888888888",
      "partnerid": "1900000109",
      "prepayid": "WX1217752501201407033233368018",
      "package": "Sign=WXPay",
      "noncestr": "5K8264ILTKCH16CQ2502SI8ZNMTM67VS",
      "timestamp": "1412000000",
      "sign": "C380BEC2BFD727A4B6845133519F3AD6"
    }
  }
}
```

### 3.2 支付结果查询

前端轮询支付状态。

- **URL**: `/app/pay/status`
- **Method**: `GET`
- **Query**: `paySn=P202602171000001`
- **Response**:

```json
{
  "success": true,
  "data": {
    "status": 1 // 0:待支付 1:成功 2:失败
  }
}
```

### 3.3 微信支付回调 (Notify)

配置在微信商户后台的回调地址，例如 `https://api.domain.com/app/pay/callback/wechat`.

- **URL**: `/app/pay/callback/wechat`
- **Method**: `POST`
- **Body**: XML 数据

---

## 4. 后端开发指南 (Java/Node.js/Go)

### 步骤 1：处理统一下单 (`/unified`)

1.  **校验业务单据**: 根据 `bizType` 和 `bizId` 查询业务表（如订单表），确认金额和用户是否匹配。
2.  **生成支付流水**:
    -   生成全局唯一的 `pay_sn`。
    -   将支付信息插入 `payment_record` 表，状态为 `0 (待支付)`。
3.  **调用微信 API**:
    -   调用微信 [UnifiedOrder](https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=9_1) 接口。
    -   `out_trade_no` 填 `pay_sn`。
    -   `total_fee` 填 `amount * 100` (转分)。
    -   `notify_url` 填公网可访问的回调地址。
4.  **返回参数**: 获取微信返回的 `prepay_id` 或 `mweb_url`，组装后返回给前端。

### 步骤 2：处理回调 (`/callback/wechat`)

1.  **验签**: 使用 API Key 验证微信回调签名的正确性。
2.  **幂等校验**: 查询 `payment_record`，如果状态已经是 `1 (成功)`，直接返回 `SUCCESS`。
3.  **金额校验**: 对比回调中的 `total_fee` 与数据库记录是否一致。
4.  **业务分发 (核心逻辑)**:
    -   更新 `payment_record` 状态为 `1`。
    -   根据 `biz_type` 分发逻辑：
        -   if `bizType == 'recharge'`: 更新钱包余额 (`user_wallet.balance += amount`)，记录充值流水。
        -   if `bizType == 'order'`: 更新订单状态 (`order.status = PAID`)，触发发货/积分赠送等逻辑。
5.  **返回响应**: 返回 XML `<return_code>SUCCESS</return_code>`。

### 步骤 3：通用性扩展

- 未来接入支付宝时，只需扩展 `payMethod` 枚举，后端增加支付宝下单和回调处理逻辑，业务层逻辑（如改订单状态）无需变动。
