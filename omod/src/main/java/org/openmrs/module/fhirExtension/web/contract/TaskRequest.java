package org.openmrs.module.fhirExtension.web.contract;

import lombok.Getter;
import lombok.Setter;
import org.openmrs.module.fhir2.model.FhirTask;

import java.util.Date;

@Getter
@Setter
public class TaskRequest {
	
	private String uuid;
	
	private String name;
	
	private String patientUuid;
	
	private String visitUuid;
	
	private String encounterUuid;
	
	private String taskType;
	
	private Date requestedStartTime;
	
	private Date requestedEndTime;
	
	private FhirTask.TaskStatus status;
	
	private FhirTask.TaskIntent intent;
	
	private String comment;
	
	private Boolean isSystemGeneratedTask = false;
	
	private String observationUuid;
	
	private String orderUuid;
	
}
