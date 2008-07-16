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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Definition of the code_swarm Application.
 */
public class code_swarm extends PApplet {

  public static final long serialVersionUID = 0;
  // User-defined variables
  CodeSwarmConfig config;
  int FRAME_RATE = 24;
  long UPDATE_DELTA = 0;
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

  // Graphics objects
  PFont font;
  PFont boldFont;
  PImage sprite;

  // Graphics state variables
  boolean looping = true;
  boolean showHistogram = true;
  boolean showDate = true;
  boolean showLegend = false;
  boolean showEdges = false;
  boolean showHelp = false;
  boolean takeSnapshots = false;
  boolean showDebug = false;
  boolean drawNameHalos = false;

  // Color mapper
  ColorAssigner colorAssigner;
  int currentColor;

  // Edge Len
  private int EDGE_LEN = 25;
  // Drawable object life decrement
  private int EDGE_LIFE_INIT = 255;
  private int FILE_LIFE_INIT = 255;
  private int PERSON_LIFE_INIT = 255;
  private final int EDGE_LIFE_DECREMENT = -2;
  private final int FILE_LIFE_DECREMENT = -2;
  private final int PERSON_LIFE_DECREMENT = -1;

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
    
    /** @todo to place in the CodeSwarmConfig class ? */
    if (cfg.getBooleanProperty("UseOpenGL", false)) {
      size(width, height, OPENGL);
    } else {
      size(width, height);
    }
    
    if (cfg.getBooleanProperty("ShowLegend", false)) {
      showLegend = true;
    } else {
      showLegend = false;
    }

    if (cfg.getBooleanProperty("ShowHistory", false)) {
      showHistogram = true;
    } else {
      showHistogram = false;
    }
    
    if (cfg.getBooleanProperty("ShowDate", false)) {
      showDate = true;
    } else {
      showDate = false;
    }
    
    if (cfg.getBooleanProperty("ShowEdges", false)) {
      showEdges = true;
    } else {
      showEdges = false;
    }
    
    if (cfg.getBooleanProperty("debug", false)) {
      showDebug = true;
    } else {
      showDebug = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.TAKE_SNAPSHOTS_KEY,false)) {
      takeSnapshots = true;
    } else {
      takeSnapshots = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAME_HALOS, true)) {
      drawNameHalos = true;
    } else {
      drawNameHalos = false;
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
    
    UPDATE_DELTA = cfg.getIntProperty(CodeSwarmConfig.MSEC_PER_FRAME_KEY, -1);
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
    
    smooth();
    frameRate(FRAME_RATE);

    // init data structures
    eventsQueue = new PriorityBlockingQueue<FileEvent>();
    nodes = new CopyOnWriteArrayList<FileNode>();
    edges = new CopyOnWriteArrayList<Edge>();
    people = new CopyOnWriteArrayList<PersonNode>();
    history = new LinkedList<ColorBins>();

    // Init color map
    initColors();

    // Load data
    // loadRepository( INPUT_FILE ); // repository formatted
    Thread t = new Thread(new Runnable() {
      public void run() {
        loadRepEvents(cfg.getStringProperty(CodeSwarmConfig.INPUT_FILE_KEY)); // event formatted (this will be standard)
        prevDate = eventsQueue.peek().date;
      }
    });
    t.setDaemon(true);
    t.start();
    /** @todo TODO: use adapter pattern to handle different data sources */

    SCREENSHOT_FILE = cfg.getStringProperty(CodeSwarmConfig.SNAPSHOT_LOCATION_KEY);
    EDGE_LEN = cfg.getIntProperty(CodeSwarmConfig.EDGE_LENGTH_KEY);
    if (EDGE_LEN <= 0) {
      EDGE_LEN = 25;
    }

    // Create fonts
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
          for (Edge edge : edges)
            edge.draw();
      }

      // Surround names with aura
      drawPeopleNodesBlur();

      // Draw file particles
      for (FileNode node : nodes)
        node.draw();

      // Draw names
      drawPeopleNodesSharp();

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

      if (showHistogram)
        drawHistory();

      if (showDate)
        drawDate();

      if (takeSnapshots)
        dumpFrame();

      // Stop animation when we run out of data
      if (eventsQueue.isEmpty())
        exit();
        //noLoop();
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
    if (drawNameHalos)
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
    text("- h : show Histogram", 0, 10*line++);
    text("- d : show Date", 0, 10*line++);
    text("- l : show Legend", 0, 10*line++);
    text("- e : show Edges", 0, 10*line++);
    text("- b : show deBug", 0, 10*line++);
    text("- ? : show help", 0, 10*line++);
    text("- q : Quit code_swarm", 0, 10*line++);
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

  public FileNode findNode(String name) {
    for (FileNode node : nodes) {
      if (node.name.equals(name))
        return node;
    }
    return null;
  }

  public Edge findEdge(Node n1, Node n2) {
    for (Edge edge : edges) {
      if (edge.from == n1 && edge.to == n2)
        return edge;
      // Shouldn't need this.
      // if (edge.from == n2 && edge.to == n1)
      // return edge;
    }
    return null;
  }

  public PersonNode findPerson(String name) {
    for (PersonNode p : people) {
      if (p.name.equals(name))
        return p;
    }
    return null;
  }

  /**
   *  Head function for loadRecurse
   * @deprecated
   */
  public void loadRepository(String filename) {
    XMLElement doc = new XMLElement(this, filename);
    loadRepository(doc, "", "");
    doc = null;
  }

  /**
   *  Load repository-formatted file
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
   *  Load event-formatted file
   */
  public void loadRepEvents(String filename1) {
    XMLElement doc = new XMLElement(this, filename1);
    for (int i = 0; i < doc.getChildCount(); i++) {
      XMLElement xml = doc.getChild(i);
      String filename = xml.getStringAttribute("filename");
      String datestr = xml.getStringAttribute("date");
      long date = Long.parseLong(datestr);
      String author = xml.getStringAttribute("author");
      // int linesadded = xml.getIntAttribute( "linesadded" );
      // int linesremoved = xml.getIntAttribute( "linesremoved" );
      FileEvent evt = new FileEvent(date, author, "", filename);
      eventsQueue.add(evt);
      if (eventsQueue.size() % 100 == 0)
        loadingMessage = "Creating events: " + eventsQueue.size();
    }
    loading = false;
    // reset the Frame Counter.
    frameCount = 0;
  }

  /**
   *  Load SVN log formatted file
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
      /** @todo FIXME: Should we ignored events with no author completely or log them
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
   * Keystroke callback function
   */
  public void keyPressed() {
    switch (key) {
      case ' ': {
        pauseButton();
        break;
      }
      case 'h': {
        showHistogram = !showHistogram;
        break;
      }
      case 'd': {
        showDate = !showDate;
        break;
      }
      case 'l': {
        showLegend = !showLegend;
        break;
      }
      case 'e' : {
        showEdges = !showEdges;
        break;
      }
      case 'b': {
        showDebug = !showDebug;
        break;
      }
      case '?': {
        showHelp = !showHelp;
        break;
      }
      case 'q': {
        exit();
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
    public int touches;

    /** @tode those are variables used for physic calculation, but they need more
              appropriate names, or to be put down to the "Node" class */
    float dx, dy;
    float distx, disty;

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
      touches        = 1;
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
      touches++;
    }

    // getters and setters

    public float getDX() {
      return dx;
    }

    public float getDY() {
      return dy;
    }

    public void addDX(float amt) {
      dx += amt;
    }

    public void addDY(float amt) {
      dy += amt;
    }

    public void mulDX(float amt) {
      dx *= amt;
    }

    public void mulDY(float amt) {
      dy *= amt;
    }
  }

  /**
   * An Edge link two nodes together : a File to a Person.
   */
  class Edge extends Drawable {
    Node   from;
    Node   to;
    float len;

    /**
     * 1) constructor.
     */
    Edge(Node from, Node to) {
      super(EDGE_LIFE_INIT, EDGE_LIFE_DECREMENT); // 255, -2
      this.from = from;
      this.to   = to;
      this.len  = EDGE_LEN;  // 25
    }

    /**
     * 2) calculating next frame state.
     */
    public void relax() {
      float distance;
      float fakeForce;
      // distance calculation
      distx = to.getX() - from.getX();
      disty = to.getY() - from.getY();
      distance = mag(distx, disty);
      if (distance > 0) {
        // fake force calculation (increase when distance is different from targeted len")
        fakeForce = (len - distance) / (distance * 3);
        fakeForce = fakeForce * map(life, 0, 255, 0, 1.0f);
        // fake force projection onto x and y axis
        dx = fakeForce * distx;
        dy = fakeForce * disty;

        // transmit (applying) fake force projection to file and person nodes
        /** @todo use (or permit to use) real forces, not only delta position (ie speed) modification */
        to.addDX(dx); // Person
        to.addDY(dy); // Person
        from.addDX(-dx); // File
        from.addDY(-dy); // File
      }
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 240) {
        stroke(255, life);
        strokeWeight(0.35f);
        line(from.x, from.y, to.x, to.y);
      }
    }

  }

  /**
   * A node is an abstraction for a File or a Person.
   */
  public abstract class Node extends Drawable {
    String name;
    /** @todo We SHOULD use vector for position, speed and accel, not using x and y everywhere */
    float x, y;
    /** @todo Not Used : need to be implemented in physics */
    float mass = 10;
    float accel = 0.0f;

    boolean fixed;
    /** @todo add config */
    protected float maxSpeed = 7.0f;

    
    
    /**
     * 1) constructor.
     */
    Node(int lifeInit, int lifeDecrement) {
      super(lifeInit, lifeDecrement);
      /** @todo implement new sort of (random or not) arrival, "configurables"
                => to permit things like "injection points", circular arrival, and so on */
      x = random(width);
      y = random(height);
    }

    /**
     * 3) applying next frame state.
     *
     * This is a surdefinition of the Drawable update() method
     */
    public void update() {
      if (!fixed) {
        // This block enforces a maximum absolute velocity.
        if (mag(dx, dy) > maxSpeed) {
          float div = mag(dx / maxSpeed, dy / maxSpeed);
          dx = dx / div;
          dy = dy / div;
        }

        x += dx;
        y += dy;
        x = constrain(x, 0, width);
        y = constrain(y, 0, height);
      }
      // Apply drag
      dx /= 2;
      dy /= 2;

      // shortening life
      decay();
    }

    public float getX() {
      return x;
    }

    public float getY() {
      return y;
    }
  }

  /**
   * A node describing a file, which is repulsed by other files.
   */
  class FileNode extends Node {
    int nodeHue;
    int minBold;

    // The force calculation/applications algorithms used
    /** @todo use config to select the good one */
    ForceCalcLegacyNodes  ForceCalcBetweenFiles  = new ForceCalcLegacyNodes(0.01f); // default random
    ForceApplyLegacyNodes ForceApplyBetweenFiles = new ForceApplyLegacyNodes(1);    // force divider

    /**
     * getting file node as a string
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
      fixed = false;
      life = LIFE_INIT;
      colorMode(RGB);
      minBold = (int)(LIFE_INIT * 0.95);

      nodeHue = colorAssigner.getColor(name);
    }

    /**
     * 2) calculating next frame state.
     * 
     * @todo this physic job should be uniformed between file a person nodes
     *       => then it could be moved up
     */
    public void relax() {
      ForceVector forceSummation = new ForceVector();
      
      if (life <= 0)
        return;

      // Calculation of repulsive force between persons
      for (int j = 0; j < nodes.size(); j++) {
        FileNode n = (FileNode) nodes.get(j);
        if (n.life <= 0)
          continue;

        if (n != this) {
          ForceVector forceBetween2Files = new ForceVector();
          ForceCalcBetweenFiles.calculateForceBetween(this, n, forceBetween2Files);
          forceSummation.add(forceBetween2Files);
        }
      }

      // Apply repulsive force from other files to this Node
      ForceApplyBetweenFiles.applyForceTo(this, forceSummation);
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 0) {
        /** @todo This should be in the config. Should allow a combination.
         *        Sharp and Jelly looks cool.
         *  @todo We should use class and derivation to enable multi-behavioral drawing like multi-physics
         */
        drawSharp();
        // drawFuzzy();
        drawJelly();

        /*
         * // label colorMode( RGB ); fill( 0, life ); textAlign( CENTER, CENTER );
         * text( name, x, y );
         */
      }
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

    
    // The force calculation/applications algorthims used
    /** @todo use config to select the good one */
    ForceCalcLegacyNodes  ForceCalcBetweenPersons  = new ForceCalcLegacyNodes(0.01f);  // default random
    ForceApplyLegacyNodes ForceApplyBetweenPersons = new ForceApplyLegacyNodes(12);    // force divider

    /**
     * 1) constructor.
     */
    PersonNode(String n) {
      super(PERSON_LIFE_INIT, PERSON_LIFE_DECREMENT); // -1
      maxSpeed = 2.0f;
      name = n;
      fixed = false;
      /** @todo add config */
      minBold = (int)(LIFE_INIT * 0.95);
    }

    /**
     * 2) calculating next frame state.
     * 
     * @todo this physic job should be uniformed between file a person nodes => then
     *       it could be moved up
     */
    public void relax() {
      ForceVector forceSummation = new ForceVector();

      if (life <= 0)
        return;

      // Calculation of repulsive force between persons
      for (int j = 0; j < people.size(); j++) {
        Node n = (Node) people.get(j);
        if (n.life <= 0)
          continue;

        if (n != this) {
          ForceVector forceBetween2Persons = new ForceVector();
          ForceCalcBetweenPersons.calculateForceBetween(this, n, forceBetween2Persons);
          forceSummation.add(forceBetween2Persons);
        }
      }
      
      // Apply repulsive force from other persons to this Node
      ForceApplyBetweenPersons.applyForceTo(this, forceSummation);
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life <= 0)
        return;

      textAlign(CENTER, CENTER);

      /** @todo proportional font size, or light intensity,
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
