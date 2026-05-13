package org.openmrs.module.fhirExtension.web.contract;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskFhirReference {
	
	private String reference;
	
	private String type;
}
