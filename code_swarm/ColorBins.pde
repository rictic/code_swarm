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

class ColorBins
{
  color [] colorList;
  int num;
  
  ColorBins()
  {
    colorList = new color[2];
    num = 0;
  }
  
  void add( color c )
  {
    if ( num >= colorList.length )
      colorList = expand( colorList );
      
    colorList[num] = c;
    num++;
  }
  
  void sort()
  {
    colorList = PApplet.sort( colorList );
  }
}
