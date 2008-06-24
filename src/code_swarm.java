/*
    Copyright (C) 2008 Michael Ogawa

    This file is part of code_swarm.

    code_swarm is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    code_swarm is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
*/

import processing.core.*;
import processing.xml.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.*;
import java.applet.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.sound.midi.*;
import javax.sound.midi.spi.*;
import javax.sound.sampled.*;
import javax.sound.sampled.spi.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;


public class code_swarm extends PApplet
{

// User-defined variables
int WIDTH = 640;
int HEIGHT = 480;
int FRAME_RATE = 24;
String INPUT_FILE = "../data/sample-repevents.xml";
String SPRITE_FILE = "particle.png";
String SCREENSHOT_FILE = "frames/swarm-#####.png";
long dateSkipper = 6 * 60 * 60 * 1000;  // period in ms
boolean takeSnapshots = false;

// Data storage
PriorityQueue eventsQueue; // MAC OSX: USE PROCESSING 0142 or higher
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
int currentColor;

// Formats the date string nicely
DateFormat formatter = DateFormat.getDateInstance();

/* Initialization */
public void setup()
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
  //loadRepository( INPUT_FILE );  // repository formatted
  loadRepEvents( INPUT_FILE );  // event formatted (this will be standard)
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
public void initColors()
{
  colorAssigner = new ColorAssigner();
  colorMode( HSB );
  
  //apacheColors();
  //pythonColors();
  //javaColors();
  eclipseColors();
}

public void apacheColors()
{
  colorAssigner.addRule( "/src.*", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( "/doc.*", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( "/mod.*|/contrib.*", color(25,255,255), color(40,255,255) );
}

public void pythonColors()
{
  colorAssigner.addRule( ".*\\.tex|.*\\.txt", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( ".*/Modules/.*", color(25,255,255), color(40,255,255) );
  colorAssigner.addRule( ".*\\.py|.*\\.c|.*\\.h", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( ".*/Doc/.*", color(150,255,255), color(170,255,255) );
}

public void javaColors()
{
  colorAssigner.addRule( ".*\\.java|.*/src/.*", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( ".*/docs/.*|.*/xdocs/.*", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( ".*/lib/.*", color(25,255,255), color(40,255,255) );
}

public void eclipseColors()
{
  colorAssigner.addRule( ".*\\.java|.*/src/.*", color(0,255,255), color(15,255,255) );
  colorAssigner.addRule( ".*/doc/.*|.*/xdocs/.*", color(150,255,255), color(170,255,255) );
  colorAssigner.addRule( ".*/lib/.*", color(25,255,255), color(40,255,255) );
  colorAssigner.addRule( ".*\\.gif|.*\\.jpg", color(120,255,255), color(135,255,255) );
}

/* Main loop */
public void draw()
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
public void drawPeopleNodesBlur()
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
public void drawPeopleNodesSharp()
{
  colorMode( RGB );
  for( int i = 0; i < people.size(); i++ )
  {
    PersonNode p = (PersonNode)people.get(i);
    fill( lerpColor(p.flavor, color(255), 0.5f), max( p.life-50, 0 ) );
    p.draw();
  }
}

/* Draw date in lower-right corner */
public void drawDate()
{
  fill( 255 );
  String dateText = formatter.format( prevDate );
  textAlign( RIGHT, BASELINE );
  textSize( 10 );
  text( dateText, width - 1, height - textDescent() ); 
}

/* Draw histogram in lower-left */
public void drawHistory()
{
  Iterator itr = history.iterator();
  int counter = 0;
  while( itr.hasNext() )
  {
    ColorBins cb = (ColorBins)itr.next();

    for( int i = 0; i < cb.num; i++ )
    {
      int c = cb.colorList[i];
      stroke( c, 200 );
      point( counter, height - i - 3 );
    }
    counter++;
  }
}

/* Show color codings */
public void drawLegend()
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
public void dumpFrame()
{
  if ( frameCount < 100000 )
    saveFrame( SCREENSHOT_FILE );
}

/* Update the particle positions */
public void update()
{
  // Create a new histogram line
  ColorBins cb = new ColorBins();
  history.add( cb );
  
  nextDate = new Date( prevDate.getTime() + dateSkipper );
  currentEvent = (FileEvent)eventsQueue.peek();
  
  while( currentEvent != null && currentEvent.date.before( nextDate ) )
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

public FileNode findNode( String name )
{
  for( int i = 0; i < nodes.size(); i++ )
  {
    FileNode n = (FileNode)nodes.get(i);
    if ( n.name.equals( name ) )
      return n;
  }
  return null;
}

public Edge findEdge( Node n1, Node n2 )
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

public PersonNode findPerson( String name )
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
public void loadRepository( String filename )
{
  XMLElement doc = new XMLElement( this, filename );
  loadRepository( doc, "", "" );
  doc = null;
}

/* Load repository-formatted file */
public void loadRepository( XMLElement xml, String path, String filename )
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
public void loadRepEvents( String filename1 )
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
public void loadSVNRepository( XMLElement doc )
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
    String path;
    if ( paths != null )
    {
      for( int j = 0; j < paths.getChildCount(); j++ )
      {
        path = paths.getChild(j).getContent();
        FileEvent evt = new FileEvent( date, author, "", path );
        eventsQueue.add( evt );
      }
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
public void keyPressed()
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
public void pauseButton()
{
  if ( looping )
    noLoop();
  else
    loop();
  looping = !looping;
}

class ColorAssigner
{
  ArrayList tests;
  int defaultColor = color(128, 128, 128);
  
  public ColorAssigner()
  {
    tests = new ArrayList();
  }
  
  public void addRule( String expr, int c1, int c2 )
  {
    ColorTest t = new ColorTest();
    t.expr = expr;
    t.c1 = c1;
    t.c2 = c2;
    addRule( t );
  }
  
  public void addRule( ColorTest t )
  {
    tests.add( t );
  }
  
  public int getColor( String s )
  {
    for( int i = 0; i < tests.size(); i++ )
    {
      ColorTest t = (ColorTest)tests.get(i);
      if ( t.passes( s ) )
        return t.assign();
    }
    
    return defaultColor;
  }
}

class ColorTest
{
  String expr;
  int c1, c2;
  
  public boolean passes( String s )
  {
    return s.matches( expr );
  }
  
  public int assign()
  {
    return lerpColor( c1, c2, random(1.0f) );
  }
}

class ColorBins
{
  int [] colorList;
  int num;
  
  ColorBins()
  {
    colorList = new int[2];
    num = 0;
  }
  
  public void add( int c )
  {
    if ( num >= colorList.length )
      colorList = expand( colorList );
      
    colorList[num] = c;
    num++;
  }
  
  public void sort()
  {
    colorList = PApplet.sort( colorList );
  }
}

class Edge
{
  Node from;
  Node to;
  float len;
  int life;
  
  Edge( Node from, Node to )
  {
    this.from = from;
    this.to = to;   
    this.len = 40; //25
    this.life = 255;
  }
  
  public void relax()
  {
    float vx = to.x - from.x;
    float vy = to.y - from.y;
    float d = mag( vx, vy );
    if ( d > 0 )
    {
      float f = (len - d) / (d * 3);
      f = f * map( life, 0, 255, 0, 1.0f );
      float dx = f * vx;
      float dy = f * vy;
      to.dx += dx;
      to.dy += dy;
      from.dx -= dx;
      from.dy -= dy;
    }
  }
 
  
  public void draw()
  {
    if ( life > 240 )
    {
      stroke( 255, life );
      strokeWeight( 0.35f );
      line( from.x, from.y, to.x, to.y );
    }
  }
  
  public void update()
  {
    decay();
  }
  
  public void decay()
  {
    life += -2;
    if ( life < 0 )
      life = 0;
  }
  
  public void freshen()
  {
    life = 255;
  }
}



class FileEvent implements Comparable
{
  Date date;
  String author;
  String filename;
  String path;
  int linesadded;
  int linesremoved;
  
  FileEvent( long datenum, String author, String path, String filename )
  {
    this( datenum, author, path, filename, 0, 0 );
  }
  
  FileEvent( long datenum, String author, 
             String path, String filename,
             int linesadded, int linesremoved )
  {
    this.date = new Date( (long)datenum );
    this.author = author;
    this.path = path;
    this.filename = filename;
    this.linesadded = linesadded;
    this.linesremoved = linesremoved;
  }
  
  public int compareTo( Object o )
  {
    return date.compareTo( ((FileEvent)o).date );
  }
}

class FileNode extends Node
{
  String name;
  int nodeHue;
  
  int touches;

  FileNode( FileEvent fe )
  {
    super();
    name = fe.path + fe.filename;
    fixed = false;
    life = 255;
    touches = 1;
    colorMode( RGB );
    
    nodeHue = colorAssigner.getColor( name );
  }

  public void update()
  {
    super.update();
    decay();
  }

  public void freshen()
  {
    life = 255;
    touches++;
  }

  public void decay()
  {
    life += -2.0f;
  }

  public void draw()
  {
    if ( life > 0.0f )
    {
      //drawSharp();
      drawFuzzy();
      //drawJelly();

      /*
      // label
       colorMode( RGB );
       fill( 0, life );
       textAlign( CENTER, CENTER );
       text( name, x, y );
       */
    }
  }

  public void drawSharp()
  {
    colorMode( RGB );
    fill( nodeHue, life );
    float w = 3;

    if ( life >= 244 )
    {
      stroke( 255, 128 );
      w *= 2;
    }  
    else
      noStroke();
    ellipseMode( CENTER );
    ellipse( x, y, w, w );
  }

  public void drawFuzzy()
  {
    tint( nodeHue, life );
    
    float w = 8 + sqrt( touches ) * 4;
    float dubw = w * 2;
    float halfw = w / 2;
    if ( life >= 244 )
    {
      colorMode( HSB );
      tint( hue( nodeHue ), saturation(nodeHue) - 192, 255, life );
      //image( sprite, x - w, y - w, dubw, dubw );
    }
    //else
      image( sprite, x - halfw, y - halfw, w, w );
  }
  
  public void drawJelly()
  {
    noFill();
    if ( life >= 244 )
      stroke( 255 );
    else
      stroke( nodeHue, life );
    float w = sqrt( touches );
    ellipseMode( CENTER );
    ellipse( x, y, w, w );
  }

  public void relax()
  {
    if ( life <= 0 )
      return;

    float ddx = 0;
    float ddy = 0;

    for( int j = 0; j < nodes.size(); j++ )
    {
      FileNode n = (FileNode)nodes.get(j);
      if ( n.life <= 0 )
        continue;

      if ( n != this )
      {
        float vx = x - n.x;
        float vy = y - n.y;
        float lensq = vx * vx + vy * vy;
        if ( lensq == 0 )
        {
          ddx += random(0.1f);
          ddy += random(0.1f);
        }
        else if ( lensq < 100 * 100 )
        {
          ddx += vx / lensq;
          ddy += vy / lensq;
        }
      }
    }
    float dlen = mag( ddx, ddy ) / 2;
    if ( dlen > 0 )
    {
      dx += ddx / dlen;
      dy += ddy / dlen;
    }
  }
}

abstract class Node
{
  float x, y;
  float dx, dy;
  boolean fixed;
  
  float life;
  
  Node()
  {
    x = random(width);
    y = random(height);
  }
  
  public abstract void draw();

  public void update()
  {
    if ( !fixed )
    {
      x += constrain( dx, -5, 5 );
      y += constrain( dy, -5, 5 );
      x = constrain( x, 0, width );
      y = constrain( y, 0, height );
    }
    dx /= 2;
    dy /= 2;
  }
  
  public abstract void relax();
}

class PersonNode extends Node
{
  String name;

  int flavor = color(0);
  int colorCount = 1;
  
  float mass = 10;
  float accel = 0.0f;

  PersonNode( String n )
  {
    super();
    name = n;
    fixed = false;
    life = 255;
  }

  public void update()
  {
    super.update();
    decay();
  }

  public void draw()
  {
    if ( life <= 0 )
      return;

    textAlign( CENTER, CENTER );

    if ( life >= 250 )
      textFont( boldFont );
    else
      textFont( font );

    text( name, x, y );
  }

  public void relax()
  {
    if ( life <= 0 )
      return;

    float fx = 0;
    float fy = 0;

    for( int j = 0; j < people.size(); j++ )
    {
      Node n = (Node)people.get(j);
      if ( n.life <= 0 )
        continue;

      if ( n != this )
      {
        float distx = x - n.x;
        float disty = y - n.y;
        float lensq = distx * distx + disty * disty;
        if ( lensq == 0 )
        {
          fx += random(0.01f);
          fy += random(0.01f);
        }
        else if ( lensq < 100 * 100 )
        {
          fx += distx / lensq;
          fy += disty / lensq;
        }
      }
    }
    float dlen = mag( fx, fy ) / 2;
    if ( dlen > 0 )
    {
      dx += fx / dlen;
      dy += fy / dlen;
    }

    dx /= 12;
    dy /= 12;
  }

  public void decay()
  {
    life -= 1;
    if ( life < 0 )
      life = 0;
  }

  public void freshen()
  {
    life = 255;
  }

  public void addColor( int c )
  {
    colorMode( RGB );
    flavor = lerpColor( flavor, c, 1.0f/colorCount );
    colorCount++;
  }
}

  static public void main(String args[])
  {
    PApplet.main( new String[] { "code_swarm" } );
  }
}
