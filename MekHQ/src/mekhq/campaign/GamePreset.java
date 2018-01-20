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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.logging.LogLevel;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.NullEntityException;
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

	String title;
	String description;
	CampaignOptions options;
	RandomSkillPreferences rskillPrefs;
    Hashtable<String, SkillType> skillHash;
    Hashtable<String, SpecialAbility> specialAbilities;


    public GamePreset() {
    	title = "Title missing";
    	description = "Description missing";
    	options = null;
    	rskillPrefs = null;
    	skillHash = new Hashtable<String, SkillType>();
    	specialAbilities = new Hashtable<String, SpecialAbility>();
    }

    public GamePreset(String t, String d, CampaignOptions o, RandomSkillPreferences r, Hashtable<String, SkillType> sk, Hashtable<String, SpecialAbility> sp) {
    	title = t;
    	description = d;
    	options = o;
    	rskillPrefs = r;
    	skillHash = sk;
    	specialAbilities = sp;
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

    public void apply(Campaign campaign) {
    	if(null != options) {
    		campaign.setCampaignOptions(options);
    	}
    	if(null != rskillPrefs) {
    		campaign.setRandomSkillPreferences(rskillPrefs);
    	}
    	if(null != skillHash) {
    		SkillType.setSkillTypes(skillHash);
    	}
    	if(null != specialAbilities) {
    		SpecialAbility.setSpecialAbilities(specialAbilities);
    	}
    }

    public boolean isValid() {
    	//could be used to disqualify bad presets
    	return true;
    }

	public void writeToXml(PrintWriter pw, int indent) {
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<gamePreset>");
        pw.println("\t<title>"+ MekHqXmlUtil.escape(title) + "</title>");
        pw.println("\t<description>" + MekHqXmlUtil.escape(description) + "</description>");
        if(null != options) {
        	options.writeToXml(pw,1);
        }
        if(null != skillHash) {
	        pw.println("\t<skillTypes>");
	        for (String name : SkillType.skillList) {
	            SkillType type = skillHash.get(name);
	            if (null != type) {
	                type.writeToXml(pw, 2);
	            }
	        }
	        pw.println("\t</skillTypes>");
        }
        if(null != specialAbilities) {
        	pw.println("\t<specialAbilities>");
	        for(String key : specialAbilities.keySet()) {
	        	specialAbilities.get(key).writeToXml(pw, 2);
	        }
	        pw.println("\t</specialAbilities>");
        }
        if(null != rskillPrefs) {
        	rskillPrefs.writeToXml(pw, 1);
        }
        pw.println("</gamePreset>");
	}

	public static GamePreset createGamePresetFromXMLFileInputStream(
            FileInputStream fis) throws DOMException, ParseException,
                                        NullEntityException {
	    final String METHOD_NAME = "createGamePresetFromXMLFileInputStream(FileInputStream)"; //$NON-NLS-1$

		GamePreset preset = new GamePreset();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;
        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().log(GamePreset.class, METHOD_NAME, ex);
            return preset;
        }

        Element optionsEle = xmlDoc.getDocumentElement();
        NodeList nl = optionsEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        optionsEle.normalize();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            if (wn.getParentNode() != optionsEle) {
                continue;
            }
            int xc = wn.getNodeType();
            if (xc == Node.ELEMENT_NODE) {
                String xn = wn.getNodeName();
                if (xn.equalsIgnoreCase("title")) {
                    preset.title = wn.getTextContent();
                } else if (xn.equalsIgnoreCase("description")) {
                    preset.description = wn.getTextContent();
                }
                else if (xn.equalsIgnoreCase("campaignOptions")) {
                    preset.options = CampaignOptions.generateCampaignOptionsFromXml(wn);
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    preset.rskillPrefs = RandomSkillPreferences.generateRandomSkillPreferencesFromXml(wn);
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                	NodeList wList = wn.getChildNodes();
                    // Okay, lets iterate through the children, eh?
                    for (int z = 0; z < wList.getLength(); z++) {
                        Node wn2 = wList.item(z);

                        // If it's not an element node, we ignore it.
                        if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        if (wn2.getNodeName().startsWith("ability-")) {
                            continue;
                        } else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().log(GamePreset.class, METHOD_NAME, LogLevel.ERROR,
                                    "Unknown node type not loaded in Skill Type nodes: " //$NON-NLS-1$
                                             + wn2.getNodeName());

                            continue;
                        }
                        SkillType.generateSeparateInstanceFromXML(wn2, preset.skillHash);
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
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().log(GamePreset.class, METHOD_NAME, LogLevel.ERROR,
                                    "Unknown node type not loaded in Special Ability nodes: " //$NON-NLS-1$
                                             + wn2.getNodeName());
                            continue;
                        }

                        SpecialAbility.generateSeparateInstanceFromXML(wn2, preset.specialAbilities, options);
                    }
                }
            }
        }
		return preset;
	}

	/**
	 * Collect and load all the Game Presets in files in a given directory and
	 * return an ArrayList of them
	 * @param directory
	 * @return
	 */
	public static ArrayList<GamePreset> getGamePresetsIn(String directory) {

		ArrayList<GamePreset> presets = new ArrayList<GamePreset>();

		File[] files = Utilities.getAllFiles(MekHQ.PRESET_DIR, new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        });
    	for(File file : files) {
    		// And then load the campaign object from it.
    		FileInputStream fis = null;
    		GamePreset preset = null;

    		try {
    			fis = new FileInputStream(file);
    			preset = GamePreset.createGamePresetFromXMLFileInputStream(fis);
    			if(preset.isValid()) {
    				presets.add(preset);
    			}
    			fis.close();
    		} catch (Exception ex) {
    			ex.printStackTrace();
    			/*	JOptionPane.showMessageDialog(null,
					"The campaign file could not be loaded.\nPlease check the log file for details.",
					"Campaign Loading Error",
				    JOptionPane.ERROR_MESSAGE);*/
    		}
    	}
    	return presets;
	}



}
