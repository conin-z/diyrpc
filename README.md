### 文档说明书

https://shimo.im/docs/Kg8yxQrRG3tjcp3X



### 使用指南

- 使用IDEA打开三个工程：

  `rpc_test_server`（服务提供者1），`rpc_server`（服务提供者2），

  `rpc_client`（消费者）

- 以Redis作为注册中心，打开本地Redis服务器：默认`redis.ip=localhost; redis.port=6379`
```java
可设置：
	三个工程main包->client包/server包->Spring配置类中->`RegisterCenterConfig`项：调用 set方法

可自定义Jedis的配置文件：
	set方法将其路径设置，或直接使用带参数的构造方法
```

- 首先运行`rpc_test_server`工程test包下的测试单元（这个server含有的远程服务实现类较全）
```java
此时没有服务监听者，日志输出：
================= send ONLINE msg to listeners of number { 0 } =================
```

- 运行`rpc_client`工程test包下的测试单元，观察一阵client和server的日志输出
```java
此时是观察【**远程服务的调用与处理**】，有发送记录、数据格式、心跳机制、记录接收记录、结果输出等
```

- 运行`rpc_server`工程test包下的测试单元，观察日志
```java
会有借助Redis订阅/发布模式的【**服务上线**】记录：
// server发送上线通知
================= send ONLINE msg to listeners of number { 1 } =================
// client收到上线通知
=== received ONLINE message of server {192.168.31.224::10086} from channel {ONLINE} ===
```

```java
client端会有【**本地缓存**】的更新（增加）：
=== consumer {localhost} update local Service_Candidate_Caches successful!
=========== remote services required and available candidates:
{rpc.service.ServiceA=[192.168.31.224::8989],rpc.service.ServiceC=[192.168.31.224::8989]}
=========== online servers in register center:
 [192.168.31.224::8989]
=========== online servers and their available services:
{192.168.31.224::8989=[rpc.service.ServiceB, rpc.service.ServiceA, rpc.service.ServiceC]}
// ... ...
=== consumer {localhost} update local Service_Candidate_Caches successful!
=========== remote services required and available candidates:
{rpc.service.ServiceA=[192.168.31.224::8989, 192.168.31.224::10086], rpc.service.ServiceC=[192.168.31.224::8989]}
=========== online servers in register center:
 [192.168.31.224::8989, 192.168.31.224::10086]
=========== online servers and their available services:
{192.168.31.224::8989=[rpc.service.ServiceB, rpc.service.ServiceA, rpc.service.ServiceC], 192.168.31.224::10086=[rpc.service.ServiceB, rpc.service.ServiceA]}
```

> 会有服务提供者的选择（由负载均衡策略决定）：client调用`ServiceA`方法时两个server都有处理记录

- 合适的时间后，exit方式（非stop）退出`rpc_server`工程，观察日志：

```java
会有【**下线记录**】：
// server发送下线通知
================ send OFFLINE msg to listeners of number { 1 }  ===============
// client收到下线通知
=== received OFFLINE message of server {192.168.31.224::10086} from channel {OFFLINE} ===
```

```java
会有本地缓存的更新（减少）:
=== consumer {localhost} update local Service_Candidate_Caches successful!
=========== remote services required and available candidates:
{rpc.service.ServiceA=[192.168.31.224::8989],rpc.service.ServiceC=[192.168.31.224::8989]}
=========== online servers in register center:
 [192.168.31.224::8989]
=========== online servers and their available services:
{192.168.31.224::8989=[rpc.service.ServiceB, rpc.service.ServiceA, rpc.service.ServiceC]}
```

- 再次运行`rpc_server`工程一段时间，观察日志

- 退出`rpc_client`工程，观察两个server的日志记录

- 修改`rpc_client`工程，并再次启动：

  如：在增加/减少服务调用间隔（sleep时长），或修改相关定时任务的参数，观察心跳机制的记录

  ```java
  // client:
  ===== scheduled task for subscription is to begin... =====
  // server:
  ====== server {192.168.31.224::10086} send expire command successful!
  ```

  如：创建一些这两个server不含的远程服务接口（注意使用相关注解标记），如ServiceD、ServiceE ...，观察异常情况；也可以一开始就先启动client工程来观察：

  ```java
  // 创建代理对象失败, 会关闭ioc容器，后续getBean便会报异常
  === no service provider in register center! ===
  ```

- 三个工程正常运行一段时间后

  ```java
  // 无论是server还是client，都可以观察到由日志框架定期打印的RPC状态信息
  ```

- 先后退出`rpc_test_server`工程和`rpc_server`工程 ：

```java
// client:
[]
{}
{rpc.service.ServiceA=[], rpc.service.ServiceC=[]}
[WARN ] === no provider for this service {rpc.service.ServiceA} now! ===
[WARN ] === send error, will select server candidate again to send
[WARN ] === no provider for this service {rpc.service.ServiceA} now! ===
[WARN ] === send error, will select server candidate again to send
[WARN ] === no provider for this service {rpc.service.ServiceA} now! ===
[WARN ] === send error, will select server candidate again to send
[WARN ] === no provider for this service {rpc.service.ServiceA} now! ===
[WARN ] === send error, will select server candidate again to send
[ERROR] ==== fail to call the method: test() ====
[WARN ] === no provider for this service {rpc.service.ServiceC} now! ===
[WARN ] === send error, will select server candidate again to send
[WARN ] === no provider for this service {rpc.service.ServiceC} now! ===
[WARN ] === send error, will select server candidate again to send
[WARN ] === no provider for this service {rpc.service.ServiceC} now! ===
[WARN ] === send error, will select server candidate again to send
[WARN ] === no provider for this service {rpc.service.ServiceC} now! ===
[WARN ] === send error, will select server candidate again to send
[ERROR] ==== fail to call the method: testArray() ====
// ...
```

