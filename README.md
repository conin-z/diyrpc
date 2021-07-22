# diyrpc
An efficient simple rpc framework which is based on Netty, Redis, Spring.



[TOC]



###  项目概述

----

该项目基于 Spring + Netty + Redis ，实现了一个简单高效的**远程服务调用**（RPC）框架；另外还考虑了服务的优雅下线。



- *整个项目周期包含：*

  **模块的设计：**借鉴已有成熟RPC框架的原理和所涉及的功能。从应用层角度来考虑：1. 用户端分为服务提供者与消费者；2. 需要实现两个主要功能：网络通信与服务注册/发现；3. 使用Spring提供的机制灵活管理。

  **技术的选择**：参考已有开源RPC框架，根据项目要求的功能和自身掌握的技术，选取对应一两种方案，不要求技术体系十分完善，如Dubbo框架涵盖了Zookeeper、Redis等多种服务注册/发现方案，但本项目中只使用Redis，利用其提供的方便的数据结构和Jedis客户端来实现相应功能。

  **功能的实现：**

  网络传输模块，使用Netty框架，主要涉及NIO逻辑的封装、JDK方式的动态代理的融合、消息实体的构建、序列化/反序列化和业务处理逻辑的实现；

  服务注册/发现模块，采用Redis作为注册中心，使用JedisPool来保证交互时的线程安全；

  Spring管理模块，主要使用IOC机制和事件传播机制，实现在特定时机既成特定功能；

  服务的优雅下线，使用JDK提供的方法在项目提供的全局配置类中注册JVM关闭钩子，也提供了依托Spring的方式，将下线逻辑实现在由容器主动调用的方法里，在IOC容器关闭时执行。

- *使用到的技术：*

  NIO技术(Netty ) 、Spring、Redis、动态代理、序列化/反序列化 (Kryo)、 池化

- *项目整体分为三大模块：*

  服务注册/发现、网络传输、Spring管理

- *服务上下线：*

  当服务上线时，服务提供者将自己的服务信息注册到注册中心，并通过心跳维持长连接；服务调用者通过订阅服务列表，根据可定制负载均衡算法（或交给cluster层）选择服务，将服务信息缓存在本地以提高性能。当服务下线时，需要考虑优雅下线，文档最后有详细介绍。

  其中，**服务的上线**由Spring的**事件传播机制**来控制：

  ```java
  class RpcXxxConfiguration implements ApplicationListener<ContextRefreshedEvent> ..
  ```

  



*(本文档涉及到的代码基本都是伪代码)*





### 服务注册/发现

------

该模块是采用**Redis**作为注册中心，主要涉及三个实现类：`RedisRegisterCenterConfig`, `RedisServiceRegistry`, `RedisListSubscription`。后两个负责与Redis服务交互，分别实现服务的注册和获取；`RedisRegisterCenterConfig`主要完成与Redis交互前相关配置参数的设置并进行一些必要的初始化。

![image-20210718022125362](C:\Users\zky\Desktop\image\QQ图片20210718022523.png)

#### 服务注册 （provider-related）

```java
// 功能实现在provider的入口类RpcServerConfiguration中；
// 具体是在RpcServerConfiguration实现的BeanPostProcessor的方法里；
class RpcServerConfiguration implements BeanPostProcessor...{
```

- *时机：*

  在service-related bean初始化后，完成**@RpcService标记的**service组件的注册

- *实现：*

  Spring中注册了一个`BeanPostProcessor`，是由入口类`RpcServerConfiguration`充当的，因此可在下面方法中完成服务(with `@RpcService`)注册的相关逻辑：

  ​				`public Object postProcessAfterInitialization();`

- *关联 API：*

  ![image-20210718183031556](C:\Users\zky\AppData\Roaming\Typora\typora-user-images\image-20210718183031556.png)

```java
//注册中心；包装了与redis交互的逻辑；
public class RedisServiceRegistry{   
    // ...
    public static void serviceToRegisterCenter(Class<?> clz) {/* .. */};
    public static void lclAddressToRegisterCenter() {/* .. */};
    public static long removeElement(Jedis jedis, String key, String eleName) {/* */};
    public static long deleteKey(Jedis jedis, String key) {/* .. */};
}
```



#### 服务发现与调用（consumer-related）

涉及的过程有：

- 从注册中心**订阅服务列表**，**缓存**服务列表，从中获取服务地址

  *时机*：consumer第一次涉及到`getBean()`操作（显式get，隐式get）;

- 根据地址与provider**建立连接**，并**缓存**连接信息

  *时机*：consumer第一次涉及到`getBean()`操作（显式get，隐式get）;

- **动态代理**来发送请求，使用“应用层协议”`RequestImpl`**包装请求**，实现服务调用

  *时机：*

  ​		创建代理： 涉及到对远程Service的`getBean()`操作 -> 且`getObject()`被执行时；

  ​		请求信息的包装：每次调用远程服务的方法时。

  *涉及的API：*

  ![20210718183331](C:\Users\zky\Desktop\image\QQ图片20210718183331.png)![image-20210718183508331](C:\Users\zky\AppData\Roaming\Typora\typora-user-images\image-20210718183508331.png)



从consumer调用的角度看，分为：**远程服务的识别 、缓存与 代理：**

- **识别**

  *时机：*Spring注册beanDefinition后，创建bean实例前

  *实现：*如果待管理对象是有`@RpcReference`标记，先修改beanDefinition信息：

  > 预处理：customize BeanDifinition  -> 带泛型工厂模式：		
  >
  > ​				`beanDefinition.setBeanClass(RpcInterfaceProxyFactoryBean.class)`

  *关联 API：*

  ```java
  // consumer的入口类：RpcClientConfiguration；
  class RpcClientConfiguration implements BeanDefinitionRegistryPostPro..{
      // ...
      void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry){
          RpcItfScanner itfScanner = new RpcItfScanner(registry);
          itfScanner.scan(itfPaths);
      }
  }
      
  // 扫描路径的方式进行判断、修改
  class RpcItfScanner extends ClassPathBeanDefinitionScanner{
      
      Set<BeanDefinitionHolder> doScan(String... basePackages){
          GenericBeanDefinition beanDefinition = (GenericBeanDefinition)holder
              .getBeanDefinition();
  	    beanDefinition.getConstructorArgumentValues()
              .addGenericArgumentValue(beanDefinition.getBeanClassName());
   //the form of factory bean (with genericity)       	
          beanDefinition.setBeanClass(RpcInterfaceProxyFactoryBean.class);  	
      }
      
      boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition){
          AnnotationMetadata metadata = beanDefinition.getMetadata(); 
   //distinguish here
          return metadata.hasAnnotation("com.rpc.annotations.RpcReference") &&
                  metadata.isInterface() && metadata.isIndependent();  
      }
      
  }
  ```

- **缓存**

  主要由ServerInfo来管理；主要数据结构为 `ConcurrentHashMap`；

  服务调用期间会涉及到缓存的刷新、更新、清除；

  ```java
public class ServerInfo {
  
      public static Boolean isInit = false;
  
      public static Map<String, Set<String>> itfServersMap = new ConcurrentHashMap();
      public static Map<String, Boolean> itfServersMapState = new ConcurrentHashMap();
      public static Set<String> serversList = new HashSet();  
      public static Map<String, Set<String>> servicesNameMap = new ConcurrentHashMap(); 
      public static Map<String, Channel> serverChannelMap = new ConcurrentHashMap(); 
      // request<-K,V->responses queue   // requestID<-K,V->responses
      public static Map<String, SynchronousQueue<ResponseImpl>> msgTransferMap = new ConcurrentHashMap(); 
  
      public static void clear(){
          serversList.clear();
          serverChannelMap.clear();
          itfServersMap.clear();
          itfServersMapState.clear();
          servicesNameMap.clear();
          msgTransferMap.clear();
      }
  
  }
  ```
  
- **代理**

  *时机：* 创建bean时，便创建代理对象；consumer调用方法时，启动远程调用逻辑；

  *实现：* 在 `InvocationHandler`的实现类`ClientProxyInvocation`的`invoke()`中，会从缓存查可用的服务提供者，再由选择器（**负载均衡**）按照一定规则选择一个，并从连接缓存中得到对应的channel，再进入Netty模块完成网络通信。

  *关联API：*

  ```java
  class RpcInterfaceProxyFactoryBean<T> implements FactoryBean{
      public RpcInterfaceProxyFactoryBean(Class<T> itfClass) {
          this.itfClass = itfClass;    //target itf
      }
      
      public T getObject() throws Exception {
       	registerCenter.init(path);
      //get and save serverlist
          getServerListAndConnectAll(nettyConfig);
      //proxy
          (T)Proxy.newProxyInstance(xx,ew ClientProxyInvocation(itfClass));   
      }
      // ...
  }
     
  class ClientProxyInvocation implements InvocationHandler {
      
      Object invoke(xx,xx,xx){
          new RequestImpl(MessageType.SERVER);
          send(selector,request);
    //responses:SynchronousQueue 同步序列；与请求相对应的 收到的 回复消息所存放的队列
          return responses.take().returnObj(); //here take(); handler中put();
      }
      
    boolean send(ServerSelector selector, RequestImpl request) {/* .. */}; 
   	// ...   
  }
  ```



#### Redis相关配置

项目基于Jedis客户端，用到了`JedisPool`来保证线程安全；

*API：*

```java
public class RedisRegistCenterConfig{ 
    private String ip = "localhost";  // 可修改
    private int port = 6379;
    private String password = ""; 
    private int timeout = 300;   
    private int idl;
    private int maxActive = 128;  
    // ...
    public RedisRegisterCenterConfig() { init(null); }  // 使用默认参数，表明无配置文件
    public RedisRegisterCenterConfig(String jedisConfigPath) {
        this.jedisConfigPath = jedisConfigPath;
        init(jedisConfigPath);
    } // 需要参数解析
    
    public void init(){/* .. */};
	public static JedisPool getJedisPool(){/* .. */};
    public void offlineFromRedis(){/* .. */};
    // ...
```

- 保存JedisPool的配置参数，连接参数，
- 可通过`set()`方法设置相关参数 
- 默认端口号*6379*

*数据结构：*

​		使用`set`类型，样式如下：

```java
server_lists  ---  "$iP1$::$port1$" "$iP2$::$port2$" ...
192.168.0.1::8989 --- "xx.xx.Service1" "xx.xx.Service2"...
192.168.0.2::8988 --- "xx.xx.Service1" "xx.xx.Service2"...
```

*命令：*

​		主要涉及`ADD` /`SMEMBERS` /`DEL` /`SREM`





### Netty网络传输模块

-----

#### 核心类

服务消费者与提供者均由实体类`NettyConfig`引导：

*（也可以设计成是分开的两个API）*

![QQ图片20210718212831](C:\Users\zky\Desktop\image\QQ图片20210718212831.png)

```java
class NettyConfig implements SocketConfig{
	// ...
}

interface SocketConfig {

/*serverInit()方法涉及到Netty的初始化工作，包括：NIO引导类参数配置；建立连接处理和事件处理工作组；Handlers配置：业务处理+序列化/反序列化+TCP粘/半包+心跳处理+日志记录；端口的绑定；*/
    void serverInit(int port) {/* .. */};
    
/*clientInit()方法涉及到Netty的初始化工作和网络连接的发起，包括：NIO引导类参数配置；建立事件处理工作组；Handlers配置：业务处理+序列化/反序列化+TCP粘/半包+心跳处理+日志记录；*/ 
    boolean clientInit(String ip,int port) {/* .. */};  
    
/*连接信息缓存*/ 
    void connectAndSave(String itfName, String server, Set<String> serverListForItf); 
    
/*资源的关闭：工作组的优雅关闭*/ 
    void close() {/* .. */};   
}
```



#### 消息实体的构建

![image-20210718212605127](C:\Users\zky\AppData\Roaming\Typora\typora-user-images\image-20210718212605127.png)

服务消费者封装**请求**消息：`RequestImpl`，服务提供者封装**响应**消息：`ResponseImpl`：

```java
class RequestImpl implements Serializable {/* .. */}
class ResponseImpl implements Serializable {/* .. */}
```

还有用来标志发送消息类型的枚举类 `MessageType`以及供server使用的用来标志远程调用结果的枚举类 `ResponseStatus`：

```java
public enum MessageType {
    SERVER,HEARTBEAT,DISCONNECT;
}
public enum ResponseStatus{
    OK,ERROR;
}
```



#### 业务处理逻辑

![image-20210718213213435](C:\Users\zky\AppData\Roaming\Typora\typora-user-images\image-20210718213213435.png)

*服务消费端 ：*

```java
class InvokeHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){            
        if(request.getMessageType() == MessageType.HEARTBEAT){
            //pong 发送心跳包  "i'm alive"
        }else if(request.getMessageType() == MessageType.SERVER){
            //读取msg，处理调用，封装结果，发送响应信息；
        }
    }
}
```

*服务提供者：*

```java
class ResponseHandler extends SimpleChannelInboundHandler<ResponseImpl> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ResponseImpl msg) {
     if (msg.getMessageType() == MessageType.DISCONNECT){ 
            // 服务要下线时，会通知所有连接对象，这里消费者会收到一个下线通知包
            // 然后更新本地缓存
        }else if(msg.getMessageType() == MessageType.SERVER){
            // 收到了调用结果，将其放入同步队列中
         	// 异步通信的体现，在ClientProxyInvocation的invoke()中取
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,Object evt) {
        // 空闲处理：ping 发送心跳包 
    }
}
```



#### 序列化/反序列化

使用**Kryo**框架，并用`ThreadLocal`来保证线程安全；

![image-20210718212733268](C:\Users\zky\AppData\Roaming\Typora\typora-user-images\image-20210718212733268.png)

```java
class KryoUtil{
	private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // ...
            return kryo;
        }
    };
    
    public static Kryo getInstance() { return kryoLocal.get(); }
    
    /*这三个用来构建KyroDecoder和KyroEncoder的处理逻辑*/
    public static <T> byte[] writeToByteArray(T obj) {/* .. */};
    public static <T> String writeToString(T obj) {/* .. */};
	public static <T> T readFromByteArray(ByteBuf byteArray) {/* .. */};
}
```





### Spring管理模块

----

#### 服务消费者相关

- **入口配置类（全局）** `RpcClientConfiguration`：

1. 实现接口  `BeanDefinitionRegistryPostProcessor`，具有了修改beanDefination的功能：

   >  扫描包路径下 --> 得到`bdHolders` --> 进行`bd.setBeanClass`

2. 实现接口 `DisposableBean`，在bean被销毁时由容器调用以释放资源

   >  优雅下线设计的一部分

```java
class RpcClientConfiguration implements RpcConfiguration, 		
								BeanDefinitionRegistryPostProcessor, DisposableBean {
   
    public static ApplicationContext ioc;

    private String[] itfPaths;

    private SocketConfig socketConfig = new NettyConfig();
    private int port;
    private ServerSelector serverSelector = new RandomServerSelector();

    private RegistCenterConfig registerCenter; 
    private Timer timer; 
    private long synRedisSeconds = 60;  
    private String jedisConfigPath; 
    // ...
```

3. 实现接口 `ApplicationContextAware`，回调当前ApplicationContext实例

4. 实现接口 `ApplicationListener<ContextRefreshedEvent>`，事件监听

   > 实现定期向Redis请求服务信息，以刷新本地缓存；
   >
   > **更新**配置类中的容器**启动标志**：`isIocStarted`

```java
interface RpcConfiguration extends ApplicationContextAware, 
										ApplicationListener<ContextRefreshedEvent> {
    //...
```

- **远程服务代理工厂类：** `RpcInterfaceProxyFactoryBean<T>` ：

基于`FactoryBean`的bean注册方式，并包装了动态代理的逻辑

```java
public class RpcInterfaceProxyFactoryBean<T> implements FactoryBean {

    private RpcClientConfiguration consumer;
    private Class<T> itfClass; 
    private Set<String> serverListForItf = new HashSet<>();

    public RpcInterfaceProxyFactoryBean(Class<T> itfClass) {
        this.itfClass = itfClass;
    }

    public T getObject() throws Exception {
    	//...
        Object o = Proxy.newProxyInstance(itfClass.getClassLoader(), new Class[]{itfClass}, new ClientProxyInvocation(itfClass));
        return (T)o;
    }
    // ...
}
```

- **辅助类：** `RpcItfScanner` ：

借助`BeanDefinitionRegistryPostProcessor`的功能，**介入bean生命周期**：实例化前，由`RpcItfScanner`扫描bean路径，将远程服务的实例化对象**改为**项目设计的带泛型的工厂类`RpcInterfaceProxyFactoryBean`，其中远程服务接口的类型作为**泛型参数**传入并保存

```java
public class RpcItfScanner extends ClassPathBeanDefinitionScanner {
   
    public RpcItfScanner(BeanDefinitionRegistry registry) {
        super(registry,false);
    }
    
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        if(holders!=null && holders.size()>0){
            for(BeanDefinitionHolder holder:holders){
               // ...		
          		beanDefinition.getConstructorArgumentValues().
              			   addGenericArgumentValue(beanDefinition.getBeanClassName());
               
                beanDefinition.setBeanClass(RpcInterfaceProxyFactoryBean.class); 
            }
        }
        // ...
    }   
    
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata(); 
        return metadata.hasAnnotation("com.rpc.annotations.RpcReference") &&
                metadata.isInterface() && metadata.isIndependent();  
        // default: (metadata.isIndependent() && (metadata.isConcrete()||(metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))));
    }
    // ...
}
```



#### 服务提供者相关

- **入口配置类（全局）** `RpcServiceConfiguration`：

1. 实现接口 `InitializingBean`，在入口配置类初始化时进行服务上线：

   > 在**入口配置类**初始化过程中完成 **socket启动** 及 **server上线注册**（对应一个value: `$ip$::$port$`）

2. 实现接口  `BeanPostProcessor`，使配置类兼有后置拦截处理的功能：

   > 在bean（带有@RpcService的）初始化后，即向Redis**注册** ；
   >
   > 实现定期向Redis发送心跳包，维持keys的生存时间

```java
class RpcServiceConfiguration implements RpcConfiguration, BeanPostProcessor, 																		InitializingBean{
    public static ApplicationContext ioc;

    /* -----  [key attribute]  [1-level ache for saving clients' info] ----- */
    private Map<String, Channel> clientChannels = new ConcurrentHashMap<>();

    private SocketConfig socketConfig = new NettyConfig();
    private int port;  //--by server set

    private volatile boolean isRegistered;  // there is one service registered
    private RedisRegisterCenterConfig registerCenter;  // --by server set
    private Timer timer;   // keep heartbeats with redis
    private long synRedisSeconds; // default 30s , saved in RedisConfig 

    private volatile boolean isIocStarted;
```

3. 实现接口 `ApplicationContextAware`，回调当前ApplicationContext实例

4. 实现接口 `ApplicationListener<ContextRefreshedEvent>`，进行对容器刷新事件的监听

   > Spring容器刷新，**更新**配置类中的容器**启动标志**：`isIocStarted`

```java
interface RpcConfiguration extends ApplicationContextAware, 
										ApplicationListener<ContextRefreshedEvent> {
	// ...
```





### 优雅下线

----

#### 服务端

涉及到RPC时，服务端优雅停机需要满足：

1. 服务消费者不应该从注册中心订阅到已经下线的服务提供者：

   > provider从注册中心撤销相关服务信息；
   >
   > consumer刷新本地缓存

2. 服务消费者不应该再使用网络连接channel发送请求信息

   > provider传送含有自身信息的挥手包给channel另一端
   >
   > consumer删除本地连接缓存中对应项

3. 服务提供者需要处理完毕已有socket请求，不能被停机指令中断

   > Netty已提供

*逻辑：*

```java
public void destroy() {
    if (timer != null) { timer.cancel();}
    if (registerCenter != null) { registerCenter.offlineFromRedis();}
    try {
        Thread.sleep(Constant.WAIT_REFER_UPDATE);  // 留时间给consumer
    } catch (InterruptedException e) { /**/ }
    if (socketConfig != null) { socketConfig.close();}
}
```

*实现：*

- 使用**JDK提供的方法**注册一个JVM钩子

```java
{
    Runtime.getRuntime().addShutdownHook(new Thread(()->{
        destroy(); // 下线逻辑  
    }));
}
```

- 也可**依托 Spring**将下线逻辑实现在会由容器调用的方法里

> 方式1:   实现 `ApplicationListener<ContextClosedEvent>` 的 `onApplicationEvent()`
>
> 方式2:   实现 `LifeCycle` 的 `stop()`
>
> 方式3:   实现 `Disposable`的 `destroy()`
>
> 需要用户在获取ApplicationContext实例后调用方法  `applicationContext.registerShutHook()`



#### 消费端

不再需要RPC服务时，需要考虑：

1.  关闭与Redis交互的定时任务
2.  关闭Netty模块的连接通道
3.  清空缓存

*逻辑：*

```java
public void destroy() {
    if (timer != null) { timer.cancel();}
    disconnect(); 
    if (socketConfig != null) { socketConfig.close(); } 
    ServerInfo.clear();
}

private void disconnect() {
    for (String server : ServerInfo.serverChannelMap.keySet()) {
        Channel ch = ServerInfo.serverChannelMap.get(server);
        if(ch != null && ch.isOpen()){ ch.close(); }
    }
}
```

*实现：* 类似服务端