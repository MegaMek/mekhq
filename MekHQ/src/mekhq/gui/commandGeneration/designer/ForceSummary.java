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
package mekhq.gui.commandGeneration.designer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.Unit;

/**
 * What a rolled command adds up to: how many units it has and how many were struck out, its battle value, tonnage
 * and weight split, its pilots' average skill, its ships, and the crews it brings. Read from the rolled tree, so it
 * describes the command before anything is built.
 *
 * <p>Battle value and price are worked out once per unit and remembered in the cache the caller passes, because a
 * regiment has well over a hundred units and the summary is refreshed on every change.</p>
 */
public final class ForceSummary {
    /** The summary of nothing rolled. */
    static final ForceSummary EMPTY = new ForceSummary();

    private int rolledUnits;
    private int includedUnits;
    private int battleValue;
    private double tonnage;
    private final int[] weightCounts = new int[EntityWeightClass.WEIGHT_ASSAULT + 1];
    private int pilotsCounted;
    private int gunneryTotal;
    private int pilotingTotal;
    private int jumpCapable;
    private int combatUnits;
    private Money purchaseValue = Money.zero();
    private int dropShips;
    private int jumpShips;
    private int warShips;
    private int crew;
    private int combatCrew;
    private final List<Entity> includedEntities = new ArrayList<>();
    private @Nullable ForceDescriptor topCommand;

    /** The battle value and price of each unit already worked out, by entity identity. */
    public static final class Cache {
        private final Map<Entity, Integer> battleValues = new IdentityHashMap<>();
        private final Map<Entity, Money> prices = new IdentityHashMap<>();

        int battleValue(Entity entity) {
            return battleValues.computeIfAbsent(entity, Entity::calculateBattleValue);
        }

        Money price(Entity entity, CampaignOptions campaignOptions) {
            return prices.computeIfAbsent(entity, key -> Unit.estimateBuyCost(key, campaignOptions));
        }
    }

    private ForceSummary() {
    }

    /**
     * @param modelRoot       the rolled command, as the force generator view holds it, or {@code null}
     * @param campaignOptions the options prices are worked out under
     * @param cache           remembers battle values and prices between summaries
     *
     * @return the summary
     */
    static ForceSummary of(@Nullable ForceDescriptor modelRoot, CampaignOptions campaignOptions, Cache cache) {
        if (modelRoot == null) {
            return EMPTY;
        }
        ForceSummary summary = new ForceSummary();
        // The view wraps the command in a model root of its own; the command itself is its only child.
        summary.topCommand = modelRoot.getSubForces().isEmpty() ? modelRoot : modelRoot.getSubForces().get(0);
        summary.add(modelRoot, true, campaignOptions, cache);
        return summary;
    }

    private void add(ForceDescriptor node, boolean ancestorsIncluded, CampaignOptions campaignOptions, Cache cache) {
        boolean included = ancestorsIncluded && node.isIncluded();
        Entity entity = node.getEntity();
        if (entity != null) {
            rolledUnits++;
            if (included) {
                countIncluded(node, entity, campaignOptions, cache);
            }
        }
        for (ForceDescriptor child : node.getSubForces()) {
            add(child, included, campaignOptions, cache);
        }
        for (ForceDescriptor child : node.getAttached()) {
            add(child, included, campaignOptions, cache);
        }
    }

    private void countIncluded(ForceDescriptor node, Entity entity, CampaignOptions campaignOptions, Cache cache) {
        includedUnits++;
        includedEntities.add(entity);
        battleValue += cache.battleValue(entity);
        tonnage += entity.getWeight();
        purchaseValue = purchaseValue.plus(cache.price(entity, campaignOptions));
        int unitCrew = Compute.getFullCrewSize(entity);
        crew += unitCrew;

        int unitType = entity.getUnitType();
        if (unitType == UnitType.DROPSHIP) {
            dropShips++;
            return;
        }
        if (unitType == UnitType.JUMPSHIP) {
            jumpShips++;
            return;
        }
        if ((unitType == UnitType.WARSHIP) || (unitType == UnitType.SPACE_STATION)) {
            warShips++;
            return;
        }
        combatUnits++;
        combatCrew += unitCrew;
        int weightClass = entity.getWeightClass();
        if ((weightClass >= EntityWeightClass.WEIGHT_LIGHT) && (weightClass <= EntityWeightClass.WEIGHT_ASSAULT)) {
            weightCounts[weightClass]++;
        }
        if (entity.getJumpMP() > 0) {
            jumpCapable++;
        }
        CrewDescriptor pilot = node.getCo();
        if (pilot != null) {
            pilotsCounted++;
            gunneryTotal += pilot.getGunnery();
            pilotingTotal += pilot.getPiloting();
        }
    }

    /** @return every unit rolled, struck out or not */
    public int getRolledUnits() {
        return rolledUnits;
    }

    /** @return the units the build will create */
    public int getIncludedUnits() {
        return includedUnits;
    }

    /** @return the units struck out of the roll */
    public int getStruckUnits() {
        return rolledUnits - includedUnits;
    }

    public int getBattleValue() {
        return battleValue;
    }

    public double getTonnage() {
        return tonnage;
    }

    /**
     * @param weightClass one of the {@link EntityWeightClass} light to assault constants
     *
     * @return how many of the included combat units are of that weight class
     */
    public int getWeightCount(int weightClass) {
        return ((weightClass >= 0) && (weightClass < weightCounts.length)) ? weightCounts[weightClass] : 0;
    }

    /** @return {@code true} if any included unit has a pilot to average */
    public boolean hasPilots() {
        return pilotsCounted > 0;
    }

    /** @return the included pilots' average gunnery, rounded */
    public int getAverageGunnery() {
        return (pilotsCounted == 0) ? 0 : Math.round(gunneryTotal / (float) pilotsCounted);
    }

    /** @return the included pilots' average piloting, rounded */
    public int getAveragePiloting() {
        return (pilotsCounted == 0) ? 0 : Math.round(pilotingTotal / (float) pilotsCounted);
    }

    /** @return the included combat units that can jump */
    public int getJumpCapable() {
        return jumpCapable;
    }

    /** @return the included units that are not ships */
    public int getCombatUnits() {
        return combatUnits;
    }

    /** @return what buying the included units new would cost */
    public Money getPurchaseValue() {
        return purchaseValue;
    }

    public int getDropShips() {
        return dropShips;
    }

    public int getJumpShips() {
        return jumpShips;
    }

    public int getWarShips() {
        return warShips;
    }

    /** @return the full crews of every included unit, ships included */
    public int getCrew() {
        return crew;
    }

    /** @return the full crews of the included units that are not ships */
    public int getCombatCrew() {
        return combatCrew;
    }

    /** @return the included units' entities, for estimating what they will need */
    public List<Entity> getIncludedEntities() {
        return Collections.unmodifiableList(includedEntities);
    }

    /** @return the rolled command under the view's model root, or {@code null} if nothing is rolled */
    public @Nullable ForceDescriptor getTopCommand() {
        return topCommand;
    }

    /** @return {@code true} if anything has been rolled */
    public boolean hasRoll() {
        return rolledUnits > 0;
    }
}
