#!/bin/bash
# 模拟微信/支付宝支付通知，用于测试自动记账功能
# 使用前确保已授予应用"通知使用权"权限

ADB=~/Library/Android/sdk/platform-tools/adb

# 模拟微信支付通知
echo "发送微信支付通知..."
$ADB shell am start-activity -a android.intent.action.MAIN -c android.intent.category.HOME
$ADB shell "cmd notification post -S bigtext --bigtext '微信支付付款成功' 'com.tencent.mm' 1001 '微信支付' '微信支付付款成功 ¥100.00 商户：便利店'"

sleep 2

# 模拟支付宝收款通知
echo "发送支付宝收款通知..."
$ADB shell "cmd notification post -S bigtext --bigtext '支付宝收款到账' 'com.eg.android.AlipayGphone' 1002 '支付宝' '支付宝收款到账 ￥50.00'"

echo "完成！请检查是否弹出'快速记账'通知"
