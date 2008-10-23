
public class NoAvatar extends AvatarFetcher {

  public NoAvatar(CodeSwarmConfig cfg) {
    super(cfg);
  }

  public String fetchUserImage(String username) {
    return null;
  }
}
