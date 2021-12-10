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
package mekhq.campaign.storyarc;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.Campaign;
import org.w3c.dom.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FilenameFilter;
import java.util.*;

/**
 * The Story Arc class manages a given story arc campaign
 */
public class StoryArc implements MekHqXmlSerializable {

    private String title;
    private String description;

    private Campaign campaign;

    /** Can this story arc be added to existing campaign or does it need to start fresh? **/
    private boolean startNew;

    /** A UUID for the initial event in this track  - can be null **/
    private UUID startingEventId;

    /** A hash of all possible StoryEvents in this StoryArc, referenced by UUID **/
    private Map<UUID, StoryEvent> storyEvents;

    /** directory path to the initial campaign data for this StoryArc - can be null **/
    private String initCampaignPath;

    protected static final String NL = System.lineSeparator();

    public StoryArc() {
        startNew = true;
        storyEvents =  new LinkedHashMap<>();
    }

    public void setCampaign(Campaign c) { this.campaign = c; }

    public Campaign getCampaign() { return campaign; }

    private void setTitle(String t) { this.title = t; }

    public String getTitle() { return this.title; }

    public String getDescription() { return this.description; }

    private void setDescription(String d) { this.description = d; }

    private void setStartNew(Boolean b) { this.startNew = b; }

    private void setStartingEventId(UUID u) { this.startingEventId = u; }

    private UUID getStartingEventId() { return startingEventId; }

    private void setInitCampaignPath(String s) { this.initCampaignPath =s; }

    public File getInitCampaignFile() {
        if(null == initCampaignPath) {
            return null;
        }
        return new File(initCampaignPath);
    }

    public StoryEvent getStoryEvent(UUID id) {
        if (id == null) {
            return null;
        }
        return storyEvents.get(id);
    }

    public void begin() {
        getStoryEvent(getStartingEventId()).startEvent();
    }

    //region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        String level = MekHqXmlUtil.indentStr(indent),
                level1 = MekHqXmlUtil.indentStr(indent + 1);

        StringBuilder builder = new StringBuilder(256);
        builder.append(level)
                .append("<storyArc>")
                .append(NL)
                .append(level1)
                .append("<title>")
                .append(title)
                .append("</title>")
                .append(NL)
                .append(level1)
                .append("<description>")
                .append(description)
                .append("</description>")
                .append(NL)
                .append(level1)
                .append("<startNew>")
                .append(startNew)
                .append("</startNew>")
                .append(NL)
                .append(level1)
                .append("<startingEventId>")
                .append(startingEventId)
                .append("</startingEventId>")
                .append(NL);
        pw1.print(builder.toString());

        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<storyEvents>");
        for (Map.Entry<UUID, StoryEvent> entry : storyEvents.entrySet()) {
            entry.getValue().writeToXml(pw1, indent+2);
        }
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"</storyEvents>");


    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</storyArc>");
    }

    protected void parseStoryEvents(NodeList nl) {
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE ||
                        wn.getNodeName()!="storyEvent") {
                    continue;
                }
                StoryEvent event = StoryEvent.generateInstanceFromXML(wn, getCampaign());
                if(null != event) {
                    event.setStoryArc(this);
                    storyEvents.put(event.getId(), event);
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }

    public static @Nullable StoryArc parseFromXML(final NodeList nl) {
        final StoryArc storyArc = new StoryArc();
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                switch (wn.getNodeName()) {
                    case "title":
                        storyArc.setTitle(wn.getTextContent().trim());
                        break;
                    case "description":
                        storyArc.setDescription(wn.getTextContent().trim());
                        break;
                    case "startNew":
                        storyArc.setStartNew(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "startingEventId":
                        storyArc.setStartingEventId(UUID.fromString(wn.getTextContent().trim()));
                        break;
                    case "storyEvents":
                        storyArc.parseStoryEvents(wn.getChildNodes());
                        break;


                    default:
                        break;
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }
        return storyArc;
    }



    //endregion File I/O

    /**
     * @return a list of all of the story arcs in the default and userdata folders
     */
    public static List<StoryArc> getStoryArcs() {
        final List<StoryArc> presets = loadStoryArcsFromDirectory(
                new File(MekHqConstants.STORY_ARC_DIRECTORY));
        presets.addAll(loadStoryArcsFromDirectory(
                new File(MekHqConstants.USER_STORY_ARC_DIRECTORY)));
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        presets.sort((p0, p1) -> naturalOrderComparator.compare(p0.toString(), p1.toString()));
        return presets;
    }

    public static List<StoryArc> loadStoryArcsFromDirectory(final @Nullable File directory) {
        if ((directory == null) || !directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        //get all the story arc directory names
        String[] arcDirectories = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        final List<StoryArc> storyArcs = new ArrayList<>();
        for(String arcDirectoryName : arcDirectories) {
            //find the expected items within this story arc directory
            final File storyArcFile = new File(directory.getPath() + "/" +  arcDirectoryName + "/" + MekHqConstants.STORY_ARC_FILE);
            if(!storyArcFile.exists()) {
                continue;
            }
            final StoryArc storyArc = parseFromFile(storyArcFile);
            final File initCampaignFile = new File(directory.getPath() + "/" +  arcDirectoryName + "/" + MekHqConstants.STORY_ARC_CAMPAIGN_FILE);
            if (storyArc != null) {
                if(initCampaignFile.exists()) {
                    storyArc.setInitCampaignPath(initCampaignFile.getPath());
                }
                storyArcs.add(storyArc);
            }
        }

        return storyArcs;
    }

    public static @Nullable StoryArc parseFromFile(final @Nullable File file) {
        final Document xmlDoc;
        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MekHqXmlUtil.newSafeDocumentBuilder().parse(is);
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();

        return parseFromXML(element.getChildNodes());
    }

}
