# coding=UTF-8

import os
import re
import sys

# Constants
START_STR = '<string'
END_STR = '</string'

class BaseStringScript:

  def ProcessTag(self, line):
    return line

  def ProcessFile(self, file_name):
    lines = []

    in_tag = False
    curr_tag = []

    with open(file_name, 'r') as myfile:
      data = myfile.read()

      for line in data.split('\n'):
        if START_STR in line and END_STR in line:
          lines += self.ProcessTag([line])
        else:
          if START_STR in line:
            in_tag = True

          if in_tag:
            curr_tag.append(line)
          else:
            lines.append(line)

          if END_STR in line:
            in_tag = False
            lines += self.ProcessTag(curr_tag)
            curr_tag = []

    with open(file_name, 'w') as myfile:
      myfile.write('\n'.join(lines))
