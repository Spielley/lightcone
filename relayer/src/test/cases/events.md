## 以太坊事件测试用例

### Interal Ticker 测试

1. inter ticker的解析和推送

   - 目标：测试订单成交后，internal ticker的解析和推送

   - 测试前置条件：

     1. 设置订单A 卖出100 LRC 买入 1WETH，fee 为1LRC
     2. 设置订单B 卖出1WETH 买入 100LRC，fee为1LRC

   - 测试步骤和结果验证：

     1. 发送A、B的订单

     2. 发出ohlcdata

        ==>  验证 socket推送一条internal ticker，last price 为0.01

   - 状态: Planned

   - 拥有者: 亚东

   - 其他信息：NA

### Block Gas Price 测试

1. 测试block gas price 事件

   - 目标：测试 block gas price事件对系统推荐gas price影响

   - 测试前置条件：

     1. 设置一个BlockGasPricesExtractedEvent，height设置为0，gas price 设置100个，设置5个gas Price为20Gwei，设置30个15 Gwei，设置30个 10Gwei 设置 30个5Gwei，设置5个1Gwei。

   - 测试步骤及结果验证：

     1. 将BlockGasPricesExtractedEvent发送到GasPriceActor 

        ==> 验证系统推荐的gasPrice更新为10Gwei。

   - 状态: Planned

   - 拥有者: 亚东

   - 其他信息：暂时不考虑gas price降低引发rematch， rematch采用定时尝试。

### Activity 

1. Activity 状态更新

   - 目标：测试pending的activity在后续相同的nonce 相同activity的影响更新

   - 测试前置条件:

     1. 设置一个pending 的transfer eth out 的activity，nonce 为10 — a1
     2. 设置一个success 的transfer eth out 的activity，nonce 为10  — a2

   - 测试步骤及结果验证：

     1. 发出 a1

     2. 发出a2

        ==> 验证 activity a1 更新为 activity a2

   - 状态: Planned

   - 拥有者: 亚东

   - 其他信息：failed activity 与success测试验证一样，不再重复测试。

2. Activity 被删除

   - 目标：测试pending的activity在后续相同nonce不同activity的影响

   - 测试前置条件:

     1. 设置一个pending 的transfer eth out 的activity，nonce 为10  —  a1
     2. 设置一个success 的transfer LRC out 的activity，nonce 为10  —  a2

   - 测试步骤及结果验证：

     1. 发出 a1

     2. 发出a2

        ==> 验证 activity a1被删除， a2正确存储

   - 状态: Planned

   - 拥有者: 亚东

   - 其他信息：NA

### K线数据

1. 测试ohlc data 

   - 目标：测试ohlc data能否正确存储，正确合并

   - 测试前置条件：

     1. 设置block = 96,  ohlcdata 100 LRC -> 0.1WETH  时间为t1
     2. 设置block = 101,  ohlcdata 150 LRC -> 0.2WETH  时间为t1 +75s
     3. 设置block = 102,  ohlcdata 150 LRC -> 0.2WETH  时间为t1 +90s

   - 测试步骤及结果校验：

     1. 发送上面三条 ohlcdata

     2. 请求 lrc-weth 市场，interval为1分钟，时长1小时的K线数据

        ==> 验证是不是两条数据，分别为100 LRC 和300 LRC

   - 状态: Planned

   - 拥有者: 亚东

   - 其他信息：NA
