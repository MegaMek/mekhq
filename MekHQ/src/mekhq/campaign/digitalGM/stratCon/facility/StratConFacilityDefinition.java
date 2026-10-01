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
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * A facility type, read from a definition file: its name, its map biomes, and what it does for whichever side holds
 * it. A {@link StratConFacility} on the map refers to its definition by ID and keeps only its own state, so a change
 * to the data reaches facilities already placed in running campaigns.
 *
 * <p>Either profile may be absent. A definition converted from a single old-format file has only the profile of the
 * side that file was written for; while the other side holds such a facility it has no effects.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityDefinition {
    private static final StratConFacilityProfile EMPTY_PROFILE = new StratConFacilityProfile();

    private String id;
    private String displayableName;
    private FacilityType facilityType;
    private List<StratConBiome> biomes = new ArrayList<>();
    private StratConFacilityProfile alliedProfile;
    private StratConFacilityProfile hostileProfile;

    private final transient TreeMap<Integer, StratConBiome> biomeTempMap = new TreeMap<>();

    public StratConFacilityDefinition() {
    }

    /**
     * @param id              the definition's ID
     * @param displayableName the name shown to the player
     * @param facilityType    the facility type, which picks its map icon
     * @param alliedProfile   what it does while the player's side holds it, or {@code null} for nothing
     * @param hostileProfile  what it does while the enemy holds it, or {@code null} for nothing
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityDefinition(String id, String displayableName, FacilityType facilityType,
          @Nullable StratConFacilityProfile alliedProfile, @Nullable StratConFacilityProfile hostileProfile) {
        this.id = id;
        this.displayableName = displayableName;
        this.facilityType = facilityType;
        this.alliedProfile = alliedProfile;
        this.hostileProfile = hostileProfile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    /**
     * @return the biomes the facility's hex may take its terrain from, never {@code null}
     */
    public List<StratConBiome> getBiomes() {
        return (biomes == null) ? Collections.emptyList() : Collections.unmodifiableList(biomes);
    }

    public void setBiomes(List<StratConBiome> biomes) {
        this.biomes = new ArrayList<>(biomes);
        rebuildBiomeTempMap();
    }

    /**
     * Returns the biome temperature map (note: temperature mapping is in kelvins but stored in Celsius)
     */
    public TreeMap<Integer, StratConBiome> getBiomeTempMap() {
        return biomeTempMap;
    }

    /**
     * Rebuilds the transient biome temperature map from the biome list. Called after loading from a file, which sets
     * the list directly.
     *
     * @author Illiani
     * @since 0.51.01
     */
    void rebuildBiomeTempMap() {
        biomeTempMap.clear();
        for (StratConBiome biome : getBiomes()) {
            biomeTempMap.put(biome.allowedTemperatureLowerBound, biome);
        }
    }

    public @Nullable StratConFacilityProfile getAlliedProfile() {
        return alliedProfile;
    }

    public void setAlliedProfile(@Nullable StratConFacilityProfile alliedProfile) {
        this.alliedProfile = alliedProfile;
    }

    public @Nullable StratConFacilityProfile getHostileProfile() {
        return hostileProfile;
    }

    public void setHostileProfile(@Nullable StratConFacilityProfile hostileProfile) {
        this.hostileProfile = hostileProfile;
    }

    /**
     * @param owner the side holding the facility
     *
     * @return {@code true} if the owner counts as the player's side: the player or an ally
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isAlliedToPlayer(@Nullable ForceAlignment owner) {
        return (owner == ForceAlignment.Allied) || (owner == ForceAlignment.Player);
    }

    /**
     * @param owner the side holding the facility
     *
     * @return the profile that applies while that side holds it; an empty profile when the definition has none for
     *       that side, never {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityProfile getProfileFor(@Nullable ForceAlignment owner) {
        StratConFacilityProfile profile = isAlliedToPlayer(owner) ? alliedProfile : hostileProfile;
        return (profile == null) ? EMPTY_PROFILE : profile;
    }

    /**
     * @param owner the side that would hold the facility
     *
     * @return {@code true} if the definition has a profile for that side, so it is worth placing for them
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean hasProfileFor(@Nullable ForceAlignment owner) {
        return (isAlliedToPlayer(owner) ? alliedProfile : hostileProfile) != null;
    }
}
