/*
 * CreateCharacterStoryEvent.java
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
package mekhq.campaign.storyarc.storyevent;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.storyarc.StoryEvent;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.dialog.CreateCharacterDialog;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.time.temporal.ChronoUnit;

public class CreateCharacterStoryEvent extends StoryEvent implements Serializable, MekHqXmlSerializable {

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


    public CreateCharacterStoryEvent() {
        super();
        firstname = "Bob";
        surname = "";
        bloodname = "";
        biography = "";
        commander = true;
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
        if(null == faction) {
            faction = getCampaign().getFaction();
        }
        Person p = new Person(getCampaign(), faction.getShortName());
        if(null != primaryRole) {
            p.setPrimaryRole(primaryRole);
        }
        p.setClanner(clan);
        if(p.isClanner() && null != phenotype) {
            p.setPhenotype(phenotype);
        }

        p.setCommander(commander);
        p.setGivenName(firstname);
        p.setSurname(surname);
        p.setBloodname(bloodname);
        p.setBiography(biography);
        p.setRank(rank);

        p.setBirthday(getCampaign().getLocalDate().minus(age, ChronoUnit.YEARS));

        return p;
    }

    @Override
    public void startEvent() {
        super.startEvent();
        Person person = createPerson();
        final CreateCharacterDialog personDialog = new CreateCharacterDialog(null, true, person, getCampaign(), xpPool, "Just a test Just a **test** Just a test Just a test Just a test Just a test Just a test Just a test", false);
        getCampaign().importPerson(person);
        personDialog.setVisible(true);
        completeEvent();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<xpPool>"
                +xpPool
                +"</xpPool>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
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
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }
}
