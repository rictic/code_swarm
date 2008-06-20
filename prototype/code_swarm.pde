//import processing.opengl.*;

import processing.xml.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.DateFormat;

// User-defined variables
int WIDTH = 640;
int HEIGHT = 480;
int FRAME_RATE = 30;
String INPUT_FILE = "postgres-repository.xml";
String SPRITE_FILE = "particle.png";
String SCREENSHOT_FILE = "frames/swarm-#####.png";
long dateSkipper = 6 * 60 * 60 * 1000;  // period in ms
boolean takeSnapshots = false;

// Data storage
PriorityQueue eventsQueue;
ArrayList nodes;
ArrayList edges;
ArrayList people;
LinkedList history;

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
boolean showHelp = false;

// Color mapper
ColorAssigner colorAssigner;
color currentColor;

// Formats the date string nicely
DateFormat formatter = DateFormat.getDateInstance();

/* Initialization */
void setup()
{
  size( WIDTH, HEIGHT );
  smooth();
  frameRate( FRAME_RATE );
  
  // init data structures
  eventsQueue = new PriorityQueue();
  nodes = new ArrayList();
  edges = new ArrayList();
  people = new ArrayList();
  history = new LinkedList();
  
  // Init color map
  initColors();
  
  // Load data
  loadRepository( INPUT_FILE );  // repository formatted
  //loadRepEvents( INPUT_FILE );  // event formatted
  // TODO: use adapter pattern to handle different data sources
  
  // Create fonts
  font = createFont( "SansSerif", 10 );
  boldFont = createFont( "SansSerif.bold", 14 );
  textFont( font );

  // Create the file particle image
  sprite = loadImage( SPRITE_FILE );
  // Add translucency (using itself in this case)
  sprite.mask( sprite );
  
  prevDate = ((FileEvent)eventsQueue.peek()).date;
}

/* Load a colormap */
/*   TODO: load from a file or GUI */
void initColors()
{
  colorAssigner = new ColorAssigner();
  colorMode( HSB );
  
  //apacheColors();
  //pythonColors();
  //javaColors();
  eclipseColors();
}

void apacheColors()
{
  colorAssigner.addRule( "/src.*", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( "/doc.*", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( "/mod.*|/contrib.*", color(25,255,255), color(40,255,255) );
}

void pythonColors()
{
  colorAssigner.addRule( ".*\\.tex|.*\\.txt", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( ".*/Modules/.*", color(25,255,255), color(40,255,255) );
  colorAssigner.addRule( ".*\\.py|.*\\.c|.*\\.h", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( ".*/Doc/.*", color(150,255,255), color(170,255,255) );
}

void javaColors()
{
  colorAssigner.addRule( ".*\\.java|.*/src/.*", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( ".*/docs/.*|.*/xdocs/.*", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( ".*/lib/.*", color(25,255,255), color(40,255,255) );
}

void eclipseColors()
{
  colorAssigner.addRule( ".*\\.java|.*/src/.*", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( ".*/doc/.*|.*/xdocs/.*", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( ".*/lib/.*", color(25,255,255), color(40,255,255) );
  colorAssigner.addRule( ".*\\.gif|.*\\.jpg", color(120,255,255), color(135,255,255) );
}

/* Main loop */
void draw()
{
  background( 0 );  // clear screen w/ black
  
  this.update();  // update state to next frame

  // Draw edges (for debugging only)
  //for( int i = 0; i < edges.size(); i++ )
  //  ((Edge)edges.get(i)).draw();

  // Surround names with aura
  drawPeopleNodesBlur();
  
  // Draw file particles
  for( int i = 0; i < nodes.size(); i++ )
    ((FileNode)nodes.get(i)).draw();

  // Draw names
  drawPeopleNodesSharp();

  textFont( font );

  if ( showHistogram )
    drawHistory();
  
  if ( showLegend )
    drawLegend();

  if ( showDate )
    drawDate();

  if ( takeSnapshots )
    dumpFrame();
  
  // Stop animation when we run out of data
  if ( eventsQueue.isEmpty() )
    noLoop();
}

/* Surround names with aura */
void drawPeopleNodesBlur()
{
  colorMode( HSB );
  // First draw the name
  for( int i = 0; i < people.size(); i++ )
  {
    PersonNode p = (PersonNode)people.get(i);
    fill( hue(p.flavor), 64, 255, p.life );
    p.draw();
  }

  // Then blur it
  filter( BLUR, 3 );
}

/* Draw person's name */
void drawPeopleNodesSharp()
{
  colorMode( RGB );
  for( int i = 0; i < people.size(); i++ )
  {
    PersonNode p = (PersonNode)people.get(i);
    fill( lerpColor(p.flavor, color(255), 0.5), max( p.life-50, 0 ) );
    p.draw();
  }
}

/* Draw date in lower-right corner */
void drawDate()
{
  fill( 255 );
  String dateText = formatter.format( prevDate );
  textAlign( RIGHT, BASELINE );
  textSize( 10 );
  text( dateText, width - 1, height - textDescent() ); 
}

/* Draw histogram in lower-left */
void drawHistory()
{
  Iterator itr = history.iterator();
  int counter = 0;
  while( itr.hasNext() )
  {
    ColorBins cb = (ColorBins)itr.next();

    for( int i = 0; i < cb.num; i++ )
    {
      color c = cb.colorList[i];
      stroke( c, 200 );
      point( counter, height - i - 3 );
    }
    counter++;
  }
}

/* Show color codings */
void drawLegend()
{
  noStroke();
  textFont( font );
  textAlign( LEFT, TOP );
  fill( 255, 200 );
  text( "Legend:", 0, 0 );
  for( int i = 0; i < colorAssigner.tests.size(); i++ )
  {
    ColorTest t = (ColorTest)colorAssigner.tests.get(i);
    fill( t.c1, 200 );
    text( t.expr, 10, (i+1) * 10 );
  }
}

/* Take screenshot */
void dumpFrame()
{
  if ( frameCount < 100000 )
    saveFrame( SCREENSHOT_FILE );
}

/* Update the particle positions */
void update()
{
  // Create a new histogram line
  ColorBins cb = new ColorBins();
  history.add( cb );
  
  nextDate = new Date( prevDate.getTime() + dateSkipper );
  currentEvent = (FileEvent)eventsQueue.peek();
  
  while( currentEvent.date.before( nextDate ) )
  {
    currentEvent = (FileEvent)eventsQueue.poll();
    FileNode n = findNode( currentEvent.path + currentEvent.filename );
    if ( n == null )
    {
      n = new FileNode( currentEvent );
      nodes.add( n );
    }
    else
    {
      n.freshen();
    }

    // add to color bin
    cb.add( n.nodeHue );

    PersonNode p = findPerson( currentEvent.author );
    if ( p == null )
    {
      p = new PersonNode(currentEvent.author);
      people.add( p );
    }
    else
    {
      p.freshen();
    }
    p.addColor( n.nodeHue );

    Edge ped = findEdge( n, p );
    if ( ped == null )
    {
      ped = new Edge( n, p );
      edges.add( ped );
    }
    else
      ped.freshen();

    /*
    if ( currentEvent.date.equals( prevDate ) )
     {
     Edge e = findEdge( n, prevNode );
     if ( e == null )
     {
     e = new Edge( n, prevNode );
     edges.add( e );
     }
     else
     {
     e.freshen();
     }
     }
     */

    //prevDate = currentEvent.date;
    prevNode = n;
    currentEvent = (FileEvent)eventsQueue.peek();
  }
  
  prevDate = nextDate;

  // sort colorbins
  cb.sort();

  // restrict history to drawable area
  while ( history.size() > 320 )
    history.remove();

  for( int i = 0; i < edges.size(); i++ )
    ((Edge)edges.get(i)).relax();

  for( int i = 0; i < nodes.size(); i++ )
    ((FileNode)nodes.get(i)).relax();

  for( int i = 0; i < people.size(); i++ )
    ((PersonNode)people.get(i)).relax();

  for ( int i = 0; i < edges.size(); i++ )
    ((Edge)edges.get(i)).update();

  for( int i = 0; i < nodes.size(); i++ )
    ((FileNode)nodes.get(i)).update();

  for( int i = 0; i < people.size(); i++ )
    ((PersonNode)people.get(i)).update();
}

FileNode findNode( String name )
{
  for( int i = 0; i < nodes.size(); i++ )
  {
    FileNode n = (FileNode)nodes.get(i);
    if ( n.name.equals( name ) )
      return n;
  }
  return null;
}

Edge findEdge( Node n1, Node n2 )
{
  for ( int i = 0; i < edges.size(); i++ )
  {
    Edge e = (Edge)edges.get(i);
    if ( e.from == n1 && e.to == n2 )
      return e;
    if ( e.from == n2 && e.to == n1 )
      return e;
  }
  return null;
}

PersonNode findPerson( String name )
{
  for( int i = 0; i < people.size(); i++ )
  {
    PersonNode p = (PersonNode)people.get(i);
    if ( p.name.equals( name ) )
      return p; 
  }
  return null;
}

/* Head function for loadRecurse */
void loadRepository( String filename )
{
  XMLElement doc = new XMLElement( this, filename );
  loadRepository( doc, "", "" );
  doc = null;
}

/* Load repository-formatted file */
void loadRepository( XMLElement xml, String path, String filename )
{
  String tag = xml.getName();
  if ( tag.equals( "event" ) )
  {
    String datestr = xml.getStringAttribute( "date" );
    long date = Long.parseLong( datestr );
    String author = xml.getStringAttribute( "author" );
    int linesadded = xml.getIntAttribute( "linesadded" );
    int linesremoved = xml.getIntAttribute( "linesremoved" );
    FileEvent evt = new FileEvent( date, author, path, filename, linesadded, linesremoved );
    eventsQueue.add( evt );
  }
  else if ( tag.equals( "file" ) )
  {
    String name = xml.getStringAttribute( "name" );
    for( int i = 0; i < xml.getChildCount(); i++ )
      loadRepository( xml.getChild(i), path, name );
  }
  else if ( tag.equals( "directory" ) )
  {
    String name = xml.getStringAttribute( "name" );
    for( int i = 0; i < xml.getChildCount(); i++ )
      loadRepository( xml.getChild(i), path + name, "" );
  }
  else if ( tag.equals( "log" ) )
  {
    // The tag 'log' represents the xml output of a SVN log, which
    // requires slightly different commit event parsing.
    loadSVNRepository( xml );
  }
}

/* Load event-formatted file */
void loadRepEvents( String filename1 )
{
  XMLElement doc = new XMLElement( this, filename1 );
  for( int i = 0; i < doc.getChildCount(); i++ )
  {
    XMLElement xml = doc.getChild(i);
    String filename = xml.getStringAttribute( "filename" );
    String datestr = xml.getStringAttribute( "date" );
    long date = Long.parseLong( datestr );
    String author = xml.getStringAttribute( "author" );
    //int linesadded = xml.getIntAttribute( "linesadded" );
    //int linesremoved = xml.getIntAttribute( "linesremoved" );
    FileEvent evt = new FileEvent( date, author, "", filename );
    eventsQueue.add( evt );
  }
}

/* Load SVN log formatted file */
void loadSVNRepository( XMLElement doc )
{
  // Iterate over commit nodes.
  for( int i = 0; i < doc.getChildCount(); i++ )
  {
    XMLElement xml = doc.getChild(i);
    
    // In the SVN xml log, timestamps have the following format:
    // 2008-06-19T03:46:04.335538Z
    // The date and the time need to be parsed from this so that we can convert it to a 'long'
    // with the sql.Timestamp class.
    String datestr = xml.getChild("date").getContent();
    String[] datesplit = datestr.split("\\.");
    datesplit = datesplit[0].split("T");
    datestr = datesplit[0] + " " + datesplit[1];
    Timestamp ts = Timestamp.valueOf(datestr);
    long date = ts.getTime();
    
    // When the SVN repository is created, there is no author associated with the commit, so for
    // now just log it as anonymous.
    // FIXME: Should we ignored events with no author completely or log them a different way?
    XMLElement author_node = xml.getChild("author");
    String author = "anonymous";
    if ( author_node != null )
      author = author_node.getContent();
      
    // Under each commit there is a child node named 'paths', which will have it's own
    // 'path' children which contain the paths of the files which were modified and details
    // about the change(file modified, deleted, created, etc...)
    XMLElement paths = xml.getChild("paths");
    for( int j = 0; j < paths.getChildCount(); j++ )
    {
      FileEvent evt = new FileEvent( date, author, "", paths.getChild(j).getContent());
      eventsQueue.add( evt );
    }
  }
}

/* Output file events for debugging
void printQueue()
{
  while( eventsQueue.size() > 0 )
  {
    FileEvent fe = (FileEvent)eventsQueue.poll();
    println( fe.date );
  }
}
*/

/* Keystroke callback function */
void keyPressed()
{
  switch (key)
  {
    case ' ':
      pauseButton();
      break;
    case 'h':
      showHistogram = !showHistogram;
      break;
    case 'd':
      showDate = !showDate;
      break;
    case 'l':
      showLegend = !showLegend;
      break;
    case '?':
      showHelp = !showHelp; // not implemented yet
      break;
  }
}

/* Toggle pause */
void pauseButton()
{
  if ( looping )
    noLoop();
  else
    loop();
  looping = !looping;
}
