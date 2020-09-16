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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 */
@XmlRootElement(name="StratconFacility")
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
    private Map<String, Integer> fixedGarrisonUnitStates = new HashMap<>();
    private boolean isStrategicObjective;
    
    /**
     * A temporary variable used to track situations where changing the ownership of this facility
     * hinges upon multiple objectives
     */
    private transient int ownershipChangeScore;
    
    public static StratconFacility createTestFacility() {
        StratconFacility test = new StratconFacility();
        test.displayableName = "test facility";
        test.facilityType = FacilityType.TankBase;
        test.sharedModifiers.add("AlliedTankGarrison.xml");
        test.localModifiers.add("AlliedTankGarrison.xml");
        test.localModifiers.add("AlliedTankGarrison.xml");
        test.fixedGarrisonUnitStates.put("Griffin GRF-1N", 0);
        test.owner = ForceAlignment.Opposing;
        return test;
    }
    
    public Object clone() {
        StratconFacility clone = new StratconFacility();
        clone.owner = owner;
        clone.displayableName = displayableName;
        clone.facilityType = facilityType;
        clone.visible = visible;
        clone.sharedModifiers = new ArrayList<>(sharedModifiers);
        clone.localModifiers = new ArrayList<>(localModifiers);
        return clone;
    }
    
    public ForceAlignment getOwner() {
        return owner;
    }
    
    public void setOwner(ForceAlignment owner) {
        this.owner = owner;
    }
    
    public String getDisplayableName() {
        return String.format("%s: %s", displayableName, facilityType.toString());
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
    
    public void setStrategicObjectiveRating(boolean isStrategicObjective) {
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
     * Attempt to deserialize an instance of a StratconFacilityManifest from the passed-in file 
     * @param inputFile The path to the manifest
     * @return Possibly an instance of a StratconFacilityManifest
     */
    public static StratconFacility Deserialize(String fileName) {
        StratconFacility resultingManifest = null;
        File inputFile = new File(fileName);
        if(!inputFile.exists()) {
            MekHQ.getLogger().warning(StratconFacility.class, "Deserialize", String.format("Specified file %s does not exist", fileName));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratconFacility.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MekHqXmlUtil.createSafeXmlSource(fileStream);
                JAXBElement<StratconFacility> manifestElement = um.unmarshal(inputSource, StratconFacility.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch(Exception e) {
            MekHQ.getLogger().error(StratconFacilityManifest.class, "Deserialize", String.format("Error Deserializing Facility %s", fileName), e);
        }

        return resultingManifest;
    }
}
