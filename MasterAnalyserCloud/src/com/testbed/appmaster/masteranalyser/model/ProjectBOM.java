package com.testbed.appmaster.masteranalyser.model;

import java.util.List;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectBOM {
	
	private String projName;
	private String scanDate;
	private List<Dependencies> bomData;
	
	public String toString() {
		return new Gson().toJson(this);
		
	}

}
