package com.testbed.appmaster.masteranalyser.apis;

import com.google.gson.JsonObject;

public interface ApiAgent {
	
	
	public JsonObject execute(String json) throws Exception;
	
	public String getID(String key) throws Exception;
}
