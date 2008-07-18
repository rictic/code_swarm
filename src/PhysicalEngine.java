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
 * Abstract class describing interface of any code_swarm physical engine.
 *
 * Need to be derived to define force calculation algorithms between Nodes
 */
abstract class PhysicalEngine
{
  /**
   * Method that calculate the force between to nodes.
   * 
   * @param NodeA
   * @param NodeB
   * @param force
   * 
   * @return a force Vector representing the force between to nodes
   */
  abstract void calculateForceBetween( code_swarm.Node NodeA, code_swarm.Node NodeB, Vector force );

  /**
   * Method that apply a force to a node, converting force to acceleration, that in turn modify speed.
   * 
   * @param Node the node to which the force apply
   * @param force a force Vector representing the force on a node
   * 
   * @Note Standard physics is "Speed Variation = Force x Mass / Duration" with a convention of "Duration=1" between to frames
   */
  abstract void applyForceTo( code_swarm.Node node, Vector force );

  /**
   * Method that manage speed conversion to position.
   * 
   * @param node the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  abstract void applySpeedTo( code_swarm.Node node );
}

