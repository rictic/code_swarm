#!/usr/bin/python
# Standard library imports
from datetime import datetime
from optparse import OptionParser
import os
import sys
import time
from xml.sax.saxutils import escape as h

# Some global variables
svn_sep = "------------------------------------------------------------------------"
cvs_sep = "----------------------------"

# Event to hold all of the separate events as we parse them from the logs.
class Event():
    filename = ""
    date = "0"
    author = "(no author)"
    
    def __init__(self, filename, date, author):
        self.filename = filename
        self.date = date
        self.author = author
        
    # Some version control system's logs are not in chronological order, so
    # this compare method will return a compare of the date attributes.
    def __cmp__(self, other):
        return cmp(self.date, other.date)
    
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

    p.add_option( "-o", "--output-log", dest="output_log", 
        metavar="<log file>",
        help="specify standard log output file")

    (options, args) = p.parse_args(argv)

    return (options, args)
    
    
def main():
    """ Calls the parse_args function based on the 
    command-line inputs and handles parsed arguments.
    """
    (opts, args) = parse_args(sys.argv)
    
    # Handle parsed options.
    if opts.svn_log or opts.git_log:
        # Grab the correct log file based on what was specified.
        log_file = opts.svn_log
        if opts.git_log:
            log_file = opts.git_log
        
        # Check to be sure the specified log path exists.
        if os.path.exists(log_file):
            # Iterate through log file lines to parse out the revision history, adding Event
            # entries to a list as we go.
            event_list = []
            file_handle = open(log_file,  'r')
            line = file_handle.readline()
            while len(line) > 0:
                # The svn_sep indicates a new revision history to parse.
                if line.startswith(svn_sep):
                    # Extract author and date from revision line.  Here is a sample revision line:
                    # r9 | michael.ogawa | 2008-06-19 10:23:25 -0500 (Thu, 19 Jun 2008) | 3 lines.
                    rev_line = file_handle.readline()
                    # The last line of the file is an svn_sep line, so if we try to retreive the
                    # revision line and get an empty string, we know we are at the end of the file
                    # and can break out of the loop.
                    if rev_line is '' or len(rev_line) < 2:
                        break;
                    rev_parts = rev_line.split(' | ')
                    author = rev_parts[1]
                    date_parts = rev_parts[2].split(" ")
                    date = date_parts[0] + " " + date_parts[1]
                    date = time.strptime(date, '%Y-%m-%d %H:%M:%S')
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
                        event_list.append(Event(ch_path, date, author))
                        path = file_handle.readline()
                    
                line = file_handle.readline()
            file_handle.close()
            
            # Generate standard event xml file from event_list.
            create_event_xml(event_list, log_file, opts.output_log)
        else:
            print "Please specify an existing path."
        
    if opts.cvs_log:
        log_file = opts.cvs_log
        
        # Check to be sure the specified log path exists.
        if os.path.exists(log_file):
            event_list = []
            filename = ""
           
            file_handle = open(log_file,  'r')
            line = file_handle.readline()
            while len(line) > 0:
                # The cvs_sep indicates a new revision history to parse.
                if line.startswith(cvs_sep):
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
                        event_list.append(Event(filename, date, author))
                        
                line = file_handle.readline()
                if(str(line) == ""):
                    break
                elif(line.lower().find("rcs file: ") >= 0):
                    rev_line = line.split(": ");
                    filename = rev_line[1].strip().split(',')[0]
            file_handle.close()
            
            # Generate standard event xml file from event_list.
            create_event_xml(event_list, log_file, opts.output_log)
        else:
            print "Please specify an existing path."
        
    if opts.vss_log:
        log_file = opts.vss_log
        
        if os.path.exists(log_file):
            event_list = []
            file_handle = open(log_file, 'r')
            
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
                        event_list.append(Event(filename, date, author.lower()))
                    filename = line[0:20].strip()
                    author = line[21:31].strip()
                    mdy = line[32:40].strip()
                    t = line[42:48].strip()
                    
            create_event_xml(event_list, log_file, opts.output_log)
        
    if opts.starteam_log:
        log_file = opts.starteam_log
        
        if os.path.exists(log_file):
            import re
            
            event_list = []
            file_handle = open(log_file, 'r')
            
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
                    
                    event_list.append(Event(os.path.join(folder, filename), date, author))
                    
                    #print "%s check in at %s" % (author, date)
                    continue
                
            create_event_xml(event_list, log_file, opts.output_log)

    if opts.wikiswarm_log:
        log_file = opts.wikiswarm_log
        
        # Check to be sure the specified log path exists.
        if os.path.exists(log_file):
            event_list = []
            file_handle = open(log_file, 'r')
            line = file_handle.readline()
            
            for rev_line in file_handle.readlines():
                if rev_line is '':
                    continue
                
                rev_parts = rev_line.split('|')
                #rev_id    = rev_parts[0].strip()# Don't really need this, it's mainly for the ouputter
                filename  = rev_parts[1].strip()
                author    = rev_parts[2].strip()
                date      = rev_parts[3].strip()+'000' # Padd to convert seconds into milliseconds
                event_list.append(Event(filename, date, author))
                continue
                
            # Generate standard event xml file from event_list.
            create_event_xml(event_list, log_file, opts.output_log)

    if opts.mercurial_log:
        # author: Stefan Scherfke
        # contact: stefan.scherfke at uni-oldenburg.de
        log_file = opts.mercurial_log
        
        if os.path.exists(log_file):
            event_list = []
            file_handle = open(log_file, 'r')
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
                        event_list.append(Event(filename, date, author.lower()))
                    state += 1
                elif state == 3:
                    state = 0
                else:
                    print 'Error: undifined state'

        create_event_xml(event_list, log_file, opts.output_log)
        
    if opts.wikimedia_log:
        print "Not yet implemented."
        
def create_event_xml(events, base_log, output_log=None):
    """ Write out the final XML output log file based on an input
    list of events and input log files.
    """
    # By default, the generated xml file will be the same name as the input log file
    # but with an '.xml' extension.
    log_file_path = os.path.abspath(base_log)
    dest_dir = os.path.dirname(log_file_path)
    log_file_base = os.path.basename(log_file_path)
    xml_filename = os.path.splitext(log_file_base)[0] + '.xml'
    xml_path = os.path.join(dest_dir, xml_filename)
            
    # If the user specified an output log file, then use that.
    if output_log:
        xml_path = output_log
            
    # Create new empty xml file.
    xml_handle = open(xml_path,  'w')
    xml_handle.write('<?xml version="1.0"?>\n')
    xml_handle.write('<file_events>\n')
    # Make sure the events are sorted in ascending order by date, then
    # write the events into the xml file.
    events.sort()
    for event in events:
        try:
            xml_handle.write('<event filename="%s" date="%s" author="%s" />\n' % \
                (h(event.filename), event.date, h(event.author)))
        except:
                print "Error when writing this file: " + str(event)
    xml_handle.write('</file_events>\n')
    xml_handle.close()
    
if __name__ == "__main__":
    """ Main entry point."""
    main()
   
