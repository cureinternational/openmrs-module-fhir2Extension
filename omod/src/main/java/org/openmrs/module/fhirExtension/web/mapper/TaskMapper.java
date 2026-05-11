package org.openmrs.module.fhirExtension.web.mapper;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.model.FhirTaskRequestedPeriod;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.web.contract.TaskRequest;
import org.openmrs.module.fhirExtension.web.contract.TaskResponse;
import org.openmrs.module.fhirExtension.web.contract.TaskUpdateRequest;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.validation.ValidationException;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.util.LocaleUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskMapper {
	
	@Autowired
	private EncounterService encounterService;
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private PatientService patientService;
	
	private static final String ALL_TASK_TYPE = "All Task Types";
	
	public Task fromRequest(TaskRequest taskRequest) {
		
		Task task = new Task();
		FhirTask fhirTask = new FhirTask();
		fhirTask.setName(taskRequest.getName());
		fhirTask.setTaskCode(getConceptForTaskType(taskRequest.getTaskType()));
		if (taskRequest.getPatientUuid() != null) {
			Visit activeVisit = visitService.getActiveVisitsByPatient(
			    patientService.getPatientByUuid(taskRequest.getPatientUuid())).get(0);
			FhirReference forReference = new FhirReference();
			forReference.setType(Visit.class.getTypeName());
			forReference.setReference(Visit.class.getTypeName() + "/" + activeVisit.getUuid());
			forReference.setTargetUuid(activeVisit.getUuid());
			fhirTask.setForReference(forReference);
		} else if (taskRequest.getVisitUuid() != null) {
			FhirReference forReference = new FhirReference();
			forReference.setType(Visit.class.getTypeName());
			forReference.setReference(Visit.class.getTypeName() + "/" + taskRequest.getVisitUuid());
			forReference.setTargetUuid(taskRequest.getVisitUuid());
			fhirTask.setForReference(forReference);
		}
		
		if (taskRequest.getEncounterUuid() != null) {
			FhirReference encounterReference = new FhirReference();
			encounterReference.setType(Encounter.class.getTypeName());
			encounterReference.setReference(Encounter.class.getTypeName() + "/" + taskRequest.getEncounterUuid());
			encounterReference.setTargetUuid(taskRequest.getEncounterUuid());
			fhirTask.setEncounterReference(encounterReference);
		}
		
		fhirTask.setStatus(taskRequest.getStatus());
		fhirTask.setIntent(taskRequest.getIntent());
		fhirTask.setComment(taskRequest.getComment());
		
		if (taskRequest.getRequestedStartTime() != null || taskRequest.getRequestedEndTime() != null) {
			FhirTaskRequestedPeriod fhirTaskRequestedPeriod = new FhirTaskRequestedPeriod();
			fhirTaskRequestedPeriod.setTask(fhirTask);
			fhirTaskRequestedPeriod.setRequestedStartTime(taskRequest.getRequestedStartTime());
			fhirTaskRequestedPeriod.setRequestedEndTime(taskRequest.getRequestedEndTime());
			task.setFhirTaskRequestedPeriod(fhirTaskRequestedPeriod);
		}
		
		if (taskRequest.getObservationUuid() != null) {
			FhirReference focusReference = new FhirReference();
			focusReference.setType(FhirConstants.OBSERVATION);
			focusReference.setReference(taskRequest.getObservationUuid());
			focusReference.setTargetUuid(taskRequest.getObservationUuid());
			fhirTask.setFocusReference(focusReference);
		}

		if (taskRequest.getOrderUuid() != null) {
			FhirReference orderReference = new FhirReference();
			orderReference.setType(Order.class.getTypeName());
			orderReference.setReference(taskRequest.getOrderUuid());
			orderReference.setTargetUuid(taskRequest.getOrderUuid());
			Set<FhirReference> basedOnRefs = fhirTask.getBasedOnReferences() != null
			        ? fhirTask.getBasedOnReferences()
			        : new HashSet<>();
			basedOnRefs.add(orderReference);
			fhirTask.setBasedOnReferences(basedOnRefs);
		}

		if (taskRequest.getIsSystemGeneratedTask()) {
			fhirTask.setCreator(Context.getUserService().getUserByUuid(Daemon.getDaemonUserUuid()));
		}
		task.setFhirTask(fhirTask);
		return task;
	}
	
	public TaskResponse constructResponse(Task task) {
		TaskResponse response = new TaskResponse();
		response.setName(task.getFhirTask().getName());
		response.setUuid(task.getFhirTask().getUuid());
		response.setStatus(task.getFhirTask().getStatus());
		response.setIntent(task.getFhirTask().getIntent());
		response.setPatientUuid(task.getFhirTask().getForReference().getTargetUuid());
		if (task.getFhirTaskRequestedPeriod() != null) {
			response.setRequestedStartTime(task.getFhirTaskRequestedPeriod().getRequestedStartTime());
			response.setRequestedEndTime(task.getFhirTaskRequestedPeriod().getRequestedEndTime());
		}
		response.setCreator(ConversionUtil.convertToRepresentation(task.getFhirTask().getCreator(), Representation.REF));
		response.setTaskType(ConversionUtil.convertToRepresentation(task.getFhirTask().getTaskCode(), Representation.REF));
		response.setExecutionStartTime(task.getFhirTask().getExecutionStartTime());
		response.setExecutionEndTime(task.getFhirTask().getExecutionEndTime());
		response.setComment(task.getFhirTask().getComment());
		if (task.getFhirTask().getFocusReference() != null) {
			response.setObservationUuid(task.getFhirTask().getFocusReference().getTargetUuid());
		}
		return response;
	}
	
	public void fromRequest(TaskUpdateRequest taskUpdateRequest, Task task) {
		FhirTask fhirTask = task.getFhirTask();
		
		fhirTask.setStatus(taskUpdateRequest.getStatus());
		fhirTask.setExecutionStartTime(taskUpdateRequest.getExecutionStartTime());
		fhirTask.setExecutionEndTime(taskUpdateRequest.getExecutionEndTime());
		fhirTask.setComment(taskUpdateRequest.getComment());
	}
	
	private Concept getConceptForTaskType(String taskType) {
		if (taskType == null || taskType.isEmpty()) {
			log.warn("Task type is not passed. Setting as null");
			return null;
		}
		List<ConceptClass> parentConceptClasses = new ArrayList<ConceptClass>();
		parentConceptClasses.add(Context.getConceptService().getConceptClassByName("ConvSet"));
		List<Locale> locales = Arrays.asList(Locale.ENGLISH);
		List<ConceptSearchResult> conceptsSearchResult = Context.getConceptService().getConcepts(ALL_TASK_TYPE, locales, false, parentConceptClasses, null, null, null, null, 0, null);
		if (conceptsSearchResult.size() == 0) {
			log.warn(String.format("Unable to find a concept set with name %s for mapping task types.",
					ALL_TASK_TYPE));
			throw new ValidationException(String.format("Unable to find a concept set with name [%s] for mapping task types.",
					ALL_TASK_TYPE));
		}
		List<Concept> conceptsByName = conceptsSearchResult.stream()
				.map(ConceptSearchResult::getConcept)
				.filter(concept -> concept != null)
				.flatMap(concept -> concept.getConceptSets().stream().map(ConceptSet::getConcept))
				.filter(concept -> concept.getNames(false) != null &&
						concept.getNames(false).stream().anyMatch(name -> name.getName().equals(taskType)))
				.collect(Collectors.toList());
		if (conceptsByName.size() == 1) {
			return conceptsByName.get(0);
		} else if (conceptsByName.size() == 0) {
			log.error(String.format("Unable to find a concept member with name %s in %s concept for mapping to task type.",
					taskType, ALL_TASK_TYPE));
			throw new ValidationException(String.format("Unable to find a concept member with name %s in %s concept for mapping to task type.",
					taskType, ALL_TASK_TYPE));
		} else {
			log.error(String.format("Multiple concepts found with name [%s]. ", taskType));
			throw new ValidationException(String.format("Multiple concepts found with name [%s]. ", taskType));
		}
	}
}
