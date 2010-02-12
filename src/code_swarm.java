/**
 * Copyright 2008 Michael Ogawa
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.vecmath.Vector2f;
import org.codeswarm.dependencies.sun.tools.javac.util.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;


/**
 * 
 *
 */
public class code_swarm extends PApplet {
  /** @remark needed for any serializable class */ 
  public static final long serialVersionUID = 0;

  // User-defined variables
  int FRAME_RATE = 24;
  long UPDATE_DELTA = -1;
  String SPRITE_FILE = "particle.png";
  String MASK_FILE = "src/mask.png";
  String SCREENSHOT_FILE;
  int background;
  int PARTICLE_SIZE = 2;

  // Data storage
  BlockingQueue<FileEvent> eventsQueue;
  boolean isInputSorted = false;
  boolean showUserName = false;
  protected static Map<String, FileNode> nodes;
  protected static Map<Pair<FileNode, PersonNode>, Edge> edges;
  protected static Map<String, PersonNode> people;

  // Liveness cache
  static List<PersonNode> livingPeople = new ArrayList<PersonNode>();
  static List<Edge> livingEdges = new ArrayList<Edge>();
  static List<FileNode> livingNodes = new ArrayList<FileNode>(); 
  
  LinkedList<ColorBins> history;
  boolean finishedLoading = false;
  
  // Temporary variables
  FileEvent currentEvent;
  Date nextDate;
  Date prevDate;
  FileNode prevNode;
  int maxTouches;

  // Graphics objects
  PFont font;
  PFont boldFont;
  PImage sprite;
  PImage avatarMask;

  boolean paused = false;
  
  // Graphics state variables  
  boolean showHistogram;
  boolean showDate;
  boolean showLegend;
  boolean showPopular;
  boolean showEdges;
  boolean showEngine;
  boolean showHelp;
  boolean takeSnapshots;
  boolean showDebug;
  boolean drawNamesSharp;
  boolean drawNamesHalos;
  boolean drawFilesSharp;
  boolean drawFilesFuzzy;
  boolean drawFilesJelly;
  
  //used to ensure that input is sorted when we're told it is
  long maximumDateSeenSoFar = 0;
  
  
  // Color mapper
  ColorAssigner colorAssigner;
  int currentColor;

  // Edge Length
  protected static int EDGE_LEN;
  // Drawable object life decrement
  private int EDGE_LIFE_INIT;
  private int FILE_LIFE_INIT;
  private int PERSON_LIFE_INIT;
  private int EDGE_LIFE_DECREMENT;
  private int FILE_LIFE_DECREMENT;
  private int PERSON_LIFE_DECREMENT;

  private float FILE_MASS;
  private float PERSON_MASS;

  private int HIGHLIGHT_PCT;
  
  // Physics engine configuration
  String          physicsEngineConfigDir;
  String          physicsEngineSelection;
  LinkedList<peConfig> mPhysicsEngineChoices = new LinkedList<peConfig>();
  PhysicsEngine  mPhysicsEngine = null;
  private boolean safeToToggle = false;
  private boolean wantToToggle = false;
  private boolean toggleDirection = false;
  private boolean circularAvatars = false;

  // Default Physics Engine (class) name
  static final String PHYSICS_ENGINE_DEFAULT  = "PhysicsEngineOrderly";

  // Formats the date string nicely
  DateFormat formatter = DateFormat.getDateInstance();

  //kinda a hack that these two are static
  protected static CodeSwarmConfig cfg;
  protected static String userConfigFilename = null;
  
  private long lastDrawDuration = 0;
  private String loadingMessage = "Reading input file";
  protected static int width=0;
  protected static int height=0;
  private int maxFramesSaved;

  protected int maxBackgroundThreads;
  protected ExecutorService backgroundExecutor;
  /**
   *  Used for utility functions
   *  current members:
   *      drawPoint: Pass coords and color
   *      drawLine: Pass coords and color
   */
  public static Utils utils = null;
  public AvatarFetcher avatarFetcher;

  private int fontColor;

  
  
  /**
   * Initialization
   */
  public void setup() {

    utils = new Utils();

    width=cfg.getPositiveIntProperty(CodeSwarmConfig.WIDTH_KEY);
    height=cfg.getPositiveIntProperty(CodeSwarmConfig.HEIGHT_KEY);

    if (cfg.getBooleanProperty(CodeSwarmConfig.USE_OPEN_GL)) {
      size(width, height, OPENGL);
    } else {
      size(width, height);
    }
    
    int maxBackgroundThreads = cfg.getPositiveIntProperty(CodeSwarmConfig.MAX_THREADS_KEY);
    backgroundExecutor = new ThreadPoolExecutor(1, maxBackgroundThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(4 * maxBackgroundThreads), new ThreadPoolExecutor.CallerRunsPolicy());
    
    showLegend      = cfg.getBooleanProperty(CodeSwarmConfig.SHOW_LEGEND);
    showHistogram   = cfg.getBooleanProperty(CodeSwarmConfig.SHOW_HISTORY); 
    showDate        = cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DATE);
    showEdges       = cfg.getBooleanProperty(CodeSwarmConfig.SHOW_EDGES);
    showDebug       = cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DEBUG);
    takeSnapshots   = cfg.getBooleanProperty(CodeSwarmConfig.TAKE_SNAPSHOTS_KEY);
    drawNamesSharp  = cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_SHARP);
    drawNamesHalos  = cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_HALOS); 
    drawFilesSharp  = cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_SHARP);
    drawFilesFuzzy  = cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_FUZZY);
    drawFilesJelly  = cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_JELLY);
    circularAvatars = cfg.getBooleanProperty(CodeSwarmConfig.DRAW_CIRCULAR_AVATARS);

    background = cfg.getColorProperty(CodeSwarmConfig.BACKGROUND_KEY).getRGB();
    fontColor = cfg.getColorProperty(CodeSwarmConfig.FONT_COLOR_KEY).getRGB();
    
    // Ensure we have sane values.
    EDGE_LIFE_INIT = cfg.getPositiveIntProperty(CodeSwarmConfig.EDGE_LIFE_KEY);
    FILE_LIFE_INIT = cfg.getPositiveIntProperty(CodeSwarmConfig.FILE_LIFE_KEY);
    PERSON_LIFE_INIT = cfg.getPositiveIntProperty(CodeSwarmConfig.PERSON_LIFE_KEY);
    
    
    
    /* enforce decrements < 0 */
    EDGE_LIFE_DECREMENT = cfg.getNegativeIntProperty(CodeSwarmConfig.EDGE_DECREMENT_KEY);
    FILE_LIFE_DECREMENT = cfg.getNegativeIntProperty(CodeSwarmConfig.FILE_DECREMENT_KEY);
    PERSON_LIFE_DECREMENT = cfg.getNegativeIntProperty(CodeSwarmConfig.PERSON_DECREMENT_KEY);

    FILE_MASS = cfg.getFloatProperty(CodeSwarmConfig.FILE_MASS_KEY);
    PERSON_MASS = cfg.getFloatProperty(CodeSwarmConfig.PERSON_MASS_KEY);

    HIGHLIGHT_PCT = cfg.getIntProperty(CodeSwarmConfig.HIGHLIGHT_PCT_KEY);

    double framesperday = cfg.getDoubleProperty(CodeSwarmConfig.FRAMES_PER_DAY_KEY);
    UPDATE_DELTA = (long) (86400000 / framesperday);

    isInputSorted = cfg.getBooleanProperty(CodeSwarmConfig.IS_INPUT_SORTED_KEY);
    showUserName = cfg.getBooleanProperty(CodeSwarmConfig.SHOW_USER_NAME_KEY);
    
    avatarFetcher = getAvatarFetcher(cfg.getStringProperty("AvatarFetcher"));

    /**
     * This section loads config files and calls the setup method for all physics engines.
     */

    physicsEngineConfigDir = cfg.getStringProperty( CodeSwarmConfig.PHYSICS_ENGINE_CONF_DIR);
    File f = new File(physicsEngineConfigDir);
    String[] configFiles = null;
    if ( f.exists()  &&  f.isDirectory() ) {
      configFiles = f.list();
    }
    for (int i=0; configFiles != null  &&  i<configFiles.length; i++) {
      if (configFiles[i].endsWith(".config")) {
        String ConfigPath = physicsEngineConfigDir + System.getProperty("file.separator") + configFiles[i];
        CodeSwarmConfig physicsConfig = null;
        try {
          
          physicsConfig = new CodeSwarmConfig(ConfigPath);
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(1);
        }
        String ClassName = physicsConfig.getStringProperty("name");
        if (ClassName != null) {
          PhysicsEngine pe = getPhysicsEngine(ClassName);
          pe.setup(physicsConfig);
          peConfig pec = new peConfig(ClassName,pe);
          mPhysicsEngineChoices.add(pec);
        } else {
          System.err.println("Skipping config file '" + ConfigPath + "'.  Must specify class name via the 'name' parameter.");
          System.exit(1);
        }
      }
    }

    if (mPhysicsEngineChoices.size() == 0) {
      System.err.println("No physics engine config files found in '" + physicsEngineConfigDir + "'.");
      System.exit(1);
    }

    // Physics engine configuration and instantiation
    physicsEngineSelection = cfg.getStringProperty( CodeSwarmConfig.PHYSICS_ENGINE_SELECTION);

    for (peConfig p : mPhysicsEngineChoices)
      if (physicsEngineSelection.equals(p.name))
        mPhysicsEngine = p.pe;

    if (mPhysicsEngine == null) {
      System.err.println("No physics engine matches your choice of '" + physicsEngineSelection + "'. Check '" + physicsEngineConfigDir + "' for options.");
      System.exit(1);
    }

    
    
    smooth();
    frameRate(FRAME_RATE);

    // init data structures
    nodes         = new HashMap<String,FileNode>();
    edges         = new HashMap<Pair<FileNode, PersonNode>, Edge>();
    people        = new HashMap<String,PersonNode>();
    history       = new LinkedList<ColorBins>(); 
    if (isInputSorted)
      //If the input is sorted, we only need to store the next few events
      eventsQueue = new ArrayBlockingQueue<FileEvent>(50000);
    else
      //Otherwise we need to store them all at once in a data structure that will sort them
      eventsQueue = new PriorityBlockingQueue<FileEvent>();
    
    // Init color map
    initColors();

    loadRepEvents(cfg.getStringProperty(CodeSwarmConfig.INPUT_FILE_KEY)); // event formatted (this is the standard)
    while(!finishedLoading && eventsQueue.isEmpty());
    if(eventsQueue.isEmpty()){
      System.out.println("No events found in repository xml file.");
      System.exit(1);
    }
    prevDate = eventsQueue.peek().date;

    SCREENSHOT_FILE = cfg.getStringProperty(CodeSwarmConfig.SNAPSHOT_LOCATION_KEY);
    EDGE_LEN = cfg.getPositiveIntProperty(CodeSwarmConfig.EDGE_LENGTH_KEY);
    
    maxFramesSaved = (int) Math.pow(10, SCREENSHOT_FILE.replaceAll("[^#]","").length());
    
    // Create fonts
    String fontName = cfg.getStringProperty(CodeSwarmConfig.FONT_KEY);
    String boldFontName = cfg.getStringProperty(CodeSwarmConfig.FONT_KEY_BOLD);
    Integer fontSize = cfg.getPositiveIntProperty(CodeSwarmConfig.FONT_SIZE);
    Integer fontSizeBold = cfg.getPositiveIntProperty(CodeSwarmConfig.FONT_SIZE_BOLD);
    font = createFont(fontName, fontSize);
    boldFont = createFont(boldFontName, fontSizeBold);

    textFont(font);

    String SPRITE_FILE = cfg.getStringProperty(CodeSwarmConfig.SPRITE_FILE_KEY);
    // Create the file particle image
    sprite = loadImage(SPRITE_FILE);
    avatarMask = loadImage(MASK_FILE);
    avatarMask.resize(cfg.getPositiveIntProperty("AvatarSize"), cfg.getPositiveIntProperty("AvatarSize"));
    // Add translucency (using itself in this case)
    sprite.mask(sprite);
  }

  @SuppressWarnings("unchecked")
  private AvatarFetcher getAvatarFetcher(String avatarFetcherName) {
    try {
      Class<AvatarFetcher> c = (Class<AvatarFetcher>)Class.forName(avatarFetcherName);
      return c.getConstructor(CodeSwarmConfig.class).newInstance(cfg);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load a colormap
   */
  public void initColors() {
    colorAssigner = new ColorAssigner();
    int i = 1;
    String property;
    while ((property = cfg.getColorAssignProperty(i)) != null) {
      ColorTest ct = new ColorTest();
      ct.loadProperty(property);
      colorAssigner.addRule(ct);
      i++;
    }
    // Load the default.
    ColorTest ct = new ColorTest();
    ct.loadProperty(CodeSwarmConfig.DEFAULT_COLOR_ASSIGN);
    colorAssigner.addRule(ct);
  }

  /**
   * Main loop
   */
  public void draw() {
    long start = System.currentTimeMillis();
    background(background); // clear screen with background color

    this.update(); // update state to next frame

    // Draw edges (for debugging only)
    if (showEdges) {
      for (Edge edge : edges.values()) {
        edge.draw();
      }
    }

    // Surround names with aura
    // Then blur it
    if (drawNamesHalos) {
      drawPeopleNodesBlur();
    }

    // Then draw names again, but sharp
    if (drawNamesSharp) {
      drawPeopleNodesSharp();
    }

    // Draw file particles
    for (FileNode node : getLivingNodes()) {
      node.draw();
    }

    
    textFont(font);

    // Show the physics engine name
    if (showEngine) {
      drawEngine();
    }

    // help, legend and debug information are exclusive
    if (showHelp) {
      // help override legend and debug information
      drawHelp();
    }
    else if (showDebug) {
      // debug override legend information
      drawDebugData();
    }
    else if (showLegend) {
      // legend only if nothing "more important"
      drawLegend();
    }

    if (showPopular) {
      drawPopular();
    }

    if (showHistogram) {
      drawHistory();
    }

    if (showDate) {
      drawDate();
    }

    if (takeSnapshots) {
      dumpFrame();
    }

    // Stop animation when we run out of data
    
    if (finishedLoading && eventsQueue.isEmpty()) {
      // noLoop();
      backgroundExecutor.shutdown();
      try {
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
      } catch (InterruptedException e) { /* Do nothing, just exit */}
      exit();
    }

    long end = System.currentTimeMillis();
    lastDrawDuration = end - start;
  }


  /**
   * Surround names with aura
   */
  public void drawPeopleNodesBlur() {
    colorMode(HSB);
    // First draw the name
    for (PersonNode p : getLivingPeople()) {
      fill(hue(p.flavor), 64, 255, p.life);
      p.draw();
    }

    // Then blur it
    filter(BLUR, 3);
  }

  /**
   * Draw person's name
   */
  public void drawPeopleNodesSharp() {
    colorMode(RGB);
    for (PersonNode p : getLivingPeople()) {
      fill(lerpColor(p.flavor, color(255), 0.5f), max(p.life - 50, 0));
      p.draw();
    }
  }

  /**
   * Draw date in lower-right corner
   */
  public void drawDate() {
    fill(fontColor, 255);
    String dateText = formatter.format(prevDate);
    textAlign(RIGHT, BASELINE);
    textSize(font.size);
    text(dateText, width - 3, height - (2 + textDescent()));
  }

  /**
   *  Draw histogram in lower-left
   */
  public void drawHistory() {
    int counter = 0;
    strokeWeight(PARTICLE_SIZE);
    for (ColorBins cb : history) {
      if (cb.num > 0) {
        int color = cb.colorList[0];
        int start = 0;
        int end = 0;
        for (int nextColor : cb.colorList) {
          if (nextColor == color)
            end++;
          else {
            stroke(color, 255);
            rectMode(CORNERS);
            rect(counter, height - start - 3, counter, height - end - 3);
            start = end;
            color = nextColor;
          }
        }
      }
      counter+=1;
    }
  }

  /**
   *  Show color codings
   */
  public void drawLegend() {
    noStroke();
    fill(fontColor, 255);
    textFont(font);
    textAlign(LEFT, TOP);
    text("Legend:", 3, 3);
    for (int i = 0; i < colorAssigner.tests.size(); i++) {
      ColorTest t = colorAssigner.tests.get(i);
      fill(t.c1, 200);
      text(t.label, font.size, 3 + ((i + 1) * (font.size + 2)));
    }
  }

  /**
   *  Show physics engine name
   */
  public void drawEngine() {
    fill(fontColor, 255);
    textAlign(RIGHT, BASELINE);
    textSize(10);
    text(physicsEngineSelection, width-1, height - (textDescent() * 5));
  }

  /**
   *  Show short help on available commands
   */
  public void drawHelp() {
    int line = 0;
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(fontColor, 200);
    text("Help on keyboard commands:", 0, 10*line++);
    text("space bar : pause", 0, 10*line++);
    text("           a : show name halos", 0, 10*line++);
    text("           b : show debug", 0, 10*line++);
    text("           d : show date", 0, 10*line++);
    text("           e : show edges", 0, 10*line++);
    text("           E : show physics engine name", 0, 10*line++);
    text("            f : draw files fuzzy", 0, 10*line++);
    text("           h : show histogram", 0, 10*line++);
    text("            j : draw files jelly", 0, 10*line++);
    text("            l : show legend", 0, 10*line++);
    text("           p : show popular", 0, 10*line++);
    text("           q : quit code_swarm", 0, 10*line++);
    text("           s : draw names sharp", 0, 10*line++);
    text("           S : draw files sharp", 0, 10*line++);
    text("   minus : previous physics engine", 0, 10*line++);
    text("      plus : next physics engine", 0, 10*line++);
    text("           ? : show help", 0, 10*line++);
  }
  /**
   *  Show debug information about all drawable objects
   */
  public void drawDebugData() {
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(fontColor, 200);
    text("Nodes: " + nodes.size(), 0, 0);
    text("People: " + people.size(), 0, 10);
    text("Queue: " + eventsQueue.size(), 0, 20);
    text("Last render time: " + lastDrawDuration, 0, 30);
  }
  
  /**
   * TODO This could be made to look a lot better.
   */
  public void drawPopular() {
    CopyOnWriteArrayList <FileNode> al=new CopyOnWriteArrayList<FileNode>();
    noStroke();
    textFont(font);
    textAlign(RIGHT, TOP);
    fill(fontColor, 200);
    text("Popular Nodes (touches):", width-120, 0);
    for (FileNode fn : nodes.values()) {
      if (fn.qualifies()) {
        // Insertion Sort
        if (al.size() > 0) {
          int j = 0;
          for (; j < al.size(); j++) {
            if (fn.compareTo(al.get(j)) <= 0) {
              continue;
            } else {
              break;
            }
          }
          al.add(j,fn);
        } else {
          al.add(fn);
        }
      }
    }

    int i = 1;
    ListIterator<FileNode> it = al.listIterator();
    while (it.hasNext()) {
      FileNode n = it.next();
      // Limit to the top 10.
      if (i <= 10) {
        text(n.name + "  (" + n.touches + ")", width-100, 10 * i++);
      } else if (i > 10) {
        break;
      }
    }
  }
  
  /**
   * @param name
   * @return physics engine instance
   */
  @SuppressWarnings("unchecked")
  public PhysicsEngine getPhysicsEngine(String name) {
    PhysicsEngine pe = null;
    try {
      Class<PhysicsEngine> c = (Class<PhysicsEngine>)Class.forName(name);
      Constructor<PhysicsEngine> peConstructor = c.getConstructor();
      pe = peConstructor.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    return pe;
  }

  /**
   * @return list of people whose life is > 0
   */
  public static List<PersonNode> getLivingPeople() {
    return Collections.unmodifiableList (livingPeople);
  }
  
  /**
   * @return list of edges whose life is > 0
   */
  public static List<Edge> getLivingEdges() {
    return Collections.unmodifiableList (livingEdges);
  }
  
  /**
   * @return list of file nodes whose life is > 0
   */
  public static List<FileNode> getLivingNodes() {
    return Collections.unmodifiableList (livingNodes);
  }
  
  private static <T extends Drawable> List<T> filterLiving(Collection<T> iter) {
    ArrayList<T> livingThings = new ArrayList<T>(iter.size());
    for (T thing : iter)
      if (thing.isAlive())
        livingThings.add(thing);
    return livingThings;
  }
  
  /**
   *  Take screenshot
   */
  public void dumpFrame() {
    if (frameCount < this.maxFramesSaved){
      final File outputFile = new File(insertFrame(SCREENSHOT_FILE));
      final PImage image = get();
      outputFile.getParentFile().mkdirs();

      backgroundExecutor.execute(new Runnable() {
        public void run() {
          image.save(outputFile.getAbsolutePath());
        }
      });
    }
  }

  /**
   *  Update the particle positions
   */
  public void update() {
    // Create a new histogram line
    ColorBins cb = new ColorBins();
    history.add(cb);

    nextDate = new Date(prevDate.getTime() + UPDATE_DELTA);
    currentEvent = eventsQueue.peek();

    while (currentEvent != null && currentEvent.date.before(nextDate)) {
      if (finishedLoading){
        currentEvent = eventsQueue.poll();
        if (currentEvent == null)
          return; 
      }
      else {
        try {
          currentEvent = eventsQueue.take();
        } catch (InterruptedException e) {
          System.out.println("Interrupted while fetching current event from eventsQueue");
          e.printStackTrace();
          continue;
        }
      }
      
      FileNode n = findNode(currentEvent.path + currentEvent.filename);
      if (n == null) {
        n = new FileNode(currentEvent);
        nodes.put(currentEvent.path + currentEvent.filename, n);
      } else {
        n.freshen();
      }

      // add to histogram
      cb.add(n.nodeHue);

      PersonNode p = findPerson(currentEvent.author);
      if (p == null) {
        p = new PersonNode(currentEvent.author);
        people.put(currentEvent.author, p);
      } else {
        p.freshen();
      }
      p.addColor(n.nodeHue);
      
      Edge ped = findEdge(n, p);
      if (ped == null) {
        ped = new Edge(n, p);
        edges.put(new Pair<FileNode,PersonNode>(n,p), ped);
      } else
        ped.freshen();
      
      n.setEditor(p);
      
      /*
       * if ( currentEvent.date.equals( prevDate ) ) { Edge e = findEdge( n, prevNode
       * ); if ( e == null ) { e = new Edge( n, prevNode ); edges.add( e ); } else {
       * e.freshen(); } }
       */

      // prevDate = currentEvent.date;
      prevNode = n;
      if (finishedLoading)
        currentEvent = eventsQueue.peek();
      else{
        while(eventsQueue.isEmpty());
        currentEvent = eventsQueue.peek();
      }
        
    }

    prevDate = nextDate;

    // sort colorbins
    cb.sort();

    // restrict history to drawable area
    while (history.size() > 320)
      history.remove();

    // Do not allow toggle Physics Engine yet.
    safeToToggle = false;

    // Init frame:
    mPhysicsEngine.initializeFrame();

	/*
	We cache liveness information at the beginning on the update cycle.

	Have have to do it this way as the physics engine onRelax methods
	loop on all living elements and filtering this for every element
	gets too painfull slow on logs with over 100.000 entries.	
	*/

    livingPeople = filterLiving(people.values());
    livingNodes = filterLiving(nodes.values());
    livingEdges = filterLiving(edges.values());
    
    // update velocity
    for (Edge edge : getLivingEdges()) {
      mPhysicsEngine.onRelax(edge);
    }

    // update velocity
    for (FileNode node : getLivingNodes()) {
      mPhysicsEngine.onRelax(node);
    }

    // update velocity
    for (PersonNode person : getLivingPeople()) {
      mPhysicsEngine.onRelax(person);
    }

    // update position
    for (Edge edge : getLivingEdges()) {
      mPhysicsEngine.onUpdate(edge);
    }

    // update position
    for (FileNode node : getLivingNodes()) {
      mPhysicsEngine.onUpdate(node);
    }

    // update position
    for (PersonNode person : getLivingPeople()) {
      mPhysicsEngine.onUpdate(person);
      person.mPosition.x = max(50, min(width-50,  person.mPosition.x));
      person.mPosition.y = max(45, min(height-15, person.mPosition.y));
    }

    // Finalize frame:
    mPhysicsEngine.finalizeFrame();
    
    safeToToggle = true;
    if (wantToToggle == true) {
      switchPhysicsEngine(toggleDirection);
    }
  }

  /**
   * Searches for the FileNode with a given name
   * @param name
   * @return FileNode with matching name or null if not found.
   */
  public FileNode findNode(String name) {
    return nodes.get(name);
  }

  /**
   * Searches for the Edge connecting the given nodes
   * @param n1 From
   * @param n2 To
   * @return Edge connecting n1 to n2 or null if not found
   */
  public Edge findEdge(FileNode n1, PersonNode n2) {
    return edges.get(new Pair<FileNode, PersonNode>(n1,n2));
  }

  /**
   * Searches for the PersonNode with a given name.
   * @param name
   * @return PersonNode for given name or null if not found.
   */
  public PersonNode findPerson(String name) {
    return people.get(name);
  }

  /**
   *  Load the standard event-formatted file.
   *  @param filename
   */
  public void loadRepEvents(String filename) {
    if (userConfigFilename  != null) {
      String parentPath = new File(userConfigFilename).getParentFile().getAbsolutePath();
      File fileInConfigDir = new File(parentPath, filename); 
      if (fileInConfigDir.exists())
        filename = fileInConfigDir.getAbsolutePath();
    }
    
    final String fullFilename = filename;
    Runnable eventLoader = new XMLQueueLoader(fullFilename, eventsQueue, isInputSorted);
    
    if (isInputSorted)
      backgroundExecutor.execute(eventLoader);
    else
      //we have to load all of the data before we can continue if it isn't sorted
      eventLoader.run();
  }

  /*
   * Output file events for debugging void printQueue() { while(
   * eventsQueue.size() > 0 ) { FileEvent fe = (FileEvent)eventsQueue.poll();
   * println( fe.date ); } }
   */

  /**
   * @note Keystroke callback function
   */
  public void keyPressed() {
    switch (key) {
      case ' ': {
        pauseButton();
        break;
      }
      case 'a': {
        drawNamesHalos = !drawNamesHalos;
        break;
      }
      case 'b': {
        showDebug = !showDebug;
        break;
      }
      case 'd': {
        showDate = !showDate;
        break;
      }
      case 'e' : {
        showEdges = !showEdges;
        break;
      }
      case 'E' : {
        showEngine = !showEngine;
        break;
      }
      case 'f' : {
        drawFilesFuzzy = !drawFilesFuzzy;
        break;
      }
      case 'h': {
        showHistogram = !showHistogram;
        break;
      }
      case 'j' : {
        drawFilesJelly = !drawFilesJelly;
        break;
      }
      case 'l': {
        showLegend = !showLegend;
        break;
      }
      case 'p': {
        showPopular = !showPopular;
        break;
      }
      case 'q': {
        exit();
        break;
      }
      case 's': {
        drawNamesSharp = !drawNamesSharp;
        break;
      }
      case 'S': {
        drawFilesSharp = !drawFilesSharp;
        break;
      }
      case '-': {
        wantToToggle = true;
        toggleDirection = false;
        break;
      }
      case '+': {
        wantToToggle = true;
        toggleDirection = true;
        break;
      }
      case '?': {
        showHelp = !showHelp;
        break;
      }
    }
  }

  /**
   * Method to switch between Physics Engines
   * @param direction Indicates whether or not to go left or right on the list
   */
  public void switchPhysicsEngine(boolean direction) {
    if (mPhysicsEngineChoices.size() > 1 && safeToToggle) {
      boolean found = false;
      for (int i = 0; i < mPhysicsEngineChoices.size() && !found; i++) {
        if (mPhysicsEngineChoices.get(i).pe == mPhysicsEngine) {
          found = true;
          wantToToggle = false;
          if (direction == true) {
            if ((i+1) < mPhysicsEngineChoices.size()) {
              mPhysicsEngine=mPhysicsEngineChoices.get(i+1).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(i+1).name;
            } else {
              mPhysicsEngine=mPhysicsEngineChoices.get(0).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(0).name;
            }
          } else {
            if ((i-1) >= 0) {
              mPhysicsEngine=mPhysicsEngineChoices.get(i-1).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(i-1).name;
            } else {
              mPhysicsEngine=mPhysicsEngineChoices.get(mPhysicsEngineChoices.size()-1).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(mPhysicsEngineChoices.size()-1).name;
            }
          }
        }
      }
    }
  }

  /**
   *  Toggle pause
   */
  public void pauseButton() {
    if (!paused)
      noLoop();
    else
      loop();
    paused = !paused;
  }

  private class XMLQueueLoader implements Runnable {
    private final String fullFilename;
    private BlockingQueue<FileEvent> queue;
    boolean isXMLSorted;
    private Set<String> peopleSeen = new TreeSet<String>();
    
    private XMLQueueLoader(String fullFilename, BlockingQueue<FileEvent> queue, boolean isXMLSorted) {
      this.fullFilename = fullFilename;
      this.queue = queue;
      this.isXMLSorted = isXMLSorted;
    }

    public void run(){
      XMLReader reader = null;
      try {
        reader = XMLReaderFactory.createXMLReader();
      } catch (SAXException e) {
        System.out.println("Couldn't find/create an XML SAX Reader");
        e.printStackTrace();
        System.exit(1);
      }
      reader.setContentHandler(new DefaultHandler(){
        public void startElement(String uri, String localName, String name,
            Attributes atts) throws SAXException {
          if (name.equals("event")){
            String eventFilename = atts.getValue("filename");
            String eventDatestr = atts.getValue("date");
            long eventDate = Long.parseLong(eventDatestr);
            
            //It's difficult for the user to tell that they're missing events,
            //so we should crash in this case
            if (isXMLSorted){
              if (eventDate < maximumDateSeenSoFar){
                System.out.println("Input not sorted, you must set IsInputSorted to false in your config file");
                System.exit(1);
              }
              else
                maximumDateSeenSoFar = eventDate;
            }
            
            String eventAuthor = atts.getValue("author");
            // int eventLinesAdded = atts.getValue( "linesadded" );
            // int eventLinesRemoved = atts.getValue( "linesremoved" );
            
            FileEvent evt = new FileEvent(eventDate, eventAuthor, "", eventFilename);
            
            //We want to pre-fetch images to minimize lag as images are loaded
            if (!peopleSeen.contains(eventAuthor)){
              avatarFetcher.fetchUserImage(eventAuthor);
              peopleSeen.add(eventAuthor);
            }
            
            try {
              queue.put(evt);
            } catch (InterruptedException e) {
              System.out.println("Interrupted while trying to put into eventsQueue");
              e.printStackTrace();
              System.exit(1);
            }
          }
        }
        public void endDocument(){
          finishedLoading = true;
        }
      });
      try {
        reader.parse(fullFilename);
      } catch (Exception e) {
        System.out.println("Error parsing xml:");
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  class Utils {
    Utils () {
    }
    /**
       * Draws a point.
       * @param x
       * @param y
       * @param red
       * @param green
       * @param blue
       */
    public void drawPoint (int x, int y, int red, int green, int blue) {
      noStroke();
      colorMode(RGB);
      stroke(red, green, blue);
      point(x, y);
    }

    /**
       * Draws a line.
       * @param fromX
       * @param fromY
       * @param toX
       * @param toY
       * @param red
       * @param green
       * @param blue
       */
    public void drawLine (int fromX, int fromY, int toX, int toY, int red, int green, int blue) {
      noStroke();
      colorMode(RGB);
      stroke(red, green, blue);
      strokeWeight(1.5f);
      line(fromX, fromY, toX, toY);
    }
  }

  /**
   * Class to associate the Physics Engine name to the
   * Physics Engine interface
   */
  class peConfig {
    protected String name;
    protected PhysicsEngine pe;

    peConfig(String n, PhysicsEngine p) {
      name = n;
      pe = p;
    }
  }


  /**
   * Describe an event on a file
   */
  class FileEvent implements Comparable<Object> {
    Date date;
    String author;
    String filename;
    String path;
    int linesadded;
    int linesremoved;

    /**
     * short constructor with base data
     */
    FileEvent(long datenum, String author, String path, String filename) {
      this(datenum, author, path, filename, 0, 0);
    }

    /**
     * constructor with number of modified lines
     */
    FileEvent(long datenum, String author, String path, String filename, int linesadded, int linesremoved) {
      this.date = new Date(datenum);
      this.author = author;
      this.path = path;
      this.filename = filename;
      this.linesadded = linesadded;
      this.linesremoved = linesremoved;
    }

    /**
     * Comparing two events by date (Not Used)
     * @param o
     * @return -1 if <, 0 if =, 1 if >
     */
    public int compareTo(Object o) {
      return date.compareTo(((FileEvent) o).date);
    }
  }

  /**
   * Base class for all drawable objects
   * 
   *        Lists and implements features common to all drawable objects
   *        Edge and Node, FileNode and PersonNode
   */
  abstract class Drawable {
    public int life;

    final public int LIFE_INIT;
    final public int LIFE_DECREMENT;
    /**
     * 1) constructor(s)
     * 
     * Init jobs common to all objects
     */
    Drawable(int lifeInit, int lifeDecrement) {
      // save config vars
      LIFE_INIT      = lifeInit;
      LIFE_DECREMENT = lifeDecrement;
      // init life relative vars
      life           = LIFE_INIT;
    }
        
    /**
     *  4) shortening life.
     */
    public void decay() {
      if (isAlive()) {
        life += LIFE_DECREMENT;
        if (life < 0) {
          life = 0;
        }
      }
    }

    /**
     * 5) drawing the new state => done in derived class.
     */
    public abstract void draw();

    /**
     * 6) reseting life as if new.
     */
    public abstract void freshen();
    
    /**
     * @return true if life > 0
     */
    public boolean isAlive() {
      return life > 0;
    }
    
  }

  /**
   * An Edge link two nodes together : a File to a Person.
   */
  class Edge extends Drawable {
    protected FileNode nodeFrom;
    protected PersonNode nodeTo;
    protected float len;

    
    /**
     * 1) constructor.
     * @param from FileNode
     * @param to PersonNode
     */
    Edge(FileNode from, PersonNode to) {
      super(EDGE_LIFE_INIT, EDGE_LIFE_DECREMENT);
      this.nodeFrom = from;
      this.nodeTo   = to;
      this.len      = EDGE_LEN;  // 25
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 40) {
        stroke(255, life + 100);
        strokeWeight(0.35f);
        line(nodeFrom.mPosition.x, nodeFrom.mPosition.y, nodeTo.mPosition.x, nodeTo.mPosition.y);
      }
    }

    public void freshen() {
      life = EDGE_LIFE_INIT;
    }
  }

  /**
   * A node is an abstraction for a File or a Person.
   */
  public abstract class Node extends Drawable {
    protected String name;
    protected Vector2f mPosition;
    protected Vector2f mLastPosition;
    protected float mFriction;
    protected float currentWidth;

    /**
     * mass of the node
     */
    protected float mass; // Currently unused

    /**
     * 1) constructor.
     */
    Node(int lifeInit, int lifeDecrement) {
      super(lifeInit, lifeDecrement);
      mPosition = new Vector2f();
      mLastPosition = new Vector2f();
      mFriction = 1.0f; // No friction
    }

  }

  /**
   * A node describing a file
   */
  class FileNode extends Node implements Comparable<FileNode> {
    private int nodeHue;
    private int minBold;
    protected int touches;
    private PersonNode lastEditor = null;
    
    /**
     * @return file node as a string
     */
    public String toString() {
      return "FileNode{" + "name='" + name + '\'' + ", nodeHue=" + nodeHue + ", touches=" + touches + '}';
    }

    /**
     * 1) constructor.
     */
    FileNode(FileEvent fe) {
      super(FILE_LIFE_INIT, FILE_LIFE_DECREMENT); // 255, -2
      name = fe.path + fe.filename;
      touches = 1;
      life = FILE_LIFE_INIT;
      colorMode(RGB);
      minBold = (int)(FILE_LIFE_INIT * ((100.0f - HIGHLIGHT_PCT)/100));
      nodeHue = colorAssigner.getColor(name);
      mass = FILE_MASS;
      mPosition.set(mPhysicsEngine.startLocation(this));
      mLastPosition.set(new Vector2f(mPosition));
      mLastPosition.add(mPhysicsEngine.startVelocity(this));
      mFriction = 0.9f;
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (isAlive()) {
        if (drawFilesSharp) {
          drawSharp();
        }
        if (drawFilesFuzzy) {
          drawFuzzy();
        }
        if (drawFilesJelly) {
          drawJelly();
        }
        // Draw motion blur
        // float d = mPosition.distance(mLastPosition);
        
        float nx = mPosition.x - mLastPosition.x;
        float ny = mPosition.y - mLastPosition.y;
        float d = (float) Math.sqrt(nx * nx + ny * ny);
        
        stroke(nodeHue, min(255f * (d / 10f), 255f) / 10f);
        strokeCap(ROUND);
        strokeWeight(currentWidth / 4f);
        // strokeWeight((float)life / 10.0 * (float)PARTICLE_SIZE);
        line(mPosition.x, mPosition.y, mLastPosition.x, mLastPosition.y);
        /** TODO : this would become interesting on some special event, or for special materials
         * colorMode( RGB ); fill( 0, life ); textAlign( CENTER, CENTER ); text( name, x, y );
         * Example below:
         */
        if (showPopular) {
          textAlign( CENTER, CENTER );
          fill(fontColor, 200);
          if (this.qualifies()) {
            text(touches, mPosition.x, mPosition.y - (8 + (int)Math.sqrt(touches)));
          }
        }
      }
    }

    /**
     * 6) reseting life as if new.
     */
    public void freshen() {
      life = FILE_LIFE_INIT;
      if (++touches > maxTouches) {
        maxTouches = touches;
      }
    }
    
    public boolean isAlive() {
      boolean alive = life > 0;
      if (!alive && lastEditor != null) {
        int idx = lastEditor.editing.indexOf(this);
        if (idx != -1)
          lastEditor.editing.set(idx, null);
        lastEditor = null;
      }
        
      return alive;
    }
    
    public void setEditor(PersonNode editor) {
      if (editor == lastEditor)
        return;
      if (lastEditor != null)
        lastEditor.editing.set(lastEditor.editing.indexOf(this), null);
      lastEditor = editor;
      int firstNullIndex = editor.editing.indexOf(null);
      if (firstNullIndex == -1)
        editor.editing.add(this);
      else
        editor.editing.set(firstNullIndex, this);
    }

    public boolean qualifies() {
      if (this.touches >= (maxTouches * 0.5f)) {
        return true;
      }
      return false;
    }

    public int compareTo(FileNode fn) {
      int retval = 0;
      if (this.touches < fn.touches) {
        retval = -1;
      } else if (this.touches > fn.touches) {
        retval = 1;
      }
      return retval;
    }

    public void drawSharp() {
      colorMode(RGB);
      fill(nodeHue, life);
      float w = 3 * PARTICLE_SIZE;
      currentWidth = w;
      if (life >= minBold) {
        stroke(255, 128);
        w *= 2;
      } else {
        noStroke();
      }

      ellipseMode(CENTER);
      ellipse(mPosition.x, mPosition.y, w, w);
    }

    public void drawFuzzy() {
      tint(nodeHue, life);

      float w = (8 + (sqrt(touches) * 4)) * PARTICLE_SIZE;
      currentWidth = w;
      // not used float dubw = w * 2;
      float halfw = w / 2;
      if (life >= minBold) {
        colorMode(HSB);
        tint(hue(nodeHue), saturation(nodeHue) - 192, 255, life);
        // image( sprite, x - w, y - w, dubw, dubw );
      }
      // else
      image(sprite, mPosition.x - halfw, mPosition.y - halfw, w, w);
    }

    public void drawJelly() {
      noFill();
      if (life >= minBold)
        stroke(255);
      else
        stroke(nodeHue, life);
      float w = sqrt(touches) * PARTICLE_SIZE;
      currentWidth = w;
      ellipseMode(CENTER);
      ellipse(mPosition.x, mPosition.y, w, w);
    }
  }

  /**
   * A node describing a person
   */
  class PersonNode extends Node {
    private int flavor = color(0);
    private int colorCount = 1;
    private int minBold;
    protected int touches;
    public List<FileNode> editing = new ArrayList<FileNode>();
    private PImage icon = null;
    /**
     * 1) constructor.
     */
    PersonNode(String n) {
      super(PERSON_LIFE_INIT, PERSON_LIFE_DECREMENT); // -1
      name = n;
      minBold = (int)(PERSON_LIFE_INIT * (1 - (HIGHLIGHT_PCT/100.0)));
      mass = PERSON_MASS; // bigger mass to person then to node, to stabilize them
      touches = 1;
      mPosition.set(mPhysicsEngine.startLocation(this));
      mLastPosition.set(new Vector2f(mPosition)); 
      mLastPosition.add(mPhysicsEngine.startVelocity(this));
      mFriction = 0.99f;
      String iconFile = avatarFetcher.fetchUserImage(name);
      if (iconFile != null) {
        icon = loadImage(iconFile, "unknown");
        if (circularAvatars)
          icon.mask(avatarMask);
      }
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (isAlive()) {
        textAlign(CENTER, CENTER);

        /** TODO: proportional font size, or light intensity,
                  or some sort of thing to disable the flashing */
        if (life >= minBold)
          textFont(boldFont);
        else
          textFont(font);
        
        fill(fontColor, life);
        if(showUserName)
          text(name, mPosition.x, mPosition.y+10);
        if (icon != null){
          colorMode(RGB);
          tint(255,255,255,max(0,life-80));
          image(icon, mPosition.x-(avatarFetcher.size / 2), mPosition.y-(avatarFetcher.size - ( showUserName ? 5 : 15)));
        }
      }
    }

    public void freshen () {
      life = PERSON_LIFE_INIT;
      touches++;
    }

    public void addColor(int c) {
      colorMode(RGB);
      flavor = lerpColor(flavor, c, 1.0f / colorCount);
      colorCount++;
    }
  }

  /**
   * code_swarm Entry point.
   * @param args : should be the path to the config file
   */
  static public void main(String args[]) {
    try {
      if (args.length > 0) {
        userConfigFilename = args[0];
        List<String> configFileStack = Arrays.asList(new String[]{"defaults/code_swarm.config", 
                                                                  "defaults/user.config",
                                                                  args[0]});
        start(new CodeSwarmConfig(configFileStack));
      } else {
        System.err.println("Specify a config file.");
        System.exit(2);
      }
    } catch (IOException e) {
      System.err.println("Failed due to exception: " + e.getMessage());
      System.exit(2);
    }
  }
  /**
   * the alternative entry-point for code_swarm. It gets called from
   * {@link MainView} after fetching the repository log.
   * @param config the modified config 
   *        (it's InputFile-property has been changed to reflect the 
   *        fetched repository-log)
   */
  public static void start(CodeSwarmConfig config){
    cfg = config;
    PApplet.main(new String[]{"code_swarm"});
  }

}
