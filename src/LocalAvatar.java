import java.io.File;

public class LocalAvatar extends AvatarFetcher {
  public String dir = "";
  public String empty_pic = null;

  public LocalAvatar(CodeSwarmConfig cfg) {
    super(cfg);
    dir = cfg.getStringProperty("LocalAvatarDirectory");
    empty_pic = cfg.getStringProperty("LocalAvatarDefaultPic");
  }

  public String fetchUserImage(String username) {
    String filename = dir+username+".png";
    File f = new File(filename);
    if(f.exists()) return filename;
    return dir+empty_pic;
  }


}
