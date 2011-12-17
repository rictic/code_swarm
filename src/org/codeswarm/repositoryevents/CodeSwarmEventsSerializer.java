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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Takes a list and renders it to an xml-file
 * @author tpraxl
 */
public class CodeSwarmEventsSerializer {
    EventList list;
    /**
     * creates an instance of the serializer.
     * @param list the EventList to serialize
     */
    public CodeSwarmEventsSerializer(EventList list){
        this.list = list;
    }
    /**
     * actually serializes the list to the file denoted by pathToFile
     * @param pathToFile the path to the xml file to serialize to.
     *          It gets created if it doesn't exist.
     * @throws javax.xml.parsers.ParserConfigurationException
     *          When the serialization failed
     * @throws javax.xml.transform.TransformerConfigurationException
     *          When the serialization failed
     * @throws java.io.IOException
     *          When the serialization failed
     * @throws javax.xml.transform.TransformerException
     *          When the serialization failed
     */
    public void serialize(String pathToFile) throws ParserConfigurationException, TransformerConfigurationException, IOException, TransformerException{
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element events = d.createElement("file_events");
        for(Event e : list.getEvents()){
            Element event = d.createElement("event");
            event.setAttribute("filename", e.getFilename());
            event.setAttribute("date", String.valueOf(e.getDate()));
            event.setAttribute("author", e.getAuthor());
            events.appendChild(event);
        }
        d.appendChild(events);
        Transformer t = TransformerFactory.newInstance().newTransformer();
        File f = new File(pathToFile);
        if(!f.exists()){
            f.createNewFile();
        }
        FileOutputStream out = new FileOutputStream(f);
        StreamResult result = new StreamResult(out);
        t.transform(new DOMSource(d), result);
        out.close();
    }

}
