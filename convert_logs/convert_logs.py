#!/usr/bin/python
# Standard library imports
from optparse import OptionParser
import os
import sys

def parse_args(argv):
    """ Parses command line arguments and returns an options object
    along with any extra arguments.
    """
    p = OptionParser()
    
    p.add_option("-s", "--svn-log", dest="svn_log", 
                  metavar="<log file>",
                  help="input svn xml log to convert to standard event xml")

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
        print "Not yet implemented."
        
    if opts.cvs_log:
        print "Not yet implemented."
        
    if opts.git_log:
        print "Not yet implemented."
        
    if opts.wikimedia_log:
        print "Not yet implemented."
    
if __name__ == "__main__":
    """ Main entry point."""
    main()
    