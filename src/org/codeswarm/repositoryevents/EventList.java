/* This file is part of code_swarm.

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

package org.codeswarm.repositoryevents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * represents a list of log entries. Just a simple bean.
 * @author tpraxl
 */
public class EventList {
    List<Event> events = new ArrayList<Event>();
    /**
     * add an entry to the list.
     * @param e the repository Event / log entry to add (not null)
     */
    public void addEvent(Event e){
        events.add(e);
    }
    /**
     * @return an iterator-view of the list
     */
    public Iterator<Event> iterator(){
        return events.iterator();
    }
    /**
     * @return an unmodifiableList-View of the list
     */
    public List<Event> getEvents(){
        return Collections.unmodifiableList(events);
    }
}
