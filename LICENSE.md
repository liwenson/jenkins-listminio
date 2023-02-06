
## 预览测试

```
mvn hpi:run
```



## 打包
```
mvn package
```

## 使用
```
parameters{
    Miniolist(
        host: 'http://10.200.192.26:9000', 
        name: 'ListMinio',
        target: "${JOB_NAME}",
        bucket: 'jenkins',
        credentialsId: "minio")
}

host           minio地址
target         路径
bucket         桶
credentialsId  用户ID
```



