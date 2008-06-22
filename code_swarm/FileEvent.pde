/*  
    Copyright 2008 Michael Ogawa

    This file is part of code_swarm.

    code_swarm is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    code_swarm is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
*/

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
