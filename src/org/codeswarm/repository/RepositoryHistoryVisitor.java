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
package org.codeswarm.repository;

/**
 * This interface provides hooks and methods for concrete
 * RepositoryHistoryVisitor-implementations.<br />
 * It is not tight to concrete repositories like CVS, SVN, etc. but can be
 * used as an API to visit arbitrary repositories.
 * Derived classes may be of two types:<br />
 * <dl>
 *  <dt>Repository-specific Behaviour</dt>
 *  <dd>Derived classes that implement the behaviour to visit the
 *  repository-entries of concrete repositories (like CVS, SVN).<br />
 *  These classes should be abstract.
 *  They should implement {@link #run(String, String, String)}.<br />
 *  They should not implement the other methods, but simply call them in
 *  appropriate situations, as those methods are meant to be hooks.
 * </dd>
 * <dt>Context-specific Behaviour</dt>
 * <dd>These classes are not directly derived from this interface.<br />
 *  They implement the abstract Repository-specific behaviours and perform
 *  operations on the hooks being called.
 * </dd>
 * </dl>
 *
 * NOTE: The interface is work in progress and should be improved.
 * @author tpraxl
 */
public interface RepositoryHistoryVisitor <E, T, R > {
    // TODO remove userName and password from this implementation
    // and replace it with an AuthenticationManager to be able to use arbitrary
    // authentication-mechanisms such as key-file based authentication.

    // TODO think about redesign to decouple from SVNKit.
    // This interface was designed around SVNKit's behaviour. It's design
    // should be changed to be more abstract.
    /**
     * This method gets called when the visitor shall start its operation.
     * @param url complete repository url
     *          (with protocoll prefix, such as http://, svn://)
     * @param name userName
     * @param password userPassword
     */
    public void run(String url, String name, String password);

    // HOOKS
    // EXCEPTION HOOKS:
    /**
     * gets called before any connection is performed.
     * @param url the url to connect to
     */
    public abstract void handleStart(String url);
    /**
     * An Exception has occured while trying to create a Repository driver
     * for the specified protocol. No RepositoryDriver is available for the
     * protocol. This exception is typically not recoverable.
     * @param exception The original Exception
     * @param url The url whose protocol was analyzed.
     * @return false if you want to stop further processing, true if you want to
     * proceed. Typically you will want to return false.
     */
    public abstract boolean handleCreateRepositoryException(T exception, String url);
    /**
     * An Exception has occured while trying to fetch the latest revision
     * number. This exception is caused by authentication or connection failures.
     * @param exception The original Exception
     * @return null if you want to stop further processing, a desired Revision if you want to
     * proceed. Typically you will want to return null.
     */
    public abstract R handleFetchingLatestRepositoryRevisionException(T exception);
    /**
     * An Exception has occured while trying to fetch the revision-log.
     * This exception is caused by authentication or connection failures.
     * @param exception The original Exception
     * @param url The url to the repository that was asked for the log.
     * @return false if you want to stop further processing, true if you want to
     * proceed. Typically you will want to return false.
     */
    public abstract boolean handleCollectingLogInformationException(T exception, String url);
    // PROCESSING HOOKS:
    /**
     * A revision log entry was visited. Implement code to process the entry.
     * @param entry the entry that was visited. Usually not null.
     */
    public abstract void handleLogEntry(E entry);
    /**
     * The latest repository revision was fetched. Implement code to process the
     * information.
     * @param revision the latest repository revision
     * @return false if you want to stop further processing, true if you want to
     * proceed. Return false to stop processing for example when a cached
     * version was found.
     */
    public abstract boolean handleFetchingLatestRepositoryRevision(R revision);
    /**
     * The process has successfully finished.
     */
    public abstract void finishLogEntries();
}
