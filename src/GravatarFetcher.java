import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;

public class GravatarFetcher {
  
  //these two methods taken from http://en.gravatar.com/site/implement/java
  private static String hex(byte[] array) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < array.length; ++i)
      sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
    return sb.toString();
  }
  private static String md5Hex (String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      return hex (md.digest(message.getBytes("CP1252")));
    } catch (NoSuchAlgorithmException e) {
    } catch (UnsupportedEncodingException e) {
    }
    return null;
  }
  
  public static String fetchUserImage(String username) {
    String hash = md5Hex(username);
    String filename = "image_cache/" + hash; 
    if (!new File(filename).exists()){
      try {
        new File("image_cache").mkdirs();
        URL url = new URL("http://www.gravatar.com/avatar/" + hash + "?d=identicon");
        URLConnection con = url.openConnection();
        InputStream input = con.getInputStream();
        FileOutputStream output = new FileOutputStream(filename);
        
        int length = con.getContentLength();
        if (length == -1){
          //read until exhausted
          while(true){
            int val = input.read();
            if (val == -1)
              break;
            output.write(input.read());
          }
        }
        else{
          //read length bytes
          for(int i = 0; i < length; i++)
            output.write(input.read());
        }
        
        output.close();
        input.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return null;
      }
    }
    
    return filename;
  }
}
