# coding=UTF-8

import os
import re
import sys

# Constants
START_STR = '<string'
END_STR = '</string'

class BaseStringScript:

  def ProcessTag(self, line):
    """
    Process a single string tag.
    
    :param line: an array of lines making a single string tag.
    :return: an array of lines representing the processed tag.
    """
    return line

  def ProcessFile(self, file_name):
    """
    Process and write a file of string resources.
    
    :param file_name: path to the file to process.
    :return: None.
    """
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

    # Write back to the file
    self.WriteFile(file_name, '\n'.join(lines))

  def WriteFile(self, file_name, file_contents):
    """
    Overwrite the contents of a file.
    
    :param file_name: path to the file to write.
    :param file_contents: string containing new file contents. 
    :return: None
    """
    with open(file_name, 'w') as myfile:
      myfile.write(file_contents)
