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
 * Abstract base of any force calculation in code_swarm.
 *
 * Need to be derived to define calculation of a type of forces between to Nodes
 */
abstract class ForceCalc
{
  /**
   * Method that calculate the force between to nodes.
   * 
   * @param NodeA
   * @param NodeB
   * @param force
   * 
   * @return a forceVector representing the force between to nodes
   */
  abstract void calculateForceBetween( code_swarm.Node NodeA, code_swarm.Node NodeB, Vector force );
}

