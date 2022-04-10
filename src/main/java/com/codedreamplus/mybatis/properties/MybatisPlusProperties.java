package com.codedreamplus.mybatis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定mybatis配置
 *
 * @author YuFeng
 * @date 2022/3/16 20:59
 */
@ConfigurationProperties(prefix = "codedreamplus.mybatis-plus")
public class MybatisPlusProperties {

    /**
     * mapper扫描包路径
     */
    private String[] mapperLocation;

    /**
     * 开启租户模式
     */
    private Boolean tenantMode = true;

    /**
     * 开启全表更新删除拦截
     */
    private Boolean blockAttack = true;
    /**
     * 开启sql日志
     */
    private Boolean sqlLog = true;

    /**
     * sql日志忽略打印关键字
     */
    private List<String> sqlLogExclude = new ArrayList<>();

    /**
     * 分页最大数
     */
    private Long pageLimit = 500L;

    /**
     * 溢出总页数后是否进行处理
     */
    protected Boolean overflow = false;

    /**
     * join优化
     */
    private Boolean optimizeJoin = false;

    public String[] getMapperLocation() {
        return mapperLocation;
    }

    public Boolean getBlockAttack() {
        return blockAttack;
    }

    public void setBlockAttack(Boolean blockAttack) {
        this.blockAttack = blockAttack;
    }

    public void setMapperLocation(String[] mapperLocation) {
        this.mapperLocation = mapperLocation;
    }

    public Boolean getTenantMode() {
        return tenantMode;
    }

    public void setTenantMode(Boolean tenantMode) {
        this.tenantMode = tenantMode;
    }

    public Boolean getSqlLog() {
        return sqlLog;
    }

    public void setSqlLog(Boolean sqlLog) {
        this.sqlLog = sqlLog;
    }

    public List<String> getSqlLogExclude() {
        return sqlLogExclude;
    }

    public void setSqlLogExclude(List<String> sqlLogExclude) {
        this.sqlLogExclude = sqlLogExclude;
    }

    public Long getPageLimit() {
        return pageLimit;
    }

    public void setPageLimit(Long pageLimit) {
        this.pageLimit = pageLimit;
    }

    public Boolean getOverflow() {
        return overflow;
    }

    public void setOverflow(Boolean overflow) {
        this.overflow = overflow;
    }

    public Boolean getOptimizeJoin() {
        return optimizeJoin;
    }

    public void setOptimizeJoin(Boolean optimizeJoin) {
        this.optimizeJoin = optimizeJoin;
    }
}
