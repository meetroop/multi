package com.testbed.appmaster.masteranalyser.apisImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.testbed.appmaster.masteranalyser.apis.ApiAgent;

public class HttpApiAgent implements ApiAgent {
	
	private String URL;
	
	public HttpApiAgent(String url) {
		
		this.URL = url;
		
	}

	@Override
	public JsonObject execute(String absoluteFileName) throws Exception{
		StringBuffer jsonString = new StringBuffer();
		try {
			
			URL url = new URL(this.URL);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	        connection.setDoInput(true);
	        connection.setDoOutput(true);
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Accept", "application/json");
	        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
	        
	        writer.write(absoluteFileName);
	        writer.close();
	        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        
	        String line;
	        while ((line = br.readLine()) != null) {
	                jsonString.append(line);
	        }
	        br.close();
	        connection.disconnect();
	       

		}catch (Exception e) {
			System.out.println("Exception Occured while calling http api from MasterAnalyser ");
			e.printStackTrace();
			throw e;
		}
		
		 System.out.println("Response : "+jsonString);
		 
		 //JsonObject jobj= new JsonObject();
		 
		 JsonParser gparser = new JsonParser();
		
		 return (JsonObject) gparser.parse(jsonString.toString());
	}
	
	@Override
	public String getID(String key) throws Exception{
		StringBuffer outputID = new StringBuffer();
		HttpURLConnection connection = null;
		try {
			
			URL url = new URL(this.URL+"/"+key);
			//System.out.println(url.toString());
			
			connection = (HttpURLConnection) url.openConnection();

	        connection.setDoInput(true);
	        connection.setDoOutput(true);
	        connection.setRequestMethod("GET");
	        
	        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	        	System.out.println("Exception : "+connection.getResponseCode()+" for "+ url.toString());
//				throw new RuntimeException("Failed : HTTP error code : "
//						+ connection.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(connection.getInputStream())));

			String output;
			
			while ((output = br.readLine()) != null) {
				outputID.append(output);
			}

			
			
	       

		}catch (Exception e) {
			// TODO: handle exception
			throw e;
		}finally {
			if(connection !=null) {
				connection.disconnect();
			}
		}
		
		 //System.out.println("Response : "+outputID.toString());
		
		 return outputID.toString();
	}

}
