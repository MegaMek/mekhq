/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * A facility placed on a StratCon sector. It refers to its {@link StratConFacilityDefinition} by ID and keeps only its
 * own state: who holds it, whether the player has seen it, whether it has lent its modifiers this week, whether it is
 * a strategic objective, and any scenario modifiers added when it was placed. What it does comes from the definition's
 * profile for its current owner, so capturing it is just a change of owner.
 *
 * <p>Saves from before 0.51.01 held a full copy of the facility's old-format definition instead of an ID. Those
 * fields are still read, and {@link #resolveLegacyData()} turns them into a definition ID once loading is done.</p>
 *
 * @author NickAragua
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class StratConFacility {
    private static final MMLogger LOGGER = MMLogger.create(StratConFacility.class);

    public enum FacilityType {
        MekBase,
        TankBase,
        AirBase,
        ArtilleryBase,
        SpacePort,
        DataCenter,
        IndustrialFacility,
        CommandCenter,
        EarlyWarningSystem,
        OrbitalDefense,
        BaseOfOperations
    }

    @XmlElement
    private String definitionId;
    @XmlElement
    private ForceAlignment owner;
    @XmlElement
    private boolean visible;
    @XmlElement(name = "isAvailable")
    private boolean isAvailable = true;
    @XmlElement(name = "strategicObjective")
    private boolean isStrategicObjective;
    @XmlElement(name = "additionalLocalModifier")
    private List<String> additionalLocalModifiers = new ArrayList<>();

    // Read from saves written before 0.51.01, which held a copy of the whole old-format definition. Emptied once the
    // facility is matched to a definition, so they are only written back for a facility that could not be matched.
    @XmlElement(name = "displayableName")
    private String legacyDisplayableName;
    @XmlElement(name = "facilityType")
    private FacilityType legacyFacilityType;
    @XmlElement(name = "userDescription")
    private String legacyUserDescription;
    @XmlElement(name = "sharedModifiers")
    private List<String> legacySharedModifiers;
    @XmlElement(name = "localModifiers")
    private List<String> legacyLocalModifiers;
    @XmlElement(name = "revealTrack")
    private Boolean legacyRevealTrack;
    @XmlElement(name = "increaseScanRange")
    private Boolean legacyIncreaseScanRange;
    @XmlElement(name = "scenarioOddsModifier")
    private Integer legacyScenarioOddsModifier;
    @XmlElement(name = "monthlySPModifier")
    private Integer legacyMonthlySPModifier;

    /**
     * A definition held by this facility alone rather than looked up by ID: one built in code (tests, tools), one
     * made from an unmatched old save, or a placeholder for an ID that no loaded definition has.
     */
    @XmlTransient
    private StratConFacilityDefinition detachedDefinition;

    /**
     * A temporary variable used to track situations where changing the ownership of this facility hinges upon multiple
     * objectives
     */
    @XmlTransient
    private int ownershipChangeScore;

    /**
     * For loading from saves only.
     */
    public StratConFacility() {
    }

    /**
     * Creates a facility of the given type, held by the given side.
     *
     * @param definition the facility's type
     * @param owner      the side that holds it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacility(StratConFacilityDefinition definition, ForceAlignment owner) {
        this.definitionId = definition.getId();
        this.owner = owner;
        if (StratConFacilityFactory.getDefinition(definition.getId()) != definition) {
            detachedDefinition = definition;
        }
    }

    /**
     * @return the ID of the facility's definition
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getDefinitionId() {
        return definitionId;
    }

    /**
     * @return the facility's definition. If no loaded definition has its ID, an empty placeholder named after the ID,
     *       so a facility whose data has gone missing does nothing rather than breaking the sector.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityDefinition getDefinition() {
        if (detachedDefinition != null) {
            return detachedDefinition;
        }

        StratConFacilityDefinition definition = StratConFacilityFactory.getDefinition(definitionId);
        if (definition == null) {
            LOGGER.warn("No facility definition {} is loaded; the facility will have no effects", definitionId);
            detachedDefinition = new StratConFacilityDefinition(definitionId,
                  String.valueOf(definitionId),
                  FacilityType.BaseOfOperations,
                  null,
                  null);
            return detachedDefinition;
        }

        return definition;
    }

    /**
     * @return the profile that applies while the facility's current owner holds it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityProfile getProfile() {
        return getDefinition().getProfileFor(owner);
    }

    public ForceAlignment getOwner() {
        return owner;
    }

    public void setOwner(ForceAlignment owner) {
        this.owner = owner;
    }

    /**
     * @return {@code true} if the facility owner is either allied to the player or is the player themselves,
     *       {@code false} otherwise.
     */
    public boolean isOwnerAlliedToPlayer() {
        return StratConFacilityDefinition.isAlliedToPlayer(owner);
    }

    public String getFormattedDisplayableName() {
        return String.format("%s %s", getOwner() == ForceAlignment.Allied ? "Allied" : "Hostile", getDisplayableName());
    }

    public String getDisplayableName() {
        return getDefinition().getDisplayableName();
    }

    public FacilityType getFacilityType() {
        return getDefinition().getFacilityType();
    }

    /**
     * @return what the facility does while its current owner holds it, or {@code null} if the profile has no
     *       description
     */
    public @Nullable String getUserDescription() {
        return getProfile().getDescription();
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

    public boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios in the same track as this facility.
     */
    public List<String> getSharedModifiers() {
        return getProfile().getSharedModifierIds();
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios involving this facility directly: the current
     * profile's, then any added when the facility was placed.
     */
    public List<String> getLocalModifiers() {
        List<String> modifiers = getProfile().getLocalModifierIds();
        modifiers.addAll(additionalLocalModifiers);
        return modifiers;
    }

    /**
     * @return the scenario modifier IDs added to this facility alone when it was placed, such as those of the
     *       objective it serves
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<String> getAdditionalLocalModifiers() {
        return Collections.unmodifiableList(additionalLocalModifiers);
    }

    /**
     * Adds scenario modifiers to this facility alone, on top of its profile's local modifiers.
     *
     * @param modifiers the scenario modifier IDs to add
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void addAdditionalLocalModifiers(List<String> modifiers) {
        additionalLocalModifiers.addAll(modifiers);
    }

    public boolean isStrategicObjective() {
        return isStrategicObjective;
    }

    public void setStrategicObjective(boolean isStrategicObjective) {
        this.isStrategicObjective = isStrategicObjective;
    }

    public List<StratConBiome> getBiomes() {
        return getDefinition().getBiomes();
    }

    /**
     * Returns the biome temperature map (note: temperature mapping is in kelvins but stored in Celsius)
     */
    public TreeMap<Integer, StratConBiome> getBiomeTempMap() {
        return getDefinition().getBiomeTempMap();
    }

    public void incrementOwnershipChangeScore() {
        ownershipChangeScore++;
    }

    public void decrementOwnershipChangeScore() {
        ownershipChangeScore--;
    }

    public int getOwnershipChangeScore() {
        return ownershipChangeScore;
    }

    /**
     * @return whether the facility reveals its whole sector while its current owner holds it
     */
    public boolean isRevealingTrack() {
        return getProfile().isRevealingTrack();
    }

    /**
     * @return hexes the facility adds to the scan range of forces scouting its sector while its current owner holds it
     */
    public int getScanRangeIncrease() {
        return getProfile().getScanRangeIncrease();
    }

    public int getScenarioOddsModifier() {
        return getProfile().getScenarioOddsModifier();
    }

    /**
     * @return the facility's monthly SP (Support Points) change while its current owner holds it
     */
    public int getMonthlySupportPoints() {
        return getProfile().getMonthlySupportPoints();
    }

    /**
     * @return whether the facility keeps air and space scenarios out of its sector
     */
    public boolean isPreventingAerospace() {
        return getProfile().isPreventingAerospace();
    }

    /**
     * Turns the old-format definition copy held by a facility from a save written before 0.51.01 into a definition ID.
     * The facility is matched to the loaded definition of the same {@link FacilityType}; any local modifiers beyond
     * that definition's own are kept as the facility's additional ones. A facility that matches no loaded definition
     * keeps its old data as a detached definition, so it still behaves as it did.
     *
     * <p>Does nothing for a facility that already has a definition ID.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void resolveLegacyData() {
        if ((definitionId != null) || (legacyFacilityType == null)) {
            return;
        }

        StratConFacilityDefinition definition = StratConFacilityFactory.getDefinitionForType(legacyFacilityType);
        if (definition == null) {
            detachedDefinition = buildLegacyDefinition();
            definitionId = detachedDefinition.getId();
            LOGGER.warn("No facility definition of type {} is loaded; keeping the saved data for {}",
                  legacyFacilityType,
                  legacyDisplayableName);
            return;
        }

        definitionId = definition.getId();

        List<String> remainingModifiers = new ArrayList<>();
        if (legacyLocalModifiers != null) {
            remainingModifiers.addAll(legacyLocalModifiers);
        }
        for (String profileModifier : definition.getProfileFor(owner).getLocalModifierIds()) {
            remainingModifiers.remove(profileModifier);
        }
        additionalLocalModifiers.addAll(remainingModifiers);

        legacyDisplayableName = null;
        legacyFacilityType = null;
        legacyUserDescription = null;
        legacySharedModifiers = null;
        legacyLocalModifiers = null;
        legacyRevealTrack = null;
        legacyIncreaseScanRange = null;
        legacyScenarioOddsModifier = null;
        legacyMonthlySPModifier = null;
    }

    /**
     * @return a one-sided definition built from the old-format data a save held for this facility
     */
    private StratConFacilityDefinition buildLegacyDefinition() {
        LegacyStratConFacilityData legacyData = new LegacyStratConFacilityData();
        legacyData.owner = owner;
        legacyData.displayableName = legacyDisplayableName;
        legacyData.facilityType = legacyFacilityType;
        legacyData.userDescription = legacyUserDescription;
        legacyData.sharedModifiers = legacySharedModifiers;
        legacyData.localModifiers = legacyLocalModifiers;
        legacyData.revealTrack = Boolean.TRUE.equals(legacyRevealTrack);
        legacyData.increaseScanRange = Boolean.TRUE.equals(legacyIncreaseScanRange);
        legacyData.scenarioOddsModifier = (legacyScenarioOddsModifier == null) ? 0 : legacyScenarioOddsModifier;
        legacyData.monthlySPModifier = (legacyMonthlySPModifier == null) ? 0 : legacyMonthlySPModifier;
        return legacyData.toDefinition("legacy-" + legacyFacilityType.name());
    }
}
