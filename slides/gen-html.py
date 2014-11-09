#!/usr/bin/env python3

import os

for f in sorted(os.listdir(".")):
  if f.endswith(".png"):
    print('<img src="./slides/{}" width="80%" style="border: 1px solid #000000"><br><br>'.format(f))
