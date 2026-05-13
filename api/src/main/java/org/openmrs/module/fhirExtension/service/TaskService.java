package org.openmrs.module.fhirExtension.service;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.model.TaskSearchRequest;
import org.openmrs.module.fhirExtension.utils.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Transactional
public interface TaskService {
	
	@Authorized({ PrivilegeConstants.ADD_TASKS, PrivilegeConstants.EDIT_TASKS })
	Task saveTask(Task task);
	
	@Authorized({ PrivilegeConstants.ADD_TASKS, PrivilegeConstants.EDIT_TASKS })
	List<Task> saveTask(List<Task> tasks);
	
	@Authorized({ PrivilegeConstants.GET_TASKS })
	List<Task> getTasksByVisitFilteredByTimeFrame(String visitUuid, Date startTime, Date endTime);
	
	@Authorized({ PrivilegeConstants.GET_TASKS })
	List<Task> getTasksByPatientUuidsByTimeFrame(List<String> patientUuids, Date startTime, Date endTime);
	
	@Authorized({ PrivilegeConstants.GET_TASKS })
	List<Task> getTasksByUuids(List<String> listOdUuids);
	
	@Authorized({ PrivilegeConstants.GET_TASKS })
	List<Task> searchTasks(TaskSearchRequest taskSearchRequest);
}
