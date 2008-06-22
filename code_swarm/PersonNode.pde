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

class PersonNode extends Node
{
  String name;

  color flavor = color(0);
  int colorCount = 1;
  
  float mass = 10;
  float accel = 0.0;

  PersonNode( String n )
  {
    super();
    name = n;
    fixed = false;
    life = 255;
  }

  void update()
  {
    super.update();
    decay();
  }

  void draw()
  {
    if ( life <= 0 )
      return;

    textAlign( CENTER, CENTER );

    if ( life >= 250 )
      textFont( boldFont );
    else
      textFont( font );

    text( name, x, y );
  }

  void relax()
  {
    if ( life <= 0 )
      return;

    float fx = 0;
    float fy = 0;

    for( int j = 0; j < people.size(); j++ )
    {
      Node n = (Node)people.get(j);
      if ( n.life <= 0 )
        continue;

      if ( n != this )
      {
        float distx = x - n.x;
        float disty = y - n.y;
        float lensq = distx * distx + disty * disty;
        if ( lensq == 0 )
        {
          fx += random(0.01);
          fy += random(0.01);
        }
        else if ( lensq < 100 * 100 )
        {
          fx += distx / lensq;
          fy += disty / lensq;
        }
      }
    }
    float dlen = mag( fx, fy ) / 2;
    if ( dlen > 0 )
    {
      dx += fx / dlen;
      dy += fy / dlen;
    }

    dx /= 12;
    dy /= 12;
  }

  void decay()
  {
    life -= 1;
    if ( life < 0 )
      life = 0;
  }

  void freshen()
  {
    life = 255;
  }

  void addColor( color c )
  {
    colorMode( RGB );
    flavor = lerpColor( flavor, c, 1.0/colorCount );
    colorCount++;
  }
}
