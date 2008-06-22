import java.util.*;

class FileEvent implements Comparable
{
  Date date;
  String author;
  String filename;
  String path;
  int linesadded;
  int linesremoved;
  
  FileEvent( long datenum, String author, String path, String filename )
  {
    this( datenum, author, path, filename, 0, 0 );
  }
  
  FileEvent( long datenum, String author, 
             String path, String filename,
             int linesadded, int linesremoved )
  {
    this.date = new Date( (long)datenum );
    this.author = author;
    this.path = path;
    this.filename = filename;
    this.linesadded = linesadded;
    this.linesremoved = linesremoved;
  }
  
  int compareTo( Object o )
  {
    return date.compareTo( ((FileEvent)o).date );
  }
}
