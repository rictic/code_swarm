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
import java.util.regex.*;
import processing.core.PApplet;
import processing.core.PConstants;

class ColorTest
{
	Pattern expr;
	String label;
	int c1, c2;

	public boolean passes( String s )
	{
		Matcher m = expr.matcher(s);
		return m.matches();
	}

	public int assign()
	{
		return PApplet.lerpColor( c1, c2, (float)Math.random(), PConstants.RGB );
	}

	public void loadProperty( String value )
	{
		String [] tokens;
		// should have the format "label", "regex", r1,g1,b1, r2,g2,b2
		// get the stuff in quotes first
		int firstQ = value.indexOf( '\"' );
		int lastQ = value.lastIndexOf( '\"' );
		String firstpart = value.substring( firstQ + 1, lastQ );
		tokens = firstpart.split( "\"" );
        label = tokens[0];
		if (tokens.length == 3) {
          expr = Pattern.compile( tokens[2] );
		} else {
          expr = Pattern.compile( tokens[0] );
		}
		// then the comma delimited colors
		String rest = value.substring( lastQ + 1 );
		tokens = rest.split( "," );
		int [] components = new int[6];

		int j = 0;
		for( int i = 0; i < tokens.length; i++ )
		{
			String tok = tokens[i].trim();
			if ( tok.length() > 0 )
			{
				components[j++] = Integer.parseInt( tok );
			}
		}
		c1 = new Color( components[0], components[1], components[2] ).getRGB();
		c2 = new Color( components[3], components[4], components[5] ).getRGB();
	}

	public static void main( String [] args )
	{
		ColorTest ct = new ColorTest();
		System.out.println( "input=" + args[0] );
		ct.loadProperty( args[0] );
		//System.out.println( "regex=" + ct.expr );
		System.out.println( "color1=" + Integer.toHexString(ct.c1) );
		System.out.println( "color2=" + Integer.toHexString(ct.c2) );
	}
}

