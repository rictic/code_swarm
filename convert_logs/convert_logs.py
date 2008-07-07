#!/usr/bin/python
# Standard library imports
from datetime import datetime
from optparse import OptionParser
import os
import sys
import time

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
                  
    p.add_option("-w", "--wikimedia-log", dest="wikimedia_log", 
                  metavar="<log file>",
                  help="input wikimedia log to convert to standard event xml")

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
    if opts.svn_log:
        log_file = opts.svn_log
        
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
                        ch_path = path[5:].split(" (from")[0].replace("\n", "")
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
        
    if opts.git_log:
        print "Not yet implemented."
        
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
                (event.filename, event.date, event.author))
        except:
                print "Error when writing this file: " + str(event)
    xml_handle.write('</file_events>\n')
    xml_handle.close()
    
if __name__ == "__main__":
    """ Main entry point."""
    main()


    
