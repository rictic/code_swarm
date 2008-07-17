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
 * Legacy speed conversion to position on nodes (either files or persons).
 */
public class SpeedToPositionLegacyNodes extends SpeedToPosition
{
  final private float multiplier;
  
  /**
   * Constructor for initializing parameters.
   */
  SpeedToPositionLegacyNodes(float paramMultiplier)
  {
    multiplier = paramMultiplier;
  }
    
  /**
   * Method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node the node to which the force apply
    */
  public void applySpeedTo( code_swarm.Node node )
  {
    float div;
    // This block enforces a maximum absolute velocity.
    if (node.getSpeed() > node.maxSpeed) {
      Vector mag = new Vector(node.getDX() / node.maxSpeed, node.getDY() / node.maxSpeed);
      div = mag.norm();
      node.mulDX( 1/div );
      node.mulDY( 1/div );
    }
    
    // This block convert Speed to Position
    node.addX( node.getDX() );
    node.addY( node.getDY() );
    
    // Apply drag (reduce Speed for next frame calculation)
    node.mulDX( multiplier );
    node.mulDY( multiplier );
  }
}

