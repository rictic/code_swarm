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

import java.util.Properties;
import javax.vecmath.Vector2f;

/**
 * @brief Simple algorithms describing all physicals interactions between nodes (files and persons)
 * 
 * This is a free test to explore dans demonstrate PhysicalEngine designs.
 * 
 * @see PhysicsEngine Physics Engine Interface
 */
public class PhysicsEngineSimple implements PhysicsEngine
{
  private Properties cfg;
  
  private float FORCE_EDGE_MULTIPLIER;
  private float FORCE_NODES_MULTIPLIER;
  private float FORCE_TO_SPEED_MULTIPLIER;
  private float SPEED_TO_POSITION_MULTIPLIER;
  
  /**
   * Method for initializing parameters.
   * @param p Properties from the config file.
   */
  //PhysicalEngineSimple(float forceEdgeMultiplier, float forceToSpeedMultiplier, float speedToPositionDrag)
  public void setup (java.util.Properties p)
  {
    cfg = p;
    FORCE_EDGE_MULTIPLIER = Float.parseFloat(cfg.getProperty("edgeMultiplier","1.0"));
    FORCE_NODES_MULTIPLIER = Float.parseFloat(cfg.getProperty("nodesMultiplier","1.0"));
    FORCE_TO_SPEED_MULTIPLIER = Float.parseFloat(cfg.getProperty("speedMultiplier","1.0"));
    SPEED_TO_POSITION_MULTIPLIER = Float.parseFloat(cfg.getProperty("drag","0.5"));
  }
  
  /**
   * Simple method that calculate the attractive/repulsive force between a person and one of its file along their link (the edge).
   * 
   * @param edge the link between a person and one of its file 
   * @return force force calculated between those two nodes
   */
  public Vector2f calculateForceAlongAnEdge( code_swarm.Edge edge )
  {
    float distance;
    float deltaDistance;
    Vector2f force = new Vector2f();
    Vector2f tforce = new Vector2f();
    
    // distance calculation
    tforce.sub( edge.nodeTo.mPosition, edge.nodeFrom.mPosition);
    distance = tforce.length();
    // force calculation (increase when distance is different from targeted len)
    deltaDistance = (edge.len - distance);
    // force projection onto x and y axis
    tforce.scale( deltaDistance * FORCE_EDGE_MULTIPLIER );
    force.set(tforce);
    
    return force;
  }
  
  /**
   * Simple method that calculate the repulsive force between two similar nodes (either files or persons).
   * 
   * @param nodeA [in]
   * @param nodeB [in]
   * @return force force calculated between those two nodes
   */
  public Vector2f calculateForceBetweenNodes( code_swarm.Node nodeA, code_swarm.Node nodeB )
  {
    float distance;
    Vector2f force = new Vector2f();
    Vector2f normVec = new Vector2f();
    
    /**
     * Get the distance between nodeA and nodeB
     */
    normVec.sub(nodeA.mPosition, nodeB.mPosition);
    distance = normVec.length();
    if (distance > 0) {
      // No collision
      normVec.scale(1/distance * FORCE_NODES_MULTIPLIER);
      force.set(normVec);
    }
    
    return force;
  }
  
  /**
   * Simple method that apply a force to a node, converting acceleration to speed.
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
   * Simple method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node the node to which the force apply
    */
  public void applySpeedTo( code_swarm.Node node )
  {
    float div;
    // This block enforces a maximum absolute velocity.
    // TODO : I want to remove all this
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
   * Method that allows Physics Engine to modify forces between files and people during the relax stage
   * 
   * @param edge the edge to which the force apply (both ends)
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxEdge(code_swarm.Edge edge) {
    
    if (edge.life <= 0) {
      return;
    }
    
    Vector2f force    = new Vector2f();

    // Calculate force between the node "from" and the node "to"
    force = calculateForceAlongAnEdge(edge);

    // transmit (applying) fake force projection to file and person nodes
    applyForceTo(edge.nodeTo, force);
    force.negate(); // force is inverted for the other end of the edge
    applyForceTo(edge.nodeFrom, force);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxNode(code_swarm.FileNode fNode ) {
    
    if (fNode.life <= 0) {
      return;
    }
    
    Vector2f forceBetweenFiles = new Vector2f();
    Vector2f forceSummation    = new Vector2f();
      
    // Calculation of repulsive force between persons
    for (int j = 0; j < code_swarm.nodes.size(); j++) {
      code_swarm.FileNode n = (code_swarm.FileNode) code_swarm.nodes.get(j);
      if (n.life <= 0)
        continue;

      if (n != fNode) {
        // elemental force calculation, and summation
        forceBetweenFiles = calculateForceBetweenNodes(fNode, n);
        forceSummation.add(forceBetweenFiles);
      }
    }
    // Apply repulsive force from other files to this Node
    applyForceTo(fNode, forceSummation);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxPerson(code_swarm.PersonNode pNode) {

    if (pNode.life <= 0) {
      return;
    }
    
    Vector2f forceBetweenPersons = new Vector2f();
    Vector2f forceSummation      = new Vector2f();

    // Calculation of repulsive force between persons
    for (int j = 0; j < code_swarm.people.size(); j++) {
      code_swarm.Node n = (code_swarm.Node) code_swarm.people.get(j);
      if (n.life <= 0)
        continue;

      if (n != pNode) {
        // elemental force calculation, and summation
        forceBetweenPersons = calculateForceBetweenNodes(pNode, n);
        forceSummation.add(forceBetweenPersons);
      }
    }
    
    // Apply repulsive force from other persons to this Node
    applyForceTo(pNode, forceSummation);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param edge the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdateEdge(code_swarm.Edge edge) {
    if (edge.life <= 0) {
      return;
    }
    edge.decay();
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdateNode(code_swarm.FileNode fNode) {
    if (fNode.life <= 0) {
      return;
    }
    // Apply Speed to Position on nodes
    applySpeedTo(fNode);
    
    // ensure coherent resulting position
    fNode.mPosition.set(constrain(fNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(fNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    // shortening life
    fNode.decay();
  }
  
  private float constrain(float value, float min, float max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    }
    
    return value;
  }

  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdatePerson(code_swarm.PersonNode pNode) {
    if (pNode.life <= 0) {
      return;
    }
    // Apply Speed to Position on nodes
    applySpeedTo(pNode);
    
    // ensure coherent resulting position
    pNode.mPosition.set(constrain(pNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(pNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    // shortening life
    pNode.decay();
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting location for a Person Node
   */
  public Vector2f pStartLocation() {
    Vector2f vec = new Vector2f(code_swarm.width*(float)Math.random(), code_swarm.height*(float)Math.random());
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting location for a File Node
   */
  public Vector2f fStartLocation() {
    Vector2f vec = new Vector2f(code_swarm.width*(float)Math.random(), code_swarm.height*(float)Math.random());
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a Person Node
   */
  public Vector2f pStartVelocity() {
    Vector2f vec = new Vector2f((float)Math.random()*2 - 1, (float)Math.random()*2 -1);
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity() {
    Vector2f vec = new Vector2f((float)Math.random()*2 - 1, (float)Math.random()*2 -1);
    return vec;
  }
}

