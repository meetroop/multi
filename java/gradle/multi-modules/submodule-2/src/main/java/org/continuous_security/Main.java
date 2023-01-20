package org.continuous_security;

import java.io.ByteArrayInputStream;
import org.apache.commons.fileupload.MultipartStream;

public class Main {

  private static void filterXMLSignature() throws Exception {
    byte[] bytes = new byte[256];
    new MultipartStream(new ByteArrayInputStream(bytes), bytes);
  }

  public static void main(String[] args) throws Exception {
    filterXMLSignature();
  }
}
