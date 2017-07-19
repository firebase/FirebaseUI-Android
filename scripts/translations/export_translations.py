# coding=UTF-8

import os
import re
import sys

from base_string_script import BaseStringScript

PREFIXED_NAME_START = 'name="fui_'
UNPREFIXED_NAME_START = 'name="'

class ExportTranslationsScript(BaseStringScript):

    def ProcessTag(self, line, type):
        joined = '\n'.join(line)

        if PREFIXED_NAME_START in joined:
            joined = joined.replace(PREFIXED_NAME_START, UNPREFIXED_NAME_START)
            return joined.split('\n')
        else:
            return line

    def WriteFile(self, file_name, file_contents):
        # Override to just print the contents
        print file_contents

if __name__ == '__main__':
    ets = ExportTranslationsScript()
    ets.ProcessFile('auth/src/main/res/values/strings.xml')
