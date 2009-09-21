MekHQ
9/21/2009

INTRODUCTION
--------------
MekHQ is a desktop program for managing units for play in the game of MegaMek. 
Currently, MekHQ is in an early development phase and not all features will be fully 
functional.  

RUNNING MEKHQ
--------------
MekHQ requires the installation of Java 6 in order to run properly. In most operating 
systems, you can simply double-click on the MekHQ.jar file to start the program. 
You can also start it from the command line with the command:

java -jar MekHQ.jar

GETTING STARTED
----------------
There are a couple of ways to get started. You can build up your own force by using 
the "Purchase Units" and "Hire" items in the "Marketplace" menu. When all of your units
are ready, you can use the "Deploy Units" button to save these units to a MUL file that 
can be read in by MegaMek. After playing a game of MegaMek you can then reload these
entities into MekHQ with the "Retrieve Units" button. The units and pilots should then
be updated based on the damage they received in battle. You can also collect salvage by
reading in the salvage file that MegaMek produces in logs/salvage.mul after the completion
of your game.  

If you already have a MUL file containing your units and pilots that you would like to
start MekHQ with, you can also read this file in directly with the 
Manage > Load Units from a MUL file menu item.  Never use the "Retrieve Units" button to do
this as it will not read in new pilots.  There is an Example.mul file provided in this copy
of MekHQ that you can use to get some practice with making repairs and healing pilots. 
Hire some Techs and Doctors and start fixing things. In order to see what tasks need to 
be performed for each entity, just click on that entity and you will see a list of items 
in the Task display.  In order to do a task, you must also select a Tech from the Tech 
display.