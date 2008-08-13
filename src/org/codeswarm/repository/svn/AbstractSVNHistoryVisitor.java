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

import java.util.Collection;
import java.util.Iterator;

import org.codeswarm.repository.RepositoryHistoryVisitor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Provides access to the repository log of svn-repositories. Using SVNKit.<br />
 * It simply visits all entries of the log and calls the corresponding hooks.<br />
 * 
 * Derived classes must implement the hooks to perform corresponding operations.
 * @see org.codeswarm.repository.RepositoryHistoryVisitor
 * NOTE: The class is work in progress and should be improved.
 * @author tpraxl
 */
public abstract class AbstractSVNHistoryVisitor implements RepositoryHistoryVisitor<SVNLogEntry, SVNException, Long>{
    /**
     * Starts the repository-log-lookup from the first to the last revision.
     * @see #run(String, Long, Long, String, String)
     * @param url the complete url to the repository 
     * (including the protocol (http://, svn://,...))
     * @param name username for authentication
     * @param password users password for authentication
     */
    public void run(String url, String name, String password){
        run(url, null, null, name, password);
    }
    /**
     * Starts the repository-log-lookup.
     * @see #run(String, String, String)
     * @param url the complete url to the repository 
     * (including the protocol (http://, svn://,...))
     * @param pStartrevision the revision to start with
     * @param pEndrevision the last revision to take into account.<br />
     * NOTE: Currently this parameter is not supported. 
     * The log will always be fetched until the last revision.
     * @param name username for authentication
     * @param password users password for authentication
     */
    public void run(String url, Long pStartrevision, Long pEndrevision, String name, String password) {
        handleStart(url);
        long startRevision = pStartrevision!=null?pStartrevision.longValue():0;
        long endRevision = pEndrevision!=null?pEndrevision.longValue():-1;//HEAD (the latest) revision
        /*
         * Initializes the library (it must be done before ever using the
         * library itself)
         */
        setupLibrary();

        SVNRepository repository = null;
        
        try {
            /*
             * Creates an instance of SVNRepository to work with the repository.
             * All user's requests to the repository are relative to the
             * repository location used to create this SVNRepository.
             * SVNURL is a wrapper for URL strings that refer to repository locations.
             */
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        } catch (SVNException svne) {
            if(!handleCreateRepositoryException(svne, url)){
                return;
            }
        }

        /*
         * User's authentication information (name/password) is provided via  an 
         * ISVNAuthenticationManager  instance.  SVNWCUtil  creates  a   default 
         * authentication manager given user's name and password.
         * 
         * Default authentication manager first attempts to use provided user name 
         * and password and then falls back to the credentials stored in the 
         * default Subversion credentials storage that is located in Subversion 
         * configuration area. If you'd like to use provided user name and password 
         * only you may use BasicAuthenticationManager class instead of default 
         * authentication manager:
         * 
         *  authManager = new BasicAuthenticationsManager(userName, userPassword);
         *  
         * You may also skip this point - anonymous access will be used. 
         */
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
        repository.setAuthenticationManager(authManager);

        /*
         * Gets the latest revision number of the repository
         */
        try {
            endRevision = repository.getLatestRevision();
            if(!handleFetchingLatestRepositoryRevision(endRevision)){
                return;
            }
        } catch (SVNException svne) {
            Long revision = handleFetchingLatestRepositoryRevisionException(svne);
            if(revision==null){
                return;
            }else{
                endRevision = revision;
            }
        }

        Collection logEntries = null;
        try {
            /*
             * Collects SVNLogEntry objects for all revisions in the range
             * defined by its start and end points [startRevision, endRevision].
             * For each revision commit information is represented by
             * SVNLogEntry.
             * 
             * the 1st parameter (targetPaths - an array of path strings) is set
             * when restricting the [startRevision, endRevision] range to only
             * those revisions when the paths in targetPaths were changed.
             * 
             * the 2nd parameter if non-null - is a user's Collection that will
             * be filled up with found SVNLogEntry objects; it's just another
             * way to reach the scope.
             * 
             * startRevision, endRevision - to define a range of revisions you are
             * interested in; by default in this program - startRevision=0, endRevision=
             * the latest (HEAD) revision of the repository.
             * 
             * the 5th parameter - a boolean flag changedPath - if true then for
             * each revision a corresponding SVNLogEntry will contain a map of
             * all paths which were changed in that revision.
             * 
             * the 6th parameter - a boolean flag strictNode - if false and a
             * changed path is a copy (branch) of an existing one in the repository
             * then the history for its origin will be traversed; it means the 
             * history of changes of the target URL (and all that there's in that 
             * URL) will include the history of the origin path(s).
             * Otherwise if strictNode is true then the origin path history won't be
             * included.
             * 
             * The return value is a Collection filled up with SVNLogEntry Objects.
             */
            logEntries = repository.log(new String[] {""}, null,
                    startRevision, endRevision, true, true);

        } catch (SVNException svne) {
            handleCollectingLogInformationException(svne,url);
        }
        for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
            
            /*
             * gets a next SVNLogEntry
             */
            SVNLogEntry logEntry = (SVNLogEntry) entries.next();
            handleLogEntry(logEntry);
        }
        finishLogEntries();
    }

    /*
     * Initializes the library to work with a repository via 
     * different protocols.
     */
    private static void setupLibrary() {
        /*
         * For using over http:// and https://
         */
        DAVRepositoryFactory.setup();
        /*
         * For using over svn:// and svn+xxx://
         */
        SVNRepositoryFactoryImpl.setup();
        
        /*
         * For using over file:///
         */
        FSRepositoryFactory.setup();
    }

    
}