/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.campaign.stratcon;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;

/**
 * This represents a facility in the StratCon context
 * @author NickAragua
 */
@XmlRootElement(name = "StratconFacility")
public class StratconFacility implements Cloneable {
    public enum FacilityType {
        MekBase,
        TankBase,
        AirBase,
        ArtilleryBase,
        SupplyDepot,
        DataCenter,
        IndustrialFacility,
        CommandCenter,
        EarlyWarningSystem,
        OrbitalDefense,
        BaseOfOperations
    }
    
    private ForceAlignment owner;
    private String displayableName;
    private FacilityType facilityType;
    private boolean visible;
    private int aggroRating;
    private List<String> sharedModifiers = new ArrayList<>();
    private List<String> localModifiers = new ArrayList<>();
    private String capturedDefinition;
    private boolean revealTrack;
    private int scenarioOddsModifier;
    private int weeklySPModifier;
    private boolean preventAerospace;
    //TODO: post-MVP
    //private Map<String, Integer> fixedGarrisonUnitStates = new HashMap<>();
    private boolean isStrategicObjective;
    
    /**
     * A temporary variable used to track situations where changing the ownership of this facility
     * hinges upon multiple objectives
     */
    private transient int ownershipChangeScore;
    
    @Override
    public StratconFacility clone() {
        StratconFacility clone = new StratconFacility();
        clone.owner = owner;
        clone.displayableName = displayableName;
        clone.facilityType = facilityType;
        clone.visible = visible;
        clone.sharedModifiers = new ArrayList<>(sharedModifiers);
        clone.localModifiers = new ArrayList<>(localModifiers);
        clone.setCapturedDefinition(capturedDefinition); 
        clone.revealTrack = revealTrack;
        clone.scenarioOddsModifier = scenarioOddsModifier;
        clone.weeklySPModifier = weeklySPModifier;
        clone.preventAerospace = preventAerospace;
        return clone;
    }
    
    /**
     * Copies data from the source facility to here. Does not copy transient or cosmetic data.
     */
    public void copyRulesDataFrom(StratconFacility facility) {
        setCapturedDefinition(facility.getCapturedDefinition());
        setLocalModifiers(new ArrayList<>(facility.getLocalModifiers()));
        setSharedModifiers(new ArrayList<>(facility.getSharedModifiers()));
        setOwner(facility.getOwner());
        setRevealTrack(facility.getRevealTrack());
        setScenarioOddsModifier(facility.getScenarioOddsModifier());
        setWeeklySPModifier(facility.getWeeklySPModifier());
        setPreventAerospace(facility.preventAerospace());
    }
    
    public ForceAlignment getOwner() {
        return owner;
    }
    
    public void setOwner(ForceAlignment owner) {
        this.owner = owner;
    }
    
    public String getFormattedDisplayableName() {
        return String.format("%s %s", getOwner() == ForceAlignment.Allied ? "Allied" : "Hostile", getDisplayableName());
    }
    
    public String getDisplayableName() {
        return displayableName;
    }
    
    public void setDisplayableName(String displayableName) {
        this.displayableName = displayableName;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return (owner == ForceAlignment.Allied) || visible;
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios in the same track as this facility.
     */
    public List<String> getSharedModifiers() {
        return sharedModifiers;
    }

    public void setSharedModifiers(List<String> sharedModifiers) {
        this.sharedModifiers = sharedModifiers;
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios involving this facility directly. 
     */
    public List<String> getLocalModifiers() {
        return localModifiers;
    }

    public void setLocalModifiers(List<String> localModifiers) {
        this.localModifiers = localModifiers;
    }
    
    public int getAggroRating() {
        return aggroRating;
    }
    
    public void setAggroRating(int rating) {
        aggroRating = rating;
    }
    
    public boolean isStrategicObjective() {
        return isStrategicObjective;
    }
    
    public void setStrategicObjective(boolean isStrategicObjective) {
        this.isStrategicObjective = isStrategicObjective;
    }
    
    public void incrementOwnershipChangeScore() {
        ownershipChangeScore++;
    }
    
    public void decrementOwnershipChangeScore() {
        ownershipChangeScore--;
    }
    
    public void clearOwnershipChangeScore() {
        ownershipChangeScore = 0;
    }
    
    public int getOwnershipChangeScore() {
        return ownershipChangeScore;
    }
    
    /**
     * If present, this is the name of the definition file to draw from
     * when switching facility ownership.
     */
    public String getCapturedDefinition() {
        return capturedDefinition;
    }

    public void setCapturedDefinition(String capturedDefinition) {
        this.capturedDefinition = capturedDefinition;
    }

    public boolean getRevealTrack() {
        return revealTrack;
    }

    public void setRevealTrack(boolean revealTrack) {
        this.revealTrack = revealTrack;
    }

    public int getScenarioOddsModifier() {
        return scenarioOddsModifier;
    }

    public void setScenarioOddsModifier(int scenarioOddsModifier) {
        this.scenarioOddsModifier = scenarioOddsModifier;
    }

    public int getWeeklySPModifier() {
        return weeklySPModifier;
    }

    public void setWeeklySPModifier(int weeklySPModifier) {
        this.weeklySPModifier = weeklySPModifier;
    }

    /**
     * Attempt to deserialize an instance of a StratconFacility from the passed-in file name
     * @return Possibly an instance of a StratconFacility
     */
    public static StratconFacility deserialize(String fileName) {
        StratconFacility resultingFacility = null;
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            MekHQ.getLogger().warning(String.format("Specified file %s does not exist", fileName));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratconFacility.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<StratconFacility> facilityElement = um.unmarshal(inputSource, StratconFacility.class);
                resultingFacility = facilityElement.getValue();
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(String.format("Error Deserializing Facility %s", fileName), e);
        }

        return resultingFacility;
    }

    public boolean preventAerospace() {
        return preventAerospace;
    }

    public void setPreventAerospace(boolean preventAerospace) {
        this.preventAerospace = preventAerospace;
    }
}
