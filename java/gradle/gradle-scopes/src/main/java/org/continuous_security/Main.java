package org.continuous_security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.fileupload.MultipartStream;

public class Main {

  public static void main(String[] args) {
    byte[] bytes = new byte[256];
    try {
      new MultipartStream(new ByteArrayInputStream(bytes), bytes);
    } catch (IOException ignored) {
    }
    System.out.println("Gradle-Scopes project completed.");
  }
}
