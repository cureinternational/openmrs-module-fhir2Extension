package org.openmrs.module.fhirExtension.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.service.TaskService;
import org.openmrs.module.fhirExtension.web.contract.TaskRequest;
import org.openmrs.module.fhirExtension.web.contract.TaskResponse;
import org.openmrs.module.fhirExtension.web.mapper.TaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskControllerTest {
	
	@Mock
	private TaskService taskService;
	
	@Mock
	private TaskMapper taskMapper;
	
	@InjectMocks
	private TaskController taskController;
	
	@Test
	public void saveTasks_shouldCreateTasksInBulkAndReturnResponses() throws IOException {
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
		when(taskMapper.constructResponse(task1)).thenReturn(response1);
		when(taskMapper.constructResponse(task2)).thenReturn(response2);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String requestBody = objectMapper.writeValueAsString(requests);
		ResponseEntity<Object> responseEntity = taskController.saveTasks(requestBody);
		
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		List<?> responseBody = (List<?>) responseEntity.getBody();
		assertEquals(2, responseBody.size());
		assertEquals("Task 1", ((TaskResponse) responseBody.get(0)).getName());
		assertEquals("Task 2", ((TaskResponse) responseBody.get(1)).getName());
		verify(taskService).saveTask(Arrays.asList(task1, task2));
	}
	
	@Test
	public void saveTasks_shouldReturnBadRequestWhenRuntimeExceptionOccurs() throws IOException {
		when(taskMapper.fromRequest(any(TaskRequest.class))).thenThrow(new RuntimeException("mapping failed"));
		
		ObjectMapper objectMapper = new ObjectMapper();
		String requestBody = objectMapper.writeValueAsString(Arrays.asList(new TaskRequest()));
		ResponseEntity<Object> responseEntity = taskController.saveTasks(requestBody);
		
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertTrue(responseEntity.getBody() != null);
	}
}
