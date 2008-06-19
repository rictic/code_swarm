class ColorBins
{
  color [] colorList;
  int num;
  
  ColorBins()
  {
    colorList = new color[2];
    num = 0;
  }
  
  void add( color c )
  {
    if ( num >= colorList.length )
      colorList = expand( colorList );
      
    colorList[num] = c;
    num++;
  }
  
  void sort()
  {
    colorList = PApplet.sort( colorList );
  }
}
