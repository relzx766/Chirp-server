# Chirp

模仿Twitter的社交平台，使用spring cloud开发<br>
前端:[https://github.com/relzx766/Chirp-app](https://github.com/relzx766/Chirp-app)

### 系统环境

jdk 21<br>
nacos 2.2.3 <br>
nginx 1.24.0 <br>
kafka 3.5.0 <br>
redis 5.0.14.1 <br>
mysql 8.0.12

### windows部署

kafka:<br>
进入安装根目录\windows\bin<br>
cmd执行:

```shell
.\zookeeper-server-start.bat ..\..\config\zookeeper.properties
```

之后

```shell
.\kafka-server-start.bat ..\..\config\server.properties
```

nacos:<br>
进入安装根目录\bin<br>
cmd执行:

```shell
.\startup.cmd -m standalone
```

nginx:<br>
先在http模块下，修改请求最大体积

```
client_max_body_size 20m;
```

是否设置反向代理就随便了,主要还是用来处理静态资源,在server模块下

```
   location ^~/chirp/ {
           proxy_pass http://127.0.0.1:8080/; # 加上斜杠，去掉/chirp前缀
           proxy_set_header Host $proxy_host; # 修改转发请求头，让8080端口的应用可以受到真实的请求
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme; # 设置转发协议
       }
```

<br>
进入安装根目录<br>
cmd执行:

```shell
nginx
```

redis:<br>
运行

```
redis-server.exe
```

之后就直接启动服务就好了