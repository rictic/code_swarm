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

abstract class Node
{
  float x, y;
  float dx, dy;
  boolean fixed;
  
  float life;
  
  Node()
  {
    x = random(width);
    y = random(height);
  }
  
  abstract void draw();

  void update()
  {
    if ( !fixed )
    {
      x += constrain( dx, -5, 5 );
      y += constrain( dy, -5, 5 );
      x = constrain( x, 0, width );
      y = constrain( y, 0, height );
    }
    dx /= 2;
    dy /= 2;
  }
  
  abstract void relax();
}
