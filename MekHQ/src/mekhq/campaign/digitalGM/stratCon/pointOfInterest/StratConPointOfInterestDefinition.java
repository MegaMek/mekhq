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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * The fixed, per-type data of a StratCon point of interest, loaded from JSON (see
 * {@link StratConPointOfInterestDefinitions}) or registered from code.
 *
 * <p>A placed {@link StratConPointOfInterest} records only its {@link #getTypeId() type ID}; everything that is the
 * same for every point of interest of a type lives here, so that changes to a definition reach points of interest
 * already sitting in saved campaigns.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPointOfInterestDefinition {
    private static final MMLogger LOGGER = MMLogger.create(StratConPointOfInterestDefinition.class);

    private String typeId;
    private String displayableName;
    private String description;
    // the map sprite, as a path from the working directory (for example data/images/stratcon/...); none draws a marker
    private String imagePath;

    // whether this point of interest takes up its hex, the way a facility or scenario does
    private boolean occupiesHex;
    // whether this point of interest stays hidden from the player until its hex is scouted
    private boolean hiddenUntilScouted;

    // the owner newly placed points of interest start with; null means neutral
    private ForceAlignment defaultOwner;
    // how many days a newly placed point of interest lasts; 0 or less means it never expires
    private int lifespanDays;
    // the sides of a die rolled and added to lifespanDays when a point of interest is placed; 0 or less rolls nothing
    private int lifespanDieSides;
    // whether an expired point of interest is removed from the map, rather than left in place marked as expired
    private boolean removeOnExpiry;

    // sector-wide effects, applied through the type's behavior (see IStratConPointOfInterestBehavior)
    private int scenarioOddsModifier;
    private int scanRangeIncrease;

    // placement rules, used when a contract or external code places a point of interest without chosen coordinates
    private boolean landOnly = true;
    // The JSON mapper sets fields directly, bypassing the setter's null guard, so an explicit null becomes empty here.
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> allowedTerrainCategories = new ArrayList<>();
    private boolean avoidCities;

    private String behaviorId = StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID;

    /**
     * Reads a point of interest definition from the given file.
     *
     * @param filePath the JSON file to read
     *
     * @return the definition, or {@code null} if the file does not exist or could not be read (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConPointOfInterestDefinition deserialize(String filePath) {
        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", filePath);
            return null;
        }

        try {
            return StratConPointOfInterestJson.fromFile(inputFile, StratConPointOfInterestDefinition.class);
        } catch (Exception exception) {
            LOGGER.error("Error deserializing point of interest definition {}", filePath, exception);
            return null;
        }
    }

    /**
     * Writes this definition to the given JSON file, for the developer tools' point of interest editor.
     *
     * @param outputFile the destination file
     *
     * @return {@code true} if the file was written, {@code false} if an error occurred (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean serialize(File outputFile) {
        try {
            StratConPointOfInterestJson.toFile(this, outputFile);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Error serializing point of interest definition {}", outputFile.getPath(), exception);
            return false;
        }
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getDisplayableName() {
        return displayableName;
    }

    public void setDisplayableName(String displayableName) {
        this.displayableName = displayableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the path, from the working directory, of the sprite drawn on the map for this type, or {@code null} to
     *       draw a plain marker instead
     */
    public @Nullable String getImagePath() {
        return imagePath;
    }

    public void setImagePath(@Nullable String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isOccupiesHex() {
        return occupiesHex;
    }

    public void setOccupiesHex(boolean occupiesHex) {
        this.occupiesHex = occupiesHex;
    }

    public boolean isHiddenUntilScouted() {
        return hiddenUntilScouted;
    }

    public void setHiddenUntilScouted(boolean hiddenUntilScouted) {
        this.hiddenUntilScouted = hiddenUntilScouted;
    }

    /**
     * @return the owner newly placed points of interest of this type start with, or {@code null} for neutral
     */
    public @Nullable ForceAlignment getDefaultOwner() {
        return defaultOwner;
    }

    public void setDefaultOwner(@Nullable ForceAlignment defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    /**
     * @return how many days a newly placed point of interest of this type lasts; 0 or less means it never expires
     */
    public int getLifespanDays() {
        return lifespanDays;
    }

    public void setLifespanDays(int lifespanDays) {
        this.lifespanDays = lifespanDays;
    }

    /**
     * @return the number of sides of a die rolled and added to {@link #getLifespanDays()} each time a point of interest
     *       of this type is placed - so a lifespan of 0 with a 6-sided die lasts 1 to 6 days; 0 or less rolls nothing
     */
    public int getLifespanDieSides() {
        return lifespanDieSides;
    }

    public void setLifespanDieSides(int lifespanDieSides) {
        this.lifespanDieSides = lifespanDieSides;
    }

    /**
     * Works out how many days a newly placed point of interest of this type lasts: its fixed lifespan plus, if it has
     * a lifespan die, one roll of that die.
     *
     * @return the rolled lifespan in days; 0 or less means it never expires
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int rollLifespanDays() {
        if (lifespanDieSides <= 0) {
            return lifespanDays;
        }

        return lifespanDays + Compute.randomInt(lifespanDieSides) + 1;
    }

    public boolean isRemoveOnExpiry() {
        return removeOnExpiry;
    }

    public void setRemoveOnExpiry(boolean removeOnExpiry) {
        this.removeOnExpiry = removeOnExpiry;
    }

    /**
     * @return how much each point of interest of this type shifts its sector's scenario odds; applied while it is
     *       active, by default
     */
    public int getScenarioOddsModifier() {
        return scenarioOddsModifier;
    }

    public void setScenarioOddsModifier(int scenarioOddsModifier) {
        this.scenarioOddsModifier = scenarioOddsModifier;
    }

    /**
     * @return how many hexes each point of interest of this type adds to its sector's scan range; applied while it is
     *       active and held by the player or an ally, by default
     */
    public int getScanRangeIncrease() {
        return scanRangeIncrease;
    }

    public void setScanRangeIncrease(int scanRangeIncrease) {
        this.scanRangeIncrease = scanRangeIncrease;
    }

    public boolean isLandOnly() {
        return landOnly;
    }

    public void setLandOnly(boolean landOnly) {
        this.landOnly = landOnly;
    }

    /**
     * @return the terrain this point of interest may be placed on, each entry naming a terrain type (such as
     *       {@code Plains}) or a terrain category (such as {@code VEGETATION}), matched ignoring case; empty means any
     */
    public List<String> getAllowedTerrainCategories() {
        return allowedTerrainCategories;
    }

    public void setAllowedTerrainCategories(List<String> allowedTerrainCategories) {
        this.allowedTerrainCategories = (allowedTerrainCategories == null) ?
                                              new ArrayList<>() :
                                              new ArrayList<>(allowedTerrainCategories);
    }

    public boolean isAvoidCities() {
        return avoidCities;
    }

    public void setAvoidCities(boolean avoidCities) {
        this.avoidCities = avoidCities;
    }

    /**
     * @return the ID of the {@link IStratConPointOfInterestBehavior} that drives this type, as registered with
     *       {@link StratConPointOfInterestBehaviors}
     */
    public String getBehaviorId() {
        return behaviorId;
    }

    public void setBehaviorId(String behaviorId) {
        this.behaviorId = behaviorId;
    }

    @Override
    public String toString() {
        return (displayableName == null) ? typeId : displayableName;
    }
}
