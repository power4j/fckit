网关鉴权插件,网关扮演资源服务服务器角色

## 使用方式

### 依赖

```XML
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
    <dependency>
      <groupId>com.power4j.fist</groupId>
      <artifactId>fist-kit-cloud-gateway-resource-server</artifactId>
    </dependency>
  </dependencies>
```


### 代码

1. 实现 `ReactivePermissionDefinitionService`服务
2. 添加`@EnableRouteGuard` 注解


### 配置


```YAML
fist:
  oauth2:
    resourceserver:
      opaque-token:
        # see rfc7662
        introspection-uri: "lb://fist-auth/v1/token/auth-user/details"
        client-id: client
        client-secret: 123456

# Spring Cloud Gateway 的路由信息
spring:
  cloud:
    gateway:
      routes:
        # 
        - id: demo
          uri: lb://demo-service
          predicates:
            - Path=/demo/**

```
