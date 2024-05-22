title: Award Guide

Awards
======
**Author:** *Phillip "[BullseyeSmith](mailto:52634333+BullseyeSmith@users.noreply.github.com)" Starke*

The standard award set provides a way to provide military honors to personnel for recognition of performance in battle, or as filler for character background stories. They are based on real awards and decorations of the US armed forces, which have progressed over a thousand years to be the most recognized awards of all Houses and Clans throughout the known universe. [Click here](https://en.wikipedia.org/wiki/Awards_and_decorations_of_the_United_States_Armed_Forces) to learn more about the awards and decorations of the United States Armed Forces.
________________
Associated Files
----------------
### XML File
MekHQ uses an XML file located in the `\data\universe\awards` folder to generate the list of available awards. Currently, the order of awards within the file determines both its position on the list of available awards, as well as the order they appear for everyone. Each award entry has the following tags:

1) `<name>` Required. The display text for the list of available awards.

2) `<description>` Required. A brief description of the requirements to earn the award. Will be displayed on message popup when you hover over the award name in the list of available awards.

3) `<medal>` Optional. If award has an associated medal, provide the name of the medal image file (i.e. 'PrisonerOfWarM.png'). Add an 'M' at the end on the name to distinguish it from the associated ribbon image file. These images will appear in the 'Medals and Awards' section of the Personnel tab.

4) `<ribbon>` Required. Provide the name of the ribbon image file (i.e. 'PrisonerOfWar.png). These images will appear beneath the individual's portrait of the Personnel tab.

5) `<xp>` Optional. If awards warrant XP points, insert integer value. This value will appear next to the award name in parentheses in the list of available awards.

6) `<edge>` Optional. If award warrants edge points, insert integer value. This value will appear next to the award name in parentheses in the list of available awards.

7) `<stackable>` Optional. If award can be received more than once, insert Boolean value of 'true'.
#### \******ALL FOLLOWING TAGS ARE NOT YET IMPLEMENTED (NYI), BUT NEED TO BE INCLUDED FOR FUTURE USE\****

8) `<precedence>` Required. Future versions will display list of available medals in alphabetical order while showing ribbons and medals based on the order of precedence set by this position tag. The 3-part number corresponds to the order of precedence sections, subsections, and award position as described on wikipedia.
##### The following qty, item, size, and range tags work in conjunction with one another to allow MekHQ to provide suggested award(s) during scenario resolution and attach appropriate awards during random origin generation.

9) `<qty>` Required. Positive whole number value.

10) `<item>` Required. String value with no space. Reserved words include: 3SW, 4SW, AndurienWar, Civilian, Clan, Combat, Conduct, Covert, CSI, Drill, Enlist, FCCW, Foreign, Gunnery, Hero, Honor, House, Instructor, Jihad, Kills, Periphery, Pirates, Prisoner, Rank, RoninWar, Security, TFS, Time, Top, W39, W57, Wounded.

11) `<size>` Required. List of string value with no space. List options include Individual, Lance, Company, Battalion.

12) `<range>` Required. List of string value with no space. List options include Career, Mission, Scenario, Event, Months.

### Ribbon Image Files
MekHQ uses the ribbon images located in the `\data\images\awards\{XML FILE NAME}\ribbons` folder to generate the ribbons that appear beneath an individual's portrait on the Personnel tab. Every award and medal must have a ribbon image. Ribbon images must be in .png format with a 100-pixel width x 27-pixel height. The file name must match the ribbon tag of the associated XML file award data.

There is also a `\docs\Award Stuff\README\img` folder with duplicate copies of the ribbon image files with shortened names that are used to generate images within this guide.

### Medal Image Files
MekHQ uses the medal images located in the `\data\images\awards\{XML FILE NAME}\medals` folder to generate the medals that appear within the 'Medals and Awards' section on the Personnel tab. Medal images must be in a transparent .png format with an 87-pixel height. The file name must match the medal tag of the associated XML file award data, if provided.
________________
Available Awards
----------------
These awards are listed in order of precedence in accordance with the guidance found on [wikipedia](https://en.wikipedia.org/wiki/Awards_and_decorations_of_the_United_States_Armed_Forces#Order_of_precedence_2).
In relation to kill-based awards, kills are defined as the destruction of an enemy unit or the crippling of an enemy unit. Examples are immobilization of vehicles, the total destruction of the leg, gyro, or engine of a mech, or the downing of an aerofighter.
##### Stackable: Personnel may receive award every time criteria is met.
##### Non-Stackable: Personnel may receive award only once in their career.
Awards cannot be recommended or presented during gameplay without an assigned Admin/HR personnel to process the recommendation documentation, an assigned Admin/Logistical personnel to procure the ribbons and decorations, and an assigned Admin/Command personnel to approve and present the award.

The automation for the Gameplay and New Personnel is NYI. In the interim, you can use the Award Tracker discussed in the last section of this document to assist you in awarding your existing and newly assigned personnel.

#### ![](../../data/images/awards/standard/ribbons/1-01-1-MedalOfHonor.png) MEDAL OF HONOR
**_Criteria:_** Distinguished oneself conspicuously by gallantry and intrepidity at the risk of life above and beyond the call of duty.

**_Benefits:_** 10 XP, 3 Edge. Stackable.

**_Gameplay:_** In real life, as of April 2020, there are 1.3 million members of the US Armed Forces on active duty. Only two of them have received the Medal of Honor. Only 3,500 people have received the MoH since the Civil War. Only 80 of them are still alive today. It is usually only awarded posthumously. Actions for consideration should involve being outnumbered by 10-to-1, suffering injuries, and rescuing an ejected mechwarrior while dropping four or more enemy combatants to enable victory conditions for the scenario. Then for further realism, roll 24 on 4D6 if they survived the engagement, or 18 on 3D6 if they died in combat to see if the decoration submission makes it through the government bureaucracy. The point is to use discretion and be respectful. Combat personnel only.

**_New Personnel:_** Roll a 36 with 6D6. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-02-2-CombatCross.png) COMBAT CROSS
**_Criteria:_** 12 pilot kills in a scenario.

**_Benefits:_** 7 XP. Stackable.

**_Gameplay:_** Recommended at end of scenario if pilot/driver has 12 kills.

**_New Personnel:_** Roll 3D6 for a target number of 16, +gunnery skill, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-03-1-HouseDistinguishedService.png) HOUSE DISTINGUISHED SERVICE
**_Criteria:_** 432 battalion kills in a mission.

**_Benefits:_** 6 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the battalion at end of mission if battalion has 432 kills.

**_New Personnel:_** Roll 3D6 for a target number of 19, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/1-03-4-DistinguishedService.png) DISTINGUISHED SERVICE
**_Criteria:_** 12 pilot kills in a mission.

**_Benefits:_** 3 XP. Stackable.

**_Gameplay:_** Recommended at end of mission if pilot/driver has 12 kills.

**_New Personnel:_** Roll 2D6 for a target number of 10, +gunnery skill, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-04-1-SilverStar.png) SILVER STAR
**_Criteria:_** 8 pilot kills in a scenario.

**_Benefits:_** 5 XP. Stackable.

**_Gameplay:_** Recommended at end of scenario if pilot/driver has 8 kills.

**_New Personnel:_** Roll 3D6 for a target number of 14, +gunnery skill, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-05-1-HouseSuperiorService.png) HOUSE SUPERIOR SERVICE
**_Criteria:_** 324 battalion kills in a mission.

**_Benefits:_** 5 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the battalion at end of mission if battalion has 324 kills.

**_New Personnel:_** Roll 3D6 for a target number of 17, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/1-05-2-LegionOfMerit.png) LEGION OF MERIT
**_Criteria:_** 144 company kills in a mission.

**_Benefits:_** 5 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the company at end of mission if company has 144 kills.

**_New Personnel:_** Roll 3D6 for a target number of 18, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/1-07-2-ArmedForces.png) ARMED FORCES
**_Criteria:_** 108 company kills in a mission.

**_Benefits:_** 4 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the company at end of mission if company has 108 kills.

**_New Personnel:_** Roll 3D6 for a target number of 16, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/1-08-1-BronzeStar.png) BRONZE STAR
**_Criteria:_** 4 pilot kills in a scenario.

**_Benefits:_** 3 XP. Stackable.

**_Gameplay:_** Recommended at end of scenario if pilot/driver has 4 kills.

**_New Personnel:_** Roll 3D6 for a target number of 12, +gunnery skill, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-09-1-PurpleHeart.png) PURPLE HEART
**_Criteria:_** Wounded in combat due to enemy fire.

**_Benefits:_** 1 XP. Stackable.

**_Gameplay:_** Recommended at end of scenario if individual was wounded during weapons attack or physical attack phase. Only award once per scenario.

**_New Personnel:_** Roll 3D6 for a target number of 19, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment, -1 if vehicle crew, -2 if infantry. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only. If they have a Purple Heart, roll 2D6. On a result of 3, they have a permanent injury (such as a prosthetic or noticeable scar). On a result of 2, they have a permanent disabling injury (which effects one or more of their skills).

#### ![](../../data/images/awards/standard/ribbons/1-10-1-HouseMeritoriousService.png) HOUSE MERITORIOUS SERVICE
**_Criteria:_** 48 lance kills in a mission.

**_Benefits:_** 4 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the lance at end of mission if lance has 48 kills.

**_New Personnel:_** Roll 2D6 for a target number of 14, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/1-10-2-MeritoriousService.png) MERITOURIOUS SERVICE
**_Criteria:_** 36 lance kills in a mission.

**_Benefits:_** 3 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the lance at end of mission if lance has 36 kills.

**_New Personnel:_** Roll 2D6 for a target number of 12, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/1-11-3-CombatCommendation.png) COMBAT COMMENDATION
**_Criteria:_** 9 pilot kills in a mission.

**_Benefits:_** 2 XP. Stackable.

**_Gameplay:_** Recommended at end of mission if pilot/driver has 9 kills.

**_New Personnel:_** Roll 2D6 for a target number of 8, +gunnery skill, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-12-3-CombatAchievement.png) COMBAT ACHIEVEMENT
**_Criteria:_** 6 pilot kills in a mission.

**_Benefits:_** 1 XP. Stackable.

**_Gameplay:_** Recommended at end of mission if pilot/driver has 6 kills.

**_New Personnel:_** Roll 2D6 for a target number of 6, +gunnery skill, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/1-13-1-CombatAction.png) COMBAT ACTION
**_Criteria:_** Involved in combat.

**_Benefits:_** 1 Edge. Non-Stackable.

**_Gameplay:_** Recommended at end of scenario if not previously awarded.

**_New Personnel:_** Must have Tour of Duty or Covert Ops as previous assignment, otherwise roll 10 or more on 2D6. Combat personnel only.

#### ![](../../data/images/awards/standard/ribbons/2-01-2-HouseUnitCitation.png) HOUSE UNIT CITATION
**_Criteria:_** 216 battalion kills in a mission.

**_Benefits:_** 4 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the battalion at end of mission if battalion has 216 kills.

**_New Personnel:_** Roll 3D6 for a target number of 15, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/2-01-7-CombatUnitCommendation.png) COMBAT UNIT COMMENDATION
**_Criteria:_** 72 company kills in a mission.

**_Benefits:_** 3 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the company at end of mission if company has 72 kills.

**_New Personnel:_** Roll 3D6 for a target number of 14, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/2-02-2-MeritoriousUnitCommendation.png) MERITOURIOUS UNIT COMMENDATION
**_Criteria:_** 24 lance kills in a mission.

**_Benefits:_** 2 XP. Stackable.

**_Gameplay:_** Recommended for all combat and support personnel within the lance at end of mission if lance has 24 kills.

**_New Personnel:_** Roll 2D6 for a target number of 10, -1 for each previous Tour of Duty assignment, -2 for each previous Covert Ops assignment. Must have Tour of Duty or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/3-01-1-PrisonerOfWar.png) PRISONER OF WAR
**_Criteria:_** Taken prisoner during combat.

**_Benefits:_** 1 XP, 1 Edge. Stackable.

**_Gameplay:_** Recommended at end of scenario if individual is MIA.

**_New Personnel:_** Roll 3D6 for 4 or less. Must have Paramilitary Service, Tour of Duty, or Covert Ops as previous assignment.

#### ![](../../data/images/awards/standard/ribbons/3-02-4-GoodConduct.png) GOOD CONDUCT
**_Criteria:_** Served 6 years with no disciplinary action.

**_Benefits:_** 3 XP, 1 Edge. Stackable.

**_Gameplay:_** Recommended on annual rolls, computed every sixth anniversary from date of Time In Service (TIS). Deny if individual received Article 15 or Letter of Reprimand during that time frame.

**_New Personnel:_** Roll 2D6 for every six years of service. On a result of 3 or more, award ribbon for that time frame.

#### ![](../../data/images/awards/standard/ribbons/3-02-8-SupportPersonOfTheYear.png) SUPPORT PERSON OF THE YEAR
**_Criteria:_** Awarded to the most outstanding non-combat member of the year.

**_Benefits:_** 5 XP, 1 Edge. Non-Stackable.

**_Gameplay:_** Recommended on annual rolls to choose a support personnel that was the most helpful for the force. Force size must be battalion or larger. Deny if individual received Article 15 or Letter of Reprimand during that time frame.

**_New Personnel:_** If the support personnel had a prior career, award on a roll of 30 on 5D6.

#### ![](../../data/images/awards/standard/ribbons/3-04-2-Expeditionary.png) EXPEDITIONARY
**_Criteria:_** Partook in combat on a world not covered by a war.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended after Guerrilla or Extraction Raid mission for all assigned personnel.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-05-1-HouseDefense.png) HOUSE DEFENSE
**_Criteria:_** Enlisted during a time of war (TOW) in defense of a house world.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of Cadre or Garrison Duty mission on employer-controlled planet for individuals with no prior assignments.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-05-2-GalacticWarOnPirating.png) GALACTIC WAR ON PIRATING
**_Criteria:_** Partook in combat operations against pirates.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if employed by major House against pirates.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-05-5-HumanitarianService.png) HUMANITARIAN SERVICE
**_Criteria:_** Defense of civilian person or structures.

**_Benefits:_** 2 Edge. Non-Stackable.

**_Gameplay:_** Recommended at end of victorious scenario that required the protection of civilians with no civilian casualties.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-07-4-PeripheryExpeditionary.png) PERIPHERY EXPEDITIONARY
**_Criteria:_** Partook in combat operations on a periphery world.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if employed by major House against periphery world.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-01-ThirdSuccessionWar.png) THIRD SUCCESSION WAR CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the 3rd Succession War under the employment of the Federated Suns, Lyran Commonwealth, Capellan Confederation, Draconis Combine, or Free Worlds League between 2866 and 3025.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-02-FourthSuccessionWar.png) FOURTH SUCCESSION WAR CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the 4th Succession War under the employment of the Federated Suns, Lyran Commonwealth, Capellan Confederation, Draconis Combine, or Free Worlds League between August 19, 3028 and 3030.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-03-AndurienWars.png) ANDURIEN WARS CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the Andurien Wars under the employment of the Free Worlds League, Duchy of Andurien, Magistracy of Canopus, or Capellan Confederation between September 3030 and 3040.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-04-RoninWars.png) RONIN WARS CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the Ronin Wars under the employment of the Free Rasalhague Republic or the Draconis Combine between March 13, 3034 and 3035.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-05-WarOf3039.png) WAR OF 3039 CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the War of 3039 under the employment of the Draconis Combine or Federated Commonwealth between April 15, 3039 and 3040.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-06-ClanInvasion.png) CLAN INVASION CAMPAIGN
**_Criteria:_** Partook in combat actions in support of Clan Invasion under the employment of the Draconis Combine, Federated Commonwealth, Free Rasalhague Republic, or a Crusader Clan between August 3049 and May 3052.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-07-WarOf3057.png) WAR OF 3057 CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the War of 3057 under the employment of the Capellan Confederation, Free Worlds League, Federated Suns, or Lyran Alliance between September 3057 and December 3057.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-08-TaskForceSerpent.png) TASK FORCE SERPENT CAMPAIGN
**_Criteria:_** Partook combat actions in support of Task Force Serpent under the employment of the Second Star League between May 1, 3059 and April 9, 3060.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-09-CapellanStIvesWar.png) CAPELLAN-ST. IVES CIVIL WAR CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the Capellan-St. Ives War under the employment of the Capellan Confederation or the St. Ives Compact between Septemper 3060 and June 10, 3063.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-10-FedComCivilWar.png) FEDCOM CIVIL WAR CAMPAIGN
**_Criteria:_** Partook in combat actions in support of FedCom Civil War.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/3-08-11-WoBJihad.png) WORD OF BLAKE JIHAD CAMPAIGN
**_Criteria:_** Partook in combat actions in support of the WoB Jihad between 3067 and 3081.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission was in direct support of the specified campaign.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-01-2-GalacticServiceDeployment.png) GALACTIC SERVICE DEPLOYMENT
**_Criteria:_** Served over 3 consecutive months in a foreign theatre.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if mission lasted 3 months or longer on a world outside of employer's territory.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-02-2-GalacticService.png) GALACTIC SERVICE
**_Criteria:_** Served a total of 12 months in foreign theatres.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if:

- mission lasted 12 months or longer on a world outside of employer's territory.

- personnel has multiple consecutive missions for the same employer lasting a total of 12 months or more on worlds outside of employer's territory.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-02-8-CovertOps.png) COVERT OPS
**_Criteria:_** Served on a covert operations team.

**_Benefits:_** 2 XP, 1 Edge. Non-Stackable.

**_Gameplay:_** Recommended on yearly or retirement roll if individual participated in one guerrilla campaign or two or more extraction raids.

**_New Personnel:_** Award if Covert Ops was a previous assignment.

#### ![](../../data/images/awards/standard/ribbons/4-03-1-Longevity.png) LONGEVITY
**_Criteria:_** Award for every 4 years of service.

**_Benefits:_** 2 XP. Stackable.

**_Gameplay:_** On annual rolls, computed every forth anniversary from date of TIS.

**_New Personnel:_** For every four years of service, award ribbon for that time frame.

#### ![](../../data/images/awards/standard/ribbons/4-04-5-DrillInstructor.png) DRILL INSTRUCTOR
**_Criteria:_** Served as basic training drill instructor.

**_Benefits:_** 2 XP, 1 Edge. Non-Stackable.

**_Gameplay:_** Recommended at end of Cadre Duty mission if individual has a veteran or elite skill level and served as commander of training lance consisting of a majority of individuals with a green skill level.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-04-6-MilitaryTrainingInstructor.png) MILITARY TRAINING INSTRUCTOR
**_Criteria:_** Served as military instructor.

**_Benefits:_** 1 XP, 1 Edge. Non-Stackable.

**_Gameplay:_** Recommended at end of non-Cadre Duty mission if individual has a veteran or elite skill level and served as commander of training lance consisting of a majority of individuals with a green skill level.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-05-1-CeremonialDuty.png) CEREMONIAL DUTY
**_Criteria:_** Served as honor guard.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at the end of a Garrison Duty mission on a faction's capital planet. Personnel that perform marriage or funeral ceremonies are also eligible.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-05-2-SecurityGuard.png) SECURITY GUARD
**_Criteria:_** Served as security guard.

**_Benefits:_** 1 XP, 1 Edge. Non-Stackable.

**_Gameplay:_** Recommended at end of Security Duty mission for all assigned combat personnel or combat personnel assigned to a non-HQ Defend role at the end of a non-Security Duty mission.

**_New Personnel:_** Consult the Assignment Award Tables within AwardTracker.xlsx

#### ![](../../data/images/awards/standard/ribbons/4-06-1-OfficerGraduate.png) OFFICER GRADUATE
**_Criteria:_** Graduate of OCS or equivalent.

**_Benefits:_** 4 XP, 1 Edge. Non-Stackable.

**_Gameplay:_** Requires status changed to "retired" for 4 years (plus travel time) as they attend OCS. Personnel cannot leave for training during a contract time period. They return with the rank rate of O1 or the field promotion rank they received prior to leaving for training, whichever is higher. Graduate receives +1 in tactics, strategy, or leadership (player's choice). Graduation skill modifiers: green returns with -2 to piloting and gunnery; regular returns with -1 to piloting and gunnery; veteran return with -1 to piloting or gunnery (player's choice); elite returns with -1 to piloting or gunnery (whichever is higher). Warrant Officers and personnel with a dropout education are ineligible.

**_New Personnel:_** Award if Rank Rate is O1 or higher or if personnel has Graduate w/ OCS education or higher.

#### ![](../../data/images/awards/standard/ribbons/4-06-2-WarrantGraduate.png) WARRANT OFFICER GRADUATE
**_Criteria:_** Graduate of warrant officer training.

**_Benefits:_** 3 XP. Non-Stackable.

**_Gameplay:_** Requires status changed to "retired" for 2 years (plus travel time) as they attend WOT. Personnel cannot leave for training during a contract time period. They return with the rank rate of WO1 or the field promotion rank they received prior to leaving for training, whichever is higher. Graduation skill modifiers: green returns with -1 to piloting and gunnery; regular returns with -1 to piloting or gunnery (player's choice); veteran return with -1 to piloting or gunnery (flip a coin); elite returns with -1 to piloting or gunnery (whichever is higher). Officers are ineligible. 

**_New Personnel:_** Award if Rank Rate is WO1 through WO10.

#### ![](../../data/images/awards/standard/ribbons/4-07-1-NCOGraduate.png) NCO ACADEMY GRADUATE
**_Criteria:_** Graduate of NCO academy.

**_Benefits:_** 2 XP. Non-Stackable.

**_Gameplay:_** Requires status changed to "retired" for 3 months (plus travel time) as they attend NCOA. Personnel cannot leave for training during a contract time period. They return with the rank rate of E10 or the field promotion rank they received prior to leaving for training, whichever is higher. Graduation skill modifiers: regular returns with -1 to piloting or gunnery (whichever is higher); veteran returns with +1 in tactics, strategy, or leadership (player's choice); elite returns with +1 in tactics, strategy, or leadership (see table below). Non-Ranked personnel, Enlisted with green skill level, Warrant Officers, and Officers are ineligible.

|Elite NCOA Skill|1D6|
|----------------|:-:|
|Tactics         |1-2|
|Strategy        |3-4|
|Leadership      |5-6|

**_New Personnel:_** Award if Rank Rate is E10 through E20.

#### ![](../../data/images/awards/standard/ribbons/4-08-1-BasicTraining.png) BASIC TRAINING
**_Criteria:_** Graduate of basic training.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended at end of mission if non-ranked individual was a green member of a training lance. Promote to second lowest enlisted rank and apply -1 to piloting or gunnery (whichever is higher) for Cadre mission. Promote to lowest enlisted rank for non-Cadre mission.

**_New Personnel:_** Award if Rank Rate is E1 or higher.

#### ![](../../data/images/awards/standard/ribbons/5-02-4-ExpertMarksmanship.png) EXPERT MARKMANSHIP
**_Criteria:_** Gunnery Skill of 1 or 0.

**_Benefits:_** 2 XP. Non-Stackable.

**_Gameplay:_** Recommended on annual rolls if Gunnery Skill is 1 or less and not already awarded.

**_New Personnel:_** Award if Gunnery Skill is 1 or less.

#### ![](../../data/images/awards/standard/ribbons/5-02-8-Marksmanship.png) MARKMANSHIP
**_Criteria:_** Gunnery Skill of 3 or 2.

**_Benefits:_** 1 XP. Non-Stackable.

**_Gameplay:_** Recommended on annual rolls if Gunnery Skill is 3 or less and not already awarded.

**_New Personnel:_** Award if Gunnery Skill is 3 or less.
_____________
Award Tracker
-------------
In the future, MekHQ will automatically calculate eligibilities for awards and present their recommendations at the appropriate times. Until then, I have created `\docs\Award Stuff\AwardTracker.xlsx` with a PowerPivot Table that can be used to import campaign info saved to `\docs\Award Stuff\CampaignDataForAwards.cpnx` to see what kill-based awards have been earned for individuals, lances, companies, and battalions, as long as your TO&E is arranged by battalions, companies, lances, and units. This requires Excel 2019 or newer to run, and your results may vary if you use any other software to run this.

It also includes a _RANDOM ORIGIN GENERATOR_ to quickly create back stories for new personnel as well as _ASSIGNMENT AWARD TABLES_ to determine appropriate awards based on their previous assignments. This will also be automated into MekHQ somewhere down the line.
