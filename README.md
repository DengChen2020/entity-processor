# entity-processor
实体类表字段常量生成
引入maven依赖，加JPA注解即可自动生成
## 开始使用
- Maven:
```xml
<dependency>
    <groupId>io.github.dengchen2020</groupId>
    <artifactId>entity-processor</artifactId>
    <version>1.0.5</version>
</dependency>
```
自动识别@Entity,@Column，@Transient等JPA注解生成字段常量类
