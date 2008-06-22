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

class ColorAssigner
{
  ArrayList tests;
  color defaultColor = color(128, 128, 128);
  
  public ColorAssigner()
  {
    tests = new ArrayList();
  }
  
  void addRule( String expr, color c1, color c2 )
  {
    ColorTest t = new ColorTest();
    t.expr = expr;
    t.c1 = c1;
    t.c2 = c2;
    addRule( t );
  }
  
  void addRule( ColorTest t )
  {
    tests.add( t );
  }
  
  color getColor( String s )
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
  color c1, c2;
  
  boolean passes( String s )
  {
    return s.matches( expr );
  }
  
  color assign()
  {
    return lerpColor( c1, c2, random(1.0) );
  }
}
