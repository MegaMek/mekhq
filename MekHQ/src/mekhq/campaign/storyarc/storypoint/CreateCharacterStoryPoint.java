/*
 * CreateCharacterStoryPoint.java
 *
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import mekhq.MekHQ;
import mekhq.gui.dialog.CreateCharacterDialog.NameRestrictions;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.event.PersonNewEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.dialog.CreateCharacterDialog;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * This StoryPoint opens a {@link CreateCharacterDialog CreateCharacterDialog} which allows a player to create a new
 * character. Various initial values can be set, as well as an initial experience point pool. Additionally, the ability
 * to edit certain parts of the character can be restricted.
 */
public class CreateCharacterStoryPoint extends StoryPoint {

    /** how much XP does the player have to spend on the character **/
    int xpPool;

    /** Initial characteristics of the person we may want */
    private int rank;
    private int age;
    private PersonnelRole primaryRole;
    private String firstname;
    private String surname;
    private String bloodname;
    private String biography;
    private Faction faction;
    private boolean clan;
    private Phenotype phenotype;
    private boolean commander;
    private int edge;

    /**
     * The id of the person in the campaign. This will otherwise be set randomly. By setting it manually we can
     * reference it later.
     */
    private UUID personId;

    private String instructions;

    /**
     * variables that determine what background information can be edited
     */
    private boolean editOrigin;
    private boolean editBirthday;
    private boolean editGender;
    private boolean limitFaction;
    private NameRestrictions nameRestrictions;

    /** ids to assign person to unit and force **/
    private UUID assignedUnitId;
    private int assignedForceId;


    public CreateCharacterStoryPoint() {
        super();
        firstname = "Bob";
        surname = "";
        bloodname = "";
        biography = "";
        commander = true;
        clan = false;
        phenotype = Phenotype.NONE;
        primaryRole = PersonnelRole.MECHWARRIOR;

        editOrigin = false;
        editBirthday = false;
        limitFaction = false;
        nameRestrictions = NameRestrictions.NONE;

    }

    @Override
    public String getTitle() {
        return "temp";
    }

    @Override
    protected String getResult() {
        return null;
    }

    public Person createPerson() {
        if (null == faction) {
            faction = getCampaign().getFaction();
        }
        Person p = new Person(getCampaign(), faction.getShortName());
        if (null != primaryRole) {
            p.setPrimaryRole(getCampaign(), primaryRole);
        }
        p.setClanPersonnel(clan);
        if (p.isClanPersonnel() && null != phenotype) {
            p.setPhenotype(phenotype);
        }

        p.setCommander(commander);
        p.setGivenName(firstname);
        p.setSurname(surname);
        p.setBloodname(bloodname);
        p.setBiography(biography);
        p.setRank(rank);
        if (edge > 0) {
            p.changeEdge(edge);
            setEdgeTriggers(p);
        }

        if (null != personId) {
            p.setId(personId);
        }

        // We need to generate basic skills in order to get any phenotype bonuses
        // everything will be set to just produce the minimum proficiency in the role's
        // necessary skills
        RandomSkillPreferences skillPrefs = new RandomSkillPreferences();
        skillPrefs.setArtilleryProb(0);
        skillPrefs.setArtilleryBonus(-12);
        skillPrefs.setRandomizeSkill(false);
        skillPrefs.setAntiMekProb(0);
        skillPrefs.setSecondSkillProb(0);
        skillPrefs.setSecondSkillBonus(-12);
        skillPrefs.setTacticsMod(0,-12);
        skillPrefs.setSpecialAbilBonus(0, -12);
        skillPrefs.setOverallRecruitBonus(-12);
        skillPrefs.setCombatSmallArmsBonus(-12);
        skillPrefs.setSupportSmallArmsBonus(-12);
        AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(skillPrefs);
        skillGenerator.generateSkills(getCampaign(), p, SkillType.EXP_ULTRA_GREEN);

        p.setBirthday(getCampaign().getLocalDate().minusYears(age));

        return p;
    }

    @Override
    public void start() {
        super.start();
        Person person = createPerson();
        final CreateCharacterDialog personDialog = new CreateCharacterDialog(null, true, person,
                getCampaign(), xpPool, instructions, editOrigin, editBirthday, editGender, nameRestrictions,
                limitFaction);
        getCampaign().importPerson(person);
        personDialog.setVisible(true);
        if (null != assignedUnitId) {
            Unit u = getCampaign().getUnit(assignedUnitId);
            if (null != u && u.isUnmanned()) {
                u.addPilotOrSoldier(person, false);
                //only assign to force if properly assigned to a unit
                Force force = getCampaign().getForce(assignedForceId);
                if (null != force && null != person.getUnit()) {
                    getCampaign().addUnitToForce(u, force.getId());
                }
            }
        }
        MekHQ.triggerEvent(new PersonNewEvent(person));
        complete();
    }

    private void setEdgeTriggers(Person p) {
        //just check them all to be sure - no good way to separate these by primary role at the moment
        PersonnelOptions options = p.getOptions();

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.EDGE_ADVANTAGES)) {
                continue;
            }

            IOption option;
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                option = j.nextElement();
                if (null != option && option.getType() == IOption.BOOLEAN) {
                    p.setEdgeTrigger(option.getName(), true);
                }
            }
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "xpPool", xpPool);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "age", age);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "rank", rank);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "firstname", firstname);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "surname", surname);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "bloodname", bloodname);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "biography", biography);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "clan", clan);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "commander", commander);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "editOrigin", editOrigin);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "editBirthday", editBirthday);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "editGender", editGender);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "limitFaction", limitFaction);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "nameRestrictions", nameRestrictions.name());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "instructions", instructions);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "edge", edge);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "primaryRole", primaryRole.name());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "phenotype", phenotype.name());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "assignedForceId", assignedForceId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "assignedUnitId", assignedUnitId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "assignedUnitId", assignedUnitId);
        if (null != faction) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "faction", faction.getShortName());
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("xpPool")) {
                    xpPool = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("age")) {
                    age = Integer.parseInt(wn2.getTextContent().trim());
                }  else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                    rank = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("firstname")) {
                    firstname = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("surname")) {
                    surname = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("bloodname")) {
                    bloodname = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("biography")) {
                    biography = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                    clan = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("commander")) {
                    commander = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("primaryRole")) {
                    primaryRole = PersonnelRole.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("phenotype")) {
                    phenotype = Phenotype.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                    faction = Factions.getInstance().getFaction(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("editOrigin")) {
                    editOrigin = Boolean.parseBoolean(wn2.getTextContent().trim());
                }  else if (wn2.getNodeName().equalsIgnoreCase("editBirthday")) {
                    editBirthday = Boolean.parseBoolean(wn2.getTextContent().trim());
                }  else if (wn2.getNodeName().equalsIgnoreCase("editGender")) {
                    editGender = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("limitFaction")) {
                    limitFaction = Boolean.parseBoolean(wn2.getTextContent().trim());
                }  else if (wn2.getNodeName().equalsIgnoreCase("nameRestrictions")) {
                    nameRestrictions = NameRestrictions.valueOf(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("instructions")) {
                    instructions = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("assignedUnitId")) {
                    assignedUnitId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("assignedForceId")) {
                    assignedForceId = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("edge")) {
                    edge = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    personId = UUID.fromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
