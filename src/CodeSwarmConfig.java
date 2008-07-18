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

public class CodeSwarmConfig
{
  public static final String WIDTH_KEY = "Width";
  public static final String HEIGHT_KEY = "Height";
  public static final String INPUT_FILE_KEY = "InputFile";
  public static final String MSEC_PER_FRAME_KEY = "MillisecondsPerFrame";
  public static final String FRAMES_PER_DAY_KEY = "FramesPerDay";
  public static final String TAKE_SNAPSHOTS_KEY = "TakeSnapshots";
  public static final String NAME_HALOS_KEY = "NameHalos";
  public static final String BACKGROUND_KEY = "Background";
  public static final String COLOR_ASSIGN_KEY  =  "ColorAssign";
  public static final String SNAPSHOT_LOCATION_KEY = "SnapshotLocation";
  public static final String EDGE_LENGTH_KEY = "EdgeLength";
  public static final String SPRITE_FILE_KEY = "ParticleSpriteFile";
  public static final String EDGE_LIFE_KEY = "EdgeLife";
  public static final String FILE_LIFE_KEY = "FileLife";
  public static final String PERSON_LIFE_KEY = "PersonLife";
  public static final String USE_OPEN_GL = "UseOpenGL";
  public static final String SHOW_LEGEND = "ShowLegend";
  public static final String SHOW_HISTORY = "ShowHistory";
  public static final String SHOW_DATE = "ShowDate";
  public static final String SHOW_EDGES = "ShowEdges";
  public static final String SHOW_DEBUG = "ShowDebug";
  public static final String DRAW_NAMES_SHARP = "DrawNamesSharp";
  public static final String DRAW_NAMES_HALOS = "DrawNamesHalos";
  public static final String DRAW_FILES_SHARP = "DrawFilesSharp";
  public static final String DRAW_FILES_FUZZY = "DrawFilesFuzzy";
  public static final String DRAW_FILES_JELLY = "DrawFilesJelly";
  public static final String PHYSICAL_ENGINE_SELECTION = "PhysicalEngineSelection";
  public static String DEFAULT_COLOR_ASSIGN = "\"Misc\",\".*\",128,128,128,128,128,128";

  private Properties p = null;

  // Cache variables
  private Color _background = null;
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
  /*def.setProperty( MSEC_PER_FRAME_KEY, "21600000"); default is forbiden because it is the only way to specify a FRAMES_PER_DAY_KEY */
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
  
  public Color getBackground()
  {
    if ( _background == null )
      _background = stringToColor( p.getProperty(BACKGROUND_KEY) );
    return _background;
  }

  public boolean getBooleanProperty(String key, boolean defValue)
  {
    return Boolean.valueOf(p.getProperty(key, String.valueOf(defValue))).booleanValue();
  }

  public String getStringProperty( String key )
  {
    return p.getProperty( key );
  }

  public String getStringProperty(String key, String defValue)
  {
    return p.getProperty(key, defValue);
  }

  public int getIntProperty( String key )
  {
    return Integer.parseInt( p.getProperty(key) );
  }
  
  public int getIntProperty( String key, int defValue )
  {
    return Integer.parseInt( p.getProperty(key, String.valueOf(defValue)) );
  }
  
  public long getLongProperty( String key )
  {
    return Long.parseLong( p.getProperty(key) );
  }
  
  public long getLongProperty( String key, long defValue )
  {
    return Long.parseLong( p.getProperty(key, String.valueOf(defValue)) );
  }
  
  
  public String getColorAssignProperty( Integer index )
  {
    return p.getProperty( COLOR_ASSIGN_KEY + index.toString() );
  }

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
