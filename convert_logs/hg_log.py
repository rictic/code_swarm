# coding=utf8
"""
Get the log from a mercurial repository and save it in a code_swarm-readable
XML-File.

Most options are accessible via the command line, but within this script you can
define a list of user names that will be replace with other names, which might 
become usefull if you have two different names and want them to be merged.

@author: Stefan Scherfke
@contact: stefan.scherfke at uni-oldenburg.de
"""

from optparse import OptionParser
import os
import sys
import StringIO

import mercurial.dispatch

# Here you can define replacements for user usernames. That might be usefull
# if you were logged with two different names and want them to be merged.
replace = {
#	'oldname': 'newname'),
}

def parseArgs():
	"""
	Parse all command line options and arguments
	
	@return: a tuple with options and arguments
	"""
	parser = OptionParser(usage = '%prog [options] path/to/repo',
			description = 'Get the log from the specified mercurial repository'
					' and save it as a code_swarm-readable XML-file.')
	parser.add_option('-o', '--outfile', default = '../data/hglog.xml',
			action = 'store', dest = 'outfile',
			help = 'Path to the output XML-file, default: %default')
	parser.add_option('-p', '--person', default = False,
			action = 'store_false', dest = 'useUser',
			help = '''Use person as author:\n
					"Foo Bar <foo.bar@example.com" gets "Foo Bar"''')
	parser.add_option('-u', '--user', default = True,
			action = 'store_true', dest = 'useUser',
			help = '''Use user as author (default):\n 
					"Foo Bar <foo.bar@example.com>" gets "foo"''')
	(options, args) = parser.parse_args()
	if len(args) != 1:
		parser.error('No path to repository specified')
	return (options, args)

def getLog(repo, useUser):
	"""
	Call hg log -v (in a pythonic way) and save its output to a StringIO object,
	so we can use it like a file object.
	
	The log data is formatted this way::
		author
		date
		file1 file2 ... fileN
		
		author
		date
		file1 file2
		
		...
	
	@return: StringIO object with the log data
	"""	
	# We don't want the output on std streams
	output = StringIO.StringIO()
	errors = StringIO.StringIO()
	sys.stdout = output
	sys.stderr = errors
	
	# Change the current working directory to the repo
	oldCwd = os.getcwd()
	os.chdir(repo)
	try:
		args = ['log', '-v', '--template', '{author|%s}\n{date}\n{files}\n\n' \
				% 'user' if useUser else 'person']
		mercurial.dispatch.dispatch(args)
	finally:
		sys.stdout = sys.__stdout__
		sys.stderr = sys.__stderr__
		
	os.chdir(oldCwd)
	
	err = errors.getvalue()
	if len(err) > 0:
		print err
		exit(1)
	output.seek(0)
	return output
	
def writeLogToXml(log, filename):
	"""
	Write the log data into a file.
	
	@param log: The log data
	@type log:  StringIO
	@param filename: The output filename
	@type filename:  string
	"""	
	outfile = open(filename, 'w')
	outfile.write('<?xml version="1.0"?>\n')
	outfile.write('<file_events>\n')
	state = 0
	user = ''
	date = ''
	files = []
	for line in log:
		if state == 0:
			user = line[:-1]
			if user in replace:
				user = replace[user]
			state += 1
		elif state == 1:
			date = line[:line.find('.')]
			state += 1
		elif state == 2:
			files = line[:-1].split(' ')
			for file in files:
				outfile.write('\t<event filename="%s" date="%s000" author="%s" />\n' % (file, date, user))
			state += 1
		elif state == 3:
			state = 0
		else:
			print 'Error: undifined state'
	outfile.write('</file_events>\n\n')
	outfile.close()
	
if __name__ == '__main__':
	(options, args) = parseArgs()
	log = getLog(args[0], options.useUser)
	writeLogToXml(log, options.outfile)
