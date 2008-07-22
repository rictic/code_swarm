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

import java.util.concurrent.CopyOnWriteArrayList;
import javax.vecmath.Vector2f;

/**
 * @brief Legacy algorithms describing all physicals interactions between nodes (files and persons)
 * 
 * This is only a rewriting of the initial code_swarm prototype.
 * 
 */
public class PhysicalEngineLegacy extends PhysicalEngine
{
  final private float FORCE_EDGE_MULTIPLIER;
  final private float FORCE_CALCULATION_RANDOMIZER;
  final private float FORCE_TO_SPEED_MULTIPLIER;
  final private float SPEED_TO_POSITION_MULTIPLIER;
  
  /**
   * Constructor for initializing parameters.
   */
  PhysicalEngineLegacy(float forceEdgeMultiplier, float forceCalculationRandomizer, float forceToSpeedMultiplier, float speedToPositionDrag)
  {
    FORCE_EDGE_MULTIPLIER         = forceEdgeMultiplier;
    FORCE_CALCULATION_RANDOMIZER  = forceCalculationRandomizer;
    FORCE_TO_SPEED_MULTIPLIER     = forceToSpeedMultiplier;
    SPEED_TO_POSITION_MULTIPLIER  = speedToPositionDrag;
  }
  
  /**
   * Legacy method that calculate the attractive/repulsive force between a person and one of its file along their link (the edge).
   * 
   * @param edge the link between a person and one of its file 
   * @return force force calculated between those two nodes
   */
  public Vector2f calculateForceAlongAnEdge( code_swarm.Edge edge )
  {
    float distance;
    float fakeForce;
    Vector2f force = new Vector2f();
    Vector2f tforce = new Vector2f();
    
    // distance calculation
    tforce.sub( edge.nodeTo.mPosition, edge.nodeFrom.mPosition);
    distance = tforce.length();
    if (distance > 0) {
      // fake force calculation (increase when distance is different from targeted len")
      fakeForce = (edge.getLen() - distance) / (distance * 3);
      // force ponderation using a re-mapping life from 0-255 scale to 0-1.0 range
      // This allows nodes to drift apart as their life decreases.
      fakeForce *= ((float)edge.life / edge.LIFE_INIT);
      // fake force projection onto x and y axis
      force.set(tforce);
      force.scale( fakeForce * FORCE_EDGE_MULTIPLIER );
    }
    
    return force;
  }
  
  /**
   * Legacy method that calculate the repulsive force between two similar nodes (either files or persons).
   * 
   * @param nodeA [in]
   * @param nodeB [in]
   * @return force force calculated between those two nodes
   */
  public Vector2f calculateForceBetweenNodes( code_swarm.Node nodeA, code_swarm.Node nodeB )
  {
    float lensq;
    Vector2f force = new Vector2f();
    Vector2f normVec = new Vector2f();
    
    /**
     * Get the distance between nodeA and nodeB
     */
    normVec.sub(nodeA.mPosition, nodeB.mPosition);
    lensq = normVec.lengthSquared();
    /**
     * If there is a Collision.  This is assuming a radius of zero.
     * if (lensq == (radius1 + radius2)) is what to use if we have radius 
     * could use touches for files and edge_length for people?
     */
    if (lensq == 0) {
      force.set( (float)Math.random()*FORCE_CALCULATION_RANDOMIZER, (float)Math.random()*FORCE_CALCULATION_RANDOMIZER );
    } else if (lensq < 10000) {
      /**
       * No collision
       */
      normVec.scale(1/lensq);
      force.set( normVec);
    }
    
    return force;
  }
  
  /**
   * Legacy method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node [in] Node the node to which the force apply
   * @param force [in] force a force Vector representing the force on a node
   * 
   * TODO: does force should be a property of the node (or not?)
   */
  public void applyForceTo( code_swarm.Node node, Vector2f force )
  {
    float dlen;
    Vector2f mod = new Vector2f();

    /** TODO: add comment to this algorithm */
    dlen = force.length();
    if ( (dlen > 0) && (node.mass > 0)) {
      mod.set(((force.x / (node.mass / dlen)) * FORCE_TO_SPEED_MULTIPLIER),
              ((force.y / (node.mass / dlen)) * FORCE_TO_SPEED_MULTIPLIER));
      node.mSpeed.add(mod);
    }
  }

  /**
   * Legacy method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node the node to which the force apply
    */
  public void applySpeedTo( code_swarm.Node node )
  {
    float div;
    // This block enforces a maximum absolute velocity.
    if (node.mSpeed.length() > node.maxSpeed) {
      Vector2f mag = new Vector2f(node.mSpeed.x / node.maxSpeed, node.mSpeed.y / node.maxSpeed);
      div = mag.length();
      node.mSpeed.scale( 1/div );
    }
    
    // This block convert Speed to Position
    node.mPosition.add(node.mSpeed);
    
    // Apply drag (reduce Speed for next frame calculation)
    node.mSpeed.scale( SPEED_TO_POSITION_MULTIPLIER );
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param nodeList the list of People
   * @param node the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  void onRelaxNode( CopyOnWriteArrayList<code_swarm.FileNode> nodeList, code_swarm.FileNode node ) {
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param nodeList the list of People
   * @param node the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  void onRelaxPerson( CopyOnWriteArrayList<code_swarm.PersonNode> nodeList, code_swarm.PersonNode node ) {
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param nodeList the list of People
   * @param node the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  void onUpdateNode( CopyOnWriteArrayList<code_swarm.FileNode> nodeList, code_swarm.FileNode node ) {
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param nodeList the list of People
   * @param node the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  void onUpdatePerson( CopyOnWriteArrayList<code_swarm.PersonNode> nodeList, code_swarm.PersonNode node ) {
  }
}

