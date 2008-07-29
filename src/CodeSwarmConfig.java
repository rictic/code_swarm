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

import java.awt.Color;
import java.io.*;
import java.util.Properties;
import java.util.Enumeration;

/**
 * @author Michael Ogawa
 */
public class CodeSwarmConfig
{
  /** Indexes into the Properties Object */
  /** The width of window */
  public static final String WIDTH_KEY = "Width";
  /** The height of window */
  public static final String HEIGHT_KEY = "Height";
  /** The input file */
  public static final String INPUT_FILE_KEY = "InputFile";
  /** The amount of time between frames */
  public static final String MSEC_PER_FRAME_KEY = "MillisecondsPerFrame";
  /** The number of frames per day.  Used to calculate time between frames. Optional. */
  public static final String FRAMES_PER_DAY_KEY = "FramesPerDay";
  /** Boolean value, controls png creation */
  public static final String TAKE_SNAPSHOTS_KEY = "TakeSnapshots";
  /** R,G,B Determines the background color */
  public static final String BACKGROUND_KEY = "Background";
  /** R,G,B Determines the background color */
  public static final String FONT_KEY = "Font";
  /** R,G,B Determines the background color */
  public static final String FONT_SIZE = "FontSize";
  /** R,G,B Determines the background color */
  public static final String FONT_SIZE_BOLD = "BoldFontSize";
  /** Rules for color coding nodes */
  public static final String COLOR_ASSIGN_KEY  =  "ColorAssign";
  /** Location to save snapshots. TakeSnapshots must be true to use */
  public static final String SNAPSHOT_LOCATION_KEY = "SnapshotLocation";
  /** Length of edges */
  public static final String EDGE_LENGTH_KEY = "EdgeLength";
  /** Path to sprite file for nodes */
  public static final String SPRITE_FILE_KEY = "ParticleSpriteFile";
  /** How long to keep edges alive */
  public static final String EDGE_DECREMENT = "EdgeDecrement";
  /** How long to keep edges alive */
  public static final String FILE_DECREMENT = "FileDecrement";
  /** How long to keep edges alive */
  public static final String PERSON_DECREMENT = "PersonDecrement";
  /** How long to keep edges alive */
  public static final String DEFAULT_NODE_SPEED = "NodeSpeed";
  /** How long to keep edges alive */
  public static final String DEFAULT_FILE_SPEED = "FileSpeed";
  /** How long to keep edges alive */
  public static final String DEFAULT_PERSON_SPEED = "PersonSpeed";
  /** How long to keep edges alive */
  public static final String EDGE_LIFE_KEY = "EdgeLife";
  /** How long to keep nodes alive */
  public static final String FILE_LIFE_KEY = "FileLife";
  /** How long to keep people alive */
  public static final String PERSON_LIFE_KEY = "PersonLife";
  /** Boolean value, controls using the OpenGL library (experimental) */
  public static final String USE_OPEN_GL = "UseOpenGL";
  /** Boolean value, controls showing the Legend */
  public static final String SHOW_LEGEND = "ShowLegend";
  /** Boolean value, controls showing the Histogram */
  public static final String SHOW_HISTORY = "ShowHistory";
  /** Boolean value, controls showing the Date */
  public static final String SHOW_DATE = "ShowDate";
  /** Boolean value, controls showing edges between nodes and people */
  public static final String SHOW_EDGES = "ShowEdges";
  /** Boolean value, controls showing debug info */
  public static final String SHOW_DEBUG = "ShowDebug";
  /** Boolean value, controls drawing names */
  public static final String DRAW_NAMES_SHARP = "DrawNamesSharp";
  /** Boolean value, controls drawing halos around names */
  public static final String DRAW_NAMES_HALOS = "DrawNamesHalos";
  /** Boolean value, controls drawing files as a dot */
  public static final String DRAW_FILES_SHARP = "DrawFilesSharp";
  /** Boolean value, controls drawing files as a fuzzy blob */
  public static final String DRAW_FILES_FUZZY = "DrawFilesFuzzy";
  /** Boolean value, controls drawing files as an ellipse uses touches to determine size */
  public static final String DRAW_FILES_JELLY = "DrawFilesJelly";
  /** Controls which physics engine to use */
  public static final String PHYSICS_ENGINE_SELECTION = "PhysicsEngineSelection";
  /** Controls where the config files are for the Physical Engine */
  public static final String PHYSICS_ENGINE_CONF_DIR = "PhysicsEngineConfigDir";
  /** Default regex and rgb values used to match anything not caught in the config file */
  public static String DEFAULT_COLOR_ASSIGN = "\"Misc\",\".*\",128,128,128,128,128,128";

  private Properties p = null;

  // Cache variables
  private Color _background = null;
  /**
   * Constructor
   * @param configFileName passed from command line
   * @throws IOException
   */
  public CodeSwarmConfig(String configFileName) throws IOException
  {
    p = new Properties( this.createDefaults() );
    p.load( new FileInputStream(configFileName) );
  }

  private Properties createDefaults()
  {
    Properties def = new Properties();
    def.setProperty( WIDTH_KEY, "640" );
    def.setProperty( HEIGHT_KEY, "480");
    def.setProperty( INPUT_FILE_KEY, "data/sample-repevents.xml");
  /*
   * default is forbidden because it is the only way to specify a FRAMES_PER_DAY_KEY
   * def.setProperty( MSEC_PER_FRAME_KEY, "21600000");
   */
    def.setProperty( BACKGROUND_KEY, "0,0,0" );
    def.setProperty( TAKE_SNAPSHOTS_KEY, "false");
    def.setProperty( SNAPSHOT_LOCATION_KEY, "frames/snap-#####.png" );
    def.setProperty( EDGE_LENGTH_KEY, "25" );
    def.setProperty( SPRITE_FILE_KEY, "particle.png" );
    def.setProperty( PERSON_LIFE_KEY, "255" );
    def.setProperty( FILE_LIFE_KEY, "255" );
    def.setProperty( EDGE_LIFE_KEY, "255" );
    def.setProperty( COLOR_ASSIGN_KEY + "1" , DEFAULT_COLOR_ASSIGN );

    return def;
  }
  
  /**
   * 
   * @return Color
   */
  public Color getBackground()
  {
    if ( _background == null )
      _background = stringToColor( p.getProperty(BACKGROUND_KEY) );
    return _background;
  }

  /**
   * 
   * @param key
   * @param defValue
   * @return defValue if not found, value stored otherwise (true or false)
   */
  public boolean getBooleanProperty(String key, boolean defValue)
  {
    return Boolean.valueOf(p.getProperty(key, String.valueOf(defValue))).booleanValue();
  }

  /**
   * 
   * @param key
   * @return String containing value for property or null if not found.
   */
  public String getStringProperty( String key )
  {
    return p.getProperty( key );
  }

  /**
   * 
   * @param key
   * @param defValue
   * @return defValue if not found, Value of property if found.
   */
  public String getStringProperty(String key, String defValue)
  {
    return p.getProperty(key, defValue);
  }

  /**
   * 
   * @param key
   * @return value of property if found, 0 if not found.
   */
  public int getIntProperty( String key )
  {
    return Integer.parseInt( p.getProperty(key) );
  }
  
  /**
   * 
   * @param key
   * @param defValue
   * @return defValue if not found, Value of property if found.
   */
  public int getIntProperty( String key, int defValue )
  {
    return Integer.parseInt( p.getProperty(key, String.valueOf(defValue)) );
  }
  
  /**
   * 
   * @param key
   * @return value of property if found, 0 if not found.
   */
  public long getLongProperty( String key )
  {
    return Long.parseLong( p.getProperty(key) );
  }
  
  /**
   * 
   * @param key
   * @param defValue
   * @return defValue if not found, Value of property if found.
   */
  public long getLongProperty( String key, long defValue )
  {
    return Long.parseLong( p.getProperty(key, String.valueOf(defValue)) );
  }
  
  /**
   * 
   * @param key
   * @return value of property if found, 0 if not found.
   */
  public float getFloatProperty( String key )
  {
    return Float.parseFloat( p.getProperty(key) );
  }
  
  /**
   * 
   * @param key
   * @param defValue
   * @return defValue if not found, Value of property if found.
   */
  public float getFloatProperty( String key, float defValue )
  {
    return Float.parseFloat( p.getProperty(key, String.valueOf(defValue)) );
  }
  
  /**
   * 
   * @param index
   * @return String containing the regex and rgb values used to colorcode nodes, null if not found
   */
  public String getColorAssignProperty( Integer index )
  {
    return p.getProperty( COLOR_ASSIGN_KEY + index.toString() );
  }

  /**
   * 
   * @param str
   * @return Color object constructed from values in str
   */
  public static Color stringToColor( String str )
  {
    // assume format is "R,G,B"
    String [] tokens = str.split( "," );
    int [] values = new int[3];
    for( int i = 0; i < 3; i++ )
    {
      values[i] = Integer.parseInt( tokens[i] );
    }
    return new Color( values[0], values[1], values[2] );
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args)
  {
    if (args.length > 0)
    {
      CodeSwarmConfig config = null;
      try
      {
        config = new CodeSwarmConfig(args[0]);
        Enumeration<?> en = config.p.propertyNames();
        while ( en.hasMoreElements() )
        {
          String key = (String)en.nextElement();
          String value = config.p.getProperty( key );
          System.out.println( key + "=" + value );
        }
      }
      catch (IOException e)
      {
        System.err.println("Failed due to exception: " + e.getMessage());
      }
    }
    else
    {
      System.err.println("Requires config file.");
    }
  }
}
