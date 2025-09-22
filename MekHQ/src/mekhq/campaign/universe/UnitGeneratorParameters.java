/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.Parameters;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;

/**
 * Data structure that contains parameters relevant to unit generation via the IUnitGenerator interface and is capable
 * of translating itself to megamek.client.ratgenerator.parameters
 *
 * @author NickAragua
 */
public class UnitGeneratorParameters implements Cloneable {
    private static final MMLogger logger = MMLogger.create(UnitGeneratorParameters.class);

    private String faction;
    private int unitType;
    private int weightClass;
    private int year;
    private int quality;
    private Collection<EntityMovementMode> movementModes;
    private Predicate<MekSummary> filter;
    private Collection<MissionRole> missionRoles;

    public UnitGeneratorParameters() {
        movementModes = new ArrayList<>();
        setMissionRoles(new ArrayList<>());
    }

    /**
     * Thorough deep clone of this generator parameters object.
     */
    @Override
    public @Nullable UnitGeneratorParameters clone() {
        try {
            UnitGeneratorParameters unitGeneratorParameters = (UnitGeneratorParameters) super.clone();
            unitGeneratorParameters.setFaction(faction);
            unitGeneratorParameters.setUnitType(unitType);
            unitGeneratorParameters.setWeightClass(weightClass);
            unitGeneratorParameters.setYear(year);
            unitGeneratorParameters.setQuality(quality);
            unitGeneratorParameters.setFilter(filter);

            Collection<EntityMovementMode> newModes = new ArrayList<>(movementModes);

            unitGeneratorParameters.setMovementModes(newModes);

            // We need a separate copy of the missionRoles collection to avoid concurrent modification
            for (MissionRole missionRole : new ArrayList<>(missionRoles)) {
                unitGeneratorParameters.addMissionRole(missionRole);
            }

            return unitGeneratorParameters;
        } catch (CloneNotSupportedException e) {
            logger.error("Failed to clone UnitGeneratorParameters. State of the object: {}", this, e);
            return null;
        }
    }

    /**
     * Translate the contents of this data structure into a megamek.client.ratgenerator.Parameters object
     *
     */
    public Parameters getRATGeneratorParameters() {
        FactionRecord fRec = Factions.getInstance().getFactionRecordOrFallback(getFaction());
        String rating = RATGeneratorConnector.getFactionSpecificRating(fRec, getQuality());
        List<Integer> weightClasses = new ArrayList<>();

        if (getWeightClass() != AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED) {
            weightClasses.add(getWeightClass());
        }

        return new Parameters(fRec,
              getUnitType(),
              getYear(),
              rating,
              weightClasses,
              ModelRecord.NETWORK_NONE,
              getMovementModes(),
              getMissionRoles(),
              2,
              fRec);
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(int unitType) {
        this.unitType = unitType;
    }

    public int getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(int weightClass) {
        this.weightClass = weightClass;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public Collection<EntityMovementMode> getMovementModes() {
        return movementModes;
    }

    public void setMovementModes(Collection<EntityMovementMode> movementModes) {
        this.movementModes = movementModes;
    }

    public void clearMovementModes() {
        movementModes.clear();
    }

    public Collection<MissionRole> getMissionRoles() {
        return missionRoles;
    }

    public void setMissionRoles(Collection<MissionRole> missionRoles) {
        this.missionRoles = missionRoles;
    }

    public void addMissionRole(MissionRole role) {
        missionRoles.add(role);
    }

    public Predicate<MekSummary> getFilter() {
        return filter;
    }

    public void setFilter(Predicate<MekSummary> filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return "UnitGeneratorParameters{" +
                     "faction=" +
                     faction +
                     ", unitType=" +
                     unitType +
                     ", weightClass=" +
                     weightClass +
                     ", year=" +
                     year +
                     ", quality=" +
                     quality +
                     ", filter=" +
                     filter +
                     ", movementModes=" +
                     movementModes +
                     ", missionRoles=" +
                     missionRoles +
                     '}';
    }
}
