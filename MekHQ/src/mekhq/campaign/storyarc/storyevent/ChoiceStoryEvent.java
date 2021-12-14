/*
 * ChoiceStoryEvent.java
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
import mekhq.campaign.storyarc.StoryEvent;
import mekhq.gui.dialog.StoryChoiceDialog;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChoiceStoryEvent extends StoryEvent implements Serializable, MekHqXmlSerializable {

    private String title;
    private String question;

    Map<String, String> choices;

    private String chosen;

    public ChoiceStoryEvent() {
        super();
        choices = new LinkedHashMap<>();
    }

    @Override
    public String getTitle() { return title; }

    public String getQuestion() { return question; }

    public Map<String, String> getChoices() {
        return choices;
    }

    @Override
    public void startEvent() {
        super.startEvent();
        final StoryChoiceDialog choiceDialog = new StoryChoiceDialog(null, this);
        choiceDialog.setVisible(true);
        chosen = choiceDialog.getChoice();
        completeEvent();
    }

    @Override
    protected String getResult() {
        return chosen;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<title>"
                +title
                +"</title>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<question>"
                +question
                +"</question>");
        if(null != chosen) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    +"<chosen>"
                    +chosen
                    +"</chosen>");
        }
        for (Map.Entry<String, String> entry : choices.entrySet()) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    +"<choice id=\""
                    +entry.getKey()
                    +"\">"
                    +entry.getValue()
                    +"</choice>");
        }
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
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
                MekHQ.getLogger().error(e);
            }
        }
    }
}
