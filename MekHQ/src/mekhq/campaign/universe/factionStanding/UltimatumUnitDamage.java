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
package mekhq.campaign.universe.factionStanding;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import megamek.common.compute.damage.PreExistingDamageApplier;
import megamek.common.compute.damage.PreExistingDamageCommitter;
import megamek.common.compute.damage.PreExistingDamageLevel;
import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;

/**
 * Damages a campaign's units after a violent Faction Standing ultimatum, to represent the fighting between the
 * personnel who followed the player's decision and those who refused.
 *
 * <p>The more of the campaign's personnel that left, the more units are damaged and the heavier the damage: the share
 * of eligible units damaged equals the share of personnel who left, and the damage level is picked from that share
 * (see {@link #getDamageLevel(double)}). Damage is rolled with MegaMek's pre-existing damage rules, the same ones the
 * unit editor uses, so it never destroys or immobilizes a unit.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class UltimatumUnitDamage {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingUltimatumDialog";

    /** Below this share of personnel leaving, units take Light damage. */
    static final double MODERATE_DAMAGE_THRESHOLD = 0.15;

    /** At or above this share of personnel leaving, units take Heavy damage. */
    static final double HEAVY_DAMAGE_THRESHOLD = 0.35;

    private UltimatumUnitDamage() {}

    /**
     * Captures who was still with the campaign before the ultimatum resolved, so the departures can be counted
     * afterward.
     *
     * @param campaign the current campaign
     *
     * @return the active personnel, excluding prisoners and camp followers
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Person> snapshotPersonnel(Campaign campaign) {
        return new ArrayList<>(campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false));
    }

    /**
     * Works out what share of the personnel captured before the ultimatum have since left the campaign or died.
     *
     * @param personnelBefore the personnel captured by {@link #snapshotPersonnel(Campaign)}
     *
     * @return the share who left, from 0.0 to 1.0; 0.0 if there was nobody to begin with
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getDepartedShare(Collection<Person> personnelBefore) {
        if (personnelBefore.isEmpty()) {
            return 0.0;
        }

        int departedCount = 0;
        for (Person person : personnelBefore) {
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit() || status.isDead()) {
                departedCount++;
            }
        }

        return (double) departedCount / personnelBefore.size();
    }

    /**
     * Picks how badly each damaged unit is hurt.
     *
     * @param departedShare the share of personnel who left, from 0.0 to 1.0
     *
     * @return {@link PreExistingDamageLevel#NONE} if nobody left, {@link PreExistingDamageLevel#LIGHT} below
     *       {@link #MODERATE_DAMAGE_THRESHOLD}, {@link PreExistingDamageLevel#MODERATE} below
     *       {@link #HEAVY_DAMAGE_THRESHOLD}, and {@link PreExistingDamageLevel#HEAVY} otherwise
     *
     * @author Illiani
     * @since 0.51.01
     */
    static PreExistingDamageLevel getDamageLevel(double departedShare) {
        if (departedShare <= 0.0) {
            return PreExistingDamageLevel.NONE;
        } else if (departedShare < MODERATE_DAMAGE_THRESHOLD) {
            return PreExistingDamageLevel.LIGHT;
        } else if (departedShare < HEAVY_DAMAGE_THRESHOLD) {
            return PreExistingDamageLevel.MODERATE;
        }
        return PreExistingDamageLevel.HEAVY;
    }

    /**
     * Works out how many units to damage. The share of units damaged matches the share of personnel who left, rounded
     * up, so any departure damages at least one unit.
     *
     * @param eligibleUnitCount the number of units that can be damaged
     * @param departedShare     the share of personnel who left, from 0.0 to 1.0
     *
     * @return the number of units to damage, from 0 to {@code eligibleUnitCount}
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getUnitsToDamage(int eligibleUnitCount, double departedShare) {
        if ((eligibleUnitCount <= 0) || (departedShare <= 0.0)) {
            return 0;
        }

        int unitsToDamage = (int) Math.ceil(eligibleUnitCount * Math.min(departedShare, 1.0));
        return Math.min(unitsToDamage, eligibleUnitCount);
    }

    /**
     * Determines whether a unit can be damaged by the fighting: it must be with the campaign and in service, and of a
     * type the pre-existing damage rules cover (Meks, combat vehicles, and fighters).
     *
     * @param unit the unit to check
     *
     * @return {@code true} if the unit can be damaged
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isEligible(Unit unit) {
        Entity entity = unit.getEntity();
        if (entity == null || entity.isDestroyed()) {
            return false;
        }

        if (!unit.isPresent() || unit.isMothballed() || unit.isRefitting()) {
            return false;
        }

        return PreExistingDamageApplier.isSupported(entity);
    }

    /**
     * Damages a share of the campaign's eligible units, picked at random, according to how many personnel left.
     *
     * @param campaign      the current campaign
     * @param departedShare the share of personnel who left, from 0.0 to 1.0
     *
     * @return the units that were damaged
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Unit> damageUnits(Campaign campaign, double departedShare) {
        PreExistingDamageLevel damageLevel = getDamageLevel(departedShare);
        if (damageLevel == PreExistingDamageLevel.NONE) {
            return List.of();
        }

        List<Unit> eligibleUnits = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if (isEligible(unit)) {
                eligibleUnits.add(unit);
            }
        }

        int unitsToDamage = getUnitsToDamage(eligibleUnits.size(), departedShare);
        if (unitsToDamage == 0) {
            return List.of();
        }

        Collections.shuffle(eligibleUnits);
        List<Unit> damagedUnits = new ArrayList<>();
        for (Unit unit : eligibleUnits.subList(0, unitsToDamage)) {
            if (PreExistingDamageCommitter.rollAndApply(unit.getEntity(), damageLevel)) {
                unit.runDiagnostic(false);
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
                damagedUnits.add(unit);
            }
        }

        if (!damagedUnits.isEmpty()) {
            campaign.addReport(DailyReportType.TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "FactionStandingUltimatumDialog.unitDamage.report",
                  damagedUnits.size(),
                  getTextAt(RESOURCE_BUNDLE, "FactionStandingUltimatumDialog.unitDamage.level." + damageLevel.name())));
        }

        return damagedUnits;
    }
}
