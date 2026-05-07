package org.openmrs.module.fhirExtension.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.service.TaskService;
import org.openmrs.module.fhirExtension.web.contract.PatientTaskResponse;
import org.openmrs.module.fhirExtension.web.contract.TaskRequest;
import org.openmrs.module.fhirExtension.web.contract.TaskResponse;
import org.openmrs.module.fhirExtension.web.contract.TaskUpdateRequest;
import org.openmrs.module.fhirExtension.web.mapper.TaskMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/tasks")
public class TaskController extends BaseRestController {
	
	@Autowired
	private TaskService taskService;
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private TaskMapper taskMapper;
	
	private Log log = LogFactory.getLog(this.getClass());
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Object> saveTask(@Valid @RequestBody TaskRequest taskRequest) throws IOException {
		try {
			Task task = taskMapper.fromRequest(taskRequest);
			taskService.saveTask(task);
			return new ResponseEntity<>(taskMapper.constructResponse(task), HttpStatus.OK);
		}
		catch (RuntimeException ex){
			log.error("Runtime error while trying to create new task", ex);
			return new ResponseEntity<>(RestUtil.wrapErrorResponse(ex, ex.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, params = {"startTime", "endTime"})
	@ResponseBody
	public ResponseEntity<Object> getTasks(@RequestParam(value = "startTime") Long startTime,
										   @RequestParam(value = "endTime") Long endTime,
										   @RequestParam(value = "visitUuid", required = false) String visitUuid,
										   @RequestParam(value = "patientUuids", required = false) List<String> patientUuids,
										   @RequestParam(value = "observationUuids", required = false) List<String> observationUuids) throws IOException {
		try {
			if (observationUuids != null && !observationUuids.isEmpty()) {
				List<Task> tasks = taskService.getTasksByObservationUuids(observationUuids).stream()
				        .map(fhirTask -> new Task(fhirTask, null)).collect(Collectors.toList());
				return new ResponseEntity<>(tasks.stream().map(taskMapper::constructResponse).collect(Collectors.toList()), HttpStatus.OK);
			}
			if (visitUuid != null && !visitUuid.isEmpty()) {
				List<Task> tasks = taskService.getTasksByVisitFilteredByTimeFrame(visitUuid, new Date(startTime), new Date(endTime));
				return new ResponseEntity<>(tasks.stream().map(taskMapper::constructResponse).collect(Collectors.toList()), HttpStatus.OK);
			}
			if (patientUuids != null || !patientUuids.isEmpty()) {
				List<PatientTaskResponse> groupedResponses = getTasksByPatientFilteredByTimeFrame(patientUuids, new Date(startTime), new Date(endTime));
				return new ResponseEntity<>(groupedResponses, HttpStatus.OK);
			}
			return new ResponseEntity<>("Error Processing Request. Either Visit or Patient UUID is mandatory", HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("Runtime error while fetching patient medication summaries", e);
			return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Object> updateTask(@Valid @RequestBody ArrayList<TaskUpdateRequest> taskUpdateRequests) throws IOException {
		try {
			List<String> listOfUuid = taskUpdateRequests.stream().map(task -> task.getUuid()).collect(Collectors.toList());
			List<Task> tasks = taskService.getTasksByUuids(listOfUuid);
			if(tasks.size() == listOfUuid.size()) {
				taskUpdateRequests.forEach(taskUpdateRequest -> {
					Optional<Task> taskToBeUpdated = tasks.stream().filter(task -> task.getFhirTask().getUuid().equals(taskUpdateRequest.getUuid())).findFirst();
					taskMapper.fromRequest(taskUpdateRequest, taskToBeUpdated.get());
					taskService.saveTask(taskToBeUpdated.get());
				});
			return new ResponseEntity<>(tasks.stream().map(taskMapper::constructResponse).collect(Collectors.toList()), HttpStatus.OK);
			}
			throw new Exception();
		}
		catch (RuntimeException ex) {
			log.error("Runtime error while updating a task", ex);
			return new ResponseEntity<>(RestUtil.wrapErrorResponse(ex, ex.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("One or more task(s) uuid is not valid", e);
			return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}
	
	private List<PatientTaskResponse> getTasksByPatientFilteredByTimeFrame(List<String> patientUuids, Date startTime, Date endTime) {
		List<Task> response = taskService.getTasksByPatientUuidsByTimeFrame(patientUuids, startTime, endTime);
		List<Visit> activeVisits= visitService.getVisits(null,null,null,null,null,null,null,null,null,false,false);
		Map<String, Visit> visitPatientMap = activeVisits.stream()
				.collect(Collectors.toMap(Visit::getUuid, visit -> visit));

		Map<String, List<Task>> groupedByVisit = response.stream()
				.collect(Collectors.groupingBy(task -> task.getFhirTask().getForReference().getTargetUuid()));

		List<PatientTaskResponse> patientTaskResponses = new ArrayList<>();
		groupedByVisit.forEach((visitUUid, tasks) -> {
			List<TaskResponse> taskResponses = tasks.stream().map(taskMapper::constructResponse).collect(Collectors.toList());
			PatientTaskResponse patientTaskResponse = new PatientTaskResponse(visitPatientMap.get(visitUUid).getPatient().getUuid(),visitUUid, taskResponses);
			patientTaskResponses.add(patientTaskResponse);
		});
		return patientTaskResponses;
	}
}
