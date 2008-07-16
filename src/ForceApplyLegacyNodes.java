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
 * Legacy force application on nodes (either files or persons).
 */
public class ForceApplyLegacyNodes extends ForceApply
{
  final private int multiplier;
  
  /**
   * Constructor for initializing parameters.
   */
  ForceApplyLegacyNodes(int paramMultiplier)
  {
    multiplier = paramMultiplier;
  }
	  
  /**
   * Method that apply a force to a node.
   * 
   * @param Node the node to which the force apply
   * @param force a forceVector representing the force on a node
   */
  public void applyForceTo( code_swarm.Node Node, ForceVector force )
  {
    double dlen;

    /** @todo comment this algorithm */
    dlen = force.norm() / 2;
    if (dlen > 0) {
      Node.addDX( (float)( force.getX() / dlen ) );
      Node.addDY( (float)( force.getY() / dlen ) );
    }
    Node.mulDX( multiplier );
    Node.mulDY( multiplier );
  }
}

