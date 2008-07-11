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

//not used: import processing.core.PApplet;
import java.awt.Color;
import java.util.*;

class ColorAssigner
{
	ArrayList<ColorTest> tests;
	//int defaultColor = PApplet.color(128, 128, 128);
	int defaultColor = Color.gray.getRGB();

	public ColorAssigner()
	{
		tests = new ArrayList<ColorTest>();
	}

	public void addRule( String label, String expr, int c1, int c2 )
	{
		ColorTest t = new ColorTest();
		t.expr = java.util.regex.Pattern.compile( expr );
		t.label = label;
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
		for (ColorTest t : tests)
		{
			if (t.passes(s))
				return t.assign();
		}

		return defaultColor;
	}
}

