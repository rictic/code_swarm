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
	public static final String TAKE_SNAPSHOTS_KEY = "TakeSnapshots";
	public static final String NAME_HALOS_KEY = "NameHalos";
	public static final String BACKGROUND_KEY = "Background";
	public static final String COLOR_ASSIGN_KEY [] = { "ColorAssign1",
	                                                   "ColorAssign2",
	                                                   "ColorAssign3",
	                                                   "ColorAssign4",
	                                                   "ColorAssign5",
	                                                   "ColorAssign6",
	                                                   "ColorAssign7" };
	public static String DEFAULT_COLOR_ASSIGN = "\".*\",128,128,128,128,128,128";

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
		def.setProperty( MSEC_PER_FRAME_KEY, "21600000");
		def.setProperty( BACKGROUND_KEY, "0,0,0" );
		def.setProperty( TAKE_SNAPSHOTS_KEY, "false");

		for( int i = 0; i < COLOR_ASSIGN_KEY.length; i++ )
		{
			def.setProperty( COLOR_ASSIGN_KEY[i], DEFAULT_COLOR_ASSIGN );
		}

		return def;
	}

	public int getWidth()
	{
		return Integer.valueOf(p.getProperty(WIDTH_KEY));
	}

	public int getHeight()
	{
		return Integer.valueOf(p.getProperty(HEIGHT_KEY));
	}

	public String getInputFile()
	{
		return p.getProperty(INPUT_FILE_KEY);
	}

	public long getMSecPerFrame()
	{
		return Long.valueOf(p.getProperty(MSEC_PER_FRAME_KEY));
	}

	public Color getBackground()
	{
		if ( _background == null )
			_background = stringToColor( p.getProperty(BACKGROUND_KEY) );
		return _background;
	}

	public boolean getTakeSnapshots()
	{
		return Boolean.valueOf(p.getProperty(TAKE_SNAPSHOTS_KEY));
	}

	public boolean getBooleanProperty(String key, boolean defValue)
	{
		return Boolean.valueOf(p.getProperty(key, String.valueOf(defValue)));
	}

	public String getStringProperty(String key, String defValue)
	{
		return p.getProperty(key, defValue);
	}

	public String getColorAssignProperty( int index )
	{
		return p.getProperty( COLOR_ASSIGN_KEY[index] );
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
				Enumeration en = config.p.propertyNames();
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
