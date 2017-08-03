# coding=UTF-8

import os
import re
import sys

from base_string_script import BaseStringScript

NON_TRANSLATABLE = 'translatable="false"'

class RemoveNonTranslatableScript(BaseStringScript):

  def ProcessTag(self, line, type):
    # Ignore non-strings
    if type != BaseStringScript.TYPE_STR:
      return line

    if NON_TRANSLATABLE in '\n'.join(line):
      return []
    else:
      return line

# Process all files
for file_name in sys.argv[1:]:
  rnts = RemoveNonTranslatableScript()
  rnts.ProcessFile(file_name)
