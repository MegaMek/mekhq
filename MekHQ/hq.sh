#!/bin/sh

# MegaMek -
# Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
#
#  This program is free software; you can redistribute it and/or modify it
#  under the terms of the GNU General Public License as published by the Free
#  Software Foundation; either version 2 of the License, or (at your option)
#  any later version.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
#  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
#  for more details.
#

# Define script constants.
cd $(dirname "${BASH_SOURCE[0]}")

# Try to find the executable for Java.
JAVA=/usr/bin/java
test -x "$JAVA_HOME/bin/java" && JAVA="$JAVA_HOME/bin/java"

$JAVA -Xmx1200m -jar MekHQ.jar
