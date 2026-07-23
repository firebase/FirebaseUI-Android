# coding=UTF-8

import math
import os
import re
import sys

from base_string_script import BaseStringScript

PREFIXED_NAME_START = 'name="fui_'
UNPREFIXED_NAME_START = 'name="'

CHAR_LIMIT_PATTERN = re.compile(r'\[CHAR_LIMIT=\d+\]')
TRANSLATION_DESC_PATTERN = re.compile(r'(translation_description="[^"]*?)(")')
XML_TAG_PATTERN = re.compile(r'<[^>]+>')
STRING_VALUE_PATTERN = re.compile(r'>([^<]*(?:<xliff:g[^>]*>[^<]*</xliff:g>[^<]*)*)</string>')


def _extract_visible_text(value):
    """Strip XML tags to get the visible text length."""
    return XML_TAG_PATTERN.sub('', value).strip()


def _calculate_char_limit(english_text):
    """Return ~1.5x the English text length, rounded up to the nearest 5."""
    length = len(english_text)
    limit = math.ceil(length * 1.5)
    return int(math.ceil(limit / 5.0) * 5)


class ExportTranslationsScript(BaseStringScript):

    def ProcessTag(self, line, type):
        joined = '\n'.join(line)

        if PREFIXED_NAME_START in joined:
            joined = joined.replace(PREFIXED_NAME_START, UNPREFIXED_NAME_START)

        if 'translation_description=' in joined and not CHAR_LIMIT_PATTERN.search(joined):
            match = STRING_VALUE_PATTERN.search(joined)
            if match:
                visible = _extract_visible_text(match.group(1))
                if visible:
                    limit = _calculate_char_limit(visible)
                    joined = TRANSLATION_DESC_PATTERN.sub(
                        r'\1 [CHAR_LIMIT=%d]\2' % limit, joined, count=1)

        return joined.split('\n')

    def WriteFile(self, file_name, file_contents):
        print(file_contents)

if __name__ == '__main__':
    ets = ExportTranslationsScript()
    ets.ProcessFile('auth/src/main/res/values/strings.xml')
