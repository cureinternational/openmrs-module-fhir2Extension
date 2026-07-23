package org.openmrs.module.fhirExtension.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.service.TaskService;
import org.openmrs.module.fhirExtension.web.contract.TaskRequest;
import org.openmrs.module.fhirExtension.web.contract.TaskResponse;
import org.openmrs.module.fhirExtension.web.mapper.TaskMapper;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, RestUtil.class })
@PowerMockIgnore("javax.management.*")
public class TaskControllerTest {
	
	@Mock
	private TaskService taskService;
	
	@Mock
	private TaskMapper taskMapper;
	
	@InjectMocks
	private TaskController taskController;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(RestUtil.class);
		when(RestUtil.wrapErrorResponse(any(Exception.class), any())).thenReturn(new SimpleObject());
	}
	
	@Test
	public void saveTasks_shouldCreateTasksInBulkAndReturnResponses() {
		TaskRequest request1 = new TaskRequest();
		request1.setName("Task 1");
		TaskRequest request2 = new TaskRequest();
		request2.setName("Task 2");
		List<TaskRequest> requests = Arrays.asList(request1, request2);
		
		Task task1 = new Task();
		task1.setFhirTask(new FhirTask());
		Task task2 = new Task();
		task2.setFhirTask(new FhirTask());
		
		TaskResponse response1 = new TaskResponse();
		response1.setName("Task 1");
		TaskResponse response2 = new TaskResponse();
		response2.setName("Task 2");
		
		when(taskMapper.fromRequest(request1)).thenReturn(task1);
		when(taskMapper.fromRequest(request2)).thenReturn(task2);
		when(taskService.saveTask(Arrays.asList(task1, task2))).thenReturn(Arrays.asList(task1, task2));
		when(taskMapper.constructResponse(task1)).thenReturn(response1);
		when(taskMapper.constructResponse(task2)).thenReturn(response2);
		
		ResponseEntity<Object> responseEntity = taskController.saveTasks(requests);
		
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		List<?> responseBody = (List<?>) responseEntity.getBody();
		assertEquals(2, responseBody.size());
		assertEquals("Task 1", ((TaskResponse) responseBody.get(0)).getName());
		assertEquals("Task 2", ((TaskResponse) responseBody.get(1)).getName());
		verify(taskService).saveTask(Arrays.asList(task1, task2));
	}
	
	@Test
	public void saveTasks_shouldReturnBadRequestWhenRuntimeExceptionOccurs() {
		when(taskMapper.fromRequest(any(TaskRequest.class))).thenThrow(new RuntimeException("mapping failed"));
		
		ResponseEntity<Object> responseEntity = taskController.saveTasks(Arrays.asList(new TaskRequest()));
		
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertNotNull(responseEntity.getBody());
	}
	
	@Test
	public void saveTasks_shouldReturnBadRequestForEmptyList() {
		ResponseEntity<Object> responseEntity = taskController.saveTasks(Collections.emptyList());
		
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertNotNull(responseEntity.getBody());
	}
	
	@Test
	public void saveTasks_shouldReturnBadRequestWhenServiceThrowsException() {
		TaskRequest request = new TaskRequest();
		request.setName("Task 1");
		Task task = new Task();
		task.setFhirTask(new FhirTask());
		when(taskMapper.fromRequest(request)).thenReturn(task);
		when(taskService.saveTask(any(List.class))).thenThrow(new RuntimeException("db error"));
		
		ResponseEntity<Object> responseEntity = taskController.saveTasks(Arrays.asList(request));
		
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertNotNull(responseEntity.getBody());
	}
}
