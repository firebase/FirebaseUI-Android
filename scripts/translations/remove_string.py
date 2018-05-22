# coding=UTF-8
#
# Description:
# Removes a string resource from resource files.
#
# Usage:
# python remove_string.py [string_name] [file1] [file2] ...
#   string_name: the name of a string resource, like app_name

import os
import re
import sys

from base_string_script import BaseStringScript

# Get string to remove
QUERY = 'name="{}"'.format(sys.argv[1])

# List of iles
FILES = sys.argv[2:]

# Remove any tags with the wrong name
class RemoveStringScript(BaseStringScript):

  def ProcessTag(self, line, type):
    if QUERY in '\n'.join(line):
      return []
    else:
      return line

# Process all files
for file_name in FILES:
  rss = RemoveStringScript()
  rss.ProcessFile(file_name)
