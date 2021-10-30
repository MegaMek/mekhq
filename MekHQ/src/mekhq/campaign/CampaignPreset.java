/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.utils.MegaMekXmlUtil;
import mekhq.MekHQ;
import mekhq.MekHQOptions;
import mekhq.MekHqConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Systems;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * This is an object which holds a set of objects that collectively define the initial options setup
 * for a campaign.
 *
 * It includes both startup values, which are only used on initial startup (the date, starting
 * planet, and rank system), and continuous options, which can be applied at any time (campaign
 * options, skills, SPAs).
 *
 * It also includes a short title and description that allows one to create and save different
 * presets. The goal is to allow users to create and load various different presets.
 * @author Justin "Windchild" Bowen
 */
public class CampaignPreset implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 7753055687319002688L;

    private final boolean userData;

    private String title;
    private String description;

    // Startup
    private LocalDate date;
    private Faction faction;
    private Planet planet;
    private RankSystem rankSystem;
    private int contractCount;

    // Continuous
    private GameOptions gameOptions;
    private CampaignOptions campaignOptions;
    private RandomSkillPreferences randomSkillPreferences;
    private Hashtable<String, SkillType> skills;
    private Hashtable<String, SpecialAbility> specialAbilities;
    //endregion Variable Declarations

    //region Constructors
    public CampaignPreset() {
        this(false);
    }

    public CampaignPreset(final boolean userData) {
        this("Title", "", userData, null, null, null, null,
                2, null, null, null,
                new Hashtable<>(), new Hashtable<>());
    }

    public CampaignPreset(final Campaign campaign) {
        this(campaign.getName(), "", true, campaign.getLocalDate(), campaign.getFaction(),
                campaign.getCurrentSystem().getPrimaryPlanet(), campaign.getRankSystem(), 2,
                campaign.getGameOptions(), campaign.getCampaignOptions(),
                campaign.getRandomSkillPreferences(), SkillType.getSkillHash(),
                SpecialAbility.getAllSpecialAbilities());
    }

    public CampaignPreset(final String title, final String description, final boolean userData,
                          final @Nullable LocalDate date, final @Nullable Faction faction,
                          final @Nullable Planet planet, final @Nullable RankSystem rankSystem,
                          final int contractCount, final @Nullable GameOptions gameOptions,
                          final @Nullable CampaignOptions campaignOptions,
                          final @Nullable RandomSkillPreferences randomSkillPreferences,
                          final Hashtable<String, SkillType> skills,
                          final Hashtable<String, SpecialAbility> specialAbilities) {
        this.userData = userData;

        setTitle(title);
        setDescription(description);

        // Startup
        setDate(date);
        setFaction(faction);
        setPlanet(planet);
        setRankSystem(rankSystem);
        setContractCount(contractCount);

        // Continuous
        setGameOptions(gameOptions);
        setCampaignOptions(campaignOptions);
        setRandomSkillPreferences(randomSkillPreferences);
        setSkills(skills);
        setSpecialAbilities(specialAbilities);
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean isUserData() {
        return userData;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    //region Startup
    public @Nullable LocalDate getDate() {
        return date;
    }

    public void setDate(final @Nullable LocalDate date) {
        this.date = date;
    }

    public @Nullable Faction getFaction() {
        return faction;
    }

    public void setFaction(final @Nullable Faction faction) {
        this.faction = faction;
    }

    public @Nullable Planet getPlanet() {
        return planet;
    }

    public void setPlanet(final @Nullable Planet planet) {
        this.planet = planet;
    }

    public @Nullable RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(final @Nullable RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }

    public int getContractCount() {
        return contractCount;
    }

    public void setContractCount(final int contractCount) {
        this.contractCount = contractCount;
    }
    //endregion Startup

    //region Continuous
    public @Nullable GameOptions getGameOptions() {
        return gameOptions;
    }

    public void setGameOptions(final @Nullable GameOptions gameOptions) {
        this.gameOptions = gameOptions;
    }

    public @Nullable CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public void setCampaignOptions(final @Nullable CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;
    }

    public @Nullable RandomSkillPreferences getRandomSkillPreferences() {
        return randomSkillPreferences;
    }

    public void setRandomSkillPreferences(final @Nullable RandomSkillPreferences randomSkillPreferences) {
        this.randomSkillPreferences = randomSkillPreferences;
    }

    public Hashtable<String, SkillType> getSkills() {
        return skills;
    }

    public void setSkills(final Hashtable<String, SkillType> skills) {
        this.skills = skills;
    }

    public Hashtable<String, SpecialAbility> getSpecialAbilities() {
        return specialAbilities;
    }

    public void setSpecialAbilities(final Hashtable<String, SpecialAbility> specialAbilities) {
        this.specialAbilities = specialAbilities;
    }
    //endregion Continuous
    //endregion Getters/Setters

    /**
     * @return a list of all of the campaign presets in the default and userdata folders
     */
    public static List<CampaignPreset> getCampaignPresets() {
        final List<CampaignPreset> presets = loadCampaignPresetsFromDirectory(
                new File(MekHqConstants.CAMPAIGN_PRESET_DIRECTORY));
        presets.addAll(loadCampaignPresetsFromDirectory(
                new File(MekHqConstants.USER_CAMPAIGN_PRESET_DIRECTORY)));
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        presets.sort((p0, p1) -> naturalOrderComparator.compare(p0.toString(), p1.toString()));
        return presets;
    }

    public void applyContinuousToCampaign(final Campaign campaign) {
        if (getGameOptions() != null) {
            campaign.setGameOptions(getGameOptions());
            if (getCampaignOptions() == null) {
                campaign.updateCampaignOptionsFromGameOptions();
            }
        }

        if (getCampaignOptions() != null) {
            campaign.setCampaignOptions(getCampaignOptions());
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign, getCampaignOptions()));
        }

        if (getRandomSkillPreferences() != null) {
            campaign.setRandomSkillPreferences(getRandomSkillPreferences());
        }

        if (!getSkills().isEmpty()) {
            SkillType.setSkillHash(getSkills());
        }

        if (!getSpecialAbilities().isEmpty()) {
            SpecialAbility.setSpecialAbilities(getSpecialAbilities());
        }
    }

    //region File I/O
    public void writeToFile(final JFrame frame, @Nullable File file) {
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }
        try (OutputStream fos = new FileOutputStream(file);
             OutputStream bos = new BufferedOutputStream(fos);
             OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            writeToXML(pw, 0);
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign", new EncodeControl());
            JOptionPane.showMessageDialog(frame, resources.getString("CampaignPresetSaveFailure.text"),
                    resources.getString("CampaignPresetSaveFailure.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        MegaMekXmlUtil.writeSimpleXMLOpenTag(pw, indent++, "campaignPreset", "version", MekHQOptions.VERSION);
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "title", toString());
        if (!getDescription().isBlank()) {
            MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "description", getDescription());
        }

        //region Startup
        if (getDate() != null) {
            MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "date", getDate());
        }

        if (getFaction() != null) {
            MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "faction", getFaction().getShortName());
        }

        if (getPlanet() != null) {
            MekHqXmlUtil.writeSimpleXMLAttributedTag(pw, indent, "planet", "system",
                    getPlanet().getParentSystem().getId(), getPlanet().getId());
        }

        if (getRankSystem() != null) {
            getRankSystem().writeToXML(pw, indent, false);
        }
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "contractCount", getContractCount());
        //endregion Startup

        //region Continuous
        if (getGameOptions() != null) {
            getGameOptions().writeToXML(pw, indent);
        }

        if (getCampaignOptions() != null) {
            getCampaignOptions().writeToXml(pw, indent);
        }

        if (getRandomSkillPreferences() != null) {
            getRandomSkillPreferences().writeToXml(pw, indent);
        }

        if (!getSkills().isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenTag(pw, indent++, "skillTypes");
            for (final String name : SkillType.skillList) {
                final SkillType type = getSkills().get(name);
                if (type != null) {
                    type.writeToXml(pw, indent);
                }
            }
            MekHqXmlUtil.writeSimpleXMLCloseTag(pw, --indent, "skillTypes");
        }

        if (!getSpecialAbilities().isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenTag(pw, indent++, "specialAbilities");
            for (final String key : getSpecialAbilities().keySet()) {
                getSpecialAbilities().get(key).writeToXml(pw, indent);
            }
            MekHqXmlUtil.writeSimpleXMLCloseTag(pw, --indent, "specialAbilities");
        }
        //endregion Continuous
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw, --indent, "campaignPreset");
    }

    public static List<CampaignPreset> loadCampaignPresetsFromDirectory(final @Nullable File directory) {
        if ((directory == null) || !directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        final List<CampaignPreset> presets = new ArrayList<>();
        for (final File file : Objects.requireNonNull(directory.listFiles())) {
            final CampaignPreset preset = parseFromFile(file);
            if (preset != null) {
                presets.add(preset);
            }
        }

        return presets;
    }

    public static @Nullable CampaignPreset parseFromFile(final @Nullable File file) {
        final Document xmlDoc;
        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MekHqXmlUtil.newSafeDocumentBuilder().parse(is);
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();

        if (!element.getNodeName().equalsIgnoreCase("gamePreset") // Legacy - 0.49.3 removal
                && !element.getNodeName().equalsIgnoreCase("campaignPreset")) {
            return null;
        }

        // Legacy Parsing method for any presets created before 0.47.11, as they did not include a version
        final String versionString = element.getAttribute("version");
        final Version version = new Version(versionString.isBlank() ? "0.47.11" : versionString);
        return parseFromXML(element.getChildNodes(), version);
    }

    public static @Nullable CampaignPreset parseFromXML(final NodeList nl, final Version version) {
        final CampaignPreset preset = new CampaignPreset();
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                switch (wn.getNodeName()) {
                    case "title":
                        preset.setTitle(wn.getTextContent().trim());
                        break;
                    case "description":
                        preset.setDescription(wn.getTextContent().trim());
                        break;

                    //region Startup
                    case "date":
                        preset.setDate(MekHqXmlUtil.parseDate(wn.getTextContent().trim()));
                        break;
                    case "faction":
                        preset.setFaction(Factions.getInstance().getFaction(wn.getTextContent().trim()));
                        break;
                    case "planet":
                        preset.setPlanet(Systems.getInstance()
                                .getSystemById(wn.getAttributes().getNamedItem("system").getTextContent().trim())
                                .getPlanetById(wn.getTextContent().trim()));
                        break;
                    case "rankSystem":
                        preset.setRankSystem(RankSystem.generateInstanceFromXML(wn.getChildNodes(), version));
                        break;
                    case "contractCount":
                        preset.setContractCount(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    //endregion Startup

                    //region Continuous
                    case "gameOptions":
                        preset.setGameOptions(new GameOptions());
                        preset.getGameOptions().fillFromXML(wn.getChildNodes());
                        break;
                    case "campaignOptions":
                        preset.setCampaignOptions(CampaignOptions.generateCampaignOptionsFromXml(wn, version));
                        break;
                    case "randomSkillPreferences":
                        preset.setRandomSkillPreferences(RandomSkillPreferences.generateRandomSkillPreferencesFromXml(wn, version));
                        break;
                    case "skillTypes": {
                        final NodeList nl2 = wn.getChildNodes();
                        for (int y = 0; y < nl2.getLength(); y++) {
                            final Node wn2 = nl2.item(y);
                            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            } else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
                                MekHQ.getLogger().error("Unknown node type not loaded in Skill Type nodes: " + wn2.getNodeName());
                                continue;
                            }
                            SkillType.generateSeparateInstanceFromXML(wn2, preset.getSkills());
                        }
                        break;
                    }
                    case "specialAbilities": {
                        final PersonnelOptions options = new PersonnelOptions();
                        final NodeList nl2 = wn.getChildNodes();
                        for (int y = 0; y < nl2.getLength(); y++) {
                            final Node wn2 = nl2.item(y);
                            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            } else if (!wn2.getNodeName().equalsIgnoreCase("ability")) {
                                MekHQ.getLogger().error("Unknown node type not loaded in Special Ability nodes: " + wn2.getNodeName());
                                continue;
                            }
                            SpecialAbility.generateSeparateInstanceFromXML(wn2, preset.getSpecialAbilities(), options);
                        }
                        break;
                    }
                    //end Continuous

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }
        return preset;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return title;
    }
}
