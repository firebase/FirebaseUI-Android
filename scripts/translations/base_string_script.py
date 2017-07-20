# coding=UTF-8

import os
import re
import sys

class BaseStringScript:

  # State
  STATE_SEARCHING='STATE_SEARCHING'
  STATE_IN_STR='STATE_IN_STR'
  STATE_IN_PLUR='STATE_IN_PLUR'

  # Tag types
  TYPE_STR='TYPE_STR'
  TYPE_PLUR='TYPE_PLUR'

  # String tag start/end
  START_STR = '<string'
  END_STR = '</string'

  # Plurals tag start/end
  START_PLUR='<plurals'
  END_PLUR = '</plurals'

  def ProcessTag(self, line, type):
    """
    Process a single string tag.
    
    :param line: an array of lines making a single string tag.
    :param type: the tag type, such as TYPE_STR or TYPE_PLUR
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

    state = self.STATE_SEARCHING
    curr_tag = []

    pending_process_type = None

    with open(file_name, 'r') as myfile:
      data = myfile.read()

      for line in data.split('\n'):
        # Searching for a new tag
        if state == self.STATE_SEARCHING:
          if self.START_STR in line:
            state = self.STATE_IN_STR
          elif self.START_PLUR in line:
            state = self.STATE_IN_PLUR
          else:
            lines.append(line)

        # Inside of a string tag
        if state == self.STATE_IN_STR:
          curr_tag.append(line)
          if self.END_STR in line:
            pending_process_type = self.TYPE_STR

        # Inside of a plurals tag
        if state == self.STATE_IN_PLUR:
          curr_tag.append(line)
          if self.END_PLUR in line:
            pending_process_type = self.TYPE_PLUR

        # Some processing needs doing
        if pending_process_type:
          # Do processing
          lines += self.ProcessTag(curr_tag, pending_process_type)

          # Reset processing state
          pending_process_type = None
          state = self.STATE_SEARCHING
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
