package org.continuous_security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.fileupload.MultipartStream;

public class Main {

<<<<<<< HEAD
    public static void main(String[] args) {
        byte[] bytes = new byte[256];
        try {
            new MultipartStream(new ByteArrayInputStream(bytes), bytes);
           String user = "abc";
		System.out.println(getpassword(user));
        } catch (IOException ignored) {
        }
        System.out.println("Program completed.");
    }

private static void getpassword(String user){
     String password = "password";
     switch(user){
    case "abs": password = "abspassword";
			break;
case "cde" : password = "cdepassword";
             break;
}
return password;
 }
=======
  public static void main(String[] args) {
    byte[] bytes = new byte[256];
    try {
      new MultipartStream(new ByteArrayInputStream(bytes), bytes);
    } catch (IOException ignored) {
    }
    System.out.println("Program completed.");
  }
>>>>>>> bfd7cb1a37f2f67c432b453699b1868faa463a4f
}
