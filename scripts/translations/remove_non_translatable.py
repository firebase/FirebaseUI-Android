# coding=UTF-8

import os
import re
import sys

from base_string_script import BaseStringScript

NON_TRANSLATABLE = 'translatable="false"'

class RemoveNonTranslatableScript(BaseStringScript):

  def ProcessTag(self, lines):
    if NON_TRANSLATABLE in '\n'.join(lines):
      return []
    else:
      return lines

# Process all files
for file_name in sys.argv[1:]:
  rnts = RemoveNonTranslatableScript()
  rnts.ProcessFile(file_name)