package com.fbi.engine.plugins.postgres;

import java.io.File;
import java.util.Properties;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbi.engine.api.DataSourceConnection;
import com.fbi.engine.api.DataSourceDriver;
import com.fbi.engine.plugins.core.DataSourceDriverImpl;
import com.fbi.engine.plugins.core.json.JacksonFactory;
import com.fbi.engine.plugins.core.sql.DriverLoadingStrategy;
import com.fbi.engine.plugins.core.sql.DynamicDriverLoadingStrategy;
import com.fbi.engine.plugins.test.AbstractQueryExecutorUnitTest;

public class PostgresQueryExecutorTest extends AbstractQueryExecutorUnitTest<PostgresQueryExecutor> {

	private DataSourceDriver driver = DataSourceDriverImpl.of(new File("src/test/resources/postgresql-9.4.1212.jar"),
			"postgresql", "org.postgresql", "9.4.1212");

	private ObjectMapper obj = JacksonFactory.getInstance().getObjectMapper();

	private DriverLoadingStrategy strat = new DynamicDriverLoadingStrategy();

	@Override
	protected PostgresQueryExecutor configureQueryExecutor() {
		return new PostgresQueryExecutor(strat, new DataSourceConnection() {

			@Override
			public String getConnectionString() {
				return "jdbc:postgresql://localhost:" + container.getFirstMappedPort() + "/services";
			}

			@Override
			public Properties getConnectionProperties() {
				Properties properties = new Properties();
				properties.put("username", "postgres");
				properties.put("password", "admin");
				return properties;
			}
		}, obj, driver);
	}

	@Override
	protected PostgresQueryExecutor misconfigureQueryExecutor() {
		return new PostgresQueryExecutor(strat, new DataSourceConnection() {

			@Override
			public String getConnectionString() {
				return "jdbc:postgresql://localhost:" + container.getFirstMappedPort() + "/notExistingDatabase";
			}

			@Override
			public Properties getConnectionProperties() {
				Properties properties = new Properties();
				properties.put("username", "postgres");
				properties.put("password", "admin");
				return properties;
			}
		}, obj, driver);
	}

	@Override
	protected GenericContainer<?> configureTargetDataSource() {
		return new GenericContainer<>("postgres:9.6.12").withEnv("POSTGRES_USER", "postgres")
				.withEnv("POSTGRES_PASSWORD", "admin").withEnv("POSTGRES_DB", "services").withExposedPorts(5432)
				.withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"),
						"/docker-entrypoint-initdb.d/init.sql");
	}

}