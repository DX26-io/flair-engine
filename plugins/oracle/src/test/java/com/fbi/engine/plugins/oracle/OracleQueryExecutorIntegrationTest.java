package com.fbi.engine.plugins.oracle;

import java.io.File;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbi.engine.api.DataSourceConnection;
import com.fbi.engine.api.DataSourceDriver;
import com.fbi.engine.plugins.core.DataSourceDriverImpl;
import com.fbi.engine.plugins.core.json.JacksonFactory;
import com.fbi.engine.plugins.core.sql.DriverLoadingStrategy;
import com.fbi.engine.plugins.core.sql.DynamicDriverLoadingStrategy;
import com.fbi.engine.plugins.test.AbstractQueryExecutorUnitTest;

public class OracleQueryExecutorIntegrationTest extends AbstractQueryExecutorUnitTest<OracleQueryExecutor> {

	private DataSourceDriver driver = DataSourceDriverImpl.of(new File("src/main/resources/ojdbc6-11.2.0.3.jar"),
			"ojdbc6", "oracle", "11.2.0.3");

	private ObjectMapper obj = JacksonFactory.getInstance().getObjectMapper();

	private DriverLoadingStrategy strat = new DynamicDriverLoadingStrategy();

	private static int port = 1521;
	private static String host = "it-oracle-database";

	@Override
	protected OracleQueryExecutor configureQueryExecutor() {
		return new OracleQueryExecutor(strat, new DataSourceConnection() {

			@Override
			public String getConnectionString() {
				return "jdbc:oracle:thin:@//" + host + ":" + port + "/xe";
			}

			@Override
			public Properties getConnectionProperties() {
				Properties properties = new Properties();
				properties.put("username", "sys as sysdba");
				properties.put("password", "oracle");
				return properties;
			}
		}, obj, driver);
	}

	@Override
	protected String testConnection() {
		return "SELECT * FROM dual";
	}

	@Override
	protected OracleQueryExecutor misconfigureQueryExecutor() {
		return new OracleQueryExecutor(strat, new DataSourceConnection() {

			@Override
			public String getConnectionString() {
				return "jdbc:oracle:thin:@//" + host + ":" + port + "/notExist";
			}

			@Override
			public Properties getConnectionProperties() {
				Properties properties = new Properties();
				properties.put("username", "sys as sysdba");
				properties.put("password", "oracle");
				return properties;
			}
		}, obj, driver);
	}

}
