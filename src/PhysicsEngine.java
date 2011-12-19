import javax.vecmath.Vector2f;


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
 * Abstract interface of any code_swarm physical engine.
 *
 * @note Need to be derived to define force calculation algorithms between Nodes
 * @note Need to use the constructor to apply some configuration options
 *
 * @note For portability, no Processing library should be use there, only standard Java packages
 */
public abstract class PhysicsEngine
{

  /**
   * Initialize the Physical Engine
   * @param p Properties file
   */
  public abstract void setup (CodeSwarmConfig p);

  /**
   * Method that allows Physics Engine to initialize the Frame
   *
   */
  public void initializeFrame() {}


  /**
   * Method that allows Physics Engine to finalize the Frame
   *
   */
  public void finalizeFrame() {}


  public void onRelax(code_swarm.PersonNode p){}
  public void onRelax(code_swarm.FileNode f){}
  public void onRelax(code_swarm.Edge e){}

  public void onUpdate(code_swarm.PersonNode p){
    updateNode(p);
  }
  public void onUpdate(code_swarm.FileNode f){
    updateNode(f);
  }
  public void onUpdate(code_swarm.Edge edge) {
    edge.decay();
  }
  private void updateNode(code_swarm.Node node) {
    Vector2f tforce = new Vector2f(node.mPosition.x - node.mLastPosition.x, node.mPosition.y - node.mLastPosition.y);
    node.mLastPosition = new Vector2f(node.mPosition);
    tforce.scale(node.mFriction); // Friction!
    node.mPosition.add(tforce);

    node.decay();
  }

  /**
   *
   * @return Vector2f vector holding the starting location for a Person Node
   */
  public Vector2f startLocation(code_swarm.PersonNode node){
    return randomLocation();
  }


  /**
   *
   * @return Vector2f vector holding the starting location for a File Node
   */
  public Vector2f startLocation(code_swarm.FileNode node){
    return randomLocation();
  }

  /**
   *
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f startVelocity(code_swarm.PersonNode node) {
    return new Vector2f();
  }

  public Vector2f startVelocity(code_swarm.FileNode node) {
    Vector2f vec = new Vector2f(((float)Math.random()*2 - 1), ((float)Math.random()*2-1));
    vec.scale((1 / vec.length()) * (float)Math.random() * 15 / node.mass);
    return vec;
  }

  public Vector2f randomLocation() {
    Vector2f vec = new Vector2f(code_swarm.width*(float)Math.random(), code_swarm.height*(float)Math.random());
    return vec;
  }

  protected float constrain(float value, float min, float max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    }

    return value;
  }
}

