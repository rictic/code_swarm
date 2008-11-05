#!/usr/bin/python
# Standard library imports
from datetime import datetime
from optparse import OptionParser
import os
import sys
import time
from xml.sax.saxutils import escape as h
from sys import stderr
import re
import sre_constants
from itertools import ifilter


# Some global variables
SVN_SEP = "------------------------------------------------------------------------"
CVS_SEP = "----------------------------"


class Event(object):
    """ Event to hold all of the separate events as we parse them from the logs. """
    
    def __init__(self, filename, date, author):
        self.filename = filename
        self.date = date
        self.author = author
        
    # Some version control system's logs are not in chronological order, so
    # this compare method will return a compare of the date attributes.
    def __cmp__(self, other):
        return cmp(self.date, other.date)
    

def main(argv):
    """ Calls the parse_args function based on the 
    command-line inputs and handles parsed arguments.
    """
    (opts, args) = parse_args(argv)
    output = sys.stdout
    # If the user specified an output log file, then use that.
    if opts.output_log is not None:
        output = open(opts.output_log, "w")
        
    # Handle parsed options.
    if opts.svn_log:
        log_file = opts.svn_log
        parser = parse_svn_log
    elif opts.git_log:
        log_file = opts.git_log
        parser = parse_svn_log
    elif opts.cvs_log:
        log_file = opts.cvs_log
        parser = parse_cvs_log
    elif opts.vss_log:
        log_file = opts.vss_log
        parser = parse_vss_log
    elif opts.starteam_log:
        log_file = opts.starteam_log
        parser = parse_starteam_log
    elif opts.wikiswarm_log:
        log_file = opts.wikiswarm_log
        parser = parse_wikiswarm_log
    elif opts.mercurial_log:
        log_file = opts.mercurial_log
        parser = parse_mercurial_log
    elif opts.gnu_log:
        log_file = opts.gnu_log
        parser = parse_gnu_changelog
    elif opts.perforce_path:
        #special case
        create_event_xml(parse_perforce_path(opts.perforce_path), output)
        return
    else:
        print >>stderr, "No repository format given, for more info see:\n   convert_logs.py --help"
        sys.exit(1)

    # check for valid cmd line arguments before doing anything
    if opts.ignore_author is not None:
        try:
            re.compile(opts.ignore_author)
        except sre_constants.error:
            print >>stderr, "Unable to compile author reg ex: %s" % \
                  opts.ignore_author
            sys.exit(1)
    
    if not os.path.exists(log_file):
        #hacky, but OptionParse doesn't support options that only sometimes
        # have an extra value
        if log_file == "stdin":
            log_file = sys.stdin
        else:
            print >>stderr, "Unable to find input log %s" % log_file
            sys.exit(1)
    else:
        log_file = open(log_file, 'r')

    events = parser(log_file, opts)

    # Remove all authors we wanted to ignore here
    if opts.ignore_author is not None:
        events = remove_ignored_author(opts.ignore_author, events)
        
    #its really best if we don't have to sort, but by default most are
    # in the reverse order that we want, so we sort by default
    if not opts.nosort:
        events= sorted(list(events))
    # Generate standard event xml file from event_list.
    create_event_xml(events, output)


def create_event_xml(events, output):
    """ Write out the final XML given an input iterator of events."""
    from xml.sax.saxutils import XMLGenerator
    from xml.sax.xmlreader import AttributesNSImpl
    
    generator = XMLGenerator(output, "utf-8")
    generator.startDocument()
    
    generator.startElementNS((None, 'file_events'), 'file_events', AttributesNSImpl({},{}))
    
    qnames = {(None, "date"):"date",
              (None, "filename"):"filename",
              (None, "author"):"author"}
    
    for event in events:
        generator.startElementNS((None, "event"), "event", AttributesNSImpl({
                (None,"date"):str(event.date),
                (None,"filename"):event.filename,
                (None,"author"):event.author
        }, qnames))
        
        generator.endElementNS((None, "event"), "event")
    
    generator.endElementNS((None, 'file_events'), 'file_events')
    generator.endDocument()

def parse_args(argv):
    """ Parses command line arguments and returns an options object
    along with any extra arguments.
    """
    p = OptionParser()

    p.add_option("-s", "--svn-log", dest="svn_log", 
        metavar="<log file>",
        help="input svn log to convert to standard event xml")

    p.add_option("-c", "--cvs-log", dest="cvs_log", 
        metavar="<log file>",
        help="input cvs log to convert to standard event xml")

    p.add_option("-g", "--git-log", dest="git_log", 
        metavar="<log file>",
        help="input git log to convert to standard event xml")

    p.add_option("-v", "--vss-log", dest="vss_log",
        metavar="<log file>",
        help="input vss report to convert to standard event xml")

    p.add_option("-t", "--starteam-log", dest="starteam_log",
        metavar="<log file>",
        help="input starteam log to convert to standard event xml")

    p.add_option("-w", "--wikiswarm-log", dest="wikiswarm_log", 
        metavar="<log file>",
        help="input wikiswarm log to convert to standard event xml")

    p.add_option("-m", "--mercurial-log", dest="mercurial_log", 
        metavar="<log file>",
        help="input mercurial log to convert to standard event xml")

    p.add_option("-l", "--gnu-changelog", dest="gnu_log",
        metavar="<log file>",
        help="input GNU Changelog to convert to standard event xml")

    p.add_option( "-p", "--perforce-path", dest="perforce_path",
        metavar="<log file>",
        help="get data from perforce and save it to standard event xml")
    
    p.add_option( "-o", "--output-log", dest="output_log", 
        metavar="<log file>",
        help="specify event log output file")
    
    p.add_option("--nosort", dest="nosort", default=False, action="store_true",
        help="use if the input log is already in chronological order for speed")

    p.add_option("--ignore-author", dest="ignore_author", default=None,
                action="store",
                help="Ignore authors that match this regular expression.")


    (options, args) = p.parse_args(argv)

    return (options, args)


def parse_svn_log(file_handle, opts):
    """Takes an iterator of lines in svn log -v format and
    yields a stream of Events"""
    # Iterate through log file lines to parse out the revision history, adding Event
    # entries to a list as we go.
    line = file_handle.readline()
    while len(line) > 0:
        # The SVN_SEP indicates a new revision history to parse.
        if line.startswith(SVN_SEP):
            # Extract author and date from revision line.  Here is a sample revision line:
            # r9 | michael.ogawa | 2008-06-19 10:23:25 -0500 (Thu, 19 Jun 2008) | 3 lines.
            rev_line = file_handle.readline()
            # The last line of the file is an SVN_SEP line, so if we try to retreive the
            # revision line and get an empty string, we know we are at the end of the file
            # and can break out of the loop.
            if rev_line == '' or len(rev_line) < 2:
                break
            rev_parts = rev_line.split(' | ')
            try:
                author = rev_parts[1]
            except IndexError:
                print >>sys.stderr, "Skipping bad line: %s" % rev_line
                line = file_handle.readline()
                continue
            date_parts = rev_parts[2].split(" ")
            date = date_parts[0] + " " + date_parts[1]
            try:
                date = time.strptime(date, '%Y-%m-%d %H:%M:%S')
            except ValueError:
                print >>sys.stderr, "Skipping malformed date: " + str(date)
                continue
            date = int(time.mktime(date))*1000

            # Skip the 'Changed paths:' line and start reading in the changed filenames.
            file_handle.readline()
            path = file_handle.readline()
            while len(path) > 1:
                ch_path = None
                if opts.svn_log:
                    ch_path = path[5:].split(" (from")[0].replace("\n", "")
                else:
                    # git uses quotes if filename contains unprintable characters
                    ch_path = path[2:].replace("\n", "").replace("\"", "")
                yield Event(ch_path, date, author)
                path = file_handle.readline()

        line = file_handle.readline()

def parse_cvs_log(file_handle, opts):
    filename = ""

    line = file_handle.readline()
    while len(line) > 0:
        # The CVS_SEP indicates a new revision history to parse.
        if line.startswith(CVS_SEP):
            #Read the revision number
            rev_line = file_handle.readline()

            # Extract author and date from revision line.
            rev_line = file_handle.readline()
            if(rev_line.lower().find("date:") == 0):
                rev_parts = rev_line.split(';  ')
                date_parts = rev_parts[0].split(": ")
                date = time.strptime(date_parts[1], '%Y/%m/%d %H:%M:%S')
                date = int(time.mktime(date))*1000
                author = rev_parts[1].split(": ")[1]
                yield Event(filename, date, author)

        line = file_handle.readline()
        if(str(line) == ""):
            break
        elif(line.lower().find("rcs file: ") >= 0):
            rev_line = line.split(": ")
            filename = rev_line[1].strip().split(',')[0]
    file_handle.close()

def parse_vss_log(file_handle, opts):
    filename = ""
    author = ""
    mdy = ""
    t = ""

    # The VSS report can have multiple lines for each entry, where the values
    # are continued within a column range.
    for line in file_handle.readlines():
        if line.startswith(' '):
            # Handle a continuation line
            filename += line[0:20].strip()
            author += line[22:31].strip()
        else:
            if mdy:
                filename = filename.replace("$", "")
                date = datetime.strptime(mdy + ' ' + t[:-1]+t[-1].upper()+'M', "%m/%d/%y %I:%M%p")
                date = int(time.mktime(date.timetuple())*1000)
                yield Event(filename, date, author.lower())
            filename = line[0:20].strip()
            author = line[21:31].strip()
            mdy = line[32:40].strip()
            t = line[42:48].strip()

def parse_starteam_log(file_handle, opts):
    folder = None
    filename = None

    # The Starteam log can have multiple lines for each entry
    for line in file_handle.readlines():                
        m = re.compile("^Folder: (\w*)  \(working dir: (.*)\)$").match(line)

        if m:
            folder = m.group(2)
            #print "parsing folder %s @ %s" % (m.group(1), folder)
            continue

        m = re.compile("^History for: (.*)$").match(line)

        if m:
            filename = m.group(1)
            #print "parsing file %s" % filename
            continue

        m = re.compile("^Author: (.*) Date: (.*) \w+$").match(line)

        if m:
            author = m.group(1)
            date = datetime.strptime(m.group(2), "%m/%d/%y %I:%M:%S %p")
            date = int(time.mktime(date.timetuple())*1000)

            yield Event(os.path.join(folder, filename), date, author)
            continue

def parse_wikiswarm_log(file_handle, opts):
    line = file_handle.readline()

    for rev_line in file_handle.readlines():
        if rev_line == '':
            continue

        rev_parts = rev_line.split('|')
        #rev_id    = rev_parts[0].strip()# Don't really need this, it's mainly for the ouputter
        filename  = rev_parts[1].strip()
        author    = rev_parts[2].strip()
        date      = rev_parts[3].strip()+'000' # Padd to convert seconds into milliseconds
        yield Event(filename, date, author)


def parse_mercurial_log(file_handle, opts):
    state = 0
    user = ''
    date = ''
    files = []
    for line in file_handle.readlines():
        if state == 0:
            author = line[:-1]
            state += 1
        elif state == 1:
            date = line[:line.find('.')] + '000'
            state += 1
        elif state == 2:
            files = line[:-1].split(' ')
            for filename in files:
                yield Event(filename, date, author.lower())
            state += 1
        elif state == 3:
            state = 0
        else:
            print >>stderr, 'Error: undifined state'

def parse_gnu_changelog(file_handle, opts):
    newdate_re = re.compile("(\d{4})\-(\d\d)\-(\d\d) (.*) (<|\()")
    olddate_re = re.compile("(\w{3} .* \d{4}) (.*) (<|\()")
    filename_re = re.compile("\*\s([\w\./\-_]*):?")
    for line in file_handle:
        # Newer, common date format for GNU Changelogs
        m = newdate_re.match(line)
        
        #Found a person and a date, now just have to know which files were modified
        if m:
            year  = m.group(1)
            month = m.group(2)
            day   = m.group(3)
            
            if(int(month) > 12): #Malformed date? Try to fix it.
                tmp = month;
                month = day;
                day = tmp;
            
            date = year+month+day
            date = time.strptime(date,"%Y%m%d")
            date = int(time.mktime(date))*1000

            author = m.group(4).strip()
            
            line = file_handle.next()
            #Now read lines as long as we find files to add to this person.
            while not line[0].isdigit() and not line[0].isalpha():
                n = filename_re.search(line)
                if n:
                    filename = n.group(1)
                    yield Event(filename,date,author)
                line = file_handle.next()
            continue
                
        #Try older date format
        m = olddate_re.match(line)
        
        if m:
            date = m.group(1)
            date = time.strptime(date) #Format string defaults to ctime(), which is what matched
            date = int(time.mktime(date))*1000
            
            author = m.group(2).strip()
            
            line = file_handle.next()
            #Now read lines as long as we find files to add to this person.
            while not line[0].isdigit() and not line[0].isalpha():
                n = filename_re.search(line)
                if n:
                    filename = n.group(1)
                    yield Event(filename,date,author)
                line = file_handle.next()
            continue

def parse_perforce_path(file_handle, opts):
    changelists = run_marshal('p4 -G changelists "' + opts.perforce_path + '"')
    file_key_re = re.compile("^depotFile")
    for changelist in changelists:
        files = run_marshal('p4 -G describe -s "' + changelist['change'] + '"')
        for fi in files:
            for key_name, file_name in fi.iteritems():
                if file_key_re.match(key_name):
                    yield Event(file_name, int(changelist['time'] + '000'), changelist['user'])


def remove_ignored_author(ignore, events):
    """ Remove the events that match the given ignore reg ex. """
    events = ifilter(lambda evt: re.match(ignore, evt.author) is None,
                    events)
    return events


def run_marshal(command):
    import marshal

    # run marshal.load in a loop
    results = []
    stream = os.popen(command,'rb')
    try:
        while 1:
            results.append(marshal.load(stream))
    except EOFError:
        pass
    stream.close()
    return results


# Main entry point.
if __name__ == "__main__":
    main(sys.argv)
