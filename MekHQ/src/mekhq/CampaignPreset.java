/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq;

import static megamek.SuiteConstants.LAST_MILESTONE;
import static mekhq.MHQConstants.CAMPAIGN_PRESET_DIRECTORY;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import megamek.SuiteConstants;
import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsMarshaller;
import mekhq.campaign.campaignOptions.CampaignOptionsUnmarshaller;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is an object which holds a set of objects that collectively define the initial options setup for a campaign.
 * <p>
 * It includes both startup values, which are only used on initial startup (the date, starting planet, and rank system),
 * and continuous options, which can be applied at any time (campaign options, skills, SPAs).
 * <p>
 * It also includes a short title and description that allows one to create and save different presets. The goal is to
 * allow users to create and load various different presets.
 *
 * @author Justin "Windchild" Bowen
 */
public class CampaignPreset {

    /**
     * Represents the last version we accept presets from. Normally, this will just be the last milestone. Sometimes,
     * however, major changes can require us to manually assign a specific version.
     *
     * @see SuiteConstants#LAST_MILESTONE
     */
    private static final Version LAST_COMPATIBLE_VERSION = LAST_MILESTONE;

    private static final MMLogger LOGGER = MMLogger.create(CampaignPreset.class);

    // region Variable Declarations
    private final boolean userData;

    private String title;
    private String description;

    // Startup
    private LocalDate date;
    private Faction faction;
    private Planet planet;
    private RankSystem rankSystem;
    private int contractCount;
    private boolean gm;
    private CompanyGenerationOptions companyGenerationOptions;

    // Continuous
    private GameOptions gameOptions;
    private CampaignOptions campaignOptions;
    private RandomSkillPreferences randomSkillPreferences;
    private Map<String, SkillType> skills;
    private Map<String, SpecialAbility> specialAbilities;
    // endregion Variable Declarations

    // region Constructors
    public CampaignPreset() {
        this(false);
    }

    public CampaignPreset(final boolean userData) {
        this("Title",
              "",
              userData,
              null,
              null,
              null,
              null,
              2,
              true,
              null,
              null,
              null,
              null,
              new HashMap<>(),
              new HashMap<>());
    }

    public CampaignPreset(final String title, final String description, final boolean userData,
          final @Nullable LocalDate date, final @Nullable Faction faction, final @Nullable Planet planet,
          final @Nullable RankSystem rankSystem, final int contractCount, final boolean gm,
          final @Nullable CompanyGenerationOptions companyGenerationOptions, final @Nullable GameOptions gameOptions,
          final @Nullable CampaignOptions campaignOptions,
          final @Nullable RandomSkillPreferences randomSkillPreferences, final Map<String, SkillType> skills,
          final Map<String, SpecialAbility> specialAbilities) {
        this.userData = userData;

        setTitle(title);
        setDescription(description);

        // Startup
        setDate(date);
        setFaction(faction);
        setPlanet(planet);
        setRankSystem(rankSystem);
        setContractCount(contractCount);
        setGM(gm);
        setCompanyGenerationOptions(companyGenerationOptions);

        // Continuous
        setGameOptions(gameOptions);
        setCampaignOptions(campaignOptions);
        setRandomSkillPreferences(randomSkillPreferences);
        setSkills(skills);
        setSpecialAbilities(specialAbilities);
    }
    // endregion Constructors

    // region Getters/Setters
    public boolean isUserData() {
        return userData;
    }

    /**
     * @return the title of the {@link CampaignPreset}
     */
    public String getTitle() {
        return title;
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

    // region Startup
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

    public boolean isGM() {
        return gm;
    }

    public void setGM(final boolean gm) {
        this.gm = gm;
    }

    public CompanyGenerationOptions getCompanyGenerationOptions() {
        return companyGenerationOptions;
    }

    public void setCompanyGenerationOptions(final @Nullable CompanyGenerationOptions companyGenerationOptions) {
        this.companyGenerationOptions = companyGenerationOptions;
    }
    // endregion Startup

    // region Continuous
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

    public Map<String, SkillType> getSkills() {
        return skills;
    }

    public void setSkills(final Map<String, SkillType> skills) {
        this.skills = skills;
    }

    public Map<String, SpecialAbility> getSpecialAbilities() {
        return specialAbilities;
    }

    public void setSpecialAbilities(final Map<String, SpecialAbility> specialAbilities) {
        this.specialAbilities = specialAbilities;
    }
    // endregion Continuous
    // endregion Getters/Setters

    /**
     * Retrieves a combined list of all campaign presets from the default directory, the old user data directory, and
     * the modern (user-defined) user data directory. The campaign presets are sourced, merged, and sorted in natural
     * order before being returned.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *   <li>Loads campaign presets from the default campaign presets directory.</li>
     *   <li>Loads campaign presets from the old user-specific campaign presets directory.</li>
     *   <li>Loads campaign presets from the modern user-specific campaign presets directory,
     *       typically defined by the user's current preferences.</li>
     *   <li>Combines all presets into a single list.</li>
     *   <li>Sorts the combined list of campaign presets in natural order.</li>
     * </ul>
     *
     * @return a {@link List} of all combined and sorted campaign presets found in the default and user data folders.
     */
    public static List<CampaignPreset> getCampaignPresets() {
        // Main directory
        final List<CampaignPreset> presets = loadCampaignPresetsFromDirectory(new File(MHQConstants.CAMPAIGN_PRESET_DIRECTORY));

        // Old user data directory
        presets.addAll(loadCampaignPresetsFromDirectory(new File(MHQConstants.USER_CAMPAIGN_PRESET_DIRECTORY)));

        // Modern user data directory
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        File presetUserDirectory = new File(userDirectory + '/' + CAMPAIGN_PRESET_DIRECTORY);
        presets.addAll(loadCampaignPresetsFromDirectory(presetUserDirectory));

        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();

        presets.sort((p0, p1) -> naturalOrderComparator.compare(p0.toString(), p1.toString()));

        return presets;
    }

    // region File I/O
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
        } catch (Exception ex) {
            LOGGER.error("writeToFile() Exception", ex);
            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
                  MekHQ.getMHQOptions().getLocale());
            JOptionPane.showMessageDialog(frame,
                  resources.getString("CampaignPresetSaveFailure.text"),
                  resources.getString("CampaignPresetSaveFailure.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        MMXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "campaignPreset", "version", MHQConstants.VERSION);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "title", toString());
        if (!getDescription().isBlank()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "description", getDescription());
        }

        // region Startup
        if (getDate() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "date", getDate());
        }

        if (getFaction() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "faction", getFaction().getShortName());
        }

        if (getPlanet() != null) {
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw,
                  indent,
                  "planet",
                  "system",
                  getPlanet().getParentSystem().getId(),
                  getPlanet().getId());
        }

        if (getRankSystem() != null) {
            getRankSystem().writeToXML(pw, indent, false);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractCount", getContractCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gm", isGM());
        if (getCompanyGenerationOptions() != null) {
            getCompanyGenerationOptions().writeToXML(pw, indent, null);
        }
        // endregion Startup

        // region Continuous
        if (getGameOptions() != null) {
            getGameOptions().writeToXML(pw, indent);
        }

        if (getCampaignOptions() != null) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(getCampaignOptions(), pw, indent);
        }

        if (getRandomSkillPreferences() != null) {
            getRandomSkillPreferences().writeToXML(pw, indent);
        }

        if (!getSkills().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skillTypes");
            for (final SkillTypeNew type : SkillTypeNew.values()) {
                if (type != null) {
                    type.writeToXML(pw, indent);
                }
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "skillTypes");
        }

        if (!getSpecialAbilities().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "specialAbilities");
            for (final String key : getSpecialAbilities().keySet()) {
                getSpecialAbilities().get(key).writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "specialAbilities");
        }
        // endregion Continuous
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "campaignPreset");
    }

    public static List<CampaignPreset> loadCampaignPresetsFromDirectory(final @Nullable File directory) {
        if ((directory == null) || !directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                     .map(CampaignPreset::parseFromFile)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }

    public static @Nullable CampaignPreset parseFromFile(final @Nullable File file) {
        final Document xmlDoc;

        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MHQXMLUtility.newSafeDocumentBuilder().parse(is);
        } catch (Exception ex) {
            LOGGER.error("", ex);
            return null;
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();

        final String versionString = element.getAttribute("version");
        final Version version = new Version(versionString);
        return parseFromXML(element.getChildNodes(), version);
    }

    public static @Nullable CampaignPreset parseFromXML(final NodeList nl, final Version version) {
        if (version.isHigherThan(MHQConstants.VERSION)) {
            LOGGER.error("Cannot parse Campaign Preset from newer version {} in the current version {}",
                  version,
                  MHQConstants.VERSION);
            return null;
        }

        if (version.isLowerThan(LAST_MILESTONE)) {
            LOGGER.error("Campaign Presets from {} are incompatible with version {}.", version, MHQConstants.VERSION);
            return null;
        }

        if (version.isLowerThan(LAST_COMPATIBLE_VERSION)) {
            LOGGER.error("Campaign Presets from {} are incompatible with version {}.",
                  version,
                  LAST_COMPATIBLE_VERSION);
            return null;
        }

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

                    // region Startup
                    case "date":
                        preset.setDate(MHQXMLUtility.parseDate(wn.getTextContent().trim()));
                        break;
                    case "faction":
                        preset.setFaction(Factions.getInstance().getFaction(wn.getTextContent().trim()));
                        break;
                    case "planet":
                        preset.setPlanet(Systems.getInstance()
                                               .getSystemById(wn.getAttributes()
                                                                    .getNamedItem("system")
                                                                    .getTextContent()
                                                                    .trim())
                                               .getPlanetById(wn.getTextContent().trim()));
                        break;
                    case "rankSystem":
                        preset.setRankSystem(RankSystem.generateInstanceFromXML(wn.getChildNodes(), version));
                        break;
                    case "contractCount":
                        preset.setContractCount(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "gm":
                        preset.setGM(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "companyGenerationOptions":
                        preset.setCompanyGenerationOptions(CompanyGenerationOptions.parseFromXML(wn.getChildNodes(),
                              version));
                        break;
                    // endregion Startup

                    // region Continuous
                    case "gameOptions":
                        preset.setGameOptions(new GameOptions());
                        preset.getGameOptions().fillFromXML(wn.getChildNodes());
                        break;
                    case "campaignOptions":
                        preset.setCampaignOptions(CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(wn,
                              version));
                        break;
                    case "randomSkillPreferences":
                        preset.setRandomSkillPreferences(RandomSkillPreferences.generateRandomSkillPreferencesFromXml(wn,
                              version));
                        break;
                    case "skillTypes": {
                        final NodeList nl2 = wn.getChildNodes();
                        for (int y = 0; y < nl2.getLength(); y++) {
                            final Node wn2 = nl2.item(y);
                            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            } else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
                                String message = String.format("Unknown node type not loaded in Skill Type nodes: %s",
                                      wn2.getNodeName());
                                LOGGER.error(message);
                                continue;
                            }

                            SkillType.generateSeparateInstanceFromXML(wn2, preset.getSkills(), version);
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
                                String message = String.format(
                                      "Unknown node type not loaded in Special Ability nodes: %s",
                                      wn2.getNodeName());
                                LOGGER.error(message);
                                continue;
                            }

                            SpecialAbility.generateSeparateInstanceFromXML(wn2, preset.getSpecialAbilities(), options);
                        }
                        break;
                    }
                    // end Continuous

                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("parseFromXML() Error", ex);
            return null;
        }
        return preset;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return title;
    }
}
