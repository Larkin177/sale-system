UPDATE system_config SET config_value = '【{site_name}】感谢您的购买！\n\n下载链接: {download_url}\n授权码: {auth_code}\n订单号: {order_no}\n金额: ¥{amount}\n\n请妥善保管授权码，安装时需要输入。\n如有问题请联系客服。'
WHERE config_key = 'delivery_template';
