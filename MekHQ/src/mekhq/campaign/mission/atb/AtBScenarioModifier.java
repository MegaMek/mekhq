/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.atb;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.utilities.MHQXMLUtility;

/**
 * Data structure representing a scenario modifier for dynamic AtB scenarios
 *
 * @author NickAragua
 */
@XmlRootElement(name = "AtBScenarioModifier")
public class AtBScenarioModifier implements Cloneable {
    private static final MMLogger LOGGER = MMLogger.create(AtBScenarioModifier.class);

    private static final String MODIFIER_DIRECTORY = "./data/scenariomodifiers/modifiermanifest.xml";
    private static final String MODIFIER_DIRECTORY_WILDCARD = "./data/scenariomodifiers/%s";

    private static final String MODIFIER_TEST_DIRECTORY = "testresources/data/scenariomodifiers" +
                                                                "/modifiermanifest_test.xml";
    private static final String MODIFIER_TEST_DIRECTORY_WILDCARD = "testresources/data/scenariomodifiers/%s";

    private static final String MODIFIER_USER_DIRECTORY = "./data/scenariomodifiers/usermodifiermanifest.xml";

    /**
     * Possible values for when a scenario modifier may occur: before or after primary force generation.
     */
    public enum EventTiming {
        PreForceGeneration,
        PostForceGeneration
    }

    private String modifierName = null;
    private String additionalBriefingText = null;
    private Boolean benefitsPlayer = false;
    private Boolean blockFurtherEvents = false;
    private EventTiming eventTiming = null;
    private ScenarioForceTemplate forceDefinition = null;
    private Integer skillAdjustment = null;
    private Integer qualityAdjustment = null;
    private ForceAlignment eventRecipient = null;
    private Integer battleDamageIntensity = null;
    private Integer ammoExpenditureIntensity = null;
    private Integer unitRemovalCount = null;
    private List<MapLocation> allowedMapLocations = null;
    private Boolean useAmbushLogic = null;
    private Boolean switchSides = null;
    private Integer numExtraEvents = null;
    private Double bvBudgetAdditiveMultiplier = null;
    private Integer reinforcementDelayReduction = null;
    private List<ScenarioObjective> objectives = new ArrayList<>();

    private Map<String, String> linkedModifiers = new HashMap<>();

    // ----------------------------------------------------------------
    // This section contains static variables and methods
    //
    private static ScenarioModifierManifest scenarioModifierManifest;

    public static List<String> getScenarioFileNames() {
        return scenarioModifierManifest.fileNameList;
    }

    private static Map<String, AtBScenarioModifier> scenarioModifiers;
    private static List<String> scenarioModifierKeys = new ArrayList<>();
    private static final List<String> requiredHostileFacilityModifierKeys = new ArrayList<>();
    private static final List<String> hostileFacilityModifierKeys = new ArrayList<>();
    private static final List<String> alliedFacilityModifierKeys = new ArrayList<>();
    private static final List<String> groundBattleModifierKeys = new ArrayList<>();
    private static final List<String> airBattleModifierKeys = new ArrayList<>();
    private static final List<String> positiveGroundBattleModifierKeys = new ArrayList<>();
    private static final List<String> positiveAirBattleModifierKeys = new ArrayList<>();
    private static final List<String> negativeGroundBattleModifierKeys = new ArrayList<>();
    private static final List<String> negativeAirBattleModifierKeys = new ArrayList<>();
    private static final List<String> primaryPlayerForceModifierKeys = new ArrayList<>();

    public static Map<String, AtBScenarioModifier> getScenarioModifiers() {
        return scenarioModifiers;
    }

    public static List<String> getOrderedModifierKeys() {
        return scenarioModifierKeys;
    }

    /**
     * Convenience method to get a scenario modifier with the specified key.
     *
     * @param key The key
     *
     * @return The scenario modifier, if any.
     */
    public static AtBScenarioModifier getScenarioModifier(String key) {
        if (!scenarioModifiers.containsKey(key)) {
            LOGGER.error("Scenario modifier {} does not exist.", key);
            return null;
        }

        // clone it to avoid calling code changing the modifier
        return scenarioModifiers.get(key).clone();
    }

    /**
     * Convenience method to get all the 'required' hostile facility modifiers()
     */
    public static List<AtBScenarioModifier> getRequiredHostileFacilityModifiers() {
        List<AtBScenarioModifier> retVal = new ArrayList<>();
        for (String key : requiredHostileFacilityModifierKeys) {
            retVal.add(scenarioModifiers.get(key).clone());
        }
        return retVal;
    }

    /**
     * Convenience method to get a random hostile facility modifier
     *
     * @return The scenario modifier, if any.
     */
    public static AtBScenarioModifier getRandomHostileFacilityModifier() {
        return getScenarioModifier(ObjectUtility.getRandomItem(hostileFacilityModifierKeys));
    }

    /**
     * Convenience method to get a random allied facility modifier
     *
     * @return The scenario modifier, if any.
     */
    public static AtBScenarioModifier getRandomAlliedFacilityModifier() {
        return getScenarioModifier(ObjectUtility.getRandomItem(alliedFacilityModifierKeys));
    }

    /**
     * Get a random modifier, appropriate for the map location (space, atmosphere, ground)
     */
    public static AtBScenarioModifier getRandomBattleModifier(MapLocation mapLocation) {
        return getRandomBattleModifier(mapLocation, null);
    }

    /**
     * Convenience method to get a random battle modifier
     *
     * @return The scenario modifier, if any.
     */
    public static @Nullable AtBScenarioModifier getRandomBattleModifier(MapLocation mapLocation, Boolean beneficial) {
        List<String> keyList;

        switch (mapLocation) {
            case Space:
            case LowAtmosphere:
                if (beneficial == null) {
                    keyList = airBattleModifierKeys;
                } else if (beneficial) {
                    keyList = positiveAirBattleModifierKeys;
                } else {
                    keyList = negativeAirBattleModifierKeys;
                }
                break;
            case AllGroundTerrain:
            case SpecificGroundTerrain:
            default:
                if (beneficial == null) {
                    keyList = groundBattleModifierKeys;
                } else if (beneficial) {
                    keyList = positiveGroundBattleModifierKeys;
                } else {
                    keyList = negativeGroundBattleModifierKeys;
                }
                break;
        }

        return getScenarioModifier(ObjectUtility.getRandomItem(keyList));
    }

    /**
     * Call this method before using scenario modifiers, passing the desired directory flag.
     * <p>
     * If not called, the class will not be initialized.
     *
     * @param useTestDirectory {@code true} to use testresources, false for production data
     */
    public static void initializeScenarioModifiers(boolean useTestDirectory) {
        loadManifest(useTestDirectory);
        loadScenarioModifiers(useTestDirectory);

        initializeSpecificManifest(MHQConstants.STRAT_CON_REQUIRED_HOSTILE_FACILITY_MODS,
              requiredHostileFacilityModifierKeys);
        initializeSpecificManifest(MHQConstants.STRAT_CON_HOSTILE_FACILITY_MODS, hostileFacilityModifierKeys);
        initializeSpecificManifest(MHQConstants.STRAT_CON_ALLIED_FACILITY_MODS, alliedFacilityModifierKeys);
        initializeSpecificManifest(MHQConstants.STRAT_CON_GROUND_MODS, groundBattleModifierKeys);
        initializeSpecificManifest(MHQConstants.STRAT_CON_AIR_MODS, airBattleModifierKeys);
        initializeSpecificManifest(MHQConstants.STRAT_CON_PRIMARY_PLAYER_FORCE_MODS, primaryPlayerForceModifierKeys);

        initializePositiveNegativeManifests(groundBattleModifierKeys, positiveGroundBattleModifierKeys,
              negativeGroundBattleModifierKeys);
        initializePositiveNegativeManifests(airBattleModifierKeys, positiveAirBattleModifierKeys,
              negativeAirBattleModifierKeys);
    }

    /**
     * Initializes a specific manifest file name list from a file with the given name
     */
    private static void initializeSpecificManifest(String manifestFileName, List<String> keyCollection) {
        ScenarioModifierManifest manifest = ScenarioModifierManifest.Deserialize(manifestFileName);

        if (manifest != null) {
            // add trimmed versions of each file name to the given collection
            for (String modifierName : manifest.fileNameList) {
                keyCollection.add(modifierName.trim());
            }
        }
    }

    /**
     * Divides the given modifiers into a positive and negative bucket.
     */
    private static void initializePositiveNegativeManifests(List<String> modifiers, List<String> positiveKeyCollection,
          List<String> negativeKeyCollection) {
        for (String modifier : modifiers) {
            if (!scenarioModifiers.containsKey(modifier)) {
                continue;
            }

            if (scenarioModifiers.get(modifier).benefitsPlayer) {
                positiveKeyCollection.add(modifier);
            } else {
                negativeKeyCollection.add(modifier);
            }
        }
    }

    /**
     * Loads the scenario modifier manifest.
     */
    private static void loadManifest(boolean useTestDirectory) {
        scenarioModifierManifest = ScenarioModifierManifest.Deserialize(useTestDirectory ?
                                                                              MODIFIER_TEST_DIRECTORY :
                                                                              MODIFIER_DIRECTORY);

        // load user-specified modifier list
        ScenarioModifierManifest userModList = ScenarioModifierManifest.Deserialize(MODIFIER_USER_DIRECTORY);
        if (userModList != null) {
            scenarioModifierManifest.fileNameList.addAll(userModList.fileNameList);
        }

        // go through each entry and clean it up for preceding/trailing white space
        scenarioModifierManifest.fileNameList.replaceAll(String::trim);
    }

    /**
     * Loads the defined scenario modifiers from the manifest.
     */
    private static void loadScenarioModifiers(boolean useTestDirectory) {
        scenarioModifiers = new HashMap<>();
        scenarioModifierKeys = new ArrayList<>();

        for (String fileName : scenarioModifierManifest.fileNameList) {
            String filePath = String.format(useTestDirectory ?
                                                  MODIFIER_TEST_DIRECTORY_WILDCARD :
                                                  MODIFIER_DIRECTORY_WILDCARD, fileName);

            try {
                AtBScenarioModifier modifier = Deserialize(filePath);

                if (modifier != null) {
                    scenarioModifiers.put(fileName, modifier);
                    scenarioModifierKeys.add(fileName);

                    if (modifier.getModifierName() == null) {
                        modifier.setModifierName(fileName);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Error Loading Scenario {}", filePath, ex);
            }
        }

        scenarioModifierKeys.sort(String::compareTo);
    }

    /**
     * Attempt to deserialize an instance of a scenario modifier from the passed-in file
     *
     * @param fileName Name of the file that contains the scenario modifier
     *
     * @return Possibly an instance of a scenario modifier list
     */
    public static AtBScenarioModifier Deserialize(String fileName) {
        AtBScenarioModifier resultingModifier = null;

        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioModifier.class);
            Unmarshaller um = context.createUnmarshaller();
            File xmlFile = new File(fileName);
            if (!xmlFile.exists()) {
                LOGGER.warn("Specified file {} does not exist", fileName);
                return null;
            }

            try (FileInputStream fileStream = new FileInputStream(xmlFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<AtBScenarioModifier> modifierElement = um.unmarshal(inputSource, AtBScenarioModifier.class);
                resultingModifier = modifierElement.getValue();
            }
        } catch (Exception ex) {
            LOGGER.error("Error Deserializing Scenario Modifier: {}", fileName, ex);
        }

        return resultingModifier;
    }

    /**
     * Serialize this instance of a scenario template to a File Please pass in a non-null file.
     *
     * @param outputFile The destination file.
     */
    public void Serialize(File outputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioModifier.class);
            JAXBElement<AtBScenarioModifier> templateElement = new JAXBElement<>(new QName("AtBScenarioModifier"),
                  AtBScenarioModifier.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(templateElement, outputFile);
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
    }

    /**
     * Process this scenario modifier for a particular scenario, given a particular timing indicator.
     *
     * @param eventTiming Whether this is occurring before or after primary forces have been generated.
     */
    public void processModifier(AtBDynamicScenario scenario, Campaign campaign, EventTiming eventTiming) {
        if (eventTiming == getEventTiming()) {
            if ((getAdditionalBriefingText() != null) && !getAdditionalBriefingText().isBlank()) {
                AtBScenarioModifierApplicator.appendScenarioBriefingText(scenario,
                      getAdditionalBriefingText());
            }

            if (getForceDefinition() != null) {
                AtBScenarioModifierApplicator.addForce(campaign, scenario, getForceDefinition(), eventTiming);
            }

            if ((getSkillAdjustment() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.adjustSkill(scenario, campaign, getEventRecipient(),
                      getSkillAdjustment());
            }

            if ((getQualityAdjustment() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.adjustQuality(scenario, campaign, getEventRecipient(),
                      getQualityAdjustment());
            }

            if ((getBattleDamageIntensity() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.inflictBattleDamage(scenario, campaign, getEventRecipient(),
                      getBattleDamageIntensity());
            }

            if ((getAmmoExpenditureIntensity() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.expendAmmo(scenario, campaign, getEventRecipient(),
                      getAmmoExpenditureIntensity());
            }

            if ((getUnitRemovalCount() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.removeUnits(scenario, campaign, getEventRecipient(),
                      getUnitRemovalCount());
            }

            if ((getUseAmbushLogic() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, getEventRecipient());
            }

            if ((getSwitchSides() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.switchSides(scenario, getEventRecipient());
            }

            if ((getObjectives() != null) && !getObjectives().isEmpty()) {
                for (ScenarioObjective objective : getObjectives()) {
                    AtBScenarioModifierApplicator.applyObjective(scenario, campaign, objective, eventTiming);
                }
            }

            if ((getNumExtraEvents() != null) && (getNumExtraEvents() > 0)) {
                for (int x = 0; x < getNumExtraEvents(); x++) {
                    AtBScenarioModifierApplicator.applyExtraEvent(scenario,
                          getEventRecipient() == ForceAlignment.Allied);
                }
            }

            if (getBVBudgetAdditiveMultiplier() != null) {
                scenario.setEffectivePlayerBVMultiplier(getBVBudgetAdditiveMultiplier());
            }

            if ((getReinforcementDelayReduction() != null) && (getEventRecipient() != null)) {
                AtBScenarioModifierApplicator.applyReinforcementDelayReduction(scenario, eventRecipient,
                      getReinforcementDelayReduction());
            }
        }
    }

    @Override
    public String toString() {
        return getModifierName();
    }

    @Override
    public AtBScenarioModifier clone() {
        final AtBScenarioModifier copy = new AtBScenarioModifier();
        copy.additionalBriefingText = additionalBriefingText;
        copy.allowedMapLocations = allowedMapLocations == null ? new ArrayList<>()
                                         : new ArrayList<>(allowedMapLocations);
        copy.ammoExpenditureIntensity = ammoExpenditureIntensity;
        copy.battleDamageIntensity = battleDamageIntensity;
        copy.benefitsPlayer = benefitsPlayer;
        copy.blockFurtherEvents = blockFurtherEvents;
        copy.eventRecipient = eventRecipient;
        copy.eventTiming = eventTiming;
        copy.forceDefinition = forceDefinition != null ? new ScenarioForceTemplate(forceDefinition) : null;
        copy.modifierName = modifierName;
        copy.qualityAdjustment = qualityAdjustment;
        copy.skillAdjustment = skillAdjustment;
        copy.switchSides = switchSides;
        copy.unitRemovalCount = unitRemovalCount;
        copy.useAmbushLogic = useAmbushLogic;
        copy.linkedModifiers = linkedModifiers == null ? new HashMap<>() : new HashMap<>(linkedModifiers);
        copy.objectives = objectives == null ? new ArrayList<>() : new ArrayList<>(objectives);
        copy.bvBudgetAdditiveMultiplier = bvBudgetAdditiveMultiplier;
        copy.reinforcementDelayReduction = reinforcementDelayReduction;
        return copy;
    }

    public String getModifierName() {
        return modifierName;
    }

    public void setModifierName(String modifierName) {
        this.modifierName = modifierName;
    }

    public String getAdditionalBriefingText() {
        return additionalBriefingText;
    }

    public void setAdditionalBriefingText(String additionalBriefingText) {
        this.additionalBriefingText = additionalBriefingText;
    }

    public Boolean getBenefitsPlayer() {
        return benefitsPlayer;
    }

    public void setBenefitsPlayer(Boolean benefitsPlayer) {
        this.benefitsPlayer = benefitsPlayer;
    }

    public Boolean getBlockFurtherEvents() {
        return blockFurtherEvents;
    }

    public void setBlockFurtherEvents(Boolean blockFurtherEvents) {
        this.blockFurtherEvents = blockFurtherEvents;
    }

    public EventTiming getEventTiming() {
        return eventTiming;
    }

    public void setEventTiming(EventTiming eventTiming) {
        this.eventTiming = eventTiming;
    }

    public ScenarioForceTemplate getForceDefinition() {
        return forceDefinition;
    }

    public void setForceDefinition(ScenarioForceTemplate forceDefinition) {
        this.forceDefinition = forceDefinition;
    }

    public Integer getSkillAdjustment() {
        return skillAdjustment;
    }

    public void setSkillAdjustment(Integer skillAdjustment) {
        this.skillAdjustment = skillAdjustment;
    }

    public Integer getQualityAdjustment() {
        return qualityAdjustment;
    }

    public void setQualityAdjustment(Integer qualityAdjustment) {
        this.qualityAdjustment = qualityAdjustment;
    }

    public ForceAlignment getEventRecipient() {
        return eventRecipient;
    }

    public void setEventRecipient(ForceAlignment eventRecipient) {
        this.eventRecipient = eventRecipient;
    }

    public Integer getBattleDamageIntensity() {
        return battleDamageIntensity;
    }

    public void setBattleDamageIntensity(Integer battleDamageIntensity) {
        this.battleDamageIntensity = battleDamageIntensity;
    }

    public Integer getAmmoExpenditureIntensity() {
        return ammoExpenditureIntensity;
    }

    public void setAmmoExpenditureIntensity(Integer ammoExpenditureIntensity) {
        this.ammoExpenditureIntensity = ammoExpenditureIntensity;
    }

    public Integer getUnitRemovalCount() {
        return unitRemovalCount;
    }

    public void setUnitRemovalCount(Integer unitRemovalCount) {
        this.unitRemovalCount = unitRemovalCount;
    }

    @XmlElementWrapper(name = "allowedMapLocations")
    @XmlElement(name = "allowedMapLocation")
    public List<MapLocation> getAllowedMapLocations() {
        return allowedMapLocations;
    }

    public void setAllowedMapLocations(List<MapLocation> allowedMapLocations) {
        this.allowedMapLocations = allowedMapLocations;
    }

    public Boolean getUseAmbushLogic() {
        return useAmbushLogic;
    }

    public void setUseAmbushLogic(Boolean useAmbushLogic) {
        this.useAmbushLogic = useAmbushLogic;
    }

    public Boolean getSwitchSides() {
        return switchSides;
    }

    public void setSwitchSides(Boolean switchSides) {
        this.switchSides = switchSides;
    }

    @XmlElementWrapper(name = "objectives")
    @XmlElement(name = "objective")
    public List<ScenarioObjective> getObjectives() {
        return objectives;
    }

    public Integer getNumExtraEvents() {
        return numExtraEvents;
    }

    public void setNumExtraEvents(Integer numExtraEvents) {
        this.numExtraEvents = numExtraEvents;
    }

    public Double getBVBudgetAdditiveMultiplier() {
        return bvBudgetAdditiveMultiplier;
    }

    public void setBVBudgetAdditiveMultiplier(Double bvBudgetAdditiveMultiplier) {
        this.bvBudgetAdditiveMultiplier = bvBudgetAdditiveMultiplier;
    }

    public Integer getReinforcementDelayReduction() {
        return reinforcementDelayReduction;
    }

    public void setReinforcementDelayReduction(Integer reinforcementDelayReduction) {
        this.reinforcementDelayReduction = reinforcementDelayReduction;
    }

    /**
     * Map containing string tuples: "Alternate" briefing description, name of file containing other modifiers
     * associated with this one
     */
    public Map<String, String> getLinkedModifiers() {
        return linkedModifiers;
    }

    public void setLinkedModifiers(Map<String, String> linkedModifiers) {
        this.linkedModifiers = linkedModifiers;
    }
}
