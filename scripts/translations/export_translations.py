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
ITEM_VALUE_PATTERN = re.compile(r'>([^<]*(?:<xliff:g[^>]*>[^<]*</xliff:g>[^<]*)*)</item>')

XML_ENTITIES = {'&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&apos;': "'"}
ESCAPE_PATTERN = re.compile(r'\\(u[0-9A-Fa-f]{4}|n|t|\'|")')


def _decode_entities(text):
    """Decode XML entities and Android escape sequences to get true visible length."""
    for entity, char in XML_ENTITIES.items():
        text = text.replace(entity, char)
    text = ESCAPE_PATTERN.sub('X', text)
    return text


def _extract_visible_text(value):
    """Strip XML tags and decode entities to get the visible text length."""
    text = XML_TAG_PATTERN.sub('', value).strip()
    return _decode_entities(text)


def _calculate_char_limit(english_text):
    """Return ~1.5x the English text length, rounded up to the nearest 5."""
    length = len(english_text)
    limit = math.ceil(length * 1.5)
    return int(math.ceil(limit / 5.0) * 5)


def _add_char_limit(text, value_pattern):
    """Add [CHAR_LIMIT=xxx] to translation_description if missing."""
    if 'translation_description=' not in text:
        return text
    if CHAR_LIMIT_PATTERN.search(text):
        return text
    match = value_pattern.search(text)
    if not match:
        return text
    visible = _extract_visible_text(match.group(1))
    if not visible:
        return text
    limit = _calculate_char_limit(visible)
    return TRANSLATION_DESC_PATTERN.sub(
        r'\1 [CHAR_LIMIT=%d]\2' % limit, text, count=1)


class ExportTranslationsScript(BaseStringScript):

    def ProcessTag(self, line, type):
        joined = '\n'.join(line)

        if PREFIXED_NAME_START in joined:
            joined = joined.replace(PREFIXED_NAME_START, UNPREFIXED_NAME_START)

        if type == self.TYPE_STR:
            joined = _add_char_limit(joined, STRING_VALUE_PATTERN)
        elif type == self.TYPE_PLUR:
            result_lines = []
            for l in joined.split('\n'):
                if '<item ' in l and '</item>' in l:
                    l = _add_char_limit(l, ITEM_VALUE_PATTERN)
                result_lines.append(l)
            joined = '\n'.join(result_lines)

        return joined.split('\n')

    def WriteFile(self, file_name, file_contents):
        print(file_contents)

if __name__ == '__main__':
    ets = ExportTranslationsScript()
    ets.ProcessFile('auth/src/main/res/values/strings.xml')
