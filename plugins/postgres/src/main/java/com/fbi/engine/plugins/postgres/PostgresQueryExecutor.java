package com.fbi.engine.plugins.postgres;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbi.engine.api.DataSourceConnection;
import com.fbi.engine.api.DataSourceDriver;
import com.fbi.engine.plugins.core.DataSourceDriverImpl;
import com.fbi.engine.plugins.core.sql.DriverLoadingStrategy;
import com.fbi.engine.plugins.core.sql.SqlQueryExecutor;

public class PostgresQueryExecutor extends SqlQueryExecutor {

	public PostgresQueryExecutor(DriverLoadingStrategy strategy, DataSourceConnection connection,
			ObjectMapper objectMapper, DataSourceDriver driver) {
		super(strategy, connection, objectMapper, driver);
	}

	@Override
	protected String getDriverClassName() {
		return "org.postgresql.Driver";
	}

	@Override
	protected DataSourceDriver getDefaultDriver() {
		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("postgresql-9.4.1212.jar")) {
			return DataSourceDriverImpl.of(is, "postgresql", "org.postgresql", "9.4.1212");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}