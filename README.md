## 描述
获取从minio中显示项目归档后的版本，然后在jenkins发布是的选项插件

## 运行
```
mvn hpi:run
```

## 打包
```
mvn package
```

## 远程调试
```
# linux
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
mvn hpi:run

# windows
set MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
mvn hpi:run
```




## 例子
``` 
pipeline {
    agent any

    parameters{  
        Miniolist(
            host: 'http://10.200.192.26:9000',
            name: 'ListMinio',
            target: 'test003',
            bucket: 'jenkins',
            credentialsId: "minio001")
    }
    stages {
        stage('Hello') {
            steps {
                echo "Hello World ${ListMinio}"
            }
        }
    }
}
```

自由风格
```
This project is parameterized   ---> Add Parameter  ---> List Minio Version
```