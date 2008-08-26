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
 * @brief Physics Engine implementation.  In essence, people bounce around.  Nodes are attracted to the people.
 * 
 * @see PhysicsEngine for interface information
 * @author Desmond Daignault  <nawglan at gmail>
 */
public class PhysicsEngineChaotic implements PhysicsEngine
{
  private Properties cfg;
  
  private float DRAG;
  
  
  /**
   * Method for initializing parameters.
   * @param p Properties from the config file.
   */
  public void setup (Properties p)
  {
    cfg = p;
    DRAG = Float.parseFloat(cfg.getProperty("drag","0.00001"));
  }
  
  /**
   * Method to ensure upper and lower bounds
   * @param value Value to check
   * @param min Floor value
   * @param max Ceiling value
   * @return value if between min and max, min if < max if >
   */
  private float constrain(float value, float min, float max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    }
    
    return value;
  }
  
  /**
   * Legacy method that calculate the attractive/repulsive force between a person and one of its file along their link (the edge).
   * 
   * @param edge the link between a person and one of its file 
   * @return force force calculated between those two nodes
   */
  private Vector2f calculateForceAlongAnEdge( code_swarm.Edge edge )
  {
    float distance;
    float deltaDistance;
    Vector2f force = new Vector2f();
    Vector2f tforce = new Vector2f();
    
    // distance calculation
    tforce.sub(edge.nodeTo.mPosition, edge.nodeFrom.mPosition);
    distance = tforce.length();
    if (distance > 0) {
      // force calculation (increase when distance is different from targeted len")
      deltaDistance = (edge.len - distance) / (distance * 3);
      // force ponderation using a re-mapping life from 0-255 scale to 0-1.0 range
      // This allows nodes to drift apart as their life decreases.
      deltaDistance *= ((float)edge.life / edge.LIFE_INIT);
      
      // force projection onto x and y axis
      tforce.scale(deltaDistance);
      
      force.set(tforce);
    }
    
    return force;
  }
  
  private void checkCollisionNew(code_swarm.Node nodeA, code_swarm.Node nodeB, float maxD)
  {
    Vector2f tmp = new Vector2f();
    tmp.sub(nodeA.mPosition, nodeB.mPosition);
    double distance = tmp.length();
    if (distance <= (nodeA.mass + nodeB.mass)) {
      float dx = nodeA.mPosition.x - nodeB.mPosition.x;
      float dy = nodeA.mPosition.y - nodeB.mPosition.y;
      float collision_angle = (float)Math.atan2(dx,dy);
      float magnitude1 = nodeA.mSpeed.length();
      float magnitude2 = nodeB.mSpeed.length();
      float direction1 = (float)Math.atan2(nodeA.mSpeed.y, nodeA.mSpeed.x);
      float direction2 = (float)Math.atan2(nodeB.mSpeed.y, nodeB.mSpeed.x);
      float new_xspeed1 = magnitude1 * (float)Math.cos(direction1 - collision_angle);
      float final_yspeed1 = magnitude1 * (float)Math.sin(direction1 - collision_angle);
      float new_xspeed2 = magnitude2 * (float)Math.cos(direction2 - collision_angle);
      float final_yspeed2 = magnitude2 * (float)Math.sin(direction2 - collision_angle);
      float final_xspeed1 = ((nodeA.mass-nodeB.mass)*new_xspeed1+(nodeB.mass+nodeB.mass)*new_xspeed2)/(nodeA.mass+nodeB.mass);
      float final_xspeed2 = ((nodeA.mass+nodeA.mass)*new_xspeed1+(nodeB.mass-nodeA.mass)*new_xspeed2)/(nodeA.mass+nodeB.mass);

      float nodeA_xspeed = (float)(Math.cos(collision_angle)*final_xspeed1+Math.cos(collision_angle+Math.PI/2)*final_yspeed1);
      float nodeA_yspeed = (float)(Math.sin(collision_angle)*final_xspeed1+Math.sin(collision_angle+Math.PI/2)*final_yspeed1);
      float nodeB_xspeed = (float)(Math.cos(collision_angle)*final_xspeed2+Math.cos(collision_angle+Math.PI/2)*final_yspeed2);
      float nodeB_yspeed = (float)(Math.sin(collision_angle)*final_xspeed2+Math.sin(collision_angle+Math.PI/2)*final_yspeed2);
      
      nodeA.mSpeed.set(nodeA_xspeed,nodeA_yspeed);
      nodeB.mSpeed.set(nodeB_xspeed,nodeB_yspeed);
    }
  }
  
  private void checkCollision(code_swarm.Node nodeA, code_swarm.Node nodeB, float maxD)
  {
    Vector2f dVec = new Vector2f();
    
    dVec.sub(nodeB.mPosition, nodeA.mPosition);
    double d = dVec.length();
    if (d <= (maxD)) { // Yep, a collision
      dVec.normalize();
      float Vp1 = nodeA.mSpeed.dot(dVec);
      float Vp2 = nodeB.mSpeed.dot(dVec);
      float dt = (float) ((nodeA.mass + nodeB.mass - d)/(Vp1 + Vp2));
      nodeA.mPosition.set(nodeA.mPosition.x - nodeA.mSpeed.x * dt, nodeA.mPosition.y - nodeA.mSpeed.y * dt);
      nodeB.mPosition.set(nodeB.mPosition.x - nodeB.mSpeed.x * dt, nodeB.mPosition.y - nodeB.mSpeed.y * dt);
      dVec.sub(nodeB.mPosition, nodeA.mPosition);
      d = dVec.length();
      dVec.normalize();
      float Va1 = nodeA.mSpeed.dot(dVec);
      float Va2 = nodeB.mSpeed.dot(dVec);
      float Vb1 = (-nodeA.mSpeed.x * dVec.y + nodeA.mSpeed.y * dVec.x);
      float Vb2 = (-nodeB.mSpeed.x * dVec.y + nodeB.mSpeed.y * dVec.x);
      
      float ed = 1; // ed <= 1, for elastic collision ed = 1
      float vap1 = Va1 + (1 + ed) * (Va2 - Va1) / (1 + nodeA.mass / nodeB.mass);
      float vap2 = Va2 + (1 + ed) * (Va1 - Va2) / (1 + nodeB.mass / nodeA.mass);
      
      nodeA.mSpeed.x = vap1*dVec.x - Vb1*dVec.y;
      nodeA.mSpeed.y = vap1*dVec.y + Vb1*dVec.x;
      nodeB.mSpeed.x = vap2*dVec.x - Vb2*dVec.y;
      nodeB.mSpeed.y = vap2*dVec.y + Vb2*dVec.x;
      
      nodeA.mPosition.x += nodeA.mSpeed.x * dt;
      nodeA.mPosition.y += nodeA.mSpeed.y * dt;
      nodeB.mPosition.x += nodeB.mSpeed.x * dt;
      nodeB.mPosition.y += nodeB.mSpeed.y * dt;
    }
  }
  
  
  /**
   * Legacy method that calculate the repulsive force between two similar nodes (either files or persons).
   * 
   * @param nodeA [in]
   * @param nodeB [in]
   */
  private void calculateForceBetweenfNodes( code_swarm.FileNode nodeA, code_swarm.FileNode nodeB )
  {
    if ((nodeA.life <= 0) || (nodeB.life <= 0)) {
      return;
    }
    
    //float nodeA_mass = nodeA.mass + nodeA.touches;
    //float nodeB_mass = nodeB.mass + nodeB.touches;
    
    checkCollisionNew(nodeA, nodeB, 5);
  }
  
  /**
   * Legacy method that calculate the repulsive force between two similar nodes (either files or persons).
   * 
   * @param nodeA [in]
   * @param nodeB [in]
   */
  private void calculateForceBetweenpNodes( code_swarm.PersonNode nodeA, code_swarm.PersonNode nodeB )
  {
    if ((nodeA.life <= 0) || (nodeB.life <= 0)) {
      return;
    }
    
    checkCollisionNew(nodeA, nodeB, 50);
  }
  
  
  /**
   * Legacy method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node [in] Node the node to which the force apply
   * @param force [in] force a force Vector representing the force on a node
   * 
   * TODO: does force should be a property of the node (or not?)
   */
  private void applyForceTo( code_swarm.Node node, Vector2f force )
  {
    double dlen;
    Vector2f mod = new Vector2f(force);

    /**
     * Taken from Newton's 2nd law.  F=ma
     */
    dlen = mod.length();
    if (dlen > 0) {
      mod.scale(node.mass);
      node.mSpeed.add(mod);
    }
  }

  /**
   * Legacy method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node the node to which the force apply
    */
  private void applySpeedTo( code_swarm.Node node )
  {
    // This block enforces a maximum absolute velocity.
    if (node.mSpeed.length() > node.maxSpeed) {
      Vector2f mag = new Vector2f(node.mSpeed.x / node.maxSpeed, node.mSpeed.y / node.maxSpeed);
      node.mSpeed.scale(1/mag.lengthSquared());
    }
    
    // This block convert Speed to Position
    node.mPosition.add(node.mSpeed);
  }
  
  /**
   *  Do nothing.
   */
  public void initializeFrame() {
  }
  
  /**
   *  Do nothing.
   */
  public void finalizeFrame() {
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
    //Vector2f force    = new Vector2f();

    // Calculate force between the node "from" and the node "to"
    Vector2f force = calculateForceAlongAnEdge(edge);

    // transmit force projection to file and person nodes
    force.negate();
    applyForceTo(edge.nodeFrom, force); // fNode: attract fNode to pNode
    applySpeedTo(edge.nodeFrom); // fNode: move it.
    //force.negate(); // force is inverted for the other end of the edge: repel pNodes from fNodes
    //applyForceTo(edge.nodeTo, force); // pNode
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
    // shortening life
    edge.decay();
/*    Vector2f d = new Vector2f();
    if (edge.nodeFrom.life > 0 && edge.nodeTo.life > 0) {
      d.sub(edge.nodeFrom.mPosition,edge.nodeTo.mPosition);
      if (d.length() < edge.len*1.5) {
        edge.decay();
        edge.nodeFrom.decay();
      }
    } else {
      edge.decay();
      if (edge.nodeTo.life <= 0 && edge.nodeFrom.life > 0) {
        edge.nodeFrom.decay();
      }
    }*/
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
    
    // Calculation of repulsive force between persons
    for (int j = 0; j < code_swarm.nodes.size(); j++) {
      code_swarm.FileNode n = (code_swarm.FileNode) code_swarm.nodes.get(j);
      if (n.life <= 0)
        continue;

      if (n != fNode) {
        // elemental force calculation, and summation
        calculateForceBetweenfNodes(fNode, n);
      }
    }
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
    
    fNode.decay();
    //fNode.life += Math.sqrt(fNode.touches / 2);
    
    // Apply drag (reduce Speed for next frame calculation)
    fNode.mSpeed.scale(DRAG);
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
    if (pNode.mSpeed.length() == 0) {
      // Range (-1,1)
      pNode.mSpeed.set(pNode.mass*((float)Math.random()-pNode.mass),pNode.mass*((float)Math.random()-pNode.mass));
    }
    
    pNode.mSpeed.scale(pNode.mass);
    pNode.mSpeed.normalize();
    pNode.mSpeed.scale(4);
    
    float distance = pNode.mSpeed.length();
    if (distance > 0) {
      float deltaDistance = (pNode.mass - distance) / (distance * 2);
      deltaDistance *= ((float)pNode.life / pNode.LIFE_INIT);
      
      pNode.mSpeed.scale(deltaDistance);
    }
    
    applySpeedTo(pNode);
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
    
    // Check for collisions with neighbors.
    for (int i = 0; i < code_swarm.people.size(); i++) {
      if (pNode != code_swarm.people.get(i)) {
        calculateForceBetweenpNodes(pNode,code_swarm.people.get(i));
      }
    }
    
    // ensure coherent resulting position
    pNode.mPosition.set(constrain(pNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(pNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    if ((pNode.mPosition.x < pNode.mass && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (code_swarm.width - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
      // we hit a vertical wall
      pNode.mSpeed.x = -pNode.mSpeed.x;
      while (pNode.mPosition.x < pNode.mass || pNode.mPosition.x > (code_swarm.width - pNode.mass)) {
        pNode.mPosition.x += pNode.mSpeed.x;
      }
    }
    if ((pNode.mPosition.y < pNode.mass && pNode.mSpeed.y < 0.0f) || (pNode.mPosition.y > (code_swarm.height - pNode.mass) && pNode.mSpeed.y > 0.0f)) {
      // we hit a horizontal wall
      pNode.mSpeed.y = -pNode.mSpeed.y;
      while (pNode.mPosition.y < pNode.mass || pNode.mPosition.y > (code_swarm.height - pNode.mass)) {
        pNode.mPosition.y += pNode.mSpeed.y;
      }
    }
    // shortening life
    pNode.decay();
    
    // Apply drag (reduce Speed for next frame calculation)
    pNode.mSpeed.scale(DRAG);
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
  public Vector2f pStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
}

