package org.openmrs.module.fhirExtension.web.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.model.FhirTaskRequestedPeriod;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.web.contract.TaskRequest;
import org.openmrs.module.fhirExtension.web.contract.TaskResponse;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class TaskMapperTest {
	
	@Mock
	private EncounterService encounterService;
	
	@Mock
	private VisitService visitService;
	
	@Mock
	private PatientService patientService;
	
	@InjectMocks
	private TaskMapper taskMapper;
	
	private static final String OBSERVATION_UUID = "obs-uuid-1234";
	
	private static final String ORDER_UUID = "order-uuid-5678";
	
	@Test
	public void fromRequest_shouldSetFocusReferenceWhenObservationUuidProvided() {
		TaskRequest request = new TaskRequest();
		request.setObservationUuid(OBSERVATION_UUID);
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		FhirReference focusRef = task.getFhirTask().getFocusReference();
		assertNotNull(focusRef);
		assertEquals(Obs.class.getTypeName(), focusRef.getType());
		assertEquals(OBSERVATION_UUID, focusRef.getReference());
		assertEquals(OBSERVATION_UUID, focusRef.getTargetUuid());
	}
	
	@Test
	public void fromRequest_shouldNotSetFocusReferenceWhenObservationUuidAbsent() {
		TaskRequest request = new TaskRequest();
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNull(task.getFhirTask().getFocusReference());
	}
	
	@Test
	public void fromRequest_shouldAddOrderUuidToBasedOnReferences() {
		TaskRequest request = new TaskRequest();
		request.setOrderUuid(ORDER_UUID);
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNotNull(task.getFhirTask().getBasedOnReferences());
		assertEquals(1, task.getFhirTask().getBasedOnReferences().size());
		FhirReference ref = task.getFhirTask().getBasedOnReferences().iterator().next();
		assertEquals(Order.class.getTypeName(), ref.getType());
		assertEquals(ORDER_UUID, ref.getReference());
		assertEquals(ORDER_UUID, ref.getTargetUuid());
	}
	
	@Test
	public void constructResponse_shouldIncludeObservationUuidFromFocusReference() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);
		
		FhirReference focusRef = new FhirReference();
		focusRef.setTargetUuid(OBSERVATION_UUID);
		fhirTask.setFocusReference(focusRef);
		
		FhirTaskRequestedPeriod period = new FhirTaskRequestedPeriod();
		period.setRequestedStartTime(new Date());
		period.setRequestedEndTime(new Date());
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, period));
		
		assertEquals(OBSERVATION_UUID, response.getObservationUuid());
	}
	
	@Test
	public void constructResponse_shouldNotFailWhenFocusReferenceAbsent() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);
		
		FhirTaskRequestedPeriod period = new FhirTaskRequestedPeriod();
		period.setRequestedStartTime(new Date());
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, period));
		
		assertNull(response.getObservationUuid());
	}
	
	@Test
	public void constructResponse_shouldNotFailWhenPeriodIsNull() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, null));
		
		assertNull(response.getRequestedStartTime());
		assertNull(response.getRequestedEndTime());
	}
}
