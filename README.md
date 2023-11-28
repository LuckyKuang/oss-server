# 基于MINIO实现的OSS服务

## 已实现功能

- [x] 存储桶新增/删除/查询
- [x] 设置/获取存储桶策略
- [x] 文件上传/下载/删除
- [x] 非私有存储桶文件列表查询
- [x] 私有存储桶临时连接生成
- [x] 文件分片(断点)下载
- [ ] 文件分片(断点)上传

## docker安装minio

9000端口：是minio api连接用

9090端口：是minio console浏览器访问用，此端口根据`--console-address`参数定义

```shell
docker run \
-d \
-p 9000:9000 \
-p 9090:9090 \
--name minio \
-v /home/pontus.fan/minio/data:/data \
-e "MINIO_ROOT_USER=admin" \
-e "MINIO_ROOT_PASSWORD=minio123456" \
quay.io/minio/minio server /data --console-address ":9090"
```

## minio存储桶策略参数说明

| 参数  |说明   |
| :------------ | :------------ |
|Version|标识策略的版本号，Minio中一般为"2012-10-17"|
|Statement|策略授权语句，描述策略的详细信息，包含Effect（效果）、Action（动作）、Principal（用户）、Resource（资源）和Condition（条件）。其中Condition为可选|
|Effect|Effect（效果）作用包含两种：Allow（允许）和Deny（拒绝），系统预置策略仅包含允许的授权语句，自定义策略中可以同时包含允许和拒绝的授权语句，当策略中既有允许又有拒绝的授权语句时，遵循Deny优先的原则。|
|Action|Action（动作）对资源的具体操作权限，格式为：服务名:资源类型:操作，支持单个或多个操作权限，支持通配符号*，通配符号表示所有。例如 s3:GetObject ，表示获取对象|
|Resource|Resource（资源）策略所作用的资源，支持通配符号*，通配符号表示所有。在JSON视图中，不带Resource表示对所有资源生效。Resource支持以下字符：-_0-9a-zA-Z*./\，如果Resource中包含不支持的字符，请采用通配符号*。例如：arn:aws:s3:::my-bucketname/myobject*\，表示minio中my-bucketname/myobject目录下所有对象文件。|
|Condition|Condition（条件）您可以在创建自定义策略时，通过Condition元素来控制策略何时生效。Condition包括条件键和运算符，条件键表示策略语句的Condition元素，分为全局级条件键和服务级条件键。全局级条件键（前缀为g:）适用于所有操作，服务级条件键（前缀为服务缩写，如obs:）仅适用于对应服务的操作。运算符与条件键一起使用，构成完整的条件判断语句。|



