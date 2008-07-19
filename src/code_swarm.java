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

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.xml.XMLElement;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import javax.vecmath.Vector2f;

/**
 * Definition of the code_swarm Application.
 */
public class code_swarm extends PApplet {
  /** @remark needed for any serializable class */ 
  public static final long serialVersionUID = 0;
  
  // User-defined variables
  CodeSwarmConfig config;
  int FRAME_RATE = 24;
  long UPDATE_DELTA = -1;
  String SPRITE_FILE = "particle.png";
  String SCREENSHOT_FILE;
  int background;
 
  // Data storage
  PriorityBlockingQueue<FileEvent> eventsQueue; // USE PROCESSING 0142 or higher
  CopyOnWriteArrayList<FileNode> nodes;
  CopyOnWriteArrayList<Edge> edges;
  CopyOnWriteArrayList<PersonNode> people;
  LinkedList<ColorBins> history;

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

  // Graphics state variables
  boolean looping = true;
  boolean showHistogram = true;
  boolean showDate = true;
  boolean showLegend = false;
  boolean showPopular = false;
  boolean showEdges = false;
  boolean showHelp = false;
  boolean takeSnapshots = false;
  boolean showDebug = false;
  boolean drawNamesSharp = false;
  boolean drawNamesHalos = false;
  boolean drawFilesSharp = false;
  boolean drawFilesFuzzy = false;
  boolean drawFilesJelly = false;

  // Color mapper
  ColorAssigner colorAssigner;
  int currentColor;

  // Edge Length
  private int EDGE_LEN = 25;
  // Drawable object life decrement
  private int EDGE_LIFE_INIT = 255;
  private int FILE_LIFE_INIT = 255;
  private int PERSON_LIFE_INIT = 255;
  private final int EDGE_LIFE_DECREMENT = -2;
  private final int FILE_LIFE_DECREMENT = -2;
  private final int PERSON_LIFE_DECREMENT = -1;
  // Physical engine configuration
  String          physicalEngineSelection;
  PhysicalEngine  mPhysicalEngine = null;

  // Physical algorithms (class) names
  // TODO: to complete with more physical engines
  static final String PHYSICAL_ENGINE_LEGACY  = "PhysicalEngineLegacy";
  
  // Formats the date string nicely
  DateFormat formatter = DateFormat.getDateInstance();

  private static CodeSwarmConfig cfg;
  private long lastDrawDuration = 0;
  private boolean loading = true;
  private String loadingMessage = "Reading input file";

  /**
   * Initialization
   */
  public void setup() {
    int width=cfg.getIntProperty(CodeSwarmConfig.WIDTH_KEY,640);
    if (width <= 0) {
      width = 640;
    }
    
    int height=cfg.getIntProperty(CodeSwarmConfig.HEIGHT_KEY,480);
    if (height <= 0) {
      height = 480;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.USE_OPEN_GL, false)) {
      size(width, height, OPENGL);
    } else {
      size(width, height);
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_LEGEND, false)) {
      showLegend = true;
    } else {
      showLegend = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_HISTORY, false)) {
      showHistogram = true;
    } else {
      showHistogram = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DATE, false)) {
      showDate = true;
    } else {
      showDate = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_EDGES, false)) {
      showEdges = true;
    } else {
      showEdges = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DEBUG, false)) {
      showDebug = true;
    } else {
      showDebug = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.TAKE_SNAPSHOTS_KEY,false)) {
      takeSnapshots = true;
    } else {
      takeSnapshots = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_SHARP, true)) {
      drawNamesSharp = true;
    } else {
      drawNamesSharp = false;
    }   
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_HALOS, false)) {
      drawNamesHalos = true;
    } else {
      drawNamesHalos = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_SHARP, false)) {
      drawFilesSharp = true;
    } else {
      drawFilesSharp = false;
    }   
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_FUZZY, true)) {
      drawFilesFuzzy = true;
    } else {
      drawFilesFuzzy = false;
    }   
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_JELLY, false)) {
      drawFilesJelly = true;
    } else {
      drawFilesJelly = false;
    }   
    
    background = cfg.getBackground().getRGB();
    
    // Ensure we have sane values.
    EDGE_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.EDGE_LIFE_KEY,255);
    if (EDGE_LIFE_INIT <= 0) {
      EDGE_LIFE_INIT = 255;
    }
    
    FILE_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.FILE_LIFE_KEY,255);
    if (FILE_LIFE_INIT <= 0) {
      FILE_LIFE_INIT = 255;
    }
    
    PERSON_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.PERSON_LIFE_KEY,255);
    if (PERSON_LIFE_INIT <= 0) {
      PERSON_LIFE_INIT = 255;
    }
    
    UPDATE_DELTA = cfg.getIntProperty("testsets"/*CodeSwarmConfig.MSEC_PER_FRAME_KEY*/, -1);
    if (UPDATE_DELTA == -1) {
      int framesperday = cfg.getIntProperty(CodeSwarmConfig.FRAMES_PER_DAY_KEY, 4);
      if (framesperday > 0) {
        UPDATE_DELTA = (long) (86400000 / framesperday);
      }
    }
    if (UPDATE_DELTA <= 0) {
      // Default to 4 frames per day.
      UPDATE_DELTA = 21600000;
    }

    // Physical engine configuration and instantiation
    physicalEngineSelection = cfg.getStringProperty( CodeSwarmConfig.PHYSICAL_ENGINE_SELECTION, PHYSICAL_ENGINE_LEGACY );
    
    // TODO: to complete with more physical engines
    if ( physicalEngineSelection.equals( PHYSICAL_ENGINE_LEGACY ) ) {
      mPhysicalEngine = new PhysicalEngineLegacy(1.0f, 0.01f, 1.0f, 0.5f); // (forceEdgeMultiplier, forceCalculationRandomizer, forceToSpeedMultiplier, speedToPositionDrag)
    }
    else {
      // legacy is current default
      mPhysicalEngine = new PhysicalEngineLegacy(1.0f, 0.01f, 1.0f, 0.5f); // (forceEdgeMultiplier, forceCalculationRandomizer, forceToSpeedMultiplier, speedToPositionDrag)
    }
    
    smooth();
    frameRate(FRAME_RATE);

    // init data structures
    eventsQueue = new PriorityBlockingQueue<FileEvent>();
    nodes       = new CopyOnWriteArrayList<FileNode>();
    edges       = new CopyOnWriteArrayList<Edge>();
    people      = new CopyOnWriteArrayList<PersonNode>();
    history     = new LinkedList<ColorBins>();

    // Init color map
    initColors();

    /**
     * @todo Fix this Thread code.  It is broken somehow.
     * @todo It causes valid setups to exit with no message.
     * @todo Only after several attempts will it eventually work.
     */
//    Thread t = new Thread(new Runnable() {
//      public void run() {
        loadRepEvents(cfg.getStringProperty(CodeSwarmConfig.INPUT_FILE_KEY)); // event formatted (this is the standard)
        prevDate = eventsQueue.peek().date;
//      }
//    });
//    t.setDaemon(true);
//    t.start();
    /** TODO: use adapter pattern to handle different data sources */

    SCREENSHOT_FILE = cfg.getStringProperty(CodeSwarmConfig.SNAPSHOT_LOCATION_KEY);
    EDGE_LEN = cfg.getIntProperty(CodeSwarmConfig.EDGE_LENGTH_KEY);
    if (EDGE_LEN <= 0) {
      EDGE_LEN = 25;
    }

    // Create fonts
    /**
     * @todo Put this in the config.
     */
    font = createFont("SansSerif", 10);
    boldFont = createFont("SansSerif.bold", 14);
    textFont(font);

    String SPRITE_FILE = cfg.getStringProperty(CodeSwarmConfig.SPRITE_FILE_KEY);
    // Create the file particle image
    sprite = loadImage(SPRITE_FILE);
    // Add translucency (using itself in this case)
    sprite.mask(sprite);
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
   *  kept for reference
   *  @deprecated
   */
  public void apacheColors() {
    colorAssigner.addRule("Source", "/src.*", color(0, 255, 255), color(15, 255, 255));
    colorAssigner.addRule("Docs", "/doc.*", color(150, 255, 255), color(170, 255, 255));
    colorAssigner.addRule("Modules", "/mod.*|/contrib.*", color(25, 255, 255), color(40, 255, 255));
  }

  /**
   *  kept for reference
   *  @deprecated
   */
  public void pythonColors() {
    colorAssigner.addRule("Text", ".*\\.tex|.*\\.txt", color(150, 255, 255), color( 170, 255, 255));
    colorAssigner.addRule("Modules", ".*/Modules/.*", color(25, 255, 255), color( 40, 255, 255));
    colorAssigner.addRule("Source", ".*\\.py|.*\\.c|.*\\.h", color(0, 255, 255), color(15, 255, 255));
    colorAssigner.addRule("Docs", ".*/Doc/.*", color(150, 255, 255), color(170, 255, 255));
  }

  /**
   *  kept for reference
   *  @deprecated
   */
  public void javaColors() {
    colorAssigner.addRule("Source", ".*\\.java|.*/src/.*", color(0, 255, 255), color(15, 255, 255));
    colorAssigner.addRule("Docs", ".*/docs/.*|.*/xdocs/.*", color(150, 255, 255), color(170, 255, 255));
    colorAssigner.addRule("Libs", ".*/lib/.*", color(25, 255, 255), color(40, 255, 255));
  }

  /**
   *  kept for reference
   *  @deprecated
   */
  public void eclipseColors() {
    colorAssigner.addRule("Source", ".*\\.java|.*/src/.*", color(0, 255, 255), color(15, 255, 255));
    colorAssigner.addRule("Docs", ".*/doc/.*|.*/xdocs/.*", color(150, 255, 255), color(170, 255, 255));
    colorAssigner.addRule("Libs", ".*/lib/.*", color(25, 255, 255), color(40, 255, 255));
    colorAssigner.addRule("Images", ".*\\.gif|.*\\.jpg", color(120, 255, 255), color(135, 255, 255));
  }

  /**
   * Main loop
   */
  public void draw() {
    long start = System.currentTimeMillis();
    background(background); // clear screen with background color

    if (loading) {
      drawLoading();
    }
    else {
      this.update(); // update state to next frame
      
      // Draw edges (for debugging only)
      if (showEdges) {
        for (Edge edge : edges) {
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
      for (FileNode node : nodes) {
        node.draw();
      }

      textFont(font);

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
      if (eventsQueue.isEmpty()) {
        // noLoop();
        exit();
      }
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
    for (int i = 0; i < people.size(); i++) {
      PersonNode p = (PersonNode) people.get(i);
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
    for (int i = 0; i < people.size(); i++) {
      PersonNode p = (PersonNode) people.get(i);
      fill(lerpColor(p.flavor, color(255), 0.5f), max(p.life - 50, 0));
      p.draw();
    }
  }

  /**
   * Draw date in lower-right corner
   */
  public void drawDate() {
    fill(255);
    String dateText = formatter.format(prevDate);
    textAlign(RIGHT, BASELINE);
    textSize(10);
    text(dateText, width - 1, height - textDescent());
  }

  /**
   *  Draw histogram in lower-left
   */
  public void drawHistory() {
    Iterator<ColorBins> itr = history.iterator();
    int counter = 0;

    while (itr.hasNext()) {
      ColorBins cb = itr.next();

      for (int i = 0; i < cb.num; i++) {
        int c = cb.colorList[i];
        stroke(c, 200);
        point(counter, height - i - 3);
      }
      counter++;
    }
  }
  
  /**
   * Show the Loading screen.
   */

  public void drawLoading() {
    noStroke();
    textFont(font, 20);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text(loadingMessage, 0, 0);
  }

  /**
   *  Show color codings
   */
  public void drawLegend() {
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Legend:", 0, 0);
    for (int i = 0; i < colorAssigner.tests.size(); i++) {
      ColorTest t = colorAssigner.tests.get(i);
      fill(t.c1, 200);
      text(t.label, 10, (i + 1) * 10);
    }
  }

  /**
   *  Show short help on avaible commands
   */
  public void drawHelp() {
    int line = 0;
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Help on Keyboard commands:", 0, 10*line++);
    text("- space bar : pause", 0, 10*line++);
    text("- a : show name hAlos", 0, 10*line++);
    text("- b : show deBug", 0, 10*line++);
    text("- d : show Date", 0, 10*line++);
    text("- e : show Edges", 0, 10*line++);
    text("- f : draw files Fuzzy", 0, 10*line++);
    text("- h : show Histogram", 0, 10*line++);
    text("- j : draw files Jelly", 0, 10*line++);
    text("- l : show Legend", 0, 10*line++);
    text("- p : show Popular", 0, 10*line++);
    text("- q : Quit code_swarm", 0, 10*line++);
    text("- s : draw names Sharp", 0, 10*line++);
    text("- S : draw files Sharp", 0, 10*line++);
    text("- ? : show help", 0, 10*line++);
  }
  /**
   *  Show debug information about all drawable objects
   */
  public void drawDebugData() {
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Nodes: " + nodes.size(), 0, 0);
    text("People: " + people.size(), 0, 10);
    text("Queue: " + eventsQueue.size(), 0, 20);
    text("Last render time: " + lastDrawDuration, 0, 30);
  }

  /**
   * @todo This could be made to look a lot better.
   */
  public void drawPopular() {
    SortedSet <FileNode> al=new TreeSet<FileNode>();
    noStroke();
    textFont(font);
    textAlign(RIGHT, TOP);
    fill(255, 200);
    text("Popular Nodes (touches):", width-120, 0);
    for (int i = 0; i < nodes.size(); i++) {
      FileNode fn = (FileNode) nodes.get(i);
      if (fn.qualifies()) {
        al.add(fn);
      }
    }
    
    int i = 1;
    Iterator<FileNode> it = al.iterator();
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
   *  Take screenshot
   */
  public void dumpFrame() {
    if (frameCount < 100000)
      saveFrame(SCREENSHOT_FILE);
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
      currentEvent = eventsQueue.poll();
      if (currentEvent == null)
        return;

      FileNode n = findNode(currentEvent.path + currentEvent.filename);
      if (n == null) {
        n = new FileNode(currentEvent);
        nodes.add(n);
      } else {
        n.freshen();
      }

      // add to color bin
      cb.add(n.nodeHue);

      PersonNode p = findPerson(currentEvent.author);
      if (p == null) {
        p = new PersonNode(currentEvent.author);
        people.add(p);
      } else {
        p.freshen();
      }
      p.addColor(n.nodeHue);

      Edge ped = findEdge(n, p);
      if (ped == null) {
        ped = new Edge(n, p);
        edges.add(ped);
      } else
        ped.freshen();

      /*
       * if ( currentEvent.date.equals( prevDate ) ) { Edge e = findEdge( n, prevNode
       * ); if ( e == null ) { e = new Edge( n, prevNode ); edges.add( e ); } else {
       * e.freshen(); } }
       */
      
      // prevDate = currentEvent.date;
      prevNode = n;
      currentEvent = eventsQueue.peek();
    }

    prevDate = nextDate;

    // sort colorbins
    cb.sort();

    // restrict history to drawable area
    while (history.size() > 320)
      history.remove();

    for (Edge edge : edges) {
      edge.relax();
    }

    for (FileNode node : nodes) {
      node.relax();
    }

    for (PersonNode aPeople : people) {
      aPeople.relax();
    }

    for (Edge edge : edges) {
      edge.update();
    }

    for (FileNode node : nodes) {
      node.update();
    }

    for (PersonNode aPeople : people) {
      aPeople.update();
    }
  }

  /**
   * Searches the nodes array for a given name
   * @param name
   * @return FileNode with matching name or null if not found.
   */
  public FileNode findNode(String name) {
    for (FileNode node : nodes) {
      if (node.name.equals(name))
        return node;
    }
    return null;
  }

  /**
   * Searches the nodes array for a given name
   * @param n1 From
   * @param n2 To
   * @return Edge connecting n1 to n2 or null if not found
   */
  public Edge findEdge(Node n1, Node n2) {
    for (Edge edge : edges) {
      if (edge.nodeFrom == n1 && edge.nodeTo == n2)
        return edge;
    }
    return null;
  }

  /**
   * Searches the people array for a given name.
   * @param name
   * @return PersonNode for given name or null if not found.
   */
  public PersonNode findPerson(String name) {
    for (PersonNode p : people) {
      if (p.name.equals(name))
        return p;
    }
    return null;
  }

  /**
   * Head function for loadRecurse
   * @param filename
   * @deprecated
   */
  public void loadRepository(String filename) {
    XMLElement doc = new XMLElement(this, filename);
    loadRepository(doc, "", "");
    doc = null;
  }

  /**
   * Load repository-formatted file
   * @param xml
   * @param path
   * @param filename
   * @deprecated
   */
  public void loadRepository(XMLElement xml, String path, String filename) {
    String tag = xml.getName();
    if (tag.equals("event")) {
      String datestr = xml.getStringAttribute("date");
      long date = Long.parseLong(datestr);
      String author = xml.getStringAttribute("author");
      int linesadded = xml.getIntAttribute("linesadded");
      int linesremoved = xml.getIntAttribute("linesremoved");
      FileEvent evt = new FileEvent(date, author, path, filename, linesadded, linesremoved);
      eventsQueue.add(evt);
    } else if (tag.equals("file")) {
      String name = xml.getStringAttribute("name");
      for (int i = 0; i < xml.getChildCount(); i++)
        loadRepository(xml.getChild(i), path, name);
      } else if (tag.equals("directory")) {
        String name = xml.getStringAttribute("name");
        for (int i = 0; i < xml.getChildCount(); i++)
          loadRepository(xml.getChild(i), path + name, "");
    } else if (tag.equals("log")) {
      // The tag 'log' represents the xml output of a SVN log, which
      // requires slightly different commit event parsing.
      loadSVNRepository(xml);
    }
  }

  /**
   *  Load the standard event-formatted file.
   *  @param filename
   */
  public void loadRepEvents(String filename) {
    XMLElement doc = new XMLElement(this, filename);
    for (int i = 0; i < doc.getChildCount(); i++) {
      XMLElement xml = doc.getChild(i);
      String eventFilename = xml.getStringAttribute("filename");
      String eventDatestr = xml.getStringAttribute("date");
      long eventDate = Long.parseLong(eventDatestr);
      String eventAuthor = xml.getStringAttribute("author");
      // int eventLinesAdded = xml.getIntAttribute( "linesadded" );
      // int eventLinesRemoved = xml.getIntAttribute( "linesremoved" );
      FileEvent evt = new FileEvent(eventDate, eventAuthor, "", eventFilename);
      eventsQueue.add(evt);
      if (eventsQueue.size() % 100 == 0)
        loadingMessage = "Creating events: " + eventsQueue.size();
    }
    loading = false;
    // reset the Frame Counter. Only needed if Threaded.
    // frameCount = 0;
  }

  /**
   * Load SVN log formatted file
   * @param doc 
   * @deprecated
   */
  public void loadSVNRepository(XMLElement doc) {
    // Iterate over commit nodes.
    for (int i = 0; i < doc.getChildCount(); i++) {
      XMLElement xml = doc.getChild(i);

      // In the SVN xml log, timestamps have the following format:
      // 2008-06-19T03:46:04.335538Z
      // The date and the time need to be parsed from this so that we can convert it
      // to a 'long'
      // with the sql.Timestamp class.
      String datestr = xml.getChild("date").getContent();
      String[] datesplit = datestr.split("\\.");
      datesplit = datesplit[0].split("T");
      datestr = datesplit[0] + " " + datesplit[1];
      Timestamp ts = Timestamp.valueOf(datestr);
      long date = ts.getTime();

      // When the SVN repository is created, there is no author associated with the
      // commit, so for
      // now just log it as anonymous.
      /** FIXME: Should we ignored events with no author completely or log them
                       a different way? */
      XMLElement author_node = xml.getChild("author");
      String author = "anonymous";
      if (author_node != null)
        author = author_node.getContent();

      // Under each commit there is a child node named 'paths', which will have it's
      // own
      // 'path' children which contain the paths of the files which were modified and
      // details
      // about the change(file modified, deleted, created, etc...)
      XMLElement paths = xml.getChild("paths");
      String path;
      if (paths != null) {
        for (int j = 0; j < paths.getChildCount(); j++) {
          path = paths.getChild(j).getContent();
          FileEvent evt = new FileEvent(date, author, "", path);
          eventsQueue.add(evt);
        }
      }
    }
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
      case '?': {
        showHelp = !showHelp;
        break;
      }
    }
  }

  /**
   *  Toggle pause
   */
  public void pauseButton() {
    if (looping)
      noLoop();
    else
      loop();
    looping = !looping;
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
     * 2) calculating next frame state => done in derived class
     */
    public abstract void relax();

    /**
     * 3) applying next frame state
     */
    public void update() {
      decay();
    }
    
    /**
     *  4) shortening life.
     */
    public void decay() {
      if (life > 0) {
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
    public void freshen() {
      life = LIFE_INIT;
    }
  }

  /**
   * An Edge link two nodes together : a File to a Person.
   */
  class Edge extends Drawable {
    private Node  nodeFrom;
    private Node  nodeTo;
    private float len;

    /**
     * 1) constructor.
     */
    Edge(Node from, Node to) {
      super(EDGE_LIFE_INIT, EDGE_LIFE_DECREMENT); // 255, -2
      this.nodeFrom = from;
      this.nodeTo   = to;
      this.len      = EDGE_LEN;  // 25
    }

    /**
     * 2) calculating next frame state.
     */
    public void relax() {
      Vector2f force    = new Vector2f();

      // Calculate force between the node "from" and the node "to"
      mPhysicalEngine.calculateForceAlongAnEdge(this, force);

      // transmit (applying) fake force projection to file and person nodes
      /** TODO: remove */
      nodeTo.addDX(force.getX()); // Person
      nodeTo.addDY(force.getY()); // Person
      nodeFrom.addDX(-force.getX()); // File
      nodeFrom.addDY(-force.getY()); // File
      /**/
      // transmit (applying) fake force projection to file and person nodes
      /** TODO: use this instead of above
      mPhysicalEngine.applyForceTo(nodeTo, force);
      Vector2f forceInv = new Vector2f( -force.getX(), -force.getY()); // force is inverted for the other end of the edge
      mPhysicalEngine.applyForceTo(nodeFrom, forceInv);
      */
     }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 240) {
        stroke(255, life);
        strokeWeight(0.35f);
        line(nodeFrom.x, nodeFrom.y, nodeTo.x, nodeTo.y);
      }
    }
    
    public Node getNodeFrom()
    {
      return nodeFrom;
    }
    
    public Node getNodeTo()
    {
      return nodeTo;
    }
    
    public float getLen()
    {
      return len;
    }
  }

  /**
   * A node is an abstraction for a File or a Person.
   */
  public abstract class Node extends Drawable {
    protected String name;
    protected float x, y;
    protected float dx, dy;
    /** TODO: We SHOULD use vector for position, speed and accel, not using x and y everywhere
    protected Vector2f mPosition;
    protected Vector2f mSpeed;
    */
    
    // TODO: mass would serve for "force to speed" conversion, and could be function of "life" or of node's "importance" (commit size, or touches...)
    protected float mass; 
    
    /** TODO: add configuration for max speed */
    protected float maxSpeed = 7.0f;

    /**
     * 1) constructor.
     */
    Node(int lifeInit, int lifeDecrement) {
      super(lifeInit, lifeDecrement);
      /** TODO: implement new sort of (random or not) arrival, with configuration
                => to permit things like "injection points", circular arrival, and so on */
      x = random(width);
      y = random(height);
      mass = 10.0f; // bigger mass to person then to node, to stabilize them
    }

    /**
     * 3) applying next frame state.
     *
     * This is a redefinition of the Drawable update() method
     */
    public void update() {
      // Apply Speed to Position on nodes
      mPhysicalEngine.applySpeedTo(this);
      
      // ensure coherent resulting position
      x = constrain(x, 0, width);
      y = constrain(y, 0, height);

      // shortening life
      decay();
    }

    /**
     * @return x position
     */
    public float getX() {
      return this.x;
    }

    /**
     * @return y position
     */
    public float getY() {
      return this.y;
    }
    
    /**
     * Modify X position by deltax
     * @param dx
     */
    public void addX(float dx) {
      this.x += dx;
    }

    /**
     * Modify Y position by deltay
     * @param dy
     */
    public void addY(float dy) {
      this.y += dy;
    }

    /**
     * 
     * @return deltax
     */
    public float getDX() {
      return this.dx;
    }

    /**
     * 
     * @return deltay
     */
    public float getDY() {
      return this.dy;
    }

    /**
     * 
     * @return length of the vector (Speed)
     */
    public float getSpeed() {
      Vector2f speed = new Vector2f(dx, dy);  /** TODO: use mSpeed vector */
      return speed.length();
    }

    /**
     * 
     * @param ddx
     */
    public void addDX(float ddx) {
      this.dx += ddx;
    }

    /**
     * 
     * @param ddx
     */
    public void addDY(float ddx) {
      this.dy += ddx;
    }

    /**
     * 
     * @param coef
     */
    public void mulDX(float coef) {
      this.dx *= coef;
    }

    /**
     * 
     * @param coef
     */
    public void mulDY(float coef) {
      this.dy *= coef;
    }
    
    /**
     * 
     * @return mass of the node
     */
    public float getMass() {
      return this.mass;
    }
    
  }

  /**
   * A node describing a file, which is repulsed by other files.
   */
  class FileNode extends Node implements Comparable<FileNode> {
    int nodeHue;
    int minBold;
    int touches;

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
      life = LIFE_INIT;
      colorMode(RGB);
      minBold = (int)(LIFE_INIT * 0.95f);
      nodeHue = colorAssigner.getColor(name);
      mass = 1.0f;
    }

    /**
     * 2) calculating next frame state.
     * 
     * TODO: this physic job should be uniformed between file a person nodes
     *       => then it could be moved up
     */
    public void relax() {
      Vector2f forceBetween2Files = new Vector2f();
      Vector2f forceSummation     = new Vector2f();
      
      if (life <= 0)
        return;

      // Calculation of repulsive force between persons
      for (int j = 0; j < nodes.size(); j++) {
        FileNode n = (FileNode) nodes.get(j);
        if (n.life <= 0)
          continue;

        if (n != this) {
          // elemental force calculation, and summation
          mPhysicalEngine.calculateForceBetweenNodes(this, n, forceBetween2Files);
          forceSummation.add(forceBetween2Files);
        }
      }

      // Apply repulsive force from other files to this Node
      mPhysicalEngine.applyForceTo(this, forceSummation);
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 0) {
        if (drawFilesSharp) {
          drawSharp();
        }
        if (drawFilesFuzzy) {
          drawFuzzy();
        }
        if (drawFilesJelly) {
          drawJelly();
        }
        
        /** TODO : this would become interesting on some special event, or for special materials
         * colorMode( RGB ); fill( 0, life ); textAlign( CENTER, CENTER ); text( name, x, y );
         * Example below:
         */
        if (showPopular) {
          textAlign( CENTER, CENTER );
          if (this.qualifies()) {
            text(touches, x, y - (8 + (int)sqrt(touches)));
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
    
    public boolean qualifies() {
      if (this.touches >= maxTouches * 0.5f) {
        return true;
      }
      return false;
    }
    
    // Yes, I know this is backwards.
    public int compareTo(FileNode fn) {
      int retval = 0;
      if (this.touches < fn.touches) {
        retval = 1;
      } else if (this.touches > fn.touches) {
        retval = -1;
      }
      return retval;
    }

    public void drawSharp() {
      colorMode(RGB);
      fill(nodeHue, life);
      float w = 3;

      if (life >= minBold) {
        stroke(255, 128);
        w *= 2;
      } else
        noStroke();
      ellipseMode(CENTER);
      ellipse(x, y, w, w);
    }

    public void drawFuzzy() {
      tint(nodeHue, life);

      float w = 8 + sqrt(touches) * 4;
      // not used float dubw = w * 2;
      float halfw = w / 2;
      if (life >= minBold) {
        colorMode(HSB);
        tint(hue(nodeHue), saturation(nodeHue) - 192, 255, life);
        // image( sprite, x - w, y - w, dubw, dubw );
      }
      // else
      image(sprite, x - halfw, y - halfw, w, w);
    }

    public void drawJelly() {
      noFill();
      if (life >= minBold)
        stroke(255);
      else
        stroke(nodeHue, life);
      float w = sqrt(touches);
      ellipseMode(CENTER);
      ellipse(x, y, w, w);
    }
  }

  /**
   * A node describing a person, which is repulsed by other persons.
   */
  class PersonNode extends Node {
    int flavor = color(0);
    int colorCount = 1;
    int minBold;

    /**
     * 1) constructor.
     */
    PersonNode(String n) {
      super(PERSON_LIFE_INIT, PERSON_LIFE_DECREMENT); // -1
      maxSpeed = 2.0f;
      name = n;
      /** TODO: add config */
      minBold = (int)(LIFE_INIT * 0.95f);
    }

    /**
     * 2) calculating next frame state.
     * 
     * TODO: this physic job should be uniformed between file a person nodes => then
     *       it could be moved up
     */
    public void relax() {
      Vector2f forceBetween2Persons = new Vector2f();
      Vector2f forceSummation       = new Vector2f();

      if (life <= 0)
        return;

      // Calculation of repulsive force between persons
      for (int j = 0; j < people.size(); j++) {
        Node n = (Node) people.get(j);
        if (n.life <= 0)
          continue;

        if (n != this) {
          // elemental force calculation, and summation
          mPhysicalEngine.calculateForceBetweenNodes(this, n, forceBetween2Persons);
          forceSummation.add(forceBetween2Persons);
        }
      }
      
      // Apply repulsive force from other persons to this Node
      mPhysicalEngine.applyForceTo(this, forceSummation);
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life <= 0)
        return;

      textAlign(CENTER, CENTER);

      /** TODO: proportional font size, or light intensity,
                or some sort of thing to disable the flashing */
      if (life >= minBold)
        textFont(boldFont);
      else
        textFont(font);

      text(name, x, y);
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
        System.out.println("code_swarm is free software: you can redistribute it and/or modify");
        System.out.println("it under the terms of the GNU General Public License as published by");
        System.out.println("the Free Software Foundation, either version 3 of the License, or");
        System.out.println("(at your option) any later version.");
        cfg = new CodeSwarmConfig(args[0]);
        PApplet.main(new String[] { "code_swarm" });
      } else {
        System.err.println("Specify a config file.");
      }
    } catch (IOException e) {
      System.err.println("Failed due to exception: " + e.getMessage());
    }
  }
}
