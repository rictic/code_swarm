class ColorAssigner
{
  ArrayList tests;
  color defaultColor = color(128, 128, 128);
  
  public ColorAssigner()
  {
    tests = new ArrayList();
  }
  
  void addRule( String expr, color c1, color c2 )
  {
    ColorTest t = new ColorTest();
    t.expr = expr;
    t.c1 = c1;
    t.c2 = c2;
    addRule( t );
  }
  
  void addRule( ColorTest t )
  {
    tests.add( t );
  }
  
  color getColor( String s )
  {
    for( int i = 0; i < tests.size(); i++ )
    {
      ColorTest t = (ColorTest)tests.get(i);
      if ( t.passes( s ) )
        return t.assign();
    }
    
    return defaultColor;
  }
}

class ColorTest
{
  String expr;
  color c1, c2;
  
  boolean passes( String s )
  {
    return s.matches( expr );
  }
  
  color assign()
  {
    return lerpColor( c1, c2, random(1.0) );
  }
}
