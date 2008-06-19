class FileNode extends Node
{
  String name;
  color nodeHue;
  
  int touches;

  FileNode( FileEvent fe )
  {
    super();
    name = fe.path + fe.filename;
    fixed = false;
    life = 255;
    touches = 1;
    colorMode( RGB );
    
    nodeHue = colorAssigner.getColor( name );
  }

  void update()
  {
    super.update();
    decay();
  }

  void freshen()
  {
    life = 255;
    touches++;
  }

  void decay()
  {
    life += -2.0;
  }

  void draw()
  {
    if ( life > 0.0 )
    {
      //drawSharp();
      drawFuzzy();
      //drawJelly();

      /*
      // label
       colorMode( RGB );
       fill( 0, life );
       textAlign( CENTER, CENTER );
       text( name, x, y );
       */
    }
  }

  void drawSharp()
  {
    colorMode( RGB );
    fill( nodeHue, life );
    float w = 3;

    if ( life >= 244 )
    {
      stroke( 255, 128 );
      w *= 2;
    }  
    else
      noStroke();
    ellipseMode( CENTER );
    ellipse( x, y, w, w );
  }

  void drawFuzzy()
  {
    tint( nodeHue, life );
    
    float w = 8 + sqrt( touches ) * 4;
    float dubw = w * 2;
    float halfw = w / 2;
    if ( life >= 244 )
    {
      colorMode( HSB );
      tint( hue( nodeHue ), saturation(nodeHue) - 192, 255, life );
      //image( sprite, x - w, y - w, dubw, dubw );
    }
    //else
      image( sprite, x - halfw, y - halfw, w, w );
  }
  
  void drawJelly()
  {
    noFill();
    if ( life >= 244 )
      stroke( 255 );
    else
      stroke( nodeHue, life );
    float w = sqrt( touches );
    ellipseMode( CENTER );
    ellipse( x, y, w, w );
  }

  void relax()
  {
    if ( life <= 0 )
      return;

    float ddx = 0;
    float ddy = 0;

    for( int j = 0; j < nodes.size(); j++ )
    {
      FileNode n = (FileNode)nodes.get(j);
      if ( n.life <= 0 )
        continue;

      if ( n != this )
      {
        float vx = x - n.x;
        float vy = y - n.y;
        float lensq = vx * vx + vy * vy;
        if ( lensq == 0 )
        {
          ddx += random(0.1);
          ddy += random(0.1);
        }
        else if ( lensq < 100 * 100 )
        {
          ddx += vx / lensq;
          ddy += vy / lensq;
        }
      }
    }
    float dlen = mag( ddx, ddy ) / 2;
    if ( dlen > 0 )
    {
      dx += ddx / dlen;
      dy += ddy / dlen;
    }
  }
}
