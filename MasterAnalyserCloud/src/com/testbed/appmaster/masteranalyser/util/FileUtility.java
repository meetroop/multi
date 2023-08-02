package com.testbed.appmaster.masteranalyser.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.Gson;
import com.testbed.appmaster.masteranalyser.model.ProjectBOM;

public class FileUtility {

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
	
	public static String readFile(String file) {

		StringBuffer sb = new StringBuffer();

		try (BufferedReader br = new BufferedReader(
				new FileReader(ResourceConstants.INPUT_ZIP_FILE_LOCATION + "/" + file))) {

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				sb.append(sCurrentLine);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

		// return new Gson().toJson(sb.toString(), Project.class);
		// System.out.println(sb.toString());
		return sb.toString();

	}
	
	

	public static void display(String string) {

		String file = ResourceConstants.INPUT_ZIP_FILE_LOCATION + "/" + string;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println(sCurrentLine);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public static void printBOM(ProjectBOM proj) {

		String fileName = getOutPutFileName(proj.getProjName());

		String content = new Gson().toJson(proj);

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {

			bw.write(content);

			System.out.println("Done :"+fileName);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}
	
	public static void deleteFile(String absoluteFileName) {
		
		File file = new File(absoluteFileName); 
        
        if(file.delete()) 
        { 
            System.out.println("File deleted successfully ! "+ absoluteFileName); 
        } 
        else
        { 
            System.out.println("Failed to delete the file ! "+ absoluteFileName); 
        } 
		
		
	}

	private static String getOutPutFileName(String projName) {

		DateFormat df = new SimpleDateFormat("ddMMyyHHmmss");
		Calendar calobj = Calendar.getInstance();
		String datestamp = df.format(calobj.getTime());

		return ResourceConstants.OUTPUT_FILE_PREFIX + projName.replace('.', '_') + "_" + datestamp
				+ ResourceConstants.OUTPUT_FILE_SUFFIX;

	}
	

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws Exception
     */
    public static String unzip(String zipFile) throws Exception{

    	String filePath = ResourceConstants.INPUT_UNZIPED_FILE_LOCATION + File.separator;
    	
    	String zipFilePath = ResourceConstants.INPUT_ZIP_FILE_LOCATION+File.separator+zipFile;

        try(ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))){
        	
        	ZipEntry entry = zipIn.getNextEntry();
           
        	filePath = filePath + entry.getName();
		File file = new File(filePath);
            	if(file.exists()) {
            		deleteFile(filePath);
            	}
        	extractFile(zipIn, filePath);
            
        	
        }catch (IOException e) {
        	e.printStackTrace();
			throw new Exception("Exception occured while unziping the file");
		}
        
        return filePath;

    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) {
        
    	try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))){
    		
    		bos.write(readfile(zipIn));// for java 8 and below only
    		
    		//bos.write(zipIn.readAllBytes());// for java 9 and above only
    		
    	}catch (IOException e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
        
    }	
	
	 private static byte[] readfile(InputStream jis) throws IOException {
		 
	        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
	        int readLen = buf.length;
	        int readOffset = 0;
	        int byteRead;
	        while (true) {
	        	
	            //read till EOF
	            while ((byteRead = jis.read(buf, readOffset, readLen - readOffset)) > 0) {
	            	readOffset += byteRead;
	            }

	            // have we reached EOF? break the while(true)
	            if (byteRead < 0) {
	                break;
	            }

	            if (readLen <= MAX_BUFFER_SIZE - readLen) {
	            	readLen = readLen << 1;
	            } else {
	                if (readLen == MAX_BUFFER_SIZE)
	                    throw new OutOfMemoryError("Required array size too large");
	                readLen = MAX_BUFFER_SIZE;
	            }
	            buf = Arrays.copyOf(buf, readLen);
	        }
	        return (readLen == readOffset) ? buf : Arrays.copyOf(buf, readOffset);
	    }
    
	
	

}
