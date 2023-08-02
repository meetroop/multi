package com.testbed.appmaster.masteranalyser.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class EvidenceData {

	
	private EVIDENCE_TYPE type;
	
	private EVIDENCE_CONFIDENCE confidence;
	
	private String source;
	
	private String evidenceName;
	
	private String actualEvidenceValue;
	
	private String deducedEvidenceValue;

}
