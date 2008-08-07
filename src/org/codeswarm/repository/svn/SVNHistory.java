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
// Some parts of this code have been taken from SVNKit's example-code.
// See https://wiki.svnkit.com/Printing_Out_Repository_History
package org.codeswarm.repository.svn;


import org.codeswarm.repositoryevents.CodeSwarmEventsSerializer;
import org.codeswarm.repositoryevents.Event;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.codeswarm.repositoryevents.EventList;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

/**
 * Performs the repository lookup and serializes the data.
 * @author tpraxl
 */
public class SVNHistory extends AbstractSVNHistoryVisitor {
    private static final Logger LOGGER = Logger.getLogger(SVNHistory.class.getName());
    String filename;
    String url;
    EventList list = new EventList();
    /**
     * creates an instance of SVNHistory.
     * @param filename the path to the (xml-)file to serialize the data to.
     */
    public SVNHistory(String filename){
        this.filename =filename;
    }
    /**
     * @return the path to the file the data is serialized to.
     */
    public String getFilePath(){
        return "data/"+filename+this.url.hashCode()+".xml";
    }
    /**
     * clears the entire revision cache.
     */
    public static void clearCache(){
        try {
            Preferences.userNodeForPackage(SVNHistory.class).clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(SVNHistory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * stores the repository url
     * @param url the complete repository url.
     */
    public void handleStart(String url) {
        this.url = url;
    }
    /**
     * looks up the cache. Stops proceeding if a cached version for this
     * repository was found.
     * @param pRevision the latest repository revision.
     * @return false if a cached version was found, true if the history shall 
     * be fetched from repository.
     */
    public boolean handleFetchingLatestRepositoryRevision(Long pRevision) {
        long revision = pRevision.longValue();
        Preferences p = Preferences.userNodeForPackage(SVNHistory.class);
        long l= p.getLong(this.url+"_lastRevision", -1l);
        if(l==revision){
            LOGGER.log(Level.FINE,"skip fetching {0} (latest revision is {1}) for {2}",new Object[]{String.valueOf(l),revision,this.url});
            return false;
        }else{
            LOGGER.log(Level.FINE, "proceed fetching (latest revision is {0} , cached revision is {1} for repository {2}", new Object[]{String.valueOf(pRevision), String.valueOf(l), this.url});
            Preferences.userNodeForPackage(SVNHistory.class).putLong(this.url+"_lastRevision", revision);
            try {
                Preferences.userNodeForPackage(SVNHistory.class).flush();
            } catch (BackingStoreException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        LOGGER.log(Level.FINE,"fetching until revision {0}",new Object[]{revision});
        return true;
    }
    /**
     * processes a log entry. Adds it to the EventList
     * @param logEntry the entry to process
     */
    public void handleLogEntry(SVNLogEntry logEntry) {
        Set keySet = logEntry.getChangedPaths().keySet();
        Iterator i = keySet.iterator();
        while(i.hasNext()){
            String key = (String)i.next();
            SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(key);
            list.addEvent(new Event(entryPath.getPath(),logEntry.getDate().getTime(),logEntry.getAuthor()));    
            if(LOGGER.isLoggable(Level.FINE)){
                LOGGER.log(Level.FINE, "fetched entry {0}\n date {1}\n rev. {2}\n--", new Object[]{entryPath.getPath(),logEntry.getDate(), logEntry.getRevision()});
            }
        }
        /*
         * displaying all paths that were changed in that revision; changed
         * path information is represented by SVNLogEntryPath.
         */
        if (logEntry.getChangedPaths().size() > 0) {
            /*
             * keys are changed paths
             */
            Set changedPathsSet = logEntry.getChangedPaths().keySet();

            for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
                /*
                 * obtains a next SVNLogEntryPath
                 */
                SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
                /*
                 * SVNLogEntryPath.getPath returns the changed path itself;
                 * 
                 * SVNLogEntryPath.getType returns a charecter describing
                 * how the path was changed ('A' - added, 'D' - deleted or
                 * 'M' - modified);
                 * 
                 * If the path was copied from another one (branched) then
                 * SVNLogEntryPath.getCopyPath &
                 * SVNLogEntryPath.getCopyRevision tells where it was copied
                 * from and what revision the origin path was at.
                 */
                if(LOGGER.isLoggable(Level.FINE)){
                    StringBuffer copyPathInfo = new StringBuffer();
                    if(entryPath.getCopyPath()!=null){
                        copyPathInfo.append("(from ").append(entryPath.getCopyPath());
                        copyPathInfo.append(" rev ").append(entryPath.getCopyRevision()).append(")");
                    }
                    LOGGER.log(Level.FINE,"entry: {0} {1} {2}", 
                            new Object[]{entryPath.getType(),entryPath.getPath(),copyPathInfo.toString()});
                }
            }
        }
    }
    /**
     * serializes the log entries
     */
    public void finishLogEntries() {
        try {
            CodeSwarmEventsSerializer serializer = 
                    new CodeSwarmEventsSerializer(list);
            serializer.serialize(getFilePath());
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Logs an error statement and stops further processing
     * @param e the orginal exception
     * @param url the repository url
     * @return false
     */
    public boolean handleCreateRepositoryException(SVNException e, String url) {
        /*
         * Perhaps a malformed URL is the cause of this exception.
         */
        LOGGER.log(Level.SEVERE,"error while creating an SVNRepository for the location {0} : {1}", new Object[]{url, e.getMessage()});
        return false;
    }
    /**
     * Logs an error statement and stops further processing
     * Otherwise returns the latest cached revision.
     * @param e the orginal exception
     * @return null.
     */
    public Long handleFetchingLatestRepositoryRevisionException(SVNException svne) {
        LOGGER.log(Level.FINE,"error while fetching the latest repository revision: {0}.\nFalling back to cached version (if present).",new Object[]{ svne.getMessage()});
        return null;
    }
    /**
     * Logs an error statement and stops further processing
     * @param e the orginal exception
     * @param url the repository url
     * @return false
     */
    public boolean handleCollectingLogInformationException(SVNException svne, String url) {
        LOGGER.log(Level.SEVERE,"error while collecting log information for {0} : {1}", new Object[]{url,svne.getMessage()});
        return false;
    }
}
