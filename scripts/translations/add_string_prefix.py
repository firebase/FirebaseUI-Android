# coding=UTF-8

import os
import re
import sys

from base_string_script import BaseStringScript

PREFIXED_NAME_START = 'name="fui_'
UNPREFIXED_NAME_START = 'name="'

class AddStringPrefixScript(BaseStringScript):

    def ProcessTag(self, line, type):
        joined = '\n'.join(line)

        if (UNPREFIXED_NAME_START in joined) and (PREFIXED_NAME_START not in joined):
            joined = joined.replace(UNPREFIXED_NAME_START, PREFIXED_NAME_START)
            return joined.split('\n')
        else:
            return line

if __name__ == '__main__':
    for file_name in sys.argv[1:]:
        asps = AddStringPrefixScript()
        asps.ProcessFile(file_name)

