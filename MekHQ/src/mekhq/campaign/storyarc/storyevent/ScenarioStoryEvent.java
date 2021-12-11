/*
 * StoryArc.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.storyarc.StoryEvent;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.UUID;

/**
 * Adds a scenario to the identified mission. Note that it will also create an id on the given scenario in campaign and
 * that scenario will trigger completeEvent upon completion.
 */
public class ScenarioStoryEvent extends StoryEvent implements Serializable, MekHqXmlSerializable {

    /** track the scenario itself **/
    private Scenario scenario;

    /** The UUID of the MissionStoryEvent that this ScenarioStoryEvent is a part of **/
    private UUID missionEventId;

    public ScenarioStoryEvent() {
        super();
    }

    @Override
    public void startEvent() {
        super.startEvent();
        StoryEvent missionEvent = getStoryArc().getStoryEvent(missionEventId);
        if(null != missionEvent && missionEvent instanceof MissionStoryEvent) {
            Mission m = ((MissionStoryEvent) missionEvent).getMission();
            if (null != m & null != scenario) {
                getStoryArc().getCampaign().addScenario(scenario, m);
            }
        }
    }

    private void setScenario(Scenario s) {
        s.setStoryEvent(this);
        this.scenario = s;
    }

    public Scenario getScenario() { return scenario; }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<missionEventId>"
                +missionEventId
                +"</missionEventId>");
        if(null != scenario) {
            //if the scenario has a valid id, then just save this because the scenario is saved
            //and loaded elsewhere so we need to link it
            if (scenario.getId() > 0) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<scenarioId>"
                        + scenario.getId()
                        + "</scenarioId>");
            } else {
                scenario.writeToXml(pw1, indent + 1);
            }
        }
        writeToXmlEnd(pw1, indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
        // Okay, now load mission-specific fields!
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    int scenarioId = Integer.parseInt(wn2.getTextContent().trim());
                    this.setScenario(c.getScenario(scenarioId));
                } else if (wn2.getNodeName().equalsIgnoreCase("scenario")) {
                    Scenario s = Scenario.generateInstanceFromXML(wn2, c, null);
                    if(null != s) {
                        this.setScenario(s);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("missionEventId")) {
                    missionEventId = UUID.fromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }
}
