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
	public void fromRequest_shouldSetPatientUuidWhenPatientUuidProvided() {
		String patientUuid = "patient-uuid-1234";
		TaskRequest request = new TaskRequest();
		request.setPatientUuid(patientUuid);
		request.setName("Test Task");
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		FhirReference forRef = task.getFhirTask().getForReference();
		assertNotNull(forRef);
		assertEquals("org.openmrs.Patient", forRef.getType());
		assertEquals("org.openmrs.Patient/" + patientUuid, forRef.getReference());
		assertEquals(patientUuid, forRef.getTargetUuid());
	}
	
	@Test
	public void fromRequest_shouldResolvePatientUuidFromVisitWhenOnlyVisitUuidProvided() {
		String visitUuid = "visit-uuid-5678";
		String patientUuid = "patient-uuid-from-visit";
		
		TaskRequest request = new TaskRequest();
		request.setVisitUuid(visitUuid);
		request.setName("Test Task");
		request.setIsSystemGeneratedTask(false);
		
		Visit visit = new Visit();
		Patient patient = new Patient();
		patient.setUuid(patientUuid);
		visit.setPatient(patient);
		
		when(visitService.getVisitByUuid(visitUuid)).thenReturn(visit);
		
		Task task = taskMapper.fromRequest(request);
		
		FhirReference forRef = task.getFhirTask().getForReference();
		assertNotNull(forRef);
		assertEquals("org.openmrs.Patient", forRef.getType());
		assertEquals("org.openmrs.Patient/" + patientUuid, forRef.getReference());
		assertEquals(patientUuid, forRef.getTargetUuid());
	}
	
	@Test
	public void fromRequest_shouldNotSetForReferenceWhenBothPatientAndVisitUuidsAbsent() {
		TaskRequest request = new TaskRequest();
		request.setName("Test Task");
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNull(task.getFhirTask().getForReference());
	}
	
	@Test
	public void fromRequest_shouldHandleNullVisitGracefully() {
		String visitUuid = "non-existent-visit-uuid";
		
		TaskRequest request = new TaskRequest();
		request.setVisitUuid(visitUuid);
		request.setName("Test Task");
		request.setIsSystemGeneratedTask(false);
		
		when(visitService.getVisitByUuid(visitUuid)).thenReturn(null);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNull(task.getFhirTask().getForReference());
	}
	
	@Test
	public void fromRequest_shouldHandleVisitWithNullPatientGracefully() {
		String visitUuid = "visit-uuid-with-null-patient";
		
		TaskRequest request = new TaskRequest();
		request.setVisitUuid(visitUuid);
		request.setName("Test Task");
		request.setIsSystemGeneratedTask(false);
		
		Visit visit = new Visit();
		visit.setPatient(null);
		
		when(visitService.getVisitByUuid(visitUuid)).thenReturn(visit);
		
		Task task = taskMapper.fromRequest(request);
		
		assertNull(task.getFhirTask().getForReference());
	}
	
	@Test
	public void constructResponse_shouldSetForReferenceWhenForReferenceIsSet() {
		String patientUuid = "patient-uuid-1234";
		FhirTask fhirTask = new FhirTask();
		FhirReference forRef = new FhirReference();
		forRef.setType("org.openmrs.Patient");
		forRef.setReference("org.openmrs.Patient/" + patientUuid);
		forRef.setTargetUuid(patientUuid);
		fhirTask.setForReference(forRef);
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, null));
		
		assertNotNull(response.getForReference());
		assertEquals("org.openmrs.Patient", response.getForReference().getType());
		assertEquals("org.openmrs.Patient/" + patientUuid, response.getForReference().getReference());
	}
	
	@Test
	public void constructResponse_shouldNotSetForReferenceWhenForReferenceIsNull() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setForReference(null);
		
		TaskResponse response = taskMapper.constructResponse(new Task(fhirTask, null));
		
		assertNull(response.getForReference());
	}
	
	@Test
	public void fromRequest_shouldPreferPatientUuidOverVisitUuid() {
		String patientUuid = "patient-uuid-1234";
		String visitUuid = "visit-uuid-5678";
		
		TaskRequest request = new TaskRequest();
		request.setPatientUuid(patientUuid);
		request.setVisitUuid(visitUuid);
		request.setName("Test Task");
		request.setIsSystemGeneratedTask(false);
		
		Task task = taskMapper.fromRequest(request);
		
		FhirReference forRef = task.getFhirTask().getForReference();
		assertNotNull(forRef);
		assertEquals(patientUuid, forRef.getTargetUuid());
	}
}
