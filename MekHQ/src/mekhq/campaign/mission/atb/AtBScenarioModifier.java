/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
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

package mekhq.campaign.mission.atb;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioObjective;

@XmlRootElement(name="AtBScenarioModifier")
public class AtBScenarioModifier {

    /**
     * Possible values for when a scenario modifier may occur: before or after primary force generation.
     * @author NickAragua
     *
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
    private List<ScenarioObjective> objectives = new ArrayList<>();
    
    public static AtBScenarioModifier generateTestModifier() {
        AtBScenarioModifier sm = new AtBScenarioModifier();
        sm.objectives = new ArrayList<>();
        sm.objectives.add(new ScenarioObjective());
        
        return sm;
    }
    
    /**
     * Process this scenario modifier for a particular scenario, given a particular timing indicator.
     * @param scenario
     * @param campaign
     * @param eventTiming Whether this is occurring before or after primary forces have been generated.
     */
    public void processModifier(AtBDynamicScenario scenario, Campaign campaign, EventTiming eventTiming) {
        if(eventTiming == this.getEventTiming()) {
            if(getAdditionalBriefingText() != null && getAdditionalBriefingText().length() > 0) {
                AtBScenarioModifierApplicator.appendScenarioBriefingText(scenario, getAdditionalBriefingText());
            }
            
            if(getForceDefinition() != null) {
                AtBScenarioModifierApplicator.addForce(campaign, scenario, getForceDefinition(), eventTiming);
            }
            
            if(getSkillAdjustment() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.adjustSkill(scenario, campaign, getEventRecipient(), getSkillAdjustment());
            }
            
            if(getQualityAdjustment() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.adjustQuality(scenario, campaign, getEventRecipient(), getQualityAdjustment());
            }
            
            if(getBattleDamageIntensity() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.inflictBattleDamage(scenario, campaign, getEventRecipient(), getBattleDamageIntensity());
            }
            
            if(getAmmoExpenditureIntensity() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.expendAmmo(scenario, campaign, getEventRecipient(), getAmmoExpenditureIntensity());
            }
            
            if(getUnitRemovalCount() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.removeUnits(scenario, campaign, getEventRecipient(), getUnitRemovalCount());
            }
            
            if(getUseAmbushLogic() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, getEventRecipient());
            }
            
            if(getSwitchSides() != null && getEventRecipient() != null) {
                AtBScenarioModifierApplicator.switchSides(scenario, getEventRecipient());
            }
            
            if(getObjectives() != null && getObjectives().size() > 0) {
                for(ScenarioObjective objective : getObjectives()) {
                    AtBScenarioModifierApplicator.applyObjective(scenario, campaign, objective, eventTiming);
                }
            }
        }
    }
    
    // ----------------------------------------------------------------
    // This section contains static variables and methods
    // 
    private static ScenarioModifierManifest scenarioModifierManifest;
    
    public static List<String> getScenarioFileNames() {
        return scenarioModifierManifest.fileNameList;
    }
    
    private static Map<String, AtBScenarioModifier> scenarioModifiers;
    private static List<String> scenarioModifierKeys;
    
    public static Map<String, AtBScenarioModifier> getScenarioModifiers() {
        return scenarioModifiers;
    }
    
    public static List<String> getOrderedModifierKeys() {
        return scenarioModifierKeys;
    }
    
    public static AtBScenarioModifier getRandomScenarioModifier() {
        int modIndex = Compute.randomInt(scenarioModifierKeys.size());
        return scenarioModifiers.get(scenarioModifierKeys.get(modIndex));
    }
    
    /**
     * Convenience method to get a scenario modifier with the specified key.
     * @param key The key
     * @return The scenario modifier, if any.
     */
    public static AtBScenarioModifier getScenarioModifier(String key) {
        return scenarioModifiers.get(key);
    }
    
    static {
        loadManifest();
        loadScenarioModifiers();
    }
    
    /**
     * Loads the scenario modifier manifest.
     */
    private static void loadManifest() {
        scenarioModifierManifest = ScenarioModifierManifest.Deserialize("./data/scenariomodifiers/modifiermanifest.xml");
        
        // load user-specified modifier list
        ScenarioModifierManifest userModList = ScenarioModifierManifest.Deserialize("./data/scenariomodifiers/usermodifiermanifest.xml");
        if(userModList != null) {
            scenarioModifierManifest.fileNameList.addAll(userModList.fileNameList);
        }
        
        // go through each entry and clean it up for preceding/trailing white space
        for(int x = 0; x < scenarioModifierManifest.fileNameList.size(); x++) {
            scenarioModifierManifest.fileNameList.set(x, scenarioModifierManifest.fileNameList.get(x).trim());
        }
    }
    
    /** 
     * Loads the defined scenario modifiers from the manifest.
     */
    private static void loadScenarioModifiers() {
        scenarioModifiers = new HashMap<>();
        scenarioModifierKeys = new ArrayList<String>();
        
        for(String fileName : scenarioModifierManifest.fileNameList) {
            String filePath = String.format("./data/scenariomodifiers/%s", fileName);
            
            try {
                AtBScenarioModifier modifier = Deserialize(filePath);
                
                if(modifier != null) {
                    if(modifier.getModifierName() == null) {
                        modifier.setModifierName(fileName);
                    }
                    
                    scenarioModifiers.put(modifier.getModifierName(), modifier);
                    scenarioModifierKeys.add(modifier.getModifierName());
                }
            }
            catch(Exception e) {
                MekHQ.getLogger().error(ScenarioModifierManifest.class, "Deserialize", 
                        String.format("Error Loading Scenario %s", filePath), e);
            }
        }
        
        scenarioModifierKeys.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
    }
    
    /**
     * Attempt to deserialize an instance of a scenario modifier from the passed-in file
     * @param fileName Name of the file that contains the scenario modifier
     * @return Possibly an instance of a scenario modifier list
     */
    public static AtBScenarioModifier Deserialize(String fileName) {
        AtBScenarioModifier resultingList = null;

        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioModifier.class);
            Unmarshaller um = context.createUnmarshaller();
            File xmlFile = new File(fileName);
            if(!xmlFile.exists()) {
                MekHQ.getLogger().warning(AtBScenarioModifier.class, "Deserialize", String.format("Specified file %s does not exist", fileName));
                return null;
            }

            try (FileInputStream fileStream = new FileInputStream(xmlFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<AtBScenarioModifier> templateElement = um.unmarshal(inputSource, AtBScenarioModifier.class);
                resultingList = templateElement.getValue();
            }
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioModifierManifest.class, "Deserialize", "Error Deserializing Scenario Modifier: " + fileName, e);
        }

        return resultingList;
    }
    
    /**
     * Serialize this instance of a scenario template to a File
     * Please pass in a non-null file.
     * @param outputFile The destination file.
     */
    public void Serialize(File outputFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(AtBScenarioModifier.class);
            JAXBElement<AtBScenarioModifier> templateElement = new JAXBElement<>(new QName("AtBScenarioModifier"), AtBScenarioModifier.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(templateElement, outputFile);
        } catch(Exception e) {
            MekHQ.getLogger().error(AtBScenarioModifier.class, "Serialize", e.getMessage());
        }
    }
    
    @Override
    public String toString() {
        return getModifierName();
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
    
    @XmlElementWrapper(name="objectives")
    @XmlElement(name="objective")
    public List<ScenarioObjective> getObjectives() {
        return objectives;
    }
}

/**
 * Class intended for local use that holds a manifest of scenario modifier definition file names.
 * @author NickAragua
 *
 */
@XmlRootElement(name="scenarioModifierManifest")
class ScenarioModifierManifest {
    @XmlElementWrapper(name="modifiers")
    @XmlElement(name="modifier")
    public List<String> fileNameList = new ArrayList<>();
    
    /**
     * Attempt to deserialize an instance of a scenario modifier list from the passed-in file
     * @param fileName Name of the file that contains the scenario modifier list
     * @return Possibly an instance of a scenario modifier list
     */
    public static ScenarioModifierManifest Deserialize(String fileName) {
        ScenarioModifierManifest resultingList = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioModifierManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            File xmlFile = new File(fileName);
            if(!xmlFile.exists()) {
                MekHQ.getLogger().warning(ScenarioModifierManifest.class, "Deserialize", String.format("Specified file %s does not exist", fileName));
                return null;
            }

            try (FileInputStream fileStream = new FileInputStream(xmlFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<ScenarioModifierManifest> templateElement = um.unmarshal(inputSource, ScenarioModifierManifest.class);
                resultingList = templateElement.getValue();
            }
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioModifierManifest.class, "Deserialize", "Error Deserializing Scenario Modifier List", e);
        }

        return resultingList;
    }
}
