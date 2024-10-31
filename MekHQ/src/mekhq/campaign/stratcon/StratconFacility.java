/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.stratcon;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.utilities.MHQXMLUtility;

import javax.xml.transform.Source;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * This represents a facility in the StratCon context
 *
 * @author NickAragua
 */
@XmlRootElement(name = "StratconFacility")
public class StratconFacility implements Cloneable {
    private static final MMLogger logger = MMLogger.create(StratconFacility.class);

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
    private String userDescription;
    private boolean visible;
    private int aggroRating;
    private List<String> sharedModifiers = new ArrayList<>();
    private List<String> localModifiers = new ArrayList<>();
    private String capturedDefinition;
    private boolean revealTrack;
    private int scenarioOddsModifier;
    private int monthlySPModifier;
    private boolean preventAerospace;
    // TODO: post-MVP
    // private Map<String, Integer> fixedGarrisonUnitStates = new HashMap<>();
    private boolean isStrategicObjective;
    private List<StratconBiome> biomes = new ArrayList<>();

    private transient TreeMap<Integer, StratconBiome> biomeTempMap = new TreeMap<>();

    /**
     * A temporary variable used to track situations where changing the ownership of
     * this facility
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
        clone.monthlySPModifier = monthlySPModifier;
        clone.preventAerospace = preventAerospace;
        clone.userDescription = userDescription;
        clone.biomes = new ArrayList<>(biomes);
        ReconstructTransientData(clone);
        return clone;
    }

    /**
     * Copies data from the source facility to here. Does cosmetic data.
     * Reconstructs file-driven transient data.
     */
    public void copyRulesDataFrom(StratconFacility facility) {
        setCapturedDefinition(facility.getCapturedDefinition());
        setLocalModifiers(new ArrayList<>(facility.getLocalModifiers()));
        setSharedModifiers(new ArrayList<>(facility.getSharedModifiers()));
        setOwner(facility.getOwner());
        setRevealTrack(facility.getRevealTrack());
        setScenarioOddsModifier(facility.getScenarioOddsModifier());
        setMonthlySPModifier(facility.getMonthlySPModifier());
        setPreventAerospace(facility.preventAerospace());
        setBiomes(new ArrayList<>(facility.getBiomes()));
        setUserDescription(facility.getUserDescription());
        ReconstructTransientData(this);
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
     * This is a list of scenario modifier IDs that affect scenarios in the same
     * track as this facility.
     */
    public List<String> getSharedModifiers() {
        return sharedModifiers;
    }

    public void setSharedModifiers(List<String> sharedModifiers) {
        this.sharedModifiers = sharedModifiers;
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios involving this
     * facility directly.
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

    public List<StratconBiome> getBiomes() {
        return biomes;
    }

    public void setBiomes(List<StratconBiome> biomes) {
        this.biomes = biomes;
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

    /**
     * @return The facility's monthly SP (Support Points) modifier as an integer.
     */
    public int getMonthlySPModifier() {
        return monthlySPModifier;
    }

    /**
     * Sets a new value for the monthly SP (Support Points) modifier.
     *
     * @param monthlySPModifier The new monthly SP modifier value.
     */
    public void setMonthlySPModifier(int monthlySPModifier) {
        this.monthlySPModifier = monthlySPModifier;
    }

    /**
     * Returns the biome temperature map (note: temperature mapping is in kelvins
     * but stored in celsius)
     */
    public TreeMap<Integer, StratconBiome> getBiomeTempMap() {
        return biomeTempMap;
    }

    /**
     * Attempt to deserialize an instance of a StratconFacility from the passed-in
     * file name
     *
     * @return Possibly an instance of a StratconFacility
     */
    public static StratconFacility deserialize(String fileName) {
        StratconFacility resultingFacility = null;
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            logger.warn(String.format("Specified file %s does not exist", fileName));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratconFacility.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<StratconFacility> facilityElement = um.unmarshal(inputSource, StratconFacility.class);
                resultingFacility = facilityElement.getValue();
            }
        } catch (Exception e) {
            logger.error(String.format("Error Deserializing Facility %s", fileName), e);
        }

        ReconstructTransientData(resultingFacility);

        return resultingFacility;
    }

    private static void ReconstructTransientData(StratconFacility facility) {
        for (StratconBiome biome : facility.getBiomes()) {
            facility.getBiomeTempMap().put(biome.allowedTemperatureLowerBound, biome);
        }
    }

    public boolean preventAerospace() {
        return preventAerospace;
    }

    public void setPreventAerospace(boolean preventAerospace) {
        this.preventAerospace = preventAerospace;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public void setUserDescription(String userDescription) {
        this.userDescription = userDescription;
    }
}
