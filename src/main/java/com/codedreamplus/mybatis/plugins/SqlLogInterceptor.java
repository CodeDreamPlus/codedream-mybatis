package com.codedreamplus.mybatis.plugins;

import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.JdbcParameter;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.sql.SQLUtils;
import com.codedreamplus.mybatis.properties.MybatisPlusProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 打印可执行的 sql 日志
 */
@Slf4j
public class SqlLogInterceptor extends FilterEventAdapter {
	private static final SQLUtils.FormatOption FORMAT_OPTION = new SQLUtils.FormatOption(false, false);

	private static final List<String> SQL_LOG_EXCLUDE = new ArrayList<>(Arrays.asList("ACT_RU_JOB", "ACT_RU_TIMER_JOB"));

	private final MybatisPlusProperties properties;

	public SqlLogInterceptor(MybatisPlusProperties properties) {
		this.properties = properties;
		if (properties.getSqlLogExclude().size() > 0) {
			SQL_LOG_EXCLUDE.addAll(properties.getSqlLogExclude());
		}
	}

	@Override
	protected void statementExecuteBefore(StatementProxy statement, String sql) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteBatchBefore(StatementProxy statement) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteUpdateBefore(StatementProxy statement, String sql) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteQueryBefore(StatementProxy statement, String sql) {
		statement.setLastExecuteStartNano();
	}

	@Override
	protected void statementExecuteAfter(StatementProxy statement, String sql, boolean firstResult) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	protected void statementExecuteBatchAfter(StatementProxy statement, int[] result) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	protected void statementExecuteQueryAfter(StatementProxy statement, String sql, ResultSetProxy resultSet) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	protected void statementExecuteUpdateAfter(StatementProxy statement, String sql, int updateCount) {
		statement.setLastExecuteTimeNano();
	}

	@Override
	@SneakyThrows
	public void statement_close(FilterChain chain, StatementProxy statement) {
		// 是否开启日志
		if (!properties.getSqlLog()) {
			chain.statement_close(statement);
			return;
		}
		// 是否开启调试
		if (!log.isInfoEnabled()) {
			chain.statement_close(statement);
			return;
		}
		// 打印可执行的 sql
		String sql = statement.getBatchSql();
		// sql 为空直接返回
		if (ObjectUtils.isEmpty(sql)) {
			chain.statement_close(statement);
			return;
		}
		// sql 包含排除的关键字直接返回
		if (excludeSql(sql)) {
			chain.statement_close(statement);
			return;
		}
		int parametersSize = statement.getParametersSize();
		List<Object> parameters = new ArrayList<>(parametersSize);
		for (int i = 0; i < parametersSize; ++i) {
			JdbcParameter jdbcParam = statement.getParameter(i);
			parameters.add(jdbcParam != null ? jdbcParam.getValue() : null);
		}
		String dbType = statement.getConnectionProxy().getDirectDataSource().getDbType();
		String formattedSql = SQLUtils.format(sql, DbType.of(dbType), parameters, FORMAT_OPTION);
		printSql(formattedSql, statement);
		chain.statement_close(statement);
	}

	private static void printSql(String sql, StatementProxy statement) {
		// 打印 sql
		String sqlLogger = "\n\n==============  Sql Start  ==============" +
			"\nExecute SQL : {}" +
			"\nExecute Time: {}" +
			"\n==============  Sql  End   ==============\n";
		log.info(sqlLogger, sql.trim(), format(statement.getLastExecuteTimeNano()));
	}

	private static boolean excludeSql(String sql) {
		// 判断关键字
		for (String exclude : SQL_LOG_EXCLUDE) {
			if (sql.contains(exclude)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 格式化执行时间，单位为 ms 和 s，保留三位小数
	 *
	 * @param nanos 纳秒
	 * @return 格式化后的时间
	 */
	public static String format(long nanos) {
		if (nanos < 1) {
			return "0ms";
		}
		double millis = (double) nanos / (1000 * 1000);
		// 不够 1 ms，最小单位为 ms
		if (millis > 1000) {
			return String.format("%.3fs", millis / 1000);
		} else {
			return String.format("%.3fms", millis);
		}
	}

}
