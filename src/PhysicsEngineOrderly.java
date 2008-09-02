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

import java.util.Iterator;

import javax.vecmath.Vector2f;

/**
 * @brief A modification of the Legacy engine to prevent people from piling up in the center
 * 
 * @see PhysicsEngine Physics Engine Interface
 */
public class PhysicsEngineOrderly implements PhysicsEngine
{
  private CodeSwarmConfig cfg;
  
  private float FORCE_EDGE_MULTIPLIER;
  private float FORCE_CALCULATION_RANDOMIZER;
  private float FORCE_NODES_MULTIPLIER;
  private float FORCE_TO_SPEED_MULTIPLIER;
  private float SPEED_TO_POSITION_MULTIPLIER;
  
  /**
   * Method for initializing parameters.
   * @param p The CodeSwarmConfig for this code_swarm instance
   */
  //PhysicalEngineLegacy(float forceEdgeMultiplier, float forceCalculationRandomizer, float forceToSpeedMultiplier, float speedToPositionDrag)
  public void setup (CodeSwarmConfig p)
  {
    cfg = p;
    FORCE_EDGE_MULTIPLIER = cfg.getFloatProperty("edgeMultiplier", 1.0);
    FORCE_CALCULATION_RANDOMIZER = cfg.getFloatProperty("calculationRandomizer",0.01);
    FORCE_NODES_MULTIPLIER = cfg.getFloatProperty("nodesMultiplier",1.0);
    FORCE_TO_SPEED_MULTIPLIER = cfg.getFloatProperty("speedMultiplier",1.0);
    SPEED_TO_POSITION_MULTIPLIER = cfg.getFloatProperty("drag",0.5);
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
  private Vector2f calculateAttractionBetweenPoints(Vector2f point1, Vector2f point2, float targetDistance, float remaining)
  {
    float distance;
    float deltaDistance;
    Vector2f force = new Vector2f();
    Vector2f tforce = new Vector2f();
    
    // distance calculation
    tforce.sub(point1, point2);
    distance = tforce.length();
    if (distance > 0) {
      // force calculation (increase when distance is different from targeted len")
      deltaDistance = (targetDistance - distance) / (distance * 3);
      // force ponderation using a re-mapping life from 0-255 scale to 0-1.0 range
      // This allows nodes to drift apart as their life decreases.
      deltaDistance *= remaining;
      
      // force projection onto x and y axis
      tforce.scale(deltaDistance*FORCE_EDGE_MULTIPLIER);
      
      force.set(tforce);
    }
    
    return force;
  }
  
  
  private Vector2f calculateForceBetweenPNodes( code_swarm.Node nodeA, code_swarm.Node nodeB )
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
    } else if (lensq < 1000) {
      /**
       * No collision and distance is close enough to actually matter.
       */
      normVec.scale(FORCE_NODES_MULTIPLIER * 10000/lensq);
      force.set(normVec);
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
  private void applyForceTo( code_swarm.Node node, Vector2f force )
  {
    float dlen;
    Vector2f mod = new Vector2f(force);

    /**
     * Taken from Newton's 2nd law.  F=ma
     */
    dlen = mod.length();
    if (dlen > 0) {
      mod.scale(node.mass * FORCE_TO_SPEED_MULTIPLIER);
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
    Vector2f force    = new Vector2f();

    // Calculate force between the node "from" and the node "to"
    force = calculateAttractionBetweenPoints(edge.nodeTo.mPosition, edge.nodeFrom.mPosition, edge.len, ((float)edge.life / edge.LIFE_INIT));

    // transmit (applying) fake force projection to file and person nodes
    applyForceTo(edge.nodeTo, force);
    
    edge.nodeTo.editing.add(edge.nodeFrom);
//    force.negate(); // force is inverted for the other end of the edge
//    applyForceTo(edge.nodeFrom, force);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxNode(code_swarm.FileNode fNode ) {
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxPerson(code_swarm.PersonNode pNode) {
    
    Vector2f forceBetweenPersons = new Vector2f();
    Vector2f forceSummation      = new Vector2f();

    // Calculation of repulsive force between persons
    for (int j = 0; j < code_swarm.people.size(); j++) {
      code_swarm.Node n = (code_swarm.Node) code_swarm.people.get(j);

      if (n != pNode) {
        // elemental force calculation, and summation
        forceBetweenPersons = calculateForceBetweenPNodes(pNode, n);
        forceSummation.add(forceBetweenPersons);
      }
    }
    // Apply repulsive force from other persons to this Node
    applyForceTo(pNode, forceSummation);
    
    // Don't know why, but the prototype had this.
    pNode.mSpeed.scale(1.0f/12);
    
    
    //place the edited files around the person
    Iterator<code_swarm.FileNode> iter = pNode.editing.iterator();
    int i = 0;
    int r = 25;
    final int k = 4;
    final int salt = pNode.hashCode(); // used to randomize orientation of circle of nodes
    int N = (int)((2 * r * Math.PI) / k);
    while(iter.hasNext()){
      
      if (i == N){
        r += k;
        N = (int)((2 * r * Math.PI) / k);
        i = 0;
      }
      i++;
      
      code_swarm.FileNode file = iter.next();
      if (file == null) continue;
      
      final int val = i * N + salt; 
      int x = (int)(r * Math.sin(val));
      int y = (int)(r * Math.cos(val));
      
      Vector2f force = calculateAttractionBetweenPoints(file.mPosition, new Vector2f(pNode.mPosition.x + x,pNode.mPosition.y + y), 0, ((float)file.life / file.LIFE_INIT));
      applyForceTo(file, force);
      
    }
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param edge the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdateEdge(code_swarm.Edge edge) {
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
    // Apply Speed to Position on nodes
    applySpeedTo(fNode);
    
    // ensure coherent resulting position
    fNode.mPosition.set(constrain(fNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(fNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    // shortening life
    fNode.decay();
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdatePerson(code_swarm.PersonNode pNode) {
    // Apply Speed to Position on nodes
    applySpeedTo(pNode);
    
    // ensure coherent resulting position
    pNode.mPosition.set(constrain(pNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(pNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    pNode.editing.clear();
    
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
   * @param mass Mass of person
   * @return Vector2f vector holding the starting velocity for a Person Node
   */
  public Vector2f pStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
  
  /**
   * 
   * @param mass Mass of File Node
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
}

