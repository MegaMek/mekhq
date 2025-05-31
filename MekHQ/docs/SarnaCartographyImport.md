# How to import planet data from the Sarna Cartography excel files.

1. Take the "Systems" tab of the "Systems by Era"spreadsheet, and save it as a tab-separated file. Make sure to set the
   encoding to unicode, otherwise planet names won't match up very well.
2. Remove the top two rows of the document. It should be a "era names" row and a "percentage coverage" row.
3. Remove the "status" column
4. Remove the "distance (LY)" column
5. Scroll all the way to the bottom and remove the rows containing systems without coordinates.
   Save the file again.

Now, you can import it into MekHQ to update your planet list, by opening up any campaign, clicking
File → Import → Import Planets from a TSV file, and picking your file. It'll display a short report, with a much longer
report in the mekhq log file.

The expected format is as follows:

- The top row contains three empty "cells," followed by a list of all the years in which faction change events can occur
  for planets.
- Each row thereafter contains the following cells:
    - System Name
    - X coordinate
    - Y coordinate (with Terra as the "origin")
    - faction codes for each of the years defined in the top row. An empty value in one of these cells is treated as
      "undiscovered".

This will update your in-memory list of planets, although it will not update the GUI fully (you'll be able to search for
any new planets, but they won't show up).

To export the current state of the in-memory list of planets, click File → Export → Export Planets to XML File... and
pick a file name. You should be able to then use the resulting file as your planets.xml.
