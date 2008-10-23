#! /usr/bin/env python

#sorts a code_swarm xml file
#assumes: one <event> element per line
#         the string "<event" is in each <event> line and only in <event> lines
#         the xml isn't pathological

import sys, re

try:
    xml_lines = open(sys.argv[1])
except Exception, e:
    xml_lines = sys.stdin

xml_lines = filter(lambda line: "<event" in line, xml_lines)

date_extractor = re.compile(r".*date\s*=\s*(\'|\")(\d+)(\'|\").*")
def get_date_value(line):
    return int(date_extractor.search(line).group(2))
xml_lines.sort(key=get_date_value)

print """<?xml version="1.0"?>
<file_events>"""
for line in xml_lines:
    print line
print """</file_events>"""