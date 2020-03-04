package com.fbi.engine.service;

import static com.fbi.engine.domain.ConnectionStatus.DELETED;
import static com.fbi.engine.service.constant.GrpcConstants.CONNECTION_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.fbi.engine.AbstractIntegrationTest;
import com.fbi.engine.domain.details.PostgresConnectionDetails;
import com.fbi.engine.domain.schema.ConnectionPropertiesSchema;
import com.fbi.engine.domain.schema.ConnectionProperty;
import com.fbi.engine.service.dto.ConnectionDTO;
import com.fbi.engine.service.dto.ConnectionTypeDTO;
import com.flair.bi.messages.Connection;
import com.flair.bi.messages.ConnectionType;
import com.flair.bi.messages.ConnectionTypesResponses;
import com.flair.bi.messages.DeleteConnectionRequest;
import com.flair.bi.messages.DeleteConnectionResponse;
import com.flair.bi.messages.GetAllConnectionTypesRequest;
import com.flair.bi.messages.GetConnectionRequest;
import com.flair.bi.messages.GetConnectionResponse;
import com.flair.bi.messages.ListTablesRequest;
import com.flair.bi.messages.ListTablesResponse;
import com.flair.bi.messages.SaveConnectionRequest;
import com.flair.bi.messages.SaveConnectionResponse;
import com.flair.bi.messages.TestConnectionRequest;
import com.flair.bi.messages.TestConnectionResponse;
import com.flair.bi.messages.UpdateConnectionRequest;
import com.flair.bi.messages.UpdateConnectionResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@Transactional
public class ConnectionGrpcServiceIntTest extends AbstractIntegrationTest {

	@Autowired
	private ConnectionTypeService connectionTypeService;

	@Autowired
	private ConnectionGrpcService connectionGrpcService;

	@Autowired
	private ConnectionParameterService connectionParameterService;

	@MockBean
	private TestConnectionService connectionTestService;

	@MockBean
	private ListTablesService listTablesService;

	@Autowired
	private ConnectionService connectionService;

	@Test
	public void testConnectionSucccessful() {
		StreamObserver<TestConnectionResponse> responseObserver = Mockito.mock(StreamObserver.class);

		doAnswer(invocationOnMock -> {
			TestConnectionResponse argument = invocationOnMock.getArgument(0);
			assertEquals("{\"data\":[]}", argument.getResult());
			return null;
		}).when(responseObserver).onNext(any(TestConnectionResponse.class));

		when(connectionTestService.testConnection(any(com.fbi.engine.domain.Connection.class)))
				.thenReturn("{\"data\":[]}");

		connectionGrpcService.testConnection(TestConnectionRequest.newBuilder()
				.setConnection(Connection.newBuilder().setConnectionPassword("pwd").setConnectionUsername("usr")
						.setLinkId("lnkid").setName("nm").setConnectionType(1).putDetails("@type", "MySql")
						.putDetails("serverPort", "1234").putDetails("databaseName", "services")
						.putDetails("serverIp", "localhost").build())
				.build(), responseObserver);

		verify(responseObserver, times(1)).onCompleted();
	}

	@Test
	public void getConnectionTypes() {
		ConnectionTypeDTO dto = new ConnectionTypeDTO();
		dto.setBundleClass("bundle");
		dto.setName("nm");
		ConnectionPropertiesSchema schema = new ConnectionPropertiesSchema();
		schema.setConnectionDetailsClass("class");
		schema.setConnectionDetailsType("type");
		schema.setImagePath("path");
		ConnectionProperty property = new ConnectionProperty();
		property.setDefaultValue("default");
		property.setDisplayName("display");
		property.setFieldName("field");
		property.setFieldType("type");
		property.setOrder(10);
		property.setRequired(true);
		schema.setConnectionProperties(Arrays.asList(property));
		dto.setConnectionPropertiesSchema(schema);
		connectionTypeService.save(dto);

		StreamObserver<ConnectionTypesResponses> streamObserver = Mockito.mock(StreamObserver.class);

		doAnswer(invocationOnMock -> {
			ConnectionTypesResponses argument = invocationOnMock.getArgument(0);
			assertTrue(argument.getConnectionTypesCount() > 1);
			List<ConnectionType> connectionTypesList = argument.getConnectionTypesList();

			ConnectionType connectionType = connectionTypesList.get(connectionTypesList.size() - 1);
			assertEquals("bundle", connectionType.getBundleClass());
			assertEquals("nm", connectionType.getName());

			ConnectionType.ConnectionPropertiesSchema connectionPropertiesSchema = connectionType
					.getConnectionPropertiesSchema();
			assertEquals("class", connectionPropertiesSchema.getConnectionDetailsClass());
			assertEquals("type", connectionPropertiesSchema.getConnectionDetailsType());
			assertEquals("path", connectionPropertiesSchema.getImagePath());

			List<ConnectionType.ConnectionPropertiesSchema.ConnectionProperty> connectionPropertiesList = connectionPropertiesSchema
					.getConnectionPropertiesList();
			assertEquals(1, connectionPropertiesList.size());
			assertEquals("default", connectionPropertiesList.get(0).getDefaultValue());
			assertEquals("display", connectionPropertiesList.get(0).getDisplayName());
			assertEquals("field", connectionPropertiesList.get(0).getFieldName());
			assertEquals("type", connectionPropertiesList.get(0).getFieldType());
			assertEquals(10, connectionPropertiesList.get(0).getOrder());
			assertTrue(connectionPropertiesList.get(0).getRequired());
			return null;
		}).when(streamObserver).onNext(any(ConnectionTypesResponses.class));

		connectionGrpcService.getConnectionTypes(GetAllConnectionTypesRequest.newBuilder().build(), streamObserver);

		verify(streamObserver, times(1)).onCompleted();
	}

	@Test
	public void testSaveConnection() {
		StreamObserver<SaveConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		doAnswer(invocationOnMock -> {
			SaveConnectionResponse argument = invocationOnMock.getArgument(0);
			assertNotEquals(0, argument.getConnection().getId());
			assertEquals("pwd", argument.getConnection().getConnectionPassword());
			assertEquals("usr", argument.getConnection().getConnectionUsername());
			assertNotNull(argument.getConnection().getLinkId());
			assertEquals("nm", argument.getConnection().getName());
			assertEquals(1, argument.getConnection().getConnectionType());
			assertEquals("3412", argument.getConnection().getDetailsMap().get("serverPort"));
			assertEquals("Postgres", argument.getConnection().getDetailsMap().get("@type"));
			assertEquals("localhost", argument.getConnection().getDetailsMap().get("serverIp"));
			assertEquals("value1", argument.getConnection().getConnectionParametersMap().get("param1"));
			assertEquals("value2", argument.getConnection().getConnectionParametersMap().get("param2"));
			return null;
		}).when(streamObserver).onNext(any(SaveConnectionResponse.class));

		Connection connection = Connection.newBuilder().setConnectionPassword("pwd").setConnectionUsername("usr")
				.setName("nm").setLinkId("lnk").setConnectionType(1L)
				.putAllDetails(ImmutableMap.of("serverPort", "3412", "@type", "Postgres", "serverIp", "localhost"))
				.putAllConnectionParameters(ImmutableMap.of("param1", "value1", "param2", "value2")).build();

		connectionGrpcService.saveConnection(SaveConnectionRequest.newBuilder().setConnection(connection).build(),
				streamObserver);

		verify(streamObserver, times(1)).onCompleted();
	}

	@Test
	public void testSaveConnectionFailsIfConnectionIdProvided() {

		StreamObserver<SaveConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		doAnswer(invocationOnMock -> {
			StatusRuntimeException e = invocationOnMock.getArgument(0);
			assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
			assertEquals(CONNECTION_EXISTS, e.getStatus().getDescription());
			return null;
		}).when(streamObserver).onError(any(Throwable.class));

		Connection connection = Connection.newBuilder().setId(100L).build();

		connectionGrpcService.saveConnection(SaveConnectionRequest.newBuilder().setConnection(connection).build(),
				streamObserver);

		verify(streamObserver, times(0)).onCompleted();
	}

	@Test
	public void testUpdateConnection() {

		StreamObserver<UpdateConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		ConnectionDTO dto = new ConnectionDTO();
		dto.setConnectionType(connectionTypeService.findOne(1L).get());
		dto.setConnectionPassword("pwd");
		dto.setConnectionUsername("usr");
		dto.setName("test db local");
		dto.setDetails(new PostgresConnectionDetails("localhost", 1111, "dbname"));
		ConnectionDTO savedConnectionDto = connectionService.save(dto);

		doAnswer(invocationOnMock -> {
			UpdateConnectionResponse argument = invocationOnMock.getArgument(0);
			assertEquals((long) savedConnectionDto.getId(), argument.getConnection().getId());
			assertEquals("pwd2", argument.getConnection().getConnectionPassword());
			assertEquals("usr", argument.getConnection().getConnectionUsername());
			assertEquals(savedConnectionDto.getLinkId(), argument.getConnection().getLinkId());
			assertEquals("nm", argument.getConnection().getName());
			assertEquals(1, argument.getConnection().getConnectionType());
			assertEquals("3412", argument.getConnection().getDetailsMap().get("serverPort"));
			assertEquals("Postgres", argument.getConnection().getDetailsMap().get("@type"));
			assertEquals("localhost", argument.getConnection().getDetailsMap().get("serverIp"));
			return null;
		}).when(streamObserver).onNext(any(UpdateConnectionResponse.class));

		Connection connection = Connection.newBuilder().setId(savedConnectionDto.getId()).setConnectionPassword("pwd2")
				.setConnectionUsername("usr").setName("nm").setLinkId("lnk").setConnectionType(1L)
				.putAllDetails(ImmutableMap.of("serverPort", "3412", "@type", "Postgres", "serverIp", "localhost"))
				.build();

		connectionGrpcService.updateConnection(UpdateConnectionRequest.newBuilder().setConnection(connection).build(),
				streamObserver);

		verify(streamObserver, times(1)).onCompleted();
	}

	@Test
	public void testGetConnectionByConnectionId() {
		StreamObserver<GetConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		ConnectionDTO dto = new ConnectionDTO();
		dto.setConnectionType(connectionTypeService.findOne(1L).get());
		dto.setConnectionPassword("pwd");
		dto.setConnectionUsername("usr");
		dto.setName("test db local");
		dto.setDetails(new PostgresConnectionDetails("localhost", 1111, "dbname"));

		dto = connectionService.save(dto);

		ConnectionDTO finalDto = dto;

		doAnswer(invocationOnMock -> {
			GetConnectionResponse argument = invocationOnMock.getArgument(0);
			assertEquals("pwd", argument.getConnection().getConnectionPassword());
			assertEquals("usr", argument.getConnection().getConnectionUsername());
			assertEquals(finalDto.getLinkId(), argument.getConnection().getLinkId());
			assertEquals((long) finalDto.getId(), argument.getConnection().getId());
			assertEquals("test db local", argument.getConnection().getName());
			assertEquals(1, argument.getConnection().getConnectionType());
			assertEquals("1111", argument.getConnection().getDetailsMap().get("serverPort"));
			assertEquals("Postgres", argument.getConnection().getDetailsMap().get("@type"));
			assertEquals("localhost", argument.getConnection().getDetailsMap().get("serverIp"));
			return null;
		}).when(streamObserver).onNext(any(GetConnectionResponse.class));

		connectionGrpcService.getConnection(GetConnectionRequest.newBuilder().setId(dto.getId()).build(),
				streamObserver);

		verify(streamObserver, times(0)).onError(any(Throwable.class));
		verify(streamObserver, times(1)).onCompleted();
	}

	@Test
	public void testGetConnectionByLinkId() {
		StreamObserver<GetConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		ConnectionDTO dto = new ConnectionDTO();
		dto.setConnectionType(connectionTypeService.findOne(1L).get());
		dto.setConnectionPassword("pwd");
		dto.setConnectionUsername("usr");
		dto.setName("test db local");
		dto.setDetails(new PostgresConnectionDetails("localhost", 1111, "dbname"));

		dto = connectionService.save(dto);

		ConnectionDTO finalDto = dto;

		doAnswer(invocationOnMock -> {
			GetConnectionResponse argument = invocationOnMock.getArgument(0);
			assertEquals("pwd", argument.getConnection().getConnectionPassword());
			assertEquals("usr", argument.getConnection().getConnectionUsername());
			assertEquals(finalDto.getLinkId(), argument.getConnection().getLinkId());
			assertEquals((long) finalDto.getId(), argument.getConnection().getId());
			assertEquals("test db local", argument.getConnection().getName());
			assertEquals(1, argument.getConnection().getConnectionType());
			assertEquals("1111", argument.getConnection().getDetailsMap().get("serverPort"));
			assertEquals("Postgres", argument.getConnection().getDetailsMap().get("@type"));
			assertEquals("localhost", argument.getConnection().getDetailsMap().get("serverIp"));
			return null;
		}).when(streamObserver).onNext(any(GetConnectionResponse.class));

		connectionGrpcService.getConnection(GetConnectionRequest.newBuilder().setLinkId(dto.getLinkId()).build(),
				streamObserver);

		verify(streamObserver, times(0)).onError(any(Throwable.class));
		verify(streamObserver, times(1)).onCompleted();
	}

	@Test
	public void testGetConnectionFails() {
		StreamObserver<GetConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		connectionGrpcService.getConnection(GetConnectionRequest.newBuilder().setLinkId("234234324").build(),
				streamObserver);

		verify(streamObserver, times(1)).onError(any(Throwable.class));
		verify(streamObserver, times(0)).onCompleted();
	}

	@Test
	public void testDeleteConnection() {
		StreamObserver<DeleteConnectionResponse> streamObserver = Mockito.mock(StreamObserver.class);

		ConnectionDTO dto = new ConnectionDTO();
		dto.setConnectionType(connectionTypeService.findOne(1L).get());
		dto.setConnectionPassword("pwd");
		dto.setConnectionUsername("usr");
		dto.setName("test db local");
		dto.setLinkId("1234");
		dto.setDetails(new PostgresConnectionDetails("localhost", 1111, "dbname"));

		dto = connectionService.save(dto);

		connectionParameterService.save(dto.getLinkId(), ImmutableMap.of("param1", "value1", "param2", "value2"));

		ConnectionDTO finalDto = dto;

		doAnswer(invocationOnMock -> {
			DeleteConnectionResponse argument = invocationOnMock.getArgument(0);
			assertEquals((long) finalDto.getId(), argument.getConnectionId());
			assertTrue(argument.getSuccess());
			return null;
		}).when(streamObserver).onNext(any(DeleteConnectionResponse.class));

		connectionGrpcService.deleteConnection(
				DeleteConnectionRequest.newBuilder().setConnectionId(dto.getId()).build(), streamObserver);

		verify(streamObserver, times(0)).onError(any(Throwable.class));
		verify(streamObserver, times(1)).onCompleted();

		assertEquals(DELETED, connectionService.findOne(dto.getId()).get().getStatus());

		Map<String, String> parameters = connectionParameterService.getParametersByLinkId(dto.getLinkId());
		assertEquals(0, parameters.size());
	}

	@Test
	public void testListTablesReturnsTableNames() {
		StreamObserver<ListTablesResponse> streamObserver = Mockito.mock(StreamObserver.class);

		doAnswer(invocationOnMock -> {
			ListTablesResponse argument = invocationOnMock.getArgument(0);
			assertEquals(2, argument.getTablesList().size());
			assertEquals("firstTable", argument.getTablesList().get(0).getTableName());
			assertEquals("secondTable", argument.getTablesList().get(1).getTableName());
			return null;
		}).when(streamObserver).onNext(any(ListTablesResponse.class));

		when(listTablesService.listTables(eq("connid"), eq("%hello%"), eq(10),
				isNull(com.fbi.engine.domain.Connection.class)))
						.thenReturn(ImmutableSet.of("firstTable", "secondTable"));

		connectionGrpcService.listTables(ListTablesRequest.newBuilder().setConnectionLinkId("connid")
				.setTableNameLike("%hello%").setMaxEntries(10).build(), streamObserver);

		verify(streamObserver, times(0)).onError(any(Throwable.class));
		verify(streamObserver, times(1)).onCompleted();
	}

	@Test
	public void testListTablesReturnsError() {
		StreamObserver<ListTablesResponse> streamObserver = Mockito.mock(StreamObserver.class);

		when(listTablesService.listTables(eq("connid"), eq("%hello%"), eq(10),
				isNull(com.fbi.engine.domain.Connection.class))).thenReturn(null);

		connectionGrpcService.listTables(ListTablesRequest.newBuilder().setConnectionLinkId("connid")
				.setTableNameLike("%hello%").setMaxEntries(10).build(), streamObserver);

		verify(streamObserver, times(1)).onError(any(Throwable.class));
		verify(streamObserver, times(0)).onCompleted();
		verify(streamObserver, times(0)).onNext(any(ListTablesResponse.class));
	}
}
