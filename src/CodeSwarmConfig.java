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

import java.io.*;

public class CodeSwarmConfig
{
	public static String WIDTH_KEY = "Width";
	public static String HEIGHT_KEY = "Height";
	public static String INPUT_FILE_KEY = "InputFile";
	public static String MSEC_PER_FRAME_KEY = "MillisecondsPerFrame";
	public static String TAKE_SNAPSHOTS_KEY = "TakeSnapshots";

	public int width = 640;
	public int height = 480;
	public String inputFile = "data/sample-repevents.xml";
	public long msecPerFrame = 6 * 60 * 60 * 1000;
	public boolean takeSnapshots = false;

	public CodeSwarmConfig( String configFileName )
	{
		File file = new File( configFileName );

		try
		{
			BufferedReader reader = new BufferedReader( new FileReader(file) );
			while( reader.ready() )
			{
				String line = reader.readLine();
				line = line.trim();
				if ( line.charAt(0) != '#' )
				{
					int eqindex = line.indexOf( '=' );
					if ( eqindex > 0 )
					{
						String key = line.substring( 0, eqindex );
						String value = line.substring( eqindex+1, line.length() );

						if ( key.equals( INPUT_FILE_KEY ) )
							setInputFile( value );
						else if ( key.equals( WIDTH_KEY ) )
							setWidth( value );
						else if ( key.equals( HEIGHT_KEY ) )
							setHeight( value );
						else if ( key.equals( MSEC_PER_FRAME_KEY ) )
							setMsecPerFrame( value );
						else if ( key.equals( TAKE_SNAPSHOTS_KEY ) )
							setTakeSnapshots( value );
					}
				}
			}
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	public void setInputFile( String value )
	{
		inputFile = value;
	}

	public void setWidth( String value )
	{
		width = Integer.parseInt( value );
	}

	public void setHeight( String value )
	{
		height = Integer.parseInt( value );
	}

	public void setMsecPerFrame( String value )
	{
		msecPerFrame = Long.parseLong( value );
	}

	public void setTakeSnapshots( String value )
	{
		takeSnapshots = Boolean.parseBoolean( value );
	}

	public static void main( String [] args )
	{
		if ( args.length > 0 )
		{
			CodeSwarmConfig config = new CodeSwarmConfig( args[0] );
			System.out.println( CodeSwarmConfig.WIDTH_KEY + " = " + config.width );
			System.out.println( CodeSwarmConfig.HEIGHT_KEY + " = " + config.height );
			System.out.println( CodeSwarmConfig.INPUT_FILE_KEY + " = " + config.inputFile );
			System.out.println( CodeSwarmConfig.MSEC_PER_FRAME_KEY + " = " +
config.msecPerFrame );
			System.out.println( CodeSwarmConfig.TAKE_SNAPSHOTS_KEY + " = " +
config.takeSnapshots );
		}
		else
		{
			System.err.println( "Requires config file." );
		}
	}
}
