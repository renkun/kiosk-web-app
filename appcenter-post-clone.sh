#!/usr/bin/env bash

# 创建 local.properties 文件
touch local.properties

# 使用环境变量填充 local.properties 文件
echo "sdk.dir=$ANDROID_HOME" >> local.properties
echo "app.appcenter_secret=$APPCENTER_SECRET" >> local.properties
