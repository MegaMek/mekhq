/*
 * ChoiceStoryPoint.java
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
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.dialog.StoryChoiceDialog;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This StoryPoint creates a {@link StoryChoiceDialog StoryChoiceDialog} which offers the player
 * potentially more than one possible choice or response.
 */
public class ChoiceStoryPoint extends DialogStoryPoint {

    private String title;
    private String question;

    Map<String, String> choices;

    private String chosen;

    public ChoiceStoryPoint() {
        super();
        choices = new LinkedHashMap<>();
    }

    @Override
    public String getTitle() {
        return title;
    }

    public String getQuestion() {
        return question;
    }

    public Map<String, String> getChoices() {
        return choices;
    }

    @Override
    public void start() {
        super.start();
        final StoryChoiceDialog choiceDialog = new StoryChoiceDialog(null, this);
        choiceDialog.setVisible(true);
        chosen = choiceDialog.getChoice();
        complete();
    }

    @Override
    protected String getResult() {
        return chosen;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "title", title);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "question", question);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "chosen", chosen);
        for (Entry<String, String> entry : choices.entrySet()) {
            // FIXME: not sue how to do this with attribute using new XML writing methods
            pw1.println(MHQXMLUtility.indentStr(indent)
                    +"<choice id=\""
                    +entry.getKey()
                    +"\">"
                    +entry.getValue()
                    +"</choice>");
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        super.loadFieldsFromXmlNode(wn, c, version);
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    title =wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("question")) {
                    question =wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("choice")) {
                    String id = wn2.getAttributes().getNamedItem("id").getTextContent().trim();
                    String choice =wn2.getTextContent().trim();
                    choices.put(id, choice);
                } else if (wn2.getNodeName().equalsIgnoreCase("chosen")) {
                    chosen =wn2.getTextContent().trim();
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
