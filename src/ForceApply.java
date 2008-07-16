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
 * Abstract base of any force application (ie. conversion to speed) in code_swarm.
 *
 * Need to be derived to define application of a type of forces between to Nodes
 * 
 * TODO: rename this to something like "ForceToSpeed", then the third class would be "SpeedToPosition"
 */
abstract class ForceApply
{
  /**
   * Method that apply a force to a node, converting acceleration to speed.
   * 
   * @param Node the node to which the force apply
   * @param force a forceVector representing the force on a node
   */
  abstract void applyForceTo( code_swarm.Node Node, Vector force );
}

