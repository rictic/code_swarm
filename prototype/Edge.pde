class Edge
{
  Node from;
  Node to;
  float len;
  int life;
  
  Edge( Node from, Node to )
  {
    this.from = from;
    this.to = to;   
    this.len = 40; //25
    this.life = 255;
  }
  
  void relax()
  {
    float vx = to.x - from.x;
    float vy = to.y - from.y;
    float d = mag( vx, vy );
    if ( d > 0 )
    {
      float f = (len - d) / (d * 3);
      f = f * map( life, 0, 255, 0, 1.0 );
      float dx = f * vx;
      float dy = f * vy;
      to.dx += dx;
      to.dy += dy;
      from.dx -= dx;
      from.dy -= dy;
    }
  }
 
  
  void draw()
  {
    if ( life > 240 )
    {
      stroke( 255, life );
      strokeWeight( 0.35 );
      line( from.x, from.y, to.x, to.y );
    }
  }
  
  void update()
  {
    decay();
  }
  
  void decay()
  {
    life += -2;
    if ( life < 0 )
      life = 0;
  }
  
  void freshen()
  {
    life = 255;
  }
}
