/*
	 Copyright 2008 Michael Ogawa

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
	 along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.xml.XMLElement;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;


public class code_swarm extends PApplet
{

	// User-defined variables
	CodeSwarmConfig config;
	int FRAME_RATE = 24;
	String SPRITE_FILE = "particle.png";
	String SCREENSHOT_FILE;

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
	boolean showHelp = false;

	// Color mapper
	ColorAssigner colorAssigner;
	int currentColor;

	// Edge Len
	private int EDGE_LEN				= 25;
	// Drawable object life decrement
	private final int EDGE_LIFE_INIT			= 255;
	private final int FILE_LIFE_INIT			= 255;
	private final int PERSON_LIFE_INIT		= 255;
	private final int EDGE_LIFE_DECREMENT		= -2;
	private final int FILE_LIFE_DECREMENT		= -2;
	private final int PERSON_LIFE_DECREMENT	= -1;

	// Formats the date string nicely
	DateFormat formatter = DateFormat.getDateInstance();

	private static CodeSwarmConfig cfg;
	private long lastDrawDuration = 0;
	private boolean loading = true;
	private String loadingMessage = "Reading input file";

	/* Initialization */
	public void setup()
	{
		if (cfg.getBooleanProperty("UseOpenGL", false)) {
			size( cfg.getWidth(), cfg.getHeight(), OPENGL);
		} else {
			size( cfg.getWidth(), cfg.getHeight());            
		}

		smooth();
		frameRate( FRAME_RATE );

		// init data structures
		eventsQueue = new PriorityBlockingQueue<FileEvent>();
		nodes = new CopyOnWriteArrayList<FileNode>();
		edges = new CopyOnWriteArrayList<Edge>();
		people = new CopyOnWriteArrayList<PersonNode>();
		history = new LinkedList<ColorBins>();

		// Init color map
		initColors();

		// Load data
		//loadRepository( INPUT_FILE );  // repository formatted
		Thread t = new Thread(new Runnable() {
				public void run() {
				loadRepEvents( cfg.getInputFile() );  // event formatted (this will be standard)
				prevDate = eventsQueue.peek().date;
				}
				});
		t.setDaemon(true);
		t.start();
		//! @todo TODO: use adapter pattern to handle different data sources

		SCREENSHOT_FILE = cfg.getStringProperty( CodeSwarmConfig.SNAPSHOT_LOCATION_KEY );
		EDGE_LEN = cfg.getIntProperty( CodeSwarmConfig.EDGE_LENGTH_KEY );

		// Create fonts
		font = createFont( "SansSerif", 10 );
		boldFont = createFont( "SansSerif.bold", 14 );
		textFont( font );

		String SPRITE_FILE = cfg.getStringProperty( CodeSwarmConfig.SPRITE_FILE_KEY );
		// Create the file particle image
		sprite = loadImage( SPRITE_FILE );
		// Add translucency (using itself in this case)
		sprite.mask( sprite );

	}

	/* Load a colormap */
	public void initColors()
	{
		colorAssigner = new ColorAssigner();

		for( int i = 0; i < 7; i++ )
		{
			ColorTest ct = new ColorTest();
			String property = cfg.getColorAssignProperty( i );
			ct.loadProperty( property );
			colorAssigner.addRule( ct );
		}
	}

	/* DEPRECATED, kept for reference */
	public void apacheColors()
	{
		colorAssigner.addRule( "/src.*", color(0,255,255), color(15,255,255) );
		colorAssigner.addRule( "/doc.*", color(150,255,255), color(170,255,255) );
		colorAssigner.addRule( "/mod.*|/contrib.*", color(25,255,255), color(40,255,255) );
	}

	/* DEPRECATED, kept for reference */
	public void pythonColors()
	{
		colorAssigner.addRule( ".*\\.tex|.*\\.txt", color(150,255,255), color(170,255,255) );
		colorAssigner.addRule( ".*/Modules/.*", color(25,255,255), color(40,255,255) );
		colorAssigner.addRule( ".*\\.py|.*\\.c|.*\\.h", color(0,255,255), color(15,255,255) );
		colorAssigner.addRule( ".*/Doc/.*", color(150,255,255), color(170,255,255) );
	}

	/* DEPRECATED, kept for reference */
	public void javaColors()
	{
		colorAssigner.addRule( ".*\\.java|.*/src/.*", color(0,255,255), color(15,255,255) );
		colorAssigner.addRule( ".*/docs/.*|.*/xdocs/.*", color(150,255,255), color(170,255,255) );
		colorAssigner.addRule( ".*/lib/.*", color(25,255,255), color(40,255,255) );
	}

	/* DEPRECATED, kept for reference */
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
		long start = System.currentTimeMillis();
		background( cfg.getBackground().getRGB() );  // clear screen with background color

		if (loading) {
			drawLoading();
		}
		else {
			this.update();  // update state to next frame
			// Draw edges (for debugging only)
			//for (Edge edge : edges)
			//    edge.draw();

			// Surround names with aura
			drawPeopleNodesBlur();

			// Draw file particles
			for (FileNode node : nodes)
				node.draw();

			// Draw names
			drawPeopleNodesSharp();

			textFont(font);

			if (cfg.getBooleanProperty("debug", false))
				drawDebugData();

			if (showHistogram)
				drawHistory();

			if (showLegend)
				drawLegend();

			if (showDate)
				drawDate();

			if (cfg.getTakeSnapshots())
				dumpFrame();

			// Stop animation when we run out of data
			if (eventsQueue.isEmpty())
				noLoop();
		}
		long end = System.currentTimeMillis();
		lastDrawDuration = end - start;
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
		if (cfg.getBooleanProperty("NameHalos", true))
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
		Iterator<ColorBins> itr = history.iterator();
		int counter = 0;
		while( itr.hasNext() )
		{
			ColorBins cb = itr.next();

			for( int i = 0; i < cb.num; i++ )
			{
				int c = cb.colorList[i];
				stroke( c, 200 );                
				point( counter, height - i - 3 );
			}
			counter++;
		}
	}

	public void drawLoading()
	{
		noStroke();
		textFont(font, 20);
		textAlign( LEFT, TOP );
		fill( 255, 200 );
		text(loadingMessage, 0, 0);
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
			ColorTest t = colorAssigner.tests.get(i);
			fill( t.c1, 200 );
			text( t.expr, 10, (i+1) * 10 );
		}
	}

	public void drawDebugData()
	{
		noStroke();
		textFont( font );
		textAlign( LEFT, TOP );
		fill( 255, 200 );
		text( "Nodes: " + nodes.size(), 0, 0 );
		text( "People: " + people.size(), 0, 10 );
		text( "Queue: " + eventsQueue.size(), 0, 20 );
		text( "Last render time: " + lastDrawDuration, 0, 30);
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

		nextDate = new Date( prevDate.getTime() + cfg.getMSecPerFrame() );
		currentEvent = eventsQueue.peek();

		while( currentEvent != null && currentEvent.date.before( nextDate ) )
		{
			currentEvent = eventsQueue.poll();
			if (currentEvent == null) return;

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
			currentEvent = eventsQueue.peek();
		}

		prevDate = nextDate;


		// sort colorbins
		cb.sort();

		// restrict history to drawable area
		while ( history.size() > 320 )
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

	public FileNode findNode( String name )
	{
		for (FileNode node : nodes)
		{
			if (node.name.equals(name))
				return node;
		}
		return null;
	}

	public Edge findEdge( Node n1, Node n2 )
	{
		for (Edge edge : edges)
		{
			if (edge.from == n1 && edge.to == n2)
				return edge;
			if (edge.from == n2 && edge.to == n1)
				return edge;
		}
		return null;
	}

	public PersonNode findPerson( String name )
	{
		for (PersonNode p : people)
		{
			if (p.name.equals(name))
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
			if (eventsQueue.size() % 100 == 0)
				loadingMessage = "Creating events: " + eventsQueue.size();
		}
		loading = false;
	}

	/* Load SVN log formatted file */
	/* DEPRECATED */
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
			//! @todo FIXME: Should we ignored events with no author completely or log them a different way?
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
				showHelp = !showHelp; //! @todo not implemented yet
				break;
			case 'q':
				exit();
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

	/**
	 * @brief Describe an event on a file
	*/
	class FileEvent implements Comparable
	{
		Date 	date;
		String 	author;
		String 	filename;
		String 	path;
		int 	linesadded;
		int 	linesremoved;

		/**
		 * @brief short constructor with base data
		*/
		FileEvent( long datenum, String author, String path, String filename )
		{
			this( datenum, author, path, filename, 0, 0 );
		}

		/**
		 * @brief constructor with number of modified lines
		*/
		FileEvent( 	long datenum, String author, 
					String path, String filename,
					int linesadded, int linesremoved )
		{
			this.date 			= new Date( datenum );
			this.author 		= author;
			this.path 			= path;
			this.filename 		= filename;
			this.linesadded 	= linesadded;
			this.linesremoved 	= linesremoved;
		}

		/**
		 * @brief Comparing two events by date (Not Used)
		*/
		public int compareTo( Object o )
		{
			return date.compareTo( ((FileEvent)o).date );
		}
	}

	/** 
	 * @brief Abstract base class for all drawable objects
	 *
	 * List the features common to all drawable objects
	 * Edge and Node, FileNode and PersonNode
	*/
	abstract class Drawable
	{
		public int life;
		
		//! @tode those are variables used for physic calculation, but they need more appropriate names
		float dx, dy;
		float distx, disty;

		final public int LIFE_DECREMENT;
		
		// 1) constructor(s)
		//! @todo This is in preparation for the (hypotetic) displacement of update() and decay()
		Drawable( int lifeDecrement )
		{
			LIFE_DECREMENT = lifeDecrement;
		}
		
		public abstract void relax();		// 2) calculating next frame state
		public	abstract void update()	;	// 3) applying next frame state
		public abstract void decay();		// 4) shortening life
		public abstract void draw();		// 5) drawing the new state
		public abstract void freshen();	// 6) reseting life as if new
	}

	/** 
	 * @brief An Edge link two nodes together : a File to a Person
	*/
	class Edge extends Drawable
	{
		Node	from;
		Node	to;
		float	len;

		/**
		 * 1) constructor
		*/
		Edge( Node from, Node to )
		{
			super(EDGE_LIFE_DECREMENT);	// -2
			this.from	= from;
			this.to		= to;   
			this.len	= EDGE_LEN;		//  25
			this.life	= EDGE_LIFE_INIT; // 255
		}

		/**
		 * 2) calculating next frame state
		*/
		public void relax()
		{
			distx = to.x - from.x;
			disty = to.y - from.y;
			float d = mag( distx, disty );
			if ( d > 0 )
			{
				float f = (len - d) / (d * 3);
				f = f * map( life, 0, 255, 0, 1.0f );
				dx = f * distx;
				dy = f * disty;
				
				//! @todo : remove direct access to attributes, use setters instead
				to.dx += dx;
				to.dy += dy;
				from.dx -= dx;
				from.dy -= dy;
			}
		}

		/**
		 * 3) applying next frame state
		 *
		 *  @todo shouln'd it be moved directly in Drawable ?
		*/
		public void update()
		{
			//super.update(); // @todo would be more homogeneous (then update() could be in Drawable)
			decay();
		}

		/**
		 * 4) shortening life
		 *
		 *  @todo shouln'd it be moved directly in Drawable ?
		*/
		public void decay()
		{
			life += LIFE_DECREMENT;
			if ( life < 0 ) {
				life = 0;
			}
		}

		/**
		 * 5) drawing the new state
		*/
		public void draw()
		{
			if ( life > 240 )
			{
				stroke( 255, life );
				strokeWeight( 0.35f );
				line( from.x, from.y, to.x, to.y );
			}
		}

		/**
		 * 6) reseting life as if new
		*/
		public void freshen()
		{
			life = 255;
		}
	}

	/** 
	 * @brief A node is an abstraction for a File or a Person
	 *
	 * @todo Should not be called "abstract" if there is code inside...
	*/
	abstract class Node extends Drawable
	{
		String name;
		float x, y;

		//! @tode those are variables used for physic calculation, but they need more appropriate names
		float ddx, ddy;
		float lensq, dlen;

		boolean fixed;
		protected float maxSpeed = 7.0f;
        
		/**
		 * 1) constructor
		*/
		Node( int lifeDecrement )
		{
			super(lifeDecrement);
			x = random(width);
			y = random(height);
		}

		/**
		 * 3) applying next frame state
		*/
		public void update()
		{
			if ( !fixed )
			{
				if ( mag(dx, dy) > maxSpeed )
				{
					float div = mag(dx/maxSpeed, dy/maxSpeed);
					dx = dx/div;
					dy = dy/div;
				}
				x += dx;
				y += dy;
				x = constrain( x, 0, width );
				y = constrain( y, 0, height );
			}
			dx /= 2;
			dy /= 2;
		}
	}


	/** 
	 * @brief A node describing a file, which is repulsed by other files
	*/
	class FileNode extends Node
	{
		int nodeHue;
		int touches;

		/**
		 * getting file node as a string
		*/
		public String toString()
		{
			return "FileNode{" + "name='" + name + '\'' + ", nodeHue=" + nodeHue + ", touches=" + touches + '}';
		}

		/**
		 * 1) constructor
		*/
		FileNode( FileEvent fe )
		{
			super(FILE_LIFE_DECREMENT); // -2
			name = fe.path + fe.filename;
			fixed = false;
			life = FILE_LIFE_INIT;
			touches = 1;
			colorMode( RGB );

			nodeHue = colorAssigner.getColor( name );
		}

		/**
		 * 2) calculating next frame state
		 *
		 * @todo this physic job should be uniformed between file a person nodes => then it could be moved up
		*/
		public void relax()
		{
			if ( life <= 0 )
				return;

			ddx = 0;
			ddy = 0;

			for( int j = 0; j < nodes.size(); j++ )
			{
				FileNode n = (FileNode)nodes.get(j);
				if ( n.life <= 0 )
					continue;

				if ( n != this )
				{
					distx = x - n.x;
					disty = y - n.y;
					lensq = distx * distx + disty * disty;
					if ( lensq == 0 )
					{
						ddx += random(0.1f);
						ddy += random(0.1f);
					}
					else if ( lensq < 100 * 100 )
					{
						ddx += distx / lensq;
						ddy += disty / lensq;
					}
				}
			}
			dlen = mag( ddx, ddy ) / 2;
			if ( dlen > 0 )
			{                                                        
				dx += ddx / dlen;
				dy += ddy / dlen;
			}
		}
		
		/**
		 * 3) applying next frame state
		 *
		 *  @todo shouln'd it be moved direcly in Node ?
		*/
		public void update()
		{
			super.update();
			decay();
		}

		/**
		 * 4) shortening life
		 *
		 *  @todo shouln'd it be moved directly in Drawable ?
		*/
		public void decay()
		{
			life += LIFE_DECREMENT;
			if (life <= 0) {
				life = 0;
			}
		}

		/**
		 * 5) drawing the new state
		*/
		public void draw()
		{
			if ( life > 0 )
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

		/**
		 * 6) reseting life as if new
		*/
		public void freshen()
		{
			life = 255;
			touches++;
		}
	}

	/** 
	 * @brief A node describing a person, which is repulsed by other persons
	*/
	class PersonNode extends Node
	{
		int flavor = color(0);
		int colorCount = 1;

		float mass = 10;
		float accel = 0.0f;

		/**
		 * 1) constructor
		*/
		PersonNode( String n )
		{
			super(PERSON_LIFE_DECREMENT); // -1
			maxSpeed = 2.0f;
			name = n;
			fixed = false;
			life = PERSON_LIFE_INIT;
		}

		/**
		 * 2) calculating next frame state
		 *
		 * @todo this physic job should be uniformed between file a person nodes => then it could be moved up
		*/
		public void relax()
		{
			if ( life <= 0 )
				return;

			ddx = 0;
			ddy = 0;

			for( int j = 0; j < people.size(); j++ )
			{
				Node n = (Node)people.get(j);
				if ( n.life <= 0 )
					continue;

				if ( n != this )
				{
					distx = x - n.x;
					disty = y - n.y;
					lensq = distx * distx + disty * disty;
					if ( lensq == 0 )
					{
						ddx += random(0.01f);
						ddy += random(0.01f);
					}
					else if ( lensq < 100 * 100 )
					{
						ddx += distx / lensq;
						ddy += disty / lensq;
					}
				}
			}
			dlen = mag( ddx, ddy ) / 2;
			if ( dlen > 0 )
			{
				dx += ddx / dlen;
				dy += ddy / dlen;
			}

			dx /= 12;
			dy /= 12;
		}

		/**
		 * 3) applying next frame state
		 *
		 *  @todo shouln'd it be moved direcly in Node ?
		*/
		public void update()
		{
			super.update();
			decay();
		}

		/**
		 * 4) shortening life
		 *
		 *  @todo shouln'd it be moved directly in Drawable ?
		*/
		public void decay()
		{
			life += LIFE_DECREMENT;
			if ( life < 0 )
				life = 0;
		}

		/**
		 * 5) drawing the new state
		*/
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

		/**
		 * 6) reseting life as if new
		 *
		 *  @todo shouln'd it be moved direcly in Node ? (with a "touches++" stuff like in FileNode)
		*/
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



	/**
	 * code_swarm Entry point
	*/
	static public void main(String args[])
	{
		try
		{
			if (args.length > 0)
			{
				System.out.println("code_swarm is free software: you can redistribute it and/or modify");
				System.out.println("it under the terms of the GNU General Public License as published by");
				System.out.println("the Free Software Foundation, either version 3 of the License, or");
				System.out.println("(at your option) any later version.");
				cfg = new CodeSwarmConfig(args[0]);
				PApplet.main(new String[]{"code_swarm"});
			}
			else
			{
				System.err.println("Specify a config file.");
			}
		}
		catch (IOException e)
		{
			System.err.println("Failed due to exception: " + e.getMessage());
		}
	}
}
