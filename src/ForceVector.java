/**
 * Copyright 2008 code_swarm project team
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * A small object to manipulate multidimensional force vector (2D).
 *
 * Used by physical classes
 * 
 * @remark currently 2D, but one day, 3D would be great
 * 
 * TODO: certainly need to be renamed to "Vector"
 */
class ForceVector
{
  protected double x, y; 
  protected double norm; 
  
  ForceVector()
  {
	 x = 0;
	 y = 0;
  }
  /**
   * Constructor, init the vector and calculate its norm.
   * 
   * @param x x-axis component of the force
   * @param y y-axis component of the force
   */
  ForceVector(double x, double y)
  {
    this.x = x;
    this.x = x;
  }

  public void setX(double x)
  {
    this.x = x;
  }
  
  public void setY(double y)
  {
    this.y = y;
  }
  
  public void set(double x, double y)
  {
    this.x = x;
    this.y = y;
  }
  
  public void set(ForceVector force)
  {
    this.x = force.getX();
    this.y = force.getY();
  }
  
  public void add(double x, double y)
  {
    this.x += x;
    this.y += y;
  }
  
  public void add(ForceVector force)
  {
    this.x += force.getX();
    this.y += force.getY();
  }
  
  /**
   * @return xx x-axis component of the force.
   */
  public double getX()
  {
    return x;
  }

  /**
   * @return y-axis component of the force.
   */
  public double getY()
  {
    return y;
  }
  
  /**
   * @return calculated norm of the vector (ie. its length).
   */
  public double norm()
  {
    return Math.sqrt(x*x + y*y);
  }
  
  /**
   * multiply each component by the multiplier, recalculates the resulting norm.
   * 
   * @param multiplier
   * 
   * TODO: transform to an operator redefinition  
   */
  void multiply(double multiplier)
  {
	  x    *= multiplier;
	  y    *= multiplier;
	  norm *= multiplier;
  }
  
}

