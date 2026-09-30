/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * How a contract type lays out its facilities: which types turn up and how often, how they split between the player's
 * side and the enemy's, how large they start, how many there are, and whether one anchors the sector. Each StratCon
 * contract definition carries one (see {@code StratConContractDefinition#getFacilityProfile()}); a definition without
 * one places facilities as before.
 *
 * <p>Read from and written to the contract definition's JSON by field.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConContractFacilityProfile {
    /** A paragraph for the contract's StratCon briefing, about what its facilities mean for the player. */
    private String briefing;

    /** How likely each facility type is, relative to the others; a type left out never turns up. */
    private Map<FacilityType, Integer> typeWeights = new LinkedHashMap<>();

    /**
     * The share, from 0 to 1, of the contract's non-objective facilities held by the player's employer, the rest being
     * the enemy's; or {@code null} for all of them to belong to the defending side, as before.
     */
    private Double alliedShare;

    /** How many tiers larger (or, if negative, smaller) than the contract's scale makes them its facilities start. */
    private int tierModifier;

    /** Multiplies how many non-objective facilities the contract places. */
    private double densityMultiplier = 1.0;

    /** The type of a single facility placed before any other, anchoring the sector; or {@code null} for none. */
    private FacilityType anchorType;

    /** Who holds the anchor facility. */
    private ForceAlignment anchorOwner = ForceAlignment.Opposing;

    public StratConContractFacilityProfile() {
    }

    public @Nullable String getBriefing() {
        return briefing;
    }

    public void setBriefing(@Nullable String briefing) {
        this.briefing = briefing;
    }

    public Map<FacilityType, Integer> getTypeWeights() {
        if (typeWeights == null) {
            typeWeights = new LinkedHashMap<>();
        }
        return typeWeights;
    }

    public void setTypeWeights(Map<FacilityType, Integer> typeWeights) {
        this.typeWeights = typeWeights;
    }

    public @Nullable Double getAlliedShare() {
        return alliedShare;
    }

    public void setAlliedShare(@Nullable Double alliedShare) {
        this.alliedShare = alliedShare;
    }

    public int getTierModifier() {
        return tierModifier;
    }

    public void setTierModifier(int tierModifier) {
        this.tierModifier = tierModifier;
    }

    public double getDensityMultiplier() {
        return densityMultiplier;
    }

    public void setDensityMultiplier(double densityMultiplier) {
        this.densityMultiplier = densityMultiplier;
    }

    public @Nullable FacilityType getAnchorType() {
        return anchorType;
    }

    public void setAnchorType(@Nullable FacilityType anchorType) {
        this.anchorType = anchorType;
    }

    public ForceAlignment getAnchorOwner() {
        return (anchorOwner == null) ? ForceAlignment.Opposing : anchorOwner;
    }

    public void setAnchorOwner(@Nullable ForceAlignment anchorOwner) {
        this.anchorOwner = anchorOwner;
    }

    /**
     * @param tier the tier the contract's scale gives
     *
     * @return that tier, moved by this profile's tier modifier and kept between Outpost and Stronghold
     *
     * @author Illiani
     * @since 0.51.01
     */
    public FacilityTier adjustTier(FacilityTier tier) {
        int ordinal = Math.max(0, Math.min(FacilityTier.values().length - 1, tier.ordinal() + tierModifier));
        return FacilityTier.values()[ordinal];
    }

    /**
     * Picks a facility type for the given side, weighted by this profile, among the types that side can hold.
     *
     * @param owner      the side the facility is for
     * @param percentile a roll from 0 to 99, used as a fraction of the total weight
     *
     * @return the type, or {@code null} if this profile weights no type that side can hold
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nullable FacilityType pickType(ForceAlignment owner, int percentile) {
        List<FacilityType> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = 0;
        for (Map.Entry<FacilityType, Integer> entry : getTypeWeights().entrySet()) {
            int weight = (entry.getValue() == null) ? 0 : entry.getValue();
            if ((weight > 0) && (getDefinition(entry.getKey(), owner) != null)) {
                candidates.add(entry.getKey());
                weights.add(weight);
                totalWeight += weight;
            }
        }
        if (totalWeight == 0) {
            return null;
        }

        int roll = (percentile * totalWeight) / 100;
        for (int index = 0; index < candidates.size(); index++) {
            roll -= weights.get(index);
            if (roll < 0) {
                return candidates.get(index);
            }
        }
        return candidates.getLast();
    }

    /**
     * Creates a facility for the given side, of a type picked by this profile (see {@link #pickType}), or of a random
     * type if this profile weights none that side can hold.
     *
     * @param owner the side the facility is for
     *
     * @return the facility, or {@code null} if no facility type has a profile for that side
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable StratConFacility createFacility(ForceAlignment owner) {
        FacilityType facilityType = pickType(owner, Compute.randomInt(100));
        return (facilityType == null) ? createRandomFacility(owner) : createFacility(facilityType, owner);
    }

    /**
     * @param facilityType the facility type
     * @param owner        the side the facility is for
     *
     * @return a new facility of that type, or {@code null} if that side cannot hold one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConFacility createFacility(FacilityType facilityType, ForceAlignment owner) {
        StratConFacilityDefinition definition = getDefinition(facilityType, owner);
        return (definition == null) ? null : new StratConFacility(definition, owner);
    }

    private static @Nullable StratConFacility createRandomFacility(ForceAlignment owner) {
        StratConFacility facility = StratConFacilityDefinition.isAlliedToPlayer(owner) ?
                                          StratConFacilityFactory.getRandomAlliedFacility() :
                                          StratConFacilityFactory.getRandomHostileFacility();
        if (facility != null) {
            facility.setOwner(owner);
        }
        return facility;
    }

    private static @Nullable StratConFacilityDefinition getDefinition(FacilityType facilityType,
          ForceAlignment owner) {
        for (StratConFacilityDefinition definition : StratConFacilityFactory.getDefinitionsFor(owner)) {
            if (definition.getFacilityType() == facilityType) {
                return definition;
            }
        }
        return null;
    }
}
