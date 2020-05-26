# MekHQ Story Arcs

The intent of story arcs is to create the ability for users to generate and play out "story arcs" within MekHQ that allow for a series o linked missions and scenarios possibly overlayed with a fictional narrative. The basic structure of the story arc system needs to be flexible enough to handle a variety of possible set-ups, that may include but not be limited to the following:

- A linear narrative in which the player progresses through a series of scenarios
- A branching narrative in which the player may be presented with different scenarios and missions depending on the outcome of prior scenarios and missions, as well as other decisions.
- The track system used in some official battletech products in which players will be able to choose from a list of "next missions" upon the completion of one mission.
- A heterogenous list of mission/contracts that players can decide to accept at any point if they meet certain requirements. Within missions, players may face a branching or linear set of scenarios. This structure could be used to provide players with a set of "mercenary contracts."

In addition, creators should be able to specify a variety of initial conditions for the story arc, such as:

- A starting set of units and/or personnel, and even a full TO&E.
- A set of customized campaign options.
- Starting supplies and rules about how new supplies are acquired.

## The StoryEvent class

The main engine of StoryArcs will be the StoryEvent class. This will be an abstract class that can handle several different kind of sotryline events. A StoryEvent is the basic building block of the potentially branching narrative. All StoryEvents will be stored in a hash by unique ids (or integer ids?).

The most important methods of a StoryEvent are the following:
- completeEvent: a method that will determine whether preconditions for completing this event have been met.
- getNextStoryEvent: a method that will provide the id of the next StoryEvent to trigger when this StoryEvent is complete. How that next StoryEvent gets determined will depend on the internals of the particular class.

Here are the concrete sub-classes of a StoryEvent that I am considering at the moment:

- StartMission: This StoryEvent will be used to start a new mission. This will trigger a GUI dialog to pop up for the user with a description of the mission and an optional image. Once the user presses ok (or accept if allowed to choose), the StoryEvent will progress to the first scenario via an AddScenario StoryEvent.
- AddScenario: This will add a scenario to an existing mission. In some cases, the user may be able to complete the scenario at their leisure. Other scenarios may provide a specific date and the user will not be allowed to progress time until the scenario is concluded. This is the most complex class as consideration will have to be given to scenario objectives and opfor generation. Once the scenario is completed, this should trigger completion of the AddScenario event which will typically either lead to another AddScenario or EndMission.
- EndMission: End the current mission. This will typically be triggered by the completion of a certain scenario. If the current mission still has scenarios active, then those will all be closed by the ending of the mission.
- StoryNarrative: this will be a simple storytelling device StoryEvent that when triggered displays a GUI dialog with some text and an optional image. The user then hits done to proceed to the next StoryElement.
- StoryChoice: Similar to the StoryNarrative above but the user has to make some kind of choice from a list of options and the choice may affect the next StoryElement as well.
- StoryTalk: Allows for an rpg-style dialog between characters who may be represented by portraits. This will need to have its own branching logic for how the conversation progresses due to dialog options chosen.


All StoryEvent classes would also have the ability to include boons such as equipment, new personnel, etc. or penalties such as the removal of personnel, destruction of equipment, etc. which can trigger depending on the logic of the StoryEvent.

## Saving Story Arcs

Story Arcs will be saved in an XML format. This XML format will be identical to that of a MekHQ save game in many regards. Starting units, TO&E, personnel, equipment, missions, and scenarios, etc. can all be recorded in this XML file just as they are in MekHQ. In addition there will be some top-level data for the Story Arc itself and then XML code for the StoryEvents. In addition, external files such as images, custom units, camos, etc. may need to be included and will have a data structure identical to that for MekHQ itself. An entire Story Arc will be distributed as a ZIP file and MekHQ will read the zip file directly without the need to extract it.

## Building Story arcs

For initial implementation, I will probably build some test cases in XML directly, but eventually we will want a dialog that allows people to create and edit Story Arcs. 

## Plan for Implementation

I fully subscribe to the KISS philosophy. My primary initial goal would be to get the basic structure set up by allowing for a simple branching mission and scenario structure with a StoryNarrative StoryEvent. The logic of these would be fairly simple and would basically branch on the success or failure of a single scenario (I may even go super simple on round one and only allow for a linear structure). Once this up and working, we could then add more complexity to the logic of how events are triggered as well as consider more complex narrative elements such as StoryChoice and StoryTalk. 
