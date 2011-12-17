import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GravatarFetcher extends AvatarFetcher {
  public GravatarFetcher(CodeSwarmConfig cfg) {
    super(cfg);
  }

  public String fetchUserImage(String username) {
    String email = getEmail(username);
    String hash = md5Hex(email);
    try {
      return getImage(hash, new URL("http://www.gravatar.com/avatar/" + hash + "?d="+cfg.getStringProperty("GravatarFallback")+ "&s=" + size));
    } catch (MalformedURLException e) {
      e.printStackTrace(); //should be impossible...
      return null;
    }
  }

  private Pattern emailPattern = Pattern.compile("<(.*?@.*?\\..*?)>");
  private String getEmail(String username) {
    Matcher emailMatcher = emailPattern.matcher(username);
    if (emailMatcher.find())
      return username.substring(emailMatcher.start(1), emailMatcher.end(1));
    return username;
  }
}
