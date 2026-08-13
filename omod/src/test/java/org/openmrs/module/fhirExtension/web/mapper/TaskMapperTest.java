package org.openmrs.module.fhirExtension.web.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhirExtension.web.contract.TaskFhirReference;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.model.FhirTaskRequestedPeriod;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.web.contract.TaskRequest;
import org.openmrs.module.fhirExtension.web.contract.TaskResponse;

import org.openmrs.Patient;
import org.openmrs.Visit;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

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
	public void fromRequest_shouldSetFocusReferenceWhenFocusProvided() {
		TaskRequest request = new TaskRequest();
		TaskFhirReference focus = new TaskFhirReference();
		focus.setType("Observation");
		focus.setReference("Observation/" + OBSERVATION_UUID);
		request.setFocus(focus);
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		FhirReference focusRef = task.getFhirTask().getFocusReference();
		assertNotNull(focusRef);
		assertEquals("Observation", focusRef.getType());
		assertEquals("Observation/" + OBSERVATION_UUID, focusRef.getReference());
		assertEquals(OBSERVATION_UUID, focusRef.getTargetUuid());
	}
	
	@Test
	public void fromRequest_shouldNotSetFocusReferenceWhenFocusAbsent() {
		TaskRequest request = new TaskRequest();
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNull(task.getFhirTask().getFocusReference());
	}
	
	@Test
	public void fromRequest_shouldAddBasedOnReference() {
		TaskRequest request = new TaskRequest();
		TaskFhirReference basedOn = new TaskFhirReference();
		basedOn.setType("ServiceRequest");
		basedOn.setReference("ServiceRequest/" + ORDER_UUID);
		request.setBasedOn(basedOn);
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNotNull(task.getFhirTask().getBasedOnReferences());
		assertEquals(1, task.getFhirTask().getBasedOnReferences().size());
		FhirReference ref = task.getFhirTask().getBasedOnReferences().iterator().next();
		assertEquals("ServiceRequest", ref.getType());
		assertEquals("ServiceRequest/" + ORDER_UUID, ref.getReference());
		assertEquals(ORDER_UUID, ref.getTargetUuid());
	}
	
	@Test
	public void constructResponse_shouldIncludeObservationUuidFromFocusReference() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);
		
		FhirReference focusRef = new FhirReference();
		focusRef.setReference("Observation/" + OBSERVATION_UUID);
		focusRef.setType("Observation");
		fhirTask.setFocusReference(focusRef);
		
		FhirTaskRequestedPeriod period = new FhirTaskRequestedPeriod();
		period.setRequestedStartTime(new Date());
		period.setRequestedEndTime(new Date());
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, period));
		
		assertNotNull(response.getFocus());
		assertEquals("Observation/" + OBSERVATION_UUID, response.getFocus().getReference());
		assertEquals("Observation", response.getFocus().getType());
	}
	
	@Test
	public void constructResponse_shouldIncludeBasedOnReferenceInResponse() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);

		FhirReference basedOnRef = new FhirReference();
		basedOnRef.setReference("ServiceRequest/" + ORDER_UUID);
		basedOnRef.setType("ServiceRequest");
		Set<FhirReference> basedOnRefs = new HashSet<>();
		basedOnRefs.add(basedOnRef);
		fhirTask.setBasedOnReferences(basedOnRefs);

		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, null));

		assertNotNull(response.getBasedOn());
		assertEquals("ServiceRequest/" + ORDER_UUID, response.getBasedOn().getReference());
		assertEquals("ServiceRequest", response.getBasedOn().getType());
	}
	
	@Test
	public void constructResponse_shouldNotFailWhenBasedOnReferencesAbsent() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, null));
		
		assertNull(response.getBasedOn());
	}
	
	@Test
	public void constructResponse_shouldNotSetBasedOnWhenReferencesSetIsEmpty() {
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		fhirTask.setForReference(forRef);
		fhirTask.setBasedOnReferences(new HashSet<>());

		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, null));

		assertNull(response.getBasedOn());
	}
	
	@Test
	public void constructResponse_shouldReturnBasedOnReferenceSetDuringFromRequest() {
		TaskRequest request = new TaskRequest();
		TaskFhirReference basedOn = new TaskFhirReference();
		basedOn.setType("ServiceRequest");
		basedOn.setReference("ServiceRequest/" + ORDER_UUID);
		request.setBasedOn(basedOn);
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		// forReference required by constructResponse — set it since no patient/visit in request
		FhirReference forRef = new FhirReference();
		forRef.setTargetUuid("visit-uuid");
		task.getFhirTask().setForReference(forRef);
		
		TaskResponse response = taskMapper.constructResponse(task);
		
		assertNotNull(response.getBasedOn());
		assertEquals("ServiceRequest/" + ORDER_UUID, response.getBasedOn().getReference());
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
		
		assertNull(response.getFocus());
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
	
	@Test
	public void fromRequest_shouldSetForReferenceWhenVisitUuidIsProvided() {
		TaskRequest request = new TaskRequest();
		request.setVisitUuid("visit-uuid-1234");
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		TaskResponse response = taskMapper.constructResponse(task);
		
		assertNotNull(response.getForReference());
		assertEquals(Visit.class.getTypeName(), response.getForReference().getType());
		assertEquals(Visit.class.getTypeName() + "/visit-uuid-1234", response.getForReference().getReference());
	}
	
	@Test
	public void fromRequest_shouldSetForReferenceFromActiveVisitWhenPatientUuidIsProvided() {
		String patientUuid = "patient-uuid-1234";
		String visitUuid = "visit-uuid-5678";
		
		Patient patient = new Patient();
		Visit activeVisit = new Visit();
		activeVisit.setUuid(visitUuid);
		
		when(patientService.getPatientByUuid(patientUuid)).thenReturn(patient);
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Arrays.asList(activeVisit));
		
		TaskRequest request = new TaskRequest();
		request.setPatientUuid(patientUuid);
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		TaskResponse response = taskMapper.constructResponse(task);
		
		assertNotNull(response.getForReference());
		assertEquals(Visit.class.getTypeName(), response.getForReference().getType());
		assertEquals(Visit.class.getTypeName() + "/" + visitUuid, response.getForReference().getReference());
	}
}
