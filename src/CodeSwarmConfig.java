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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author Michael Ogawa
 */
public class CodeSwarmConfig {
  /** Indexes into the Properties Object */
  /** The width of window */
  public static final String WIDTH_KEY = "Width";
  /** The height of window */
  public static final String HEIGHT_KEY = "Height";
  /** The maximum number of background threads */
  public static final String MAX_THREADS_KEY = "MaxThreads";
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
  public static final String FONT_COLOR_KEY = "FontColor";
  public static final String FONT_KEY = "Font";
  public static final String FONT_SIZE = "FontSize";
  public static final String FONT_KEY_BOLD = "BoldFont";
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
  public static final String EDGE_DECREMENT_KEY = "EdgeDecrement";
  /** How long to keep files alive */
  public static final String FILE_DECREMENT_KEY = "FileDecrement";
  /** How long to keep person alive */
  public static final String PERSON_DECREMENT_KEY = "PersonDecrement";
  /** File Mass */
  public static final String FILE_MASS_KEY = "FileMass";
  /** Person Mass */
  public static final String PERSON_MASS_KEY = "PersonMass";
  /** How long to keep edges alive */
  public static final String EDGE_LIFE_KEY = "EdgeLife";
  /** How long to keep nodes alive */
  public static final String FILE_LIFE_KEY = "FileLife";
  /** How long to keep people alive */
  public static final String PERSON_LIFE_KEY = "PersonLife";
  /** Boolean value, controls using the OpenGL library (experimental) */
  public static final String USE_OPEN_GL = "UseOpenGL";
  /** Percentage of life to highlight */
  public static final String HIGHLIGHT_PCT_KEY = "HighlightPct";
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
  public static final String IS_INPUT_SORTED_KEY = "IsInputSorted";
  public static final String SHOW_USER_NAME_KEY = "ShowUserName";
  /** Default regex and rgb values used to match anything not caught in the config file */
  public static String DEFAULT_COLOR_ASSIGN = "\"Misc\",\".*\",128,128,128,128,128,128";
  public static final String DRAW_CIRCULAR_AVATARS = "CircularAvatars";

  private List<Properties> propStack;



  // Cache variables
  private Color _background = null;

  public CodeSwarmConfig(String configFileName) throws IOException {
    initPropStack();
    addPropertiesLayer(configFileName);
  }

  public CodeSwarmConfig(Iterable<String> configFileNames) throws IOException {
    initPropStack();
    for (String filename: configFileNames)
      if (new File(filename).exists())
        addPropertiesLayer(filename);
  }

  public void addPropertiesLayer(Properties props) {
    propStack.add(0,props);
  }
  public void addPropertiesLayer(String filename) throws IOException {
    Properties props = new Properties();
    props.load(new FileInputStream(filename));
    addPropertiesLayer(props);
  }

  protected void initPropStack() {
    propStack = new LinkedList<Properties>();
    propStack.add(createDefaults());
  }

  private Properties createDefaults() {
    Properties def = new Properties();
    def.setProperty( COLOR_ASSIGN_KEY + "1" , DEFAULT_COLOR_ASSIGN );
    return def;
  }


  /**
   *
   * @param key
   * @return Returns the first key found in the stack of config files.
   */
  public String getStringProperty( String key ) {
    for(Properties props: propStack)
      if (props.containsKey(key))
        return props.getProperty(key);
    return null;
  }

  public Color getColorProperty(String key) {
    return stringToColor( getStringProperty(key) );
  }


  /**
   * Specify the path to the Xml-input file containing the repository
   * entries.<br />
   * Further versions should not use input-file but an abstract view
   * of the repository-entries.
   * @see EventList
   * @param filePath the path to the Xml-input file.
   */
  public void setInputFile(String filePath){
    propStack.get(0).setProperty(INPUT_FILE_KEY, filePath);
  }


  public boolean getBooleanProperty(String key) {
    return Boolean.valueOf(getStringProperty(key));
  }

  /**
   *
   * @param key
   * @return value of property if found, 0 if not found.
   */
  public int getIntProperty( String key ) {
    return Integer.parseInt( getStringProperty(key) );
  }

  /**
   *
   * @param key
   * @param defValue
   * @return value of property if found.
   */
  public int getPositiveIntProperty(String key) {
    int value = getIntProperty(key);
    if (value < 0)
      throw new RuntimeException(key + " must be >0, found " + value);
    return value;
  }

  /**
   *
   * @param key
   * @param defValue
   * @return defValue if not found or found value isn't negative, Value of property if found.
   */
  public int getNegativeIntProperty( String key) {
    int value = getIntProperty(key);
    if (value > 0)
      throw new RuntimeException(key + " must be >0, found " + value);
    return value;
  }



  /**
   *
   * @param key
   * @return value of property if found, 0 if not found.
   */
  public long getLongProperty( String key ) {
    return Long.parseLong( getStringProperty(key) );
  }

  /**
   *
   * @param key
   * @return value of property if found, 0 if not found.
   */
  public float getFloatProperty( String key ) {
    return Float.parseFloat( getStringProperty(key) );
  }

  /**
   *
   * @param index
   * @return String containing the regex and rgb values used to colorcode nodes, null if not found
   */
  public String getColorAssignProperty( Integer index ) {
    return getStringProperty( COLOR_ASSIGN_KEY + index.toString() );
  }

  /**
   *
   * @param str
   * @return Color object constructed from values in str
   */
  protected static Color stringToColor( String str ){
    // assume format is "R,G,B"
    String [] tokens = str.split( "," );
    int [] values = new int[3];
    for( int i = 0; i < 3; i++ )
    {
      values[i] = Integer.parseInt( tokens[i] );
    }
    return new Color( values[0], values[1], values[2] );
  }

  public double getDoubleProperty(String key) {
    return Double.parseDouble(getStringProperty(key));
  }
}
