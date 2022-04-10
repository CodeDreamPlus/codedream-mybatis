# codedreamplus-mybatis

**MybatisPlus常用配置封装**

## 使用场景
* 数据库基础字段封装，使用时实体类继承`BaseEntity`即可，其内包含基础字段
* 通用配置，例如关闭banner，开启下换线转换，插入策略等配置
* 需要使用常用插件时，例如分页插件，防止全表更新插件
* 自动填充创建人，更新人等基础字段
* SQL日志打印
* 分页参数解析

## 主要特性
* 基础配置，例如`mapperLocation`,`typeAliasesPackage`,`自带banner关闭`,`字段更新,插入策略`等配置

* 基础实体类`BaseEntity`封装，包含常用的建表的必须字段`id`,`createUser`,`createTime`,`updateUser`,

  `updateTime`,`status`,`isDeleted`

* 常用插件配置`PaginationInnerInterceptor`,`TenantLineInnerInterceptor`,`BlockAttackInnerInterceptor`

* 自动填充创建时间，创建人，更新时间，更新人，包括MybatisPlus自带填充失效场景，依然实现填充

* SQL日志打印

* 分页参数解析装载

## 使用方式

#### 1.引入

在项目的pom.xml引入codedreamplus-mybatis依赖，如下：

```xml
<dependency>
    <groupId>com.codedreamplus</groupId>
    <artifactId>codedreamplus-mybatis</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2.使用

##### 2.1 自动填充创建人，更新人时需要重新注入`MybatisPlusAutoFill`自定义插件，获取用户信息


**示例:**

```java
@Configuration
public class CodeDreamPlusConfig {
    @Bean
    public UserHandler userHandler() {
        return new UserHandler() {
            @Override
            public Long getUserId() {
                return "获取的用户id";
            }
        };
    }
    @Bean
    public MybatisPlusAutoFill mybatisPlusAutoFill(UserHandler userHandler) {
        return new MybatisPlusAutoFill(userHandler);
    }
}
```

##### 2.2 定义实体类时，继承基础实体类，实现基础字段定义

```java
public class Student extends BaseEntity {
    private String name;
    private int sex;
    private int grade;
}

```

**基础实体如下**

```java
// 基础实体如下
public class BaseEntity implements Serializable {
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建人
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新人
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 状态[1:正常]
     */
    private Integer status;

    /**
     * 状态[0:未删除,1:删除]
     */
    @TableLogic
    private Integer isDeleted;
}
```

**2.3 分页参数自动解析** 

```java
@GetMapping
public Page<Student> list(Page<Student> page){
    return studentService.page(page);
}
```

请求时传入如下参数实现Page装配`localhost:8080/students?page=1&size=10&sort=name,desc`

## 设计原理

**1. 自动填充创建人，更新人，创建时间，更新时间**

```java
//实现MybatisPlus的内部类实现填充控制
public class MybatisPlusAutoFill extends JsqlParserSupport implements InnerInterceptor {

    // 其他方法已忽略
    
    // 获取当前用户id
    private UserHandler userHandler;

    public MybatisPlusAutoFill() {
    }
    public MybatisPlusAutoFill(UserHandler userHandler) {
        this.userHandler = userHandler;
    }
    /**
     * 处理 parameterMappings 多于参数
     */
    private Map<String, Integer> indexMap = new ConcurrentHashMap<>();

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.INSERT || sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            mpBs.sql(parserMulti(mpBs.sql(), null));
            // 删除已经赋值的字段
            if (sct == SqlCommandType.INSERT) {
                Integer createTimeIndex = indexMap.get(Constant.CREATE_TIME);
                if (createTimeIndex != null) {
                    List<ParameterMapping> parameterMappings = mpBs.parameterMappings();
                    parameterMappings.remove(createTimeIndex.intValue());
                    mpBs.parameterMappings(parameterMappings);
                }
                Integer createUserIndex = indexMap.get(Constant.CREATE_USER);
                if (createUserIndex != null) {
                    List<ParameterMapping> parameterMappings = mpBs.parameterMappings();
                    parameterMappings.remove(createUserIndex.intValue());
                    mpBs.parameterMappings(parameterMappings);
                }
            }
        }
    }

    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        update.addUpdateSet(new Column(Constant.UPDATE_TIME), new StringValue(this.getTime()));
        update.addUpdateSet(new Column(Constant.UPDATE_USER), new StringValue(userHandler.getUserId().toString()));
    }

    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        List<Column> columns = insert.getColumns();
        if (CollectionUtils.isEmpty(columns)) {
            // 针对不给列名的insert 不处理
            return;
        }
        // 记录填充值的位置，填充完后删除已填充的字段
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if (Constant.CREATE_TIME.equals(column.getColumnName())) {
                indexMap.put(Constant.CREATE_TIME, i);
            }
            if (Constant.CREATE_USER.equals(column.getColumnName())) {
                indexMap.put(Constant.CREATE_USER, i);
            }
        }
        columns.add(new Column(Constant.IS_DELETED));
        if (insert.getItemsList() != null) {
            ItemsList itemsList = insert.getItemsList();
            // 对指定字段进行值填充
            ((ExpressionList) itemsList).getExpressions().set(indexMap.get(Constant.CREATE_TIME), new StringValue(this.getTime()));
            ((ExpressionList) itemsList).getExpressions().set(indexMap.get(Constant.CREATE_USER), new StringValue(userHandler.getUserId().toString()));
            ((ExpressionList) itemsList).getExpressions().add(new StringValue("0"));
        } else {
            throw ExceptionUtils.mpe("Failed to process multiple-table update, please exclude the tableName or statementId");
        }
    }
}
```

## 参考
* Mybatis: https://mybatis.org/mybatis-3/zh/index.html
* MybatisPlus: https://baomidou.com/
* SpringBoot自动配置: https://docs.spring.io/spring-boot/docs/2.6.4/reference/htmlsingle/#using.auto-configuration