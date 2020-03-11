package com.fbi.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbi.engine.domain.Connection;
import com.fbi.engine.query.QueryService;
import com.fbi.engine.service.cache.CacheMetadata;
import com.project.bi.query.FlairQuery;

@ExtendWith(MockitoExtension.class)
public class ListTablesServiceTest {

	@Mock
	private QueryService queryService;
	@Mock
	private ConnectionService connectionService;

	private ListTablesService service;

	@BeforeEach
	public void setUp() throws Exception {
		service = new ListTablesService(queryService, connectionService, new ObjectMapper());
	}

	@Test
	public void listTablesReturnsNullIfConnectionNotFound() {
		Set<String> result = service.listTables("someconnectionid", "table_", 10, new Connection());
		assertNull(result);
	}

	@Test
	public void listTablesReturnsNullIfConnectionIdAndObjectAreEmpty() {
		Set<String> result = service.listTables("", "table_", 10, null);
		assertNull(result);
	}

	@Test
	public void listTablesReturnsTableNames() {
		Connection connection = new Connection();
		when(queryService.executeQuery(eq(connection), any(FlairQuery.class))).thenReturn(new CacheMetadata()
				.setResult("{\"data\":[{\"tablename\":\"table_first\"},{\"tablename\":\"table_second\"}]}"));

		Set<String> result = service.listTables("", "table_", 10, connection);
		List<String> resultList = new ArrayList<>(result);
		assertEquals(2, resultList.size());
		assertTrue(resultList.contains("table_first"));
		assertTrue(resultList.contains("table_second"));
	}

	@Test
	public void listTablesReturnsTableNamesFiltered() {
		Connection connection = new Connection();
		when(queryService.executeQuery(eq(connection), any(FlairQuery.class))).thenReturn(new CacheMetadata().setResult(
				"{\"data\":[{\"tablename\":\"table_first\"},{\"tablename\":\"table_second\"},{\"tablename\":\"my_table\"},{\"tablename\":\"my_table_\"}]}"));

		Set<String> result = service.listTables("", "table_", 10, connection);
		List<String> resultList = new ArrayList<>(result);
		assertEquals(3, resultList.size());
		assertTrue(resultList.contains("table_first"));
		assertTrue(resultList.contains("table_second"));
		assertTrue(resultList.contains("my_table_"));
	}

	@Test
	public void listTablesReturnsTableNamesThatRepeat() {
		Connection connection = new Connection();
		when(queryService.executeQuery(eq(connection), any(FlairQuery.class))).thenReturn(new CacheMetadata().setResult(
				"{\"data\":[{\"tablename\":\"table_first\"},{\"tablename\":\"table_second\"},{\"tablename\":\"table_second\"}]}"));

		Set<String> result = service.listTables("", "table_", 10, connection);
		List<String> resultList = new ArrayList<>(result);
		assertEquals(2, resultList.size());
		assertTrue(resultList.contains("table_first"));
		assertTrue(resultList.contains("table_second"));
	}

	@Test
	public void listTablesReturnsTableNamesThatRepeatAndOverMaxEntries() {
		Connection connection = new Connection();
		when(queryService.executeQuery(eq(connection), any(FlairQuery.class))).thenReturn(new CacheMetadata().setResult(
				"{\"data\":[{\"tablename\":\"table_first\"},{\"tablename\":\"table_second\"},{\"tablename\":\"table_second\"},{\"tablename\":\"table_third\"},{\"tablename\":\"table_fourth\"}]}"));

		Set<String> result = service.listTables("", "table_", 2, connection);
		List<String> resultList = new ArrayList<>(result);
		assertEquals(2, resultList.size());
		assertTrue(!Objects.equals(resultList.get(0), resultList.get(1)));
	}

	@Test
	public void listTablesReturnsNullForInvalidJson() {
		Connection connection = new Connection();
		when(queryService.executeQuery(eq(connection), any(FlairQuery.class)))
				.thenReturn(new CacheMetadata().setResult("{\"data\":[{\""));

		Set<String> result = service.listTables("", "table_", 10, connection);
		assertEquals(0, result.size());
	}
}