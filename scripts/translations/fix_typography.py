# coding=UTF-8

import sys

from base_string_script import BaseStringScript

BAD_ELLIPSIS = "..."
GOOD_ELLIPSIS = "…"

BAD_ELLIPSIS_SPACING = " …"
GOOD_ELLIPSIS_SPACING = "…"

BAD_SINGLE_QUOTE = "\\\'%1$s\\\'"
BAD_DOUBLE_QUOTE = "\\\"%1$s\\\""
GOOD_DOUBLE_QUOTE = "“%1$s”"


class FixTypographyScript(BaseStringScript):

    def ProcessTag(self, oldLine, type):
        lineString = ''.join(oldLine) \
            .replace(BAD_ELLIPSIS, GOOD_ELLIPSIS) \
            .replace(BAD_ELLIPSIS_SPACING, GOOD_ELLIPSIS_SPACING) \
            .replace(BAD_DOUBLE_QUOTE, GOOD_DOUBLE_QUOTE) \
            .replace(BAD_SINGLE_QUOTE, GOOD_DOUBLE_QUOTE)
        newLine = lineString.split("(?!^)")

        minimizedWhitespaceLine = []
        for idx, char in enumerate(''.join(newLine)):
            if len(minimizedWhitespaceLine) < 1:
                minimizedWhitespaceLine.append(char)
                continue

            # Skip the char if we're adding whitespace in the middle of a string tag
            if minimizedWhitespaceLine[-1] == " " and char == " " and "<" in minimizedWhitespaceLine:
                pass
            else:
                minimizedWhitespaceLine.append(char)

        return ''.join(minimizedWhitespaceLine).split("(?!^)")


# Process all files
for file_name in sys.argv[1:]:
    rnts = FixTypographyScript()
    rnts.ProcessFile(file_name)
