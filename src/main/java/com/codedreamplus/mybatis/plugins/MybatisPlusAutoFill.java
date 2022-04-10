package com.codedreamplus.mybatis.plugins;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.codedreamplus.mybatis.config.Constant;
import com.codedreamplus.mybatis.plugins.handler.UserHandler;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mybatis_plus 公共字段填充
 *
 * @author ShiJianlong
 * @date 2022/3/20 15:13
 */
public class MybatisPlusAutoFill extends JsqlParserSupport implements InnerInterceptor {

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
        List<String> tableLogicField = this.getTableLogicField(update.getTable().getName());
        if (tableLogicField.contains(Constant.UPDATE_TIME)) {
            update.addUpdateSet(new Column(Constant.UPDATE_TIME), new StringValue(this.getTime()));
        }
        if (tableLogicField.contains(Constant.UPDATE_USER)) {
            update.addUpdateSet(new Column(Constant.UPDATE_USER), new StringValue(userHandler.getUserId().toString()));
        }
    }

    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        List<Column> columns = insert.getColumns();
        if (CollectionUtils.isEmpty(columns)) {
            // 针对不给列名的insert 不处理
            return;
        }
        List<String> tableLogicField = this.getTableLogicField(insert.getTable().getName());
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
        columns.add(new Column(Constant.STATUS));
        columns.add(new Column(Constant.IS_DELETED));
        if (insert.getItemsList() != null) {
            ItemsList itemsList = insert.getItemsList();
            // 对指定字段进行值填充
            if (ObjectUtils.isNotEmpty(indexMap.get(Constant.CREATE_TIME))) {
                ((ExpressionList) itemsList).getExpressions().set(indexMap.get(Constant.CREATE_TIME), new StringValue(this.getTime()));
            }
            if (ObjectUtils.isNotEmpty(indexMap.get(Constant.CREATE_USER))) {
                ((ExpressionList) itemsList).getExpressions().set(indexMap.get(Constant.CREATE_USER), new StringValue(userHandler.getUserId().toString()));
            }
            if (tableLogicField.contains(Constant.STATUS)) {
                ((ExpressionList) itemsList).getExpressions().add(new StringValue("1"));
            }
            if (tableLogicField.contains(Constant.IS_DELETED)) {
                ((ExpressionList) itemsList).getExpressions().add(new StringValue("0"));
            }
        } else {
            throw ExceptionUtils.mpe("Failed to process multiple-table update, please exclude the tableName or statementId");
        }
    }

    /**
     * 获取表名中的逻辑删除字段
     *
     * @param tableName 表名
     * @return 逻辑删除字段
     */
    private List<String> getTableLogicField(String tableName) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(tableName);
        String allSqlSelect = tableInfo.getAllSqlSelect();
        if (ObjectUtils.isNotEmpty(allSqlSelect)) {
            return Arrays.asList(allSqlSelect.split(StringPool.COMMA));
        }
        return Collections.emptyList();
    }

    private String getTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }
}
