/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component;

import megamek.common.Entity;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASRange;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.ai.utility.HasMemory;
import mekhq.campaign.ai.utility.Memory;

import java.util.HashMap;
import java.util.Map;

import static megamek.common.alphaStrike.BattleForceSUA.STD;

/**
 * @author Luana Coppio
 */
public class AcFormation extends SBFFormation implements HasMemory {

    private int targetFormationId = Entity.NONE;
    private EngagementControl engagementControl;
    private boolean engagementControlFailed;
    private boolean highStressEpisode = false;
    private boolean unitIsCrippledLatch = false;
    private final Memory memory = new Memory();
    private boolean clanFormation = false;
    private ASDamageVector stdDamage;

    private final Map<Integer, ASRange> rangeAgainstFormations = new HashMap<>();

    public EngagementControl getEngagementControl() {
        return engagementControl;
    }

    public void setEngagementControl(EngagementControl engagementControl) {
        this.engagementControl = engagementControl;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    public void setTargetFormationId(int targetFormationId) {
        this.targetFormationId = targetFormationId;
    }

    public void setIsClan(boolean clanFormation) {
        this.clanFormation = clanFormation;
    }

    public boolean isClan() {
        return this.clanFormation;
    }

    public boolean isEngagementControlFailed() {
        return engagementControlFailed;
    }

    public void setEngagementControlFailed(boolean engagementControlFailed) {
        this.engagementControlFailed = engagementControlFailed;
    }

    public boolean isRangeSet(int formationId) {
        return rangeAgainstFormations.containsKey(formationId);
    }

    public ASRange getRange(int formationId) {
        return rangeAgainstFormations.computeIfAbsent(formationId, i -> ASRange.LONG);
    }

    public void setRange(int formationId, ASRange range) {
        this.rangeAgainstFormations.put(formationId, range);
    }

    public void setHighStressEpisode(boolean highStressEpisode) {
        this.highStressEpisode = highStressEpisode;
    }

    public void reset() {
        highStressEpisode = false;
        rangeAgainstFormations.clear();
        targetFormationId = Entity.NONE;
        engagementControl = null;
        setDone(false);
    }

    public int getCurrentMovement() {
        return getUnits().stream().mapToInt(u -> Math.max(0, u.getMovement() - u.getMpCrits())).min().orElse(0);
    }

    public boolean hadHighStressEpisode() {
        return highStressEpisode;
    }

    /**
     * Checks if the formation is crippled. Rules as described on Interstellar Operations BETA pg 242 - Crippling Damage
     * @return true in case it is crippled
     */
    public boolean isCrippled() {
        if (!unitIsCrippledLatch) {
            var halfOfUnitsDoZeroDamage = getUnits().stream()
                .filter(u -> !u.getCurrentDamage().hasDamage())
                .count() >= Math.ceil(getUnits().size() / 2.0);
            var hasVeryDamagedMovement = (getCurrentMovement() <= 1) && (getMovement() >= 3);

            var halfOfUnitsHaveTwentyPercentOfArmorOrLess = getUnits().stream()
                .filter(u -> u.getCurrentArmor() <= Math.floor(0.2 * u.getArmor()))
                .count() >= Math.ceil(getUnits().size() / 2.0);

            var halfOfUnitsTookTwoTargetDamageOrMore = getUnits().stream()
                .filter(u -> u.getTargetingCrits() >= 2)
                .count() >= Math.ceil(getUnits().size() / 2.0);

            // Sets the latch for crippled variable so it is not recalculated
            unitIsCrippledLatch = hasVeryDamagedMovement
                || halfOfUnitsDoZeroDamage
                || halfOfUnitsHaveTwentyPercentOfArmorOrLess
                || halfOfUnitsTookTwoTargetDamageOrMore;
        }

        return unitIsCrippledLatch;
    }

    public void setStdDamage(ASDamageVector stdDamage) {
        this.stdDamage = stdDamage;
    }

    @Override
    public ASDamageVector getStdDamage() {
        if (hasSUA(STD)) {
            return (ASDamageVector) getSUA(STD);
        }
        return stdDamage;
    }

    @Override
    public Memory getMemory() {
        return memory;
    }
}
