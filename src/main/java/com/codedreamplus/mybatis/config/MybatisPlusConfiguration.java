package com.codedreamplus.mybatis.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.codedream.auto.properties.CodeDreamPropertySource;
import com.codedreamplus.mybatis.plugins.MybatisPlusAutoFill;
import com.codedreamplus.mybatis.plugins.SqlLogInterceptor;
import com.codedreamplus.mybatis.plugins.handler.UserHandler;
import com.codedreamplus.mybatis.properties.MybatisPlusProperties;
import com.codedreamplus.mybatis.resolver.PageArgumentResolver;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MybatisPlus配置类
 *
 * @author ShiJianlong
 * @date 2022/3/16 23:36
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MybatisPlusProperties.class)
@MapperScan("com.codedreamplus.**.mapper.**")
@CodeDreamPropertySource(value = "classpath:/codedreamplus-mybatis.yml")
public class MybatisPlusConfiguration implements WebMvcConfigurer {

    /**
     * 租户处理器
     */
    @Bean
    @ConditionalOnMissingBean(TenantLineHandler.class)
    public TenantLineHandler tenantLineHandler() {
        return new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                //todo 从线程变量中获取
                return new StringValue("0000000000000");
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return true;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(UserHandler.class)
    public UserHandler userHandler() {
        return new UserHandler() {
            @Override
            public Long getUserId() {
                //todo 从线程变量中获取
                return 13456L;
            }
        };
    }

    /**
     * 租户拦截器
     */
    @Bean
    @ConditionalOnMissingBean(TenantLineInnerInterceptor.class)
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantLineHandler tenantHandler) {
        return new TenantLineInnerInterceptor(tenantHandler);
    }

    @Bean
    @ConditionalOnMissingBean(MybatisPlusAutoFill.class)
    public MybatisPlusAutoFill mybatisPlusAutoFill(UserHandler userHandler) {
        return new MybatisPlusAutoFill(userHandler);
    }

    /**
     * mybatis-plus 拦截器集合
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineInnerInterceptor tenantLineInnerInterceptor,
                                                         MybatisPlusAutoFill mybatisPlusAutoFill,
                                                         MybatisPlusProperties mybatisPlusProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 配置租户拦截器
        if (Boolean.TRUE.equals(mybatisPlusProperties.getTenantMode())) {
            interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        }
        // 配置防止全表更新插件
        if (Boolean.TRUE.equals(mybatisPlusProperties.getBlockAttack())) {
            BlockAttackInnerInterceptor blockAttackInnerInterceptor = new BlockAttackInnerInterceptor();
            interceptor.addInnerInterceptor(blockAttackInnerInterceptor);
        }
        // 开启自动填充
        interceptor.addInnerInterceptor(mybatisPlusAutoFill);
        // 配置分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    /**
     * sql 日志
     */
    @Bean
    public SqlLogInterceptor sqlLogInterceptor(MybatisPlusProperties mybatisPlusProperties) {
        return new SqlLogInterceptor(mybatisPlusProperties);
    }

    /**
     * page 解析器
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new PageArgumentResolver());
    }

}
