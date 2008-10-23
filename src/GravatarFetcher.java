import java.net.MalformedURLException;
import java.net.URL;

public class GravatarFetcher extends AvatarFetcher {
  public GravatarFetcher(CodeSwarmConfig cfg) {
    super(cfg);
  }

  public String fetchUserImage(String username) {
    String hash = md5Hex(username);
    try {
      return getImage(hash, new URL("http://www.gravatar.com/avatar/" + hash + "?d=identicon&s=" + size));
    } catch (MalformedURLException e) {
      e.printStackTrace(); //should be impossible...
      return null;
    }
  }
}
