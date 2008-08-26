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
public class PhysicsEngineMaxwellsDemon implements PhysicsEngine
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private Properties cfg;

  private float DRAG;
  private Integer doorSize;
  private boolean doorOpen;
  private int midWayX;
  private int midWayY;
  private int startDoorY;
  Vector2f doorCenter;
  private int doorWayLeft;
  private int doorWayRight;


  /**
   * Method for initializing parameters.
   * @param p Properties from the config file.
   */
  public void setup (Properties p)
  {
    cfg = p;
    DRAG = Float.parseFloat(cfg.getProperty("drag","0.00001"));
    doorSize = Integer.parseInt(cfg.getProperty("doorSize","100"));
    doorOpen = false;
    midWayX = code_swarm.width / 2;
    midWayY = code_swarm.height / 2;
    startDoorY = midWayY - doorSize;
    doorCenter = new Vector2f(midWayX, midWayY);
    doorWayLeft = Integer.parseInt(cfg.getProperty("doorWayLeft","35"));
    doorWayRight = Integer.parseInt(cfg.getProperty("doorWayRight","50"));
  }

  /**
   * 
   * @param opened Is door open or closed?
   */
  private void drawWall() {
    // Draw the wall.
    int midWayX = code_swarm.width / 2;
    int midWayY = code_swarm.height / 2;
    int startDoorY = midWayY - doorSize;

    // draw top of wall
    code_swarm.utils.drawLine(midWayX, 0, midWayX, midWayY, 255, 255, 255);

    // draw door
    if (doorOpen) {
      code_swarm.utils.drawLine(midWayX, startDoorY, midWayX, midWayY, 0, 255, 0);
    } else {
      code_swarm.utils.drawLine(midWayX, startDoorY, midWayX, midWayY, 255, 0, 0);
    }
    // draw bottom of wall
    code_swarm.utils.drawLine(midWayX, midWayY, midWayX, code_swarm.height, 255, 255, 255);
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
   * Calculate the attractive/repulsive force between a person and one of its file
   * along their link (the edge).
   * 
   * @param edge the link between a person and one of its file 
   * @return Vector2f force calculated between those two nodes
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
  
  /**
   * Calculate the repulsive force between two similar file nodes.
   * 
   * @param nodeA
   * @param nodeB
   * @return Vector2f force calculated between those two nodes
   */
  private Vector2f calculateForceBetweenfNodes( code_swarm.FileNode nodeA, code_swarm.FileNode nodeB )
  {
    float distance;
    Vector2f force = new Vector2f();
    Vector2f normVec = new Vector2f();

    /**
     * Get the distance between nodeA and nodeB
     */
    normVec.sub(nodeA.mPosition, nodeB.mPosition);
    distance = normVec.lengthSquared();
    /**
     * If there is a Collision.  This is assuming a radius of zero.
     * if (lensq == (radius1 + radius2)) is what to use if we have radius 
     * could use touches for files and edge_length for people?
     */
    if (distance == (nodeA.touches + nodeB.touches)) {
      force.set(0.01f* (((float)Math.random()*2)-1), (0.01f* ((float)Math.random()*2)-1));
    } else if (distance < 10000) {
      /**
       * No collision and distance is close enough to actually matter.
       */
      normVec.scale(1/distance);
      force.set(normVec);
    }

    return force;
  }

  /**
   * Calculate the repulsive force between two similar person nodes
   * People ricochet off of each other and walls.
   * 
   * @param nodeA
   * @param nodeB
   * @return Vector2f force calculated between those two nodes
   */
  private Vector2f calculateForceBetweenpNodes( code_swarm.PersonNode nodeA, code_swarm.PersonNode nodeB )
  {
    Vector2f force = new Vector2f();
    Vector2f tmp = new Vector2f();

    tmp.sub(nodeA.mPosition, nodeB.mPosition);
    double distance = Math.sqrt(tmp.lengthSquared());
    if (distance <= (nodeA.mass + nodeB.mass)) {
      if (nodeA.mSpeed.x > 0 && nodeA.mSpeed.y > 0) {          // Node A down and right
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        } else {                                               // Node B down and right
          nodeB.mSpeed.x *= -1;
          nodeA.mSpeed.x *= 2;
        }
      } else if (nodeA.mSpeed.x > 0 && nodeA.mSpeed.y < 0) {   // Node A up and right
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= 2;
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        } else {                                               // Node B down and right
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        }
      } else if (nodeA.mSpeed.x < 0 && nodeA.mSpeed.y > 0) {   // Node A down and left
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeB.mSpeed.x *= -1;
          nodeA.mSpeed.x *= 2;
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        } else {                                               // Node B down and right
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        }
      } else {                                                 // Node A up and left
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= 2;
        } else {                                               // Node B down and right
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        }
      }
      while (distance <= (nodeA.mass + nodeB.mass)) {
        applySpeedTo(nodeA);
        constrainNode(nodeA, whichSide(nodeA));
        applySpeedTo(nodeB);
        constrainNode(nodeB, whichSide(nodeB));
        tmp.sub(nodeA.mPosition, nodeB.mPosition);
        distance = Math.sqrt(tmp.lengthSquared());
      }
    }
    /**
     * No collision
     */
    return force;
  }

  /**
   * Apply force to a node, converting acceleration to speed.
   * 
   * @param node Node the node to which the force apply
   * @param force force a force Vector representing the force on a node
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
   * Apply force to a node, converting speed to position.
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
  
  private boolean nearDoor(code_swarm.Node node) {
    if (node.mPosition.x > (midWayX - doorWayLeft) && node.mPosition.x < (midWayX + doorWayRight)) {
      if (node.mPosition.y >= startDoorY && node.mPosition.y <= midWayY) {
        return true;
      }
    }
    return false;
  }
  
  private void constrainNode(code_swarm.Node node, boolean rightSide) {
    if (nearDoor(node)) {
      if (doorOpen) {
        node.mPosition.set(constrain(node.mPosition.x, 0.0f, (float)code_swarm.width),constrain(node.mPosition.y, 0.0f, (float)code_swarm.height));
      } else {
        if (rightSide) {
          node.mPosition.set(constrain(node.mPosition.x, (float)(midWayX + 8), (float)code_swarm.width),constrain(node.mPosition.y, 0.0f, (float)code_swarm.height));
        } else {
          node.mPosition.set(constrain(node.mPosition.x, 0.0f, (float)(midWayX - 8)),constrain(node.mPosition.y, 0.0f, (float)code_swarm.height));
        }
      }
    } else { // not near the door.
      if (rightSide) {
        node.mPosition.set(constrain(node.mPosition.x, (float)(midWayX + 8), (float)code_swarm.width),constrain(node.mPosition.y, 0.0f, (float)code_swarm.height));
      } else {
        node.mPosition.set(constrain(node.mPosition.x, 0.0f, (float)(midWayX - 8)),constrain(node.mPosition.y, 0.0f, (float)code_swarm.height));
      }
    }
  }
  
  private boolean whichSide(code_swarm.Node node) {
    // which half of the screen are we on?
    // true = right side
    return (node.mPosition.x >= midWayX);
  }

  /**
   *  Interface methods below.
   */

  /**
   * draw the wall opened or closed, depending on closeness of people.
   */
  public void initializeFrame() {
    doorOpen = false;
    
    for (code_swarm.PersonNode p : code_swarm.getLivingPeople()) {
      if (p.mSpeed.x < 0.0f && nearDoor(p)) {
        doorOpen = true;
        break;
      }
    }
    
    drawWall();
  }
  
  /**
   * close the door until next iteration
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
    boolean fSide = whichSide(edge.nodeFrom);
    boolean pSide = whichSide(edge.nodeTo);

    if ((!doorOpen && fSide != pSide) || ((doorOpen && edge.nodeFrom.mPosition.y < startDoorY) || (doorOpen && edge.nodeFrom.mPosition.y > startDoorY + doorSize))) {
      return;
    }

    // Calculate force between the node "from" and the node "to"
    Vector2f force = calculateForceAlongAnEdge(edge);

    // transmit force projection to file and person nodes
    force.negate();
    applyForceTo(edge.nodeFrom, force); // fNode: attract fNode to pNode
    // which half of the screen are we on?
    applySpeedTo(edge.nodeFrom); // fNode: move it.
    constrainNode(edge.nodeFrom, whichSide(edge.nodeFrom)); // Keep it in bounds.
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
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxNode(code_swarm.FileNode fNode ) {
    boolean mySide = whichSide(fNode);

    Vector2f forceBetweenFiles = new Vector2f();
    Vector2f forceSummation    = new Vector2f();

    // Calculation of repulsive force between persons
    for (code_swarm.FileNode n : code_swarm.getLivingNodes()) {
      if (n != fNode && mySide == whichSide(n)) {
        // elemental force calculation, and summation
        forceBetweenFiles = calculateForceBetweenfNodes(fNode, n);
        forceSummation.add(forceBetweenFiles);
      }
    }
    // Apply repulsive force from other files to this Node
    applyForceTo(fNode, forceSummation);
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
    constrainNode(fNode, whichSide(fNode)); // Keep it in bounds.
    
    // shortening life
    fNode.decay();

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
    if (pNode.mSpeed.length() == 0) {
      // Range (-1,1)
      pNode.mSpeed.set(pNode.mass*((float)Math.random()-pNode.mass),pNode.mass*((float)Math.random()-pNode.mass));
    }

    pNode.mSpeed.scale(pNode.mass);
    pNode.mSpeed.normalize();
    pNode.mSpeed.scale(5);

    float distance = pNode.mSpeed.length();
    if (distance > 0) {
      float deltaDistance = (pNode.mass - distance) / (distance * 2);
      deltaDistance *= ((float)pNode.life / pNode.LIFE_INIT);

      pNode.mSpeed.scale(deltaDistance);
    }
  }

  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdatePerson(code_swarm.PersonNode pNode) {
    boolean rightSide = whichSide(pNode);
    
    applySpeedTo(pNode);

    // Check for collisions with neighbors.
    for (code_swarm.PersonNode p : code_swarm.getLivingPeople()) {
      if (pNode != p) {
        Vector2f force = calculateForceBetweenpNodes(pNode,p);
        pNode.mPosition.add(force);
      }
    }

    constrainNode(pNode, rightSide); // Keep it in bounds.

    if (doorOpen) {
      // Check for vertical wall collisions
      // 4 walls to check.
      //  |  |  |
      //  |  |  |
      //  |     |
      //  |  |  |
      //  |  |  |
      if (pNode.mPosition.y < startDoorY || pNode.mPosition.y > midWayY) { // Above the door, and below the door.
        if (rightSide) {
          if ((pNode.mPosition.x < (midWayX + pNode.mass) && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (code_swarm.width - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
            pNode.mSpeed.x = -pNode.mSpeed.x;
            int i = 0;
            while (pNode.mPosition.x < (midWayX + pNode.mass) || pNode.mPosition.x > (code_swarm.width - pNode.mass)) {
              pNode.mPosition.x += pNode.mSpeed.x * (i++ % 10);
            }
          }
        } else { // left side
          if ((pNode.mPosition.x < pNode.mass && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (midWayX - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
            pNode.mSpeed.x = -pNode.mSpeed.x;
            int i = 0;
            while (pNode.mPosition.x < pNode.mass || pNode.mPosition.x > (midWayX - pNode.mass)) {
              pNode.mPosition.x += pNode.mSpeed.x * (i++ % 10);
            }
          }
        }
      } else { // Same level as the door
        if ((pNode.mPosition.x < pNode.mass && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (code_swarm.width - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
          pNode.mSpeed.x = -pNode.mSpeed.x;
          int i = 0;
          while (pNode.mPosition.x < pNode.mass || pNode.mPosition.x > (code_swarm.width - pNode.mass)) {
            pNode.mPosition.x += pNode.mSpeed.x * (i++ % 10);
          }
        }
      }
    } else { // Door is closed.
      // Check for vertical wall collisions
      // 3 walls to check.
      //  |  |  |
      //  |  |  |
      //  |  |  |
      //  |  |  |
      //  |  |  |
      
      if (rightSide) {
        if ((pNode.mPosition.x < (midWayX + pNode.mass) && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (code_swarm.width - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
          pNode.mSpeed.x = -pNode.mSpeed.x;
          int i = 0;
          while (pNode.mPosition.x < (midWayX + pNode.mass) || pNode.mPosition.x > (code_swarm.width - pNode.mass)) {
            pNode.mPosition.x += pNode.mSpeed.x * (i++ % 10);
          }
        }
      } else { // left side
        if ((pNode.mPosition.x < pNode.mass && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (midWayX - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
          pNode.mSpeed.x = -pNode.mSpeed.x;
          int i = 0;
          while (pNode.mPosition.x < pNode.mass || pNode.mPosition.x > (midWayX - pNode.mass)) {
            pNode.mPosition.x += pNode.mSpeed.x * (i++ % 10);
          }
        }
      }
    }

    // Check for horizontal wall collisions
    // 2 walls to check.
    //  _______
    //
    //
    //
    //  _______
    
    if ((pNode.mPosition.y < pNode.mass && pNode.mSpeed.y < 0.0f) || ((pNode.mPosition.y > (code_swarm.height - pNode.mass) && pNode.mSpeed.y > 0.0f))) {
      pNode.mSpeed.y = -pNode.mSpeed.y;
      int i = 0;
      while (pNode.mPosition.y < pNode.mass || pNode.mPosition.y > (code_swarm.height - pNode.mass)) {
        pNode.mPosition.y += pNode.mSpeed.y * (i++ % 10);
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
    float x = (float)Math.random() * midWayX + midWayX;
    float y = (float)Math.random() * code_swarm.height;
    
    constrain(x, (midWayX + 10), (code_swarm.width - 10));
    constrain(y, 10, (code_swarm.height - 10));
    
    Vector2f vec = new Vector2f(x, y);

    return vec;
  }

  /**
   * 
   * @return Vector2f vector holding the starting location for a File Node
   */
  public Vector2f fStartLocation() {
    float x = (float)Math.random() * midWayX + midWayX;
    float y = (float)Math.random() * code_swarm.height;
    
    constrain(x, (midWayX + 10), (code_swarm.width - 10));
    constrain(y, 10, (code_swarm.height - 10));
    
    Vector2f vec = new Vector2f(x, y);

    return vec;
  }

  /**
   * 
   * @param mass 
   * @return Vector2f vector holding the starting velocity for a Person Node
   */
  public Vector2f pStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }

  /**
   * 
   * @param mass 
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
}

