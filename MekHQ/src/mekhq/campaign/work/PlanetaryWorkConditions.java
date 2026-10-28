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
package mekhq.campaign.work;

import static mekhq.campaign.personnel.skills.SkillType.S_ZERO_G_OPERATIONS;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_BASIC;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.scenarios.salvage.RecoveryTimeCalculations;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;

/**
 * How the world the force is on affects maintenance and repair work, from the Planetary Conditions Modifiers table
 * (Campaign Operations p. 193). On Colmar, at 1.27 G, a reload that takes 4 minutes on Terra takes 6, and the roll is
 * at +2.
 *
 * <p>Gravity applies wherever the work is done, as no workshop changes it. The atmosphere and the temperature only
 * apply to a unit worked on in the open, at an improvised or field site; a facility shelters the work from them.</p>
 *
 * <p>Nothing applies when the planetary modifiers option is off, or while the force is in transit between worlds.</p>
 */
public final class PlanetaryWorkConditions {
    private static final MMLogger LOGGER = MMLogger.create(PlanetaryWorkConditions.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PlanetaryWorkConditions";

    /** Standard Terran gravity, used when a planet has no gravity recorded. */
    private static final double STANDARD_GRAVITY = 1.0;
    private static final double NO_TIME_PENALTY = 1.0;

    private static final int LOW_GRAVITY_MODIFIER = 1;
    private static final int HIGH_GRAVITY_MODIFIER = 2;
    private static final int VERY_HIGH_GRAVITY_MODIFIER = 4;
    private static final int HOSTILE_ATMOSPHERE_MODIFIER = 2;
    private static final int THIN_OR_DENSE_ATMOSPHERE_MODIFIER = 1;
    private static final int EXTREME_TEMPERATURE_MODIFIER = 1;
    private static final int LOWEST_SAFE_TEMPERATURE = -30;
    private static final int HIGHEST_SAFE_TEMPERATURE = 50;

    /**
     * A gravity penalty to a maintenance or repair roll.
     *
     * @param modifier    the penalty to the target number
     * @param description the name shown for the penalty in the roll's breakdown
     */
    record GravityModifier(int modifier, String description) {}

    private PlanetaryWorkConditions() {}

    /**
     * Adds the penalties for the world the force is on to a maintenance or repair roll.
     *
     * @param campaign the campaign doing the work
     * @param unit     the unit worked on, or {@code null} for a spare part in the warehouse, which is never worked on
     *                 in the open
     * @param tech     the tech doing the work, or {@code null} when nobody is; a tech's Zero-G Operations skill offsets
     *                 the gravity penalty
     * @param target   the roll to add the penalties to
     */
    public static void addRollModifiers(Campaign campaign, @Nullable Unit unit, @Nullable Person tech,
          TargetRoll target) {
        Planet planet = findWorkPlanet(campaign);
        if (planet == null) {
            return;
        }

        GravityModifier gravityModifier = getGravityModifier(getGravity(planet));
        if (gravityModifier != null) {
            target.addModifier(gravityModifier.modifier(), gravityModifier.description());
            addZeroGOperationsModifier(getZeroGOperationsLevel(tech), gravityModifier.modifier(), target);
        }

        if (!isWorkedOnInTheOpen(unit)) {
            return;
        }
        LocalDate today = campaign.getLocalDate();
        AtmosphericTaint taint = planet.getAtmosphere(today);
        Atmosphere pressure = planet.getPressure(today);
        if (taint.isTaintedOrToxic()) {
            target.addModifier(HOSTILE_ATMOSPHERE_MODIFIER, getTextAt(RESOURCE_BUNDLE, "taintedOrToxicAtmosphere"));
        } else if (pressure.isVacuum()) {
            target.addModifier(HOSTILE_ATMOSPHERE_MODIFIER, getTextAt(RESOURCE_BUNDLE, "vacuum"));
        }
        if (pressure.isTrace() || pressure.isVeryHigh()) {
            target.addModifier(THIN_OR_DENSE_ATMOSPHERE_MODIFIER,
                  getTextAt(RESOURCE_BUNDLE, "traceOrVeryHighPressure"));
        }
        if (isExtremeTemperature(planet.getTemperature(today))) {
            target.addModifier(EXTREME_TEMPERATURE_MODIFIER, getTextAt(RESOURCE_BUNDLE, "extremeTemperature"));
        }
    }

    /**
     * Returns how much longer repair work takes on the world the force is on. Each condition adds its share of the
     * normal time, as salvage recovery does: high gravity adds half, so on Colmar a 4-minute reload takes 6 minutes,
     * and a tainted atmosphere there would add another half for 8.
     *
     * @param campaign the campaign doing the work, or {@code null} for a part not yet in a campaign
     * @param unit     the unit worked on, or {@code null} for a spare part in the warehouse, which is never worked on
     *                 in the open
     *
     * @return the factor to multiply the work time by; {@code 1.0} when nothing slows the work
     */
    public static double getTimeMultiplier(@Nullable Campaign campaign, @Nullable Unit unit) {
        if (campaign == null) {
            return NO_TIME_PENALTY;
        }
        Planet planet = findWorkPlanet(campaign);
        if (planet == null) {
            return NO_TIME_PENALTY;
        }

        LocalDate today = campaign.getLocalDate();
        double timeMultiplier = NO_TIME_PENALTY
              + RecoveryTimeCalculations.getGravityMultiplier((float) getGravity(planet));
        if (isWorkedOnInTheOpen(unit)) {
            timeMultiplier += RecoveryTimeCalculations.getAtmosphereMultiplier(planet.getPressure(today),
                  planet.getAtmosphere(today));
            timeMultiplier += RecoveryTimeCalculations.getTemperatureMultiplier(planet.getTemperature(today));
        }
        LOGGER.trace("[PlanetaryWork] Work takes {} times as long here", timeMultiplier);
        return timeMultiplier;
    }

    /**
     * Returns the penalty for working at the given surface gravity: +1 below 0.8 G, +2 above 1.2 G and +4 at 2 G or
     * more.
     *
     * @param gravity the surface gravity, in multiples of Terran standard
     *
     * @return the penalty, or {@code null} at standard gravity
     */
    static @Nullable GravityModifier getGravityModifier(double gravity) {
        if (gravity < 0.8) {
            return new GravityModifier(LOW_GRAVITY_MODIFIER, getTextAt(RESOURCE_BUNDLE, "lowGravity"));
        } else if (gravity >= 2.0) {
            return new GravityModifier(VERY_HIGH_GRAVITY_MODIFIER, getTextAt(RESOURCE_BUNDLE, "veryHighGravity"));
        } else if (gravity > 1.2) {
            return new GravityModifier(HIGH_GRAVITY_MODIFIER, getTextAt(RESOURCE_BUNDLE, "highGravity"));
        }
        return null;
    }

    /**
     * @return the world whose conditions apply to the work, or {@code null} when none do: the option is off, or the
     *       force is in transit
     */
    private static @Nullable Planet findWorkPlanet(Campaign campaign) {
        AbstractLocation location = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        if (!location.isOnPlanet()) {
            LOGGER.trace("[PlanetaryWork] The force is in transit; no planetary conditions apply");
            return null;
        }
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_PLANETARY_MODIFIERS)) {
            LOGGER.trace("[PlanetaryWork] Planetary modifiers are off; no planetary conditions apply");
            return null;
        }
        return location.getPlanet();
    }

    /** A planet may have no gravity recorded; it then counts as standard gravity. */
    private static double getGravity(Planet planet) {
        Double recordedGravity = planet.getGravity();
        return (recordedGravity != null) ? recordedGravity : STANDARD_GRAVITY;
    }

    /** A unit at an improvised or field site is worked on in the open; a spare part in the warehouse is not. */
    private static boolean isWorkedOnInTheOpen(@Nullable Unit unit) {
        return (unit != null) && (unit.getSite() < SITE_FACILITY_BASIC);
    }

    private static boolean isExtremeTemperature(int temperature) {
        return (temperature < LOWEST_SAFE_TEMPERATURE) || (temperature > HIGHEST_SAFE_TEMPERATURE);
    }

    private static int getZeroGOperationsLevel(@Nullable Person tech) {
        if (tech == null) {
            return 0;
        }
        Skill zeroGSkill = tech.getSkill(S_ZERO_G_OPERATIONS);
        return (zeroGSkill == null) ? 0 : zeroGSkill.getTotalSkillLevel(tech.getSkillModifierData());
    }

    /**
     * Offsets a gravity penalty with the tech's Zero-G Operations skill.
     *
     * <ul>
     *   <li>If {@code zeroGSkillLevel >= gravityModifier}, does nothing.</li>
     *   <li>If {@code zeroGSkillLevel < gravityModifier}, applies {@code -zeroGSkillLevel}.</li>
     * </ul>
     *
     * @param zeroGSkillLevel the Zero-G Operations skill level, which negates up to that much of the penalty
     * @param gravityModifier the penalty to offset
     * @param target          the roll to modify
     */
    private static void addZeroGOperationsModifier(int zeroGSkillLevel, int gravityModifier, TargetRoll target) {
        if (zeroGSkillLevel > 0) {
            int effectiveModifier = zeroGSkillLevel >= gravityModifier ? 0 : -zeroGSkillLevel;

            if (effectiveModifier < 0) {
                target.addModifier(effectiveModifier, getTextAt(RESOURCE_BUNDLE, "zeroGOperations"));
            }
        }
    }
}
