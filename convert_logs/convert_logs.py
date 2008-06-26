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
    command-line inputs and handles parsed arguments."""
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
                    date = datetime.strptime(date, '%Y-%m-%d %H:%M:%S')
                    date = int(time.mktime(date.timetuple()))*1000
                    
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
            # By default, the generated xml file will be the same name as the input log file
            # but with an '.xml' extension.
            log_file_path = os.path.abspath(log_file)
            dest_dir = os.path.dirname(log_file_path)
            log_file_base = os.path.basename(log_file_path)
            xml_filename = os.path.splitext(log_file_base)[0] + '.xml'
            xml_path = os.path.join(dest_dir, xml_filename)
            
            # If the user specified an output log file, then use that.
            if opts.output_log:
                xml_path = opts.output_log
            
            xml_handle = open(xml_path,  'w')
            xml_handle.write('<?xml version="1.0"?>\n')
            xml_handle.write('<file_events>\n')
            # Write in the events from the event_list.
            event_list.reverse()
            for event in event_list:
                xml_handle.write('<event filename="%s" date="%s" author="%s" />\n' % \
                    (event.filename, event.date, event.author))
            xml_handle.write('</file_events>\n')
            xml_handle.close()
        else:
            print "Please specify an existing path."
        
    if opts.cvs_log:
        log_file = opts.cvs_log
        
        # Check to be sure the specified log path exists.
        if os.path.exists(log_file):
            entry = []
            filename = ""
           
            file_handle = open(log_file,  'r')
            line = file_handle.readline()
            while line is not '':
            	tmp = {}
            	# The cvs_sep indicates a new revision history to parse.
            	if line.startswith(cvs_sep):
            		#Read the revision number
            		rev_line = file_handle.readline()
            		tmp['revision'] = rev_line.split("revision ")[-1].strip('\n')
            		if(tmp['revision'] == cvs_sep):
            			break

            		# Extract author and date from revision line.
            		rev_line = file_handle.readline()
            		if(rev_line.lower().find("date:") == 0):
            			rev_parts = rev_line.split(';  ')
            			date_parts = rev_parts[0].split(": ")
            			tmp['date'] = datetime.strptime(date_parts[1], '%Y/%m/%d %H:%M:%S')
            			tmp['date'] = int(time.mktime(tmp['date'].timetuple())*1000)
            			tmp['author'] = rev_parts[1].split(": ")[1]
                        tmp['filename'] = filename
                        entry.append(tmp)
                        
                line = file_handle.readline()
                if(str(line) == ""):
                	break
                elif(line.lower().find("rcs file: ") >= 0):
                    rev_line = line.split(": ");
                    filename = rev_line[1].strip('\n').split(',')[0]
                    
            # Generate standard event xml file from event_list.
            # By default, the generated xml file will be the same name as the input log file
            # but with an '.xml' extension.
            log_file_path = os.path.abspath(log_file)
            dest_dir = os.path.dirname(log_file_path)
            log_file_base = os.path.basename(log_file_path)
            xml_filename = os.path.splitext(log_file_base)[0] + '.xml'
            xml_path = os.path.join(dest_dir, xml_filename)
            
            # If the user specified an output log file, then use that.
            if opts.output_log:
                xml_path = opts.output_log
            
            xml_handle = open(xml_path,  'w')
            xml_handle.write('<?xml version="1.0"?>\n')
            xml_handle.write('<file_events>\n')
            for event in entry:
                try:
                	xml_handle.write("<event filename=\""+str(event['filename'])+"\" date=\""+str(event['date'])+"\" author=\""+str(event['author'])+"\" />\n")
                except:
                	print "error when writing file: "+str(event)
            xml_handle.write('</file_events>\n')
            xml_handle.close()
        else:
            print "Please specify an existing path."
        
    if opts.git_log:
        print "Not yet implemented."
        
    if opts.wikimedia_log:
        print "Not yet implemented."
    
if __name__ == "__main__":
    """ Main entry point."""
    main()


    
