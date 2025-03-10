/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.UnitTable.Parameters;
import megamek.common.EntityMovementMode;
import megamek.common.MekSummary;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;

/**
 * Data structure that contains parameters relevant to unit generation via the
 * IUnitGenerator interface
 * and is capable of translating itself to
 * megamek.client.ratgenerator.parameters
 *
 * @author NickAragua
 *
 */
public class UnitGeneratorParameters {
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
    public UnitGeneratorParameters clone() {
        UnitGeneratorParameters newParams = new UnitGeneratorParameters();

        newParams.setFaction(faction);
        newParams.setUnitType(unitType);
        newParams.setWeightClass(weightClass);
        newParams.setYear(year);
        newParams.setQuality(quality);
        newParams.setFilter(filter);

        Collection<EntityMovementMode> newModes = new ArrayList<>();
        for (EntityMovementMode movementMode : movementModes) {
            newModes.add(movementMode);
        }

        newParams.setMovementModes(newModes);

        for (MissionRole missionRole : missionRoles) {
            newParams.addMissionRole(missionRole);
        }

        return newParams;
    }

    /**
     * Translate the contents of this data structure into a
     * megamek.client.ratgenerator.Parameters object
     *
     * @return
     */
    public Parameters getRATGeneratorParameters() {
        FactionRecord fRec = Factions.getInstance().getFactionRecordOrFallback(getFaction());
        String rating = RATGeneratorConnector.getFactionSpecificRating(fRec, getQuality());
        List<Integer> weightClasses = new ArrayList<>();

        if (getWeightClass() != AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED) {
            weightClasses.add(getWeightClass());
        }

        Parameters params = new Parameters(fRec, getUnitType(), getYear(), rating, weightClasses,
                ModelRecord.NETWORK_NONE,
                getMovementModes(), getMissionRoles(), 2, fRec);

        return params;
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

    public void clearMissionRoles() {
        missionRoles.clear();
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
}
