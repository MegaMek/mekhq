/*
 * StoryArcStub.java
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
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.enums.StoryLoadingType;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class just reads in a few fields from a Story Arc XML object. it is used to produce the story arc
 * selection dialog without having to load the full story arcs which depend on the Campaign object.
 */
public class StoryArcStub {

    private String title;
    private String details;
    private String description;

    /** Can this story arc be added to existing campaign or does it need to start fresh? **/
    private StoryLoadingType storyLoadingType;

    /** directory path to the initial campaign data for this StoryArc - can be null **/
    private String initCampaignPath;

    /** directory path to this story arc **/
    private String directoryPath;

    public StoryArcStub() {
        storyLoadingType = StoryLoadingType.BOTH;
    }

    private void setTitle(String t) {
        this.title = t;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDetails() {
        return details;
    }

    private void setDetails(String d) {
        this.details = d;
    }

    public String getDescription() {
        return this.description;
    }

    private void setDescription(String d) {
        this.description = d;
    }

    private void setStoryLoadingType(StoryLoadingType type) {
        this.storyLoadingType = type;
    }

    public StoryLoadingType getStoryLoadingType() {
        return storyLoadingType;
    }

    private void setInitCampaignPath(String s) {
        this.initCampaignPath =s;
    }

    public File getInitCampaignFile() {
        if(null == initCampaignPath) {
            return null;
        }
        return new File(initCampaignPath);
    }

    public void setDirectoryPath(String p) {
        this.directoryPath = p;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }


    public static @Nullable StoryArcStub parseFromXML(final NodeList nl, Campaign c) {
        final StoryArcStub storyArcStub = new StoryArcStub();
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                switch (wn.getNodeName()) {
                    case "title":
                        storyArcStub.setTitle(wn.getTextContent().trim());
                        break;
                    case "details":
                        storyArcStub.setDetails(wn.getTextContent().trim());
                        break;
                    case "description":
                        storyArcStub.setDescription(wn.getTextContent().trim());
                        break;
                    case "storyLoadingType":
                        storyArcStub.setStoryLoadingType(StoryLoadingType.valueOf(wn.getTextContent().trim()));
                        break;

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
            return null;
        }
        return storyArcStub;
    }

    public static @Nullable StoryArcStub parseFromFile(final @Nullable File file) {
        final Document xmlDoc;
        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MHQXMLUtility.newSafeDocumentBuilder().parse(is);
        } catch (Exception e) {
            LogManager.getLogger().error(e);
            return null;
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();

        return parseFromXML(element.getChildNodes(), null);
    }

    /**
     * @return a list of all the story arcs in the default and userdata folders
     */
    public static List<StoryArcStub> getStoryArcStubs(boolean startNew) {
        final List<StoryArcStub> stubs = loadStoryArcStubsFromDirectory(
                new File(MHQConstants.STORY_ARC_DIRECTORY), startNew);
        stubs.addAll(loadStoryArcStubsFromDirectory(
                new File(MHQConstants.USER_STORY_ARC_DIRECTORY), startNew));
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        stubs.sort((p0, p1) -> naturalOrderComparator.compare(p0.toString(), p1.toString()));
        return stubs;
    }

    private static List<StoryArcStub> loadStoryArcStubsFromDirectory(final @Nullable File directory,
                                                                     boolean startNew) {
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

        final List<StoryArcStub> storyArcStubs = new ArrayList<>();
        for (String arcDirectoryName : arcDirectories) {
            //find the expected items within this story arc directory
            final File storyArcFile = new File(directory.getPath() + '/' +  arcDirectoryName + '/' + MHQConstants.STORY_ARC_FILE);
            if (!storyArcFile.exists()) {
                continue;
            }
            final StoryArcStub storyArcStub = parseFromFile(storyArcFile);
            final File initCampaignFile = new File(directory.getPath() + '/' +  arcDirectoryName + '/' + MHQConstants.STORY_ARC_CAMPAIGN_FILE);
            if (storyArcStub != null) {
                storyArcStub.setDirectoryPath(directory.getPath() + '/' +  arcDirectoryName);
                if (initCampaignFile.exists()) {
                    storyArcStub.setInitCampaignPath(initCampaignFile.getPath());
                }
                if (startNew ? storyArcStub.getStoryLoadingType().canStartNew() : storyArcStub.getStoryLoadingType().canLoadExisting()) {
                    storyArcStubs.add(storyArcStub);
                }
            }
        }

        return storyArcStubs;
    }

    public StoryArc loadStoryArc(Campaign c) {
        String filePath = getDirectoryPath() + '/' + MHQConstants.STORY_ARC_FILE;
        StoryArc storyArc = StoryArc.parseFromFile(new File(filePath), c);
        if (null != storyArc) {
            storyArc.setDirectoryPath(getDirectoryPath());
            storyArc.setInitCampaignPath(initCampaignPath);
        }
        return storyArc;
    }

    // need this method for proper sorting of story arcs
    @Override
    public String toString() {
        return getTitle();
    }
}
