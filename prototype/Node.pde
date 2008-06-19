abstract class Node
{
  float x, y;
  float dx, dy;
  boolean fixed;
  
  float life;
  
  Node()
  {
    x = random(width);
    y = random(height);
  }
  
  abstract void draw();

  void update()
  {
    if ( !fixed )
    {
      x += constrain( dx, -5, 5 );
      y += constrain( dy, -5, 5 );
      x = constrain( x, 0, width );
      y = constrain( y, 0, height );
    }
    dx /= 2;
    dy /= 2;
  }
  
  abstract void relax();
}
