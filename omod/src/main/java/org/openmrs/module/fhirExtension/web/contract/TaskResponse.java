package org.openmrs.module.fhirExtension.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.module.fhir2.model.FhirTask;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TaskResponse {
	
	private String uuid;
	
	private String name;
	
	private Date requestedStartTime;
	
	private Date requestedEndTime;
	
	private FhirTask.TaskStatus status;
	
	private FhirTask.TaskIntent intent;
	
	private List<String> partOf;
	
	private Object taskType;
	
	private Object creator;
	
	private Date executionStartTime;
	
	private Date executionEndTime;
	
	private String comment;
	
	private TaskFhirReference focus;
	
	private TaskFhirReference basedOn;
	
	@JsonProperty("for")
	private TaskFhirReference forReference;
}
