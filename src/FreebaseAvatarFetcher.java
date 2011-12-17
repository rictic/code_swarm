import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FreebaseAvatarFetcher extends AvatarFetcher{
  public FreebaseAvatarFetcher(CodeSwarmConfig cfg) {
    super(cfg);
  }

  static private Pattern imageIDPattern = Pattern.compile("\"image:id\"\\s*:\\s*\"(.*?)\"");

  private static String readURLToString(URL url) {
    try {
      URLConnection con = url.openConnection();
      BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
      StringBuilder sb = new StringBuilder();
      int length = con.getContentLength();
      if (length == -1){
        //read until exhausted
        while(true){
          String line = reader.readLine();
          if (line == null) break;
          sb.append(line);
        }
      }
      else{
        //read length bytes
        for(int i = 0; i < length; i++)
          sb.append((char)reader.read());
      }
      return sb.toString();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static String getUserImageID(String username) {
    try {
      new File("image_cache").mkdirs();
      String json = readURLToString(new URL("http://www.freebase.com/api/service/mqlread?query=" +
                        "{%22query%22:{%22!/common/image/appears_in_topic_gallery%22:"+
                        "[{%22image:id%22:null}],%22id%22:%22/user/" + username + "%22}}"));
      if (json == null) return null;
      Matcher m = imageIDPattern.matcher(json);
      if (m.find())
        return m.group(1);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  public String fetchUserImage(String username) {
    String key = md5Hex("metaweb:" + username);
    if (imageCached(key))
      return getFilename(key);
    try {
      String imageID = getUserImageID(username);
      if (imageID == null) return null;
      return getImage(key, new URL("http://www.freebase.com/api/trans/image_thumb/"
                                   + imageID
                                   + "?maxheight=" + size
                                   + "&maxwidth=" + size
                                   + "&mode=fillcrop"));
    } catch (MalformedURLException e) {
      e.printStackTrace(); //should be impossible...
      return null;
    }
  }
}
