/*
 * GamePreset.java
 *
 * Copyright (c) 2015 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;

import mekhq.Version;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.options.PilotOptions;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * This is an object which holds a set of objects that collectively define the game options
 * for a campaign. This includes the campaign options, the skill types, the random skill preferences,
 * and the special ability list. There will also be a short title and description here that allows users
 * to create and save different presets. The goal is to allow users to create and load various different
 * presets.
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class GamePreset implements MekHqXmlSerializable {
    private String title;
    private String description;
    private CampaignOptions options;
    private RandomSkillPreferences randomSkillPreferences;
    private Hashtable<String, SkillType> skillHash;
    private Hashtable<String, SpecialAbility> specialAbilities;

    //region Constructors
    public GamePreset() {
        this("Title missing", "Description missing", null, null, new Hashtable<>(), new Hashtable<>());
    }

    public GamePreset(String title, String description, CampaignOptions options,
                      RandomSkillPreferences randomSkillPreferences, Hashtable<String, SkillType> skillHash,
                      Hashtable<String, SpecialAbility> specialAbilities) {
        this.title = title;
        this.description = description;
        this.setOptions(options);
        this.setRandomSkillPreferences(randomSkillPreferences);
        this.setSkillHash(skillHash);
        this.setSpecialAbilities(specialAbilities);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String s) {
        title = s;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String s) {
        description = s;
    }

    public CampaignOptions getOptions() {
        return options;
    }

    public void setOptions(CampaignOptions options) {
        this.options = options;
    }

    public RandomSkillPreferences getRandomSkillPreferences() {
        return randomSkillPreferences;
    }

    public void setRandomSkillPreferences(RandomSkillPreferences randomSkillPreferences) {
        this.randomSkillPreferences = randomSkillPreferences;
    }

    public Hashtable<String, SkillType> getSkillHash() {
        return skillHash;
    }

    public void setSkillHash(Hashtable<String, SkillType> skillHash) {
        this.skillHash = skillHash;
    }

    public Hashtable<String, SpecialAbility> getSpecialAbilities() {
        return specialAbilities;
    }

    public void setSpecialAbilities(Hashtable<String, SpecialAbility> specialAbilities) {
        this.specialAbilities = specialAbilities;
    }

    public boolean isValid() {
        //could be used to disqualify bad presets
        return true;
    }

    @Override
    public void writeToXml(PrintWriter pw, int indent) {
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<gamePreset version=\"" + ResourceBundle.getBundle("mekhq.resources.MekHQ")
                .getString("Application.version") + "\">");
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "title", title);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "description", description);
        if (getOptions() != null) {
            getOptions().writeToXml(pw, indent);
        }

        if (getSkillHash() != null) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "skillTypes");
            for (String name : SkillType.skillList) {
                SkillType type = getSkillHash().get(name);
                if (type != null) {
                    type.writeToXml(pw, indent);
                }
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "skillTypes");
        }

        if (getSpecialAbilities() != null) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "specialAbilities");
            for (String key : getSpecialAbilities().keySet()) {
                getSpecialAbilities().get(key).writeToXml(pw, indent);
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "specialAbilities");
        }

        if (getRandomSkillPreferences() != null) {
            getRandomSkillPreferences().writeToXml(pw, --indent);
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, indent, "gamePreset");
    }

    public static GamePreset createGamePresetFromXMLFileInputStream(FileInputStream fis) throws DOMException {
        GamePreset preset = new GamePreset();

        Document xmlDoc;
        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
            return preset;
        }

        Element optionsEle = xmlDoc.getDocumentElement();
        NodeList nl = optionsEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        optionsEle.normalize();

        // Legacy Parsing method for any presets created before 0.47.11, as they did not include a version
        final String versionString = optionsEle.getAttribute("version");
        Version version = new Version(versionString.isBlank() ? "0.47.11" : versionString);

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            if (!wn.getParentNode().equals(optionsEle)) {
                continue;
            }
            int xc = wn.getNodeType();
            if (xc == Node.ELEMENT_NODE) {
                String xn = wn.getNodeName();
                if (xn.equalsIgnoreCase("title")) {
                    preset.title = wn.getTextContent();
                } else if (xn.equalsIgnoreCase("description")) {
                    preset.description = wn.getTextContent();
                } else if (xn.equalsIgnoreCase("campaignOptions")) {
                    preset.setOptions(CampaignOptions.generateCampaignOptionsFromXml(wn, version));
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    preset.setRandomSkillPreferences(RandomSkillPreferences.generateRandomSkillPreferencesFromXml(wn));
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                    NodeList wList = wn.getChildNodes();
                    // Okay, lets iterate through the children, eh?
                    for (int z = 0; z < wList.getLength(); z++) {
                        Node wn2 = wList.item(z);

                        // If it's not an element node, we ignore it.
                        if ((wn2.getNodeType() != Node.ELEMENT_NODE) || (wn2.getNodeName().startsWith("ability-"))) {
                            continue;
                        } else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().error("Unknown node type not loaded in Skill Type nodes: " + wn2.getNodeName());

                            continue;
                        }
                        SkillType.generateSeparateInstanceFromXML(wn2, preset.getSkillHash());
                    }
                } else if (xn.equalsIgnoreCase("specialAbilities")) {
                    PilotOptions options = new PilotOptions();
                    NodeList wList = wn.getChildNodes();
                    // Okay, lets iterate through the children, eh?
                    for (int z = 0; z < wList.getLength(); z++) {
                        Node wn2 = wList.item(z);
                        // If it's not an element node, we ignore it.
                        if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        if (!wn2.getNodeName().equalsIgnoreCase("ability")) {
                            MekHQ.getLogger().error("Unknown node type not loaded in Special Ability nodes: " + wn2.getNodeName());
                            continue;
                        }

                        SpecialAbility.generateSeparateInstanceFromXML(wn2, preset.getSpecialAbilities(), options);
                    }
                }
            }
        }
        return preset;
    }

    /**
     * Collect and load all the Game Presets in files in a given directory and
     * return a List of them
     * @return a list of all of the game presets in the given directory
     */
    public static List<GamePreset> getGamePresetsIn() {
        List<GamePreset> presets = new ArrayList<>();

        File[] files = Utilities.getAllFiles(MekHQ.PRESET_DIR, (dir, name) -> name.toLowerCase().endsWith(".xml"));
        for (File file : files) {
            // And then load the campaign object from it.
            GamePreset preset;

            try (FileInputStream fis = new FileInputStream(file)) {
                preset = GamePreset.createGamePresetFromXMLFileInputStream(fis);
                if (preset.isValid()) {
                    presets.add(preset);
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
        return presets;
    }
}
