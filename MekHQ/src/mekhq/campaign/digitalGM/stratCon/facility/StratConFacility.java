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
 * own state: who holds it, its tier, condition and garrison, how much the player knows about it, whether it has lent
 * its modifiers this week, whether it is a strategic objective, and any scenario modifiers added when it was placed.
 * What it does comes from the definition's profile for its current owner, so capturing it is just a change of owner.
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

    /**
     * How large a facility is. The tier caps the facility's garrison.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum FacilityTier {
        OUTPOST(1),
        BASE(2),
        STRONGHOLD(3);

        private final int garrisonMaximum;

        FacilityTier(int garrisonMaximum) {
            this.garrisonMaximum = garrisonMaximum;
        }

        /**
         * @return the most garrison steps a facility of this tier can hold
         */
        public int getGarrisonMaximum() {
            return garrisonMaximum;
        }

        /**
         * @return the next tier up, or this tier if it is already the largest
         */
        public FacilityTier next() {
            return (this == STRONGHOLD) ? STRONGHOLD : values()[ordinal() + 1];
        }
    }

    /**
     * How badly a facility is damaged. A damaged facility's numeric effects are halved and it lends no shared
     * modifiers; a crippled one has no effects at all. A destroyed facility is removed from the map.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum FacilityCondition {
        INTACT,
        DAMAGED,
        CRIPPLED;

        /**
         * @return the next condition down, or this condition if it is already the worst
         */
        public FacilityCondition worsened() {
            return (this == CRIPPLED) ? CRIPPLED : values()[ordinal() + 1];
        }
    }

    /**
     * How much the player knows about a facility. Each level shows more: its position and type once
     * {@link #LOCATED}, its tier and condition once {@link #SCOUTED}, and its garrison once {@link #DETAILED}. The
     * player always knows everything about a facility their own side holds.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum FacilityIntel {
        UNKNOWN,
        LOCATED,
        SCOUTED,
        DETAILED;

        /**
         * @param level the level to compare against
         *
         * @return {@code true} if this level is at least the given one
         */
        public boolean isAtLeast(FacilityIntel level) {
            return ordinal() >= level.ordinal();
        }
    }

    // The garrison ladder: garrison step 1 brings the profile's own local modifiers, and each step above that adds the
    // next modifier for the holding side, skipping any the profile already has.
    private static final List<String> ALLIED_GARRISON_LADDER = List.of("AlliedTurrets.json",
          "AlliedGroundSupport.json");
    private static final List<String> HOSTILE_GARRISON_LADDER = List.of("EnemyTurrets.json",
          "HostileBVBudgetIncrease.json");

    @XmlElement
    private String definitionId;
    @XmlElement
    private ForceAlignment owner;
    @XmlElement
    private FacilityTier tier;
    @XmlElement
    private FacilityCondition condition;
    @XmlElement
    private Integer garrison;
    @XmlElement
    private FacilityIntel intel;
    @XmlElement(name = "isAvailable")
    private boolean isAvailable = true;
    @XmlElement(name = "strategicObjective")
    private boolean isStrategicObjective;
    @XmlElement(name = "additionalLocalModifier")
    private List<String> additionalLocalModifiers = new ArrayList<>();

    // Read from saves written before 0.51.01, which held a copy of the whole old-format definition. Emptied once the
    // facility is matched to a definition, so they are only written back for a facility that could not be matched.
    @XmlElement(name = "visible")
    private Boolean legacyVisible;
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
        this.tier = FacilityTier.BASE;
        this.condition = FacilityCondition.INTACT;
        this.garrison = FacilityTier.BASE.getGarrisonMaximum();
        this.intel = FacilityIntel.UNKNOWN;
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

    /**
     * @return the facility's tier; {@link FacilityTier#BASE} for one saved before tiers existed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public FacilityTier getTier() {
        return (tier == null) ? FacilityTier.BASE : tier;
    }

    /**
     * Sets the facility's tier, trimming its garrison to the new tier's maximum.
     *
     * @param tier the new tier
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setTier(FacilityTier tier) {
        this.tier = tier;
        setGarrison(getGarrison());
    }

    /**
     * @return the facility's condition; {@link FacilityCondition#INTACT} for one saved before conditions existed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public FacilityCondition getCondition() {
        return (condition == null) ? FacilityCondition.INTACT : condition;
    }

    public void setCondition(FacilityCondition condition) {
        this.condition = condition;
    }

    /**
     * @return the facility's garrison, in steps from 0 to its tier's maximum; a full garrison for one saved before
     *       garrisons existed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getGarrison() {
        return (garrison == null) ? getGarrisonMaximum() : garrison;
    }

    /**
     * Sets the facility's garrison, kept between 0 and its tier's maximum.
     *
     * @param garrison the new garrison, in steps
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setGarrison(int garrison) {
        this.garrison = Math.max(0, Math.min(garrison, getGarrisonMaximum()));
    }

    /**
     * @return the most garrison steps the facility's tier allows
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getGarrisonMaximum() {
        return getTier().getGarrisonMaximum();
    }

    /**
     * @return how much the player knows about the facility. Everything, if the player's side holds it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public FacilityIntel getIntel() {
        if (isOwnerAlliedToPlayer()) {
            return FacilityIntel.DETAILED;
        }
        return (intel == null) ? FacilityIntel.UNKNOWN : intel;
    }

    /**
     * Sets how much the player knows about the facility, whether more or less than before.
     *
     * @param intel the new intel level
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setIntel(FacilityIntel intel) {
        this.intel = intel;
    }

    /**
     * Raises how much the player knows about the facility to at least the given level. Never lowers it.
     *
     * @param level the level the player now has at least
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void raiseIntel(FacilityIntel level) {
        if (!getIntel().isAtLeast(level)) {
            intel = level;
        }
    }

    /**
     * @return {@code true} if the player knows at least where the facility is
     */
    public boolean getVisible() {
        return getIntel().isAtLeast(FacilityIntel.LOCATED);
    }

    /**
     * Shorthand for the two intel changes most callers need: {@code true} raises intel to at least
     * {@link FacilityIntel#SCOUTED}, as scouting the hex does; {@code false} hides the facility entirely.
     *
     * @param visible whether the player has seen the facility
     */
    public void setVisible(boolean visible) {
        if (visible) {
            raiseIntel(FacilityIntel.SCOUTED);
        } else {
            intel = FacilityIntel.UNKNOWN;
        }
    }

    public boolean isVisible() {
        return (owner == ForceAlignment.Allied) || getVisible();
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
     * This is a list of scenario modifier IDs that affect scenarios in the same track as this facility. A damaged or
     * crippled facility lends none.
     */
    public List<String> getSharedModifiers() {
        if (getCondition() != FacilityCondition.INTACT) {
            return new ArrayList<>();
        }
        return getProfile().getSharedModifierIds();
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios involving this facility directly: its garrison's,
     * then any added when the facility was placed.
     *
     * <p>The garrison follows a ladder. With none left, the facility has no defenders of its own. At one step it has
     * the current profile's local modifiers; each step above that adds the next garrison modifier for the holding
     * side, skipping any the profile already has.</p>
     */
    public List<String> getLocalModifiers() {
        List<String> modifiers = new ArrayList<>();
        int garrisonSteps = getGarrison();
        if (garrisonSteps >= 1) {
            modifiers.addAll(getProfile().getLocalModifierIds());
        }

        List<String> ladder = isOwnerAlliedToPlayer() ? ALLIED_GARRISON_LADDER : HOSTILE_GARRISON_LADDER;
        int ladderSteps = Math.min(garrisonSteps - 1, ladder.size());
        for (int step = 0; step < ladderSteps; step++) {
            String ladderModifier = ladder.get(step);
            if (!modifiers.contains(ladderModifier)) {
                modifiers.add(ladderModifier);
            }
        }

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
     * Resets the ownership change score once a scenario's outcome has been settled, so it cannot carry over into the
     * next scenario fought on this facility.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void clearOwnershipChangeScore() {
        ownershipChangeScore = 0;
    }

    /**
     * @return whether the facility reveals its whole sector while its current owner holds it; never while crippled
     */
    public boolean isRevealingTrack() {
        return !isCrippled() && getProfile().isRevealingTrack();
    }

    /**
     * @return hexes the facility adds to the scan range of forces scouting its sector while its current owner holds
     *       it, adjusted for its condition
     */
    public int getScanRangeIncrease() {
        return applyCondition(getProfile().getScanRangeIncrease());
    }

    /**
     * @return the facility's change to its sector's scenario odds, adjusted for its condition
     */
    public int getScenarioOddsModifier() {
        return applyCondition(getProfile().getScenarioOddsModifier());
    }

    /**
     * @return the facility's monthly SP (Support Points) change while its current owner holds it, adjusted for its
     *       condition
     */
    public int getMonthlySupportPoints() {
        return applyCondition(getProfile().getMonthlySupportPoints());
    }

    /**
     * @return whether the facility keeps air and space scenarios out of its sector; never while crippled
     */
    public boolean isPreventingAerospace() {
        return !isCrippled() && getProfile().isPreventingAerospace();
    }

    private boolean isCrippled() {
        return getCondition() == FacilityCondition.CRIPPLED;
    }

    /**
     * @param value a numeric effect at full strength
     *
     * @return the value in full while intact, halved (rounding toward zero) while damaged, and nothing while crippled
     */
    private int applyCondition(int value) {
        return switch (getCondition()) {
            case INTACT -> value;
            case DAMAGED -> value / 2;
            case CRIPPLED -> 0;
        };
    }

    /**
     * Worsens the facility after a fight on it that its holder lost: its condition drops one step and its garrison
     * loses one step.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void applyAttackerVictory() {
        setCondition(getCondition().worsened());
        setGarrison(getGarrison() - 1);
    }

    /**
     * Costs the facility one garrison step after a fight on it that its holder did not lose.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void applyDefenderHeld() {
        setGarrison(getGarrison() - 1);
    }

    /**
     * Turns the old-format definition copy held by a facility from a save written before 0.51.01 into a definition ID.
     * The facility is matched to the loaded definition of the same {@link FacilityType}; any local modifiers beyond
     * that definition's own are kept as the facility's additional ones. A facility that matches no loaded definition
     * keeps its old data as a detached definition, so it still behaves as it did.
     *
     * <p>Saves from before 0.51.01 also held a plain visibility flag, which becomes an intel level: seen facilities
     * count as {@link FacilityIntel#SCOUTED}. Tier, condition and garrison take their defaults for such facilities
     * (a full {@link FacilityTier#BASE}).</p>
     *
     * <p>Matching does nothing for a facility that already has a definition ID.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void resolveLegacyData() {
        if (legacyVisible != null) {
            if (intel == null) {
                intel = legacyVisible ? FacilityIntel.SCOUTED : FacilityIntel.UNKNOWN;
            }
            legacyVisible = null;
        }

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
