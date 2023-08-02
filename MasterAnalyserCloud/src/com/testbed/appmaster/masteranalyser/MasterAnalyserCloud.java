package com.testbed.appmaster.masteranalyser;

import com.testbed.sca.masteranalyser.model.ProjectBOM;
import com.testbed.sca.masteranalyser.util.AnalyserUtility;
import com.testbed.sca.masteranalyser.util.FileUtility;
import com.testbed.sca.masteranalyser.util.ResourceConstants;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class MasterAnalyserCloud {

  public static void main(String[] args) {
    System.out.println(
        "welcome to watcher !! watching location : " + ResourceConstants.INPUT_ZIP_FILE_LOCATION);

    try {
      while (true) {

        Thread.sleep(100);

        Path path = Paths.get(ResourceConstants.INPUT_ZIP_FILE_LOCATION);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        WatchKey watckKey = watchService.take();

        List<WatchEvent<?>> events = watckKey.pollEvents();
        for (WatchEvent event : events) {
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            System.out.println("Created: " + event.context().toString());

            ProjectBOM proj = new ProjectBOM();

            DateFormat df = new SimpleDateFormat("ddMMyyHHmmss");
            Calendar calobj = Calendar.getInstance();
            String datestamp = df.format(calobj.getTime());

            proj.setProjName(event.context().toString());
            proj.setScanDate(datestamp);

            String fileName = FileUtility.unzip(event.context().toString());

            // TODO : add time-stamp to the file name to avoid cross
            System.out.println("distributing file : " + fileName);
            proj.setBomData(new AnalyserUtility().distribute(fileName));

            FileUtility.printBOM(proj);

            System.out.println("BOM created !! clearing the space now. Deleting file :" + fileName);
            FileUtility.deleteFile(fileName);
          }
        }
      }

    } catch (Exception e) {
      System.out.println("Error: " + e.toString());
      e.printStackTrace();
    }
  }
}
