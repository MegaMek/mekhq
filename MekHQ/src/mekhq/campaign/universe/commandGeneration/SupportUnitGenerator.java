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
package mekhq.campaign.universe.commandGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.VehicleCrewSource;

/**
 * Generates the free support vehicles a command is granted for its various support capabilities, and
 * files them into the TOE via {@link AddSupportUnitsToTOE}. Each capability maps to a themed vehicle
 * and a {@link SupportTOEFormationTypes} formation:
 *
 * <ul>
 *   <li>Salvage - recovery vehicles (one per formation base size, doubled for Clan)</li>
 *   <li>Logistics - flatbed trucks (one per formation base size, doubled for Clan)</li>
 *   <li>Medical - MASH trucks, scaled so their theatres can treat the command's combatants as
 *       potential patients (uses the same MASH theatre capacity as the in-play medical check)</li>
 *   <li>Commissary - mobile canteens, scaled so their field kitchens can feed the command's
 *       personnel (mirrors the daily field-kitchen check in {@link Fatigue})</li>
 *   <li>Security - rifle infantry sized to the force it protects: a company-sized force gets a
 *       squad, a battalion a platoon, and a regiment or larger a company (Clan: rifle Squads/Points)</li>
 * </ul>
 *
 * <p>These vehicles are generated crewed, so their crews are the "engineers" and support troops that
 * operate them. This is the single source of truth used by both the campaign-option confirmation
 * dialogs and the company-generation pipeline.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportUnitGenerator {
    private static final MMLogger LOGGER = MMLogger.create(SupportUnitGenerator.class);

    private static final String SECURITY_SQUAD_INNER_SPHERE = "Foot Squad (Rifle)";
    private static final String SECURITY_SQUAD_CLAN = "Clan Foot Squad (Rifle)";
    private static final String SECURITY_PLATOON_INNER_SPHERE = "Foot Platoon (Rifle)";
    private static final String SECURITY_PLATOON_CLAN = "Clan Foot Point (Rifle Light)";

    /**
     * Cargo tons assumed per truck when the faction could field no cargo vehicle at all in this year, which only
     * happens before any exists. Sizing still produces a number so the rest of the pipeline has one; the roll then
     * fields nothing, which is the correct outcome.
     */
    private static final double FALLBACK_CARGO_TONS_PER_TRUCK = 6.0;

    /** Vehicles per point for the formation-sized capabilities (salvage, logistics) in a Clan command. */
    private static final int CLAN_VEHICLES_PER_POINT = 2;

    /**
     * Combat tonnage that generates one ton of a resupply drop, the same divider
     * {@link Resupply#calculateTargetCargoTonnage} applies before its cap and floor.
     */
    private static final int COMBAT_TONNAGE_PER_CARGO_TON = 125;

    /**
     * Combat units the command fields per recovery vehicle it is given. The opposing force is built to the player's
     * own budget, so a command meets roughly its own number of units, of which about a quarter can be dragged home,
     * and one recovery vehicle is counted per wreck. Used as the divisor on the combat unit count.
     */
    private static final int COMBAT_UNITS_PER_RECOVERY_VEHICLE = 4;

    /** Combat personnel in a company-sized force; at or below this the security detail is a single squad. */
    static final int COMPANY_COMBATANT_CEILING = 12;

    /** Combat personnel in a battalion-sized force; at or below this (but above a company) the detail is one platoon. */
    static final int BATTALION_COMBATANT_CEILING = 36;

    /** Platoons that make up the company-sized security detail granted to a regiment or larger. */
    static final int PLATOONS_PER_COMPANY = 3;

    /**
     * Size of the security detail, chosen by the size of the force it protects: a company-sized force
     * gets a {@link #SQUAD}, a battalion a {@link #PLATOON}, and a regiment or larger a {@link #COMPANY}
     * (fielded as {@link #PLATOONS_PER_COMPANY} platoons).
     */
    enum SecurityTier {
        SQUAD, PLATOON, COMPANY
    }

    private SupportUnitGenerator() {
        // utility class
    }

    /**
     * Grants the vehicles of one support capability, crewed the way the campaign crews everything else: one named
     * crew member plus the temporary crew pool where that role uses temporary crews, a full crew of individual
     * personnel where it does not.
     *
     * @param capability      the support capability being granted
     * @param campaign        the campaign the vehicles are generated into
     * @param faction         the faction whose ranks the crews are given
     * @param autoAssignRanks whether generated crews have ranks assigned automatically
     */
    public static void generate(SupportCapability capability, Campaign campaign, Faction faction,
          boolean autoAssignRanks) {
        generate(capability, campaign, faction, autoAssignRanks, null);
    }

    /**
     * Grants the vehicles of one support capability, with the crewing forced rather than taken from the campaign's
     * temporary crew options. Used by the campaign-option dialog, where the player chooses.
     *
     * @param capability      the support capability being granted
     * @param campaign        the campaign the vehicles are generated into
     * @param faction         the faction whose ranks the crews are given
     * @param autoAssignRanks whether generated crews have ranks assigned automatically
     * @param crewSource      the crewing to use, or {@code null} to follow the campaign's temporary crew options
     */
    public static void generate(SupportCapability capability, Campaign campaign, Faction faction,
          boolean autoAssignRanks, @Nullable VehicleCrewSource crewSource) {
        generate(capability, campaign, faction, autoAssignRanks, capability.unitName(campaign),
              capability.targetCount(campaign, faction),
              capability.formationType(), crewSource);
    }

    /**
     * Crews a freshly built support unit and reports whether it was crewed from the temporary crew pool.
     *
     * <p>A role that uses temporary crews gets one named crew member, which is what the pool needs before it will
     * fill the remaining seats, exactly as the crew assembler leaves an infantry platoon during generation. Every
     * other role gets a full crew of individual personnel.</p>
     *
     * @param campaign   the campaign the unit belongs to
     * @param unit       the newly built, crewless unit
     * @param faction    the faction the crew are drawn from
     * @param crewSource the crewing to use, or {@code null} to follow the campaign's temporary crew options
     *
     * @return the role filled from the temporary crew pool, or {@code null} when the unit was fully crewed
     */
    static @Nullable PersonnelRole crewSupportUnit(Campaign campaign, Unit unit, Faction faction,
          @Nullable VehicleCrewSource crewSource) {
        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        PersonnelRole crewRole = unit.getDriverRole();
        boolean useTemporaryCrew;
        if (crewSource == VehicleCrewSource.TEMPORARY_CREW) {
            useTemporaryCrew = true;
        } else if (crewSource == VehicleCrewSource.NEW_CREW) {
            useTemporaryCrew = false;
        } else {
            useTemporaryCrew = (crewRole != null)
                                     && humanResources.isBlobCrewEnabled(crewRole, campaign.getCampaignOptions());
        }

        if (useTemporaryCrew && (crewRole != null)) {
            Person commander = humanResources.newPerson(campaign, crewRole);
            humanResources.recruitPerson(campaign, commander, true, true);
            unit.addDriver(commander);
            unit.resetPilotAndEntity();
            LOGGER.info("[CompanyGen][SupportUnits]     '{}' crewed with one named {} plus the temporary crew pool",
                  unit.getName(), crewRole);
            return crewRole;
        }

        Map<CrewType, Collection<Person>> newCrew = Utilities.genRandomCrewWithCombinedSkill(campaign, unit,
              faction.getShortName());
        newCrew.forEach((type, personnel) -> personnel.forEach(person -> type.getAddMethod().accept(unit, person)));
        unit.resetPilotAndEntity();
        LOGGER.info("[CompanyGen][SupportUnits]     '{}' crewed with {} individual personnel", unit.getName(),
              unit.getActiveCrew().size());
        return null;
    }

    /**
     * How many support vehicles the pipeline will still add to this campaign, given its options: flatbed trucks
     * under StratCon, canteens under fatigue, recovery vehicles under CamOps salvage and MASH trucks under MASH
     * theatres, each less what the hangar already holds. Sized the way each generator sizes itself, so the
     * personnel stage can count their mechanics before the vehicles exist.
     *
     * @param campaign the campaign the support is generated into
     *
     * @return the vehicles still to be generated
     */
    public static int vehiclesStillToGenerate(Campaign campaign, Faction faction) {
        int planned = 0;
        for (SupportCapability capability : SupportCapability.values()) {
            if (!capability.needsMechanics() || !capability.isEnabled(campaign)) {
                continue;
            }
            planned += shortfall(campaign, capability.unitName(campaign), capability.targetCount(campaign, faction));
        }
        return planned;
    }

    /**
     * One rolled model and how many of it to build, so a formation is filled with a single kind of vehicle.
     *
     * @param unitName the rolled unit's name
     * @param summary  the cache entry to build it from
     * @param count    how many to build
     */
    public record RolledBatch(String unitName, MekSummary summary, int count) {
    }

    /**
     * Fills a shortfall of {@code missing} vehicles by rolling one model per formation.
     *
     * <p>A lance is four of the same vehicle rather than four different ones, which is how a command would actually
     * be equipped, and rolling per formation rather than once per capability lets a larger command field a mixed
     * convoy - three lances of Flatbeds and one of Burros, say.</p>
     *
     * @param capability     the capability being fielded
     * @param campaign       the campaign, for the year to roll in
     * @param faction        the faction the command is organised as
     * @param missing        how many vehicles are still needed
     * @param formationSize  vehicles per formation, or {@code 0} to roll a single model for the lot
     *
     * @return one batch per formation, empty when nothing suitable could be rolled
     */
    static List<RolledBatch> rollBatches(SupportCapability capability, Campaign campaign, Faction faction,
          int missing, int formationSize) {
        List<RolledBatch> batches = new ArrayList<>();
        if (missing <= 0) {
            return batches;
        }
        String factionCode = faction.getShortName();
        int year = campaign.getLocalDate().getYear();
        String rating = null;

        int perBatch = (formationSize > 0) ? formationSize : missing;
        int remaining = missing;
        while (remaining > 0) {
            int batchSize = Math.min(perBatch, remaining);
            SupportVehicleSelector.Candidate rolled = SupportVehicleSelector.roll(capability, factionCode, year,
                  rating);
            if (rolled == null) {
                LOGGER.info("[CompanyGen][SupportUnits] {}: nothing suitable could be rolled for {} in {};"
                            + " {} vehicle(s) will not be fielded", capability, factionCode, year, remaining);
                return batches;
            }
            batches.add(new RolledBatch(rolled.unitName(), rolled.summary(), batchSize));
            remaining -= batchSize;
        }
        return batches;
    }

    /**
     * How many more of {@code unitName} the campaign needs to reach {@code targetCount}, never negative.
     *
     * @param campaign    the campaign whose hangar is counted
     * @param unitName    the support unit name to match
     * @param targetCount how many the command should field
     *
     * @return the number still missing
     */
    static int shortfall(Campaign campaign, @Nullable String unitName, int targetCount) {
        return Math.max(0, targetCount - countGeneratedUnitsNamed(campaign, unitName));
    }

    /**
     * What a capability already fields, counted by its TOE formation rather than by unit name.
     *
     * <p>A capability's vehicles are rolled, so they no longer share a name to count: a convoy can be three lances
     * of Flatbeds and one of Burros. What they do share is the formation they are filed into.</p>
     *
     * @param campaign      the campaign to inspect
     * @param formationType the capability's formation
     *
     * @return the vehicles already filed there
     */
    static int countVehiclesInFormation(Campaign campaign, SupportTOEFormationTypes formationType) {
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            if (formation.getName().equalsIgnoreCase(formationType.getLabel())) {
                return formation.getAllUnits(false).size();
            }
        }
        return 0;
    }

    /** Where the support sub-formations sit: the faction's smallest formation, a lance, Star or Level II. */
    private static final int SUPPORT_SUB_FORMATION_DEPTH = 1;

    /** Holds the labels for the support formations and their sub-formations. */
    private static final String SUPPORT_FORMATION_RESOURCE_BUNDLE = "mekhq.resources.SupportTOEFormationTypes";

    /**
     * Units per sub-formation when a capability is fielded as whole formations, or {@code 0} when its vehicles are
     * filed flat. Only the capabilities sized in whole formations are broken into lances or Stars; a command's two
     * MASH trucks or single canteen would read worse split up than listed together.
     *
     * @param faction       the faction of the command being supported, which sets the formation size
     * @param formationType the capability formation being filed into
     *
     * @return the sub-formation size, or {@code 0} for flat filing
     */
    static int subFormationSize(Faction faction, SupportTOEFormationTypes formationType) {
        boolean fieldedAsFormations = switch (formationType) {
            case SALVAGE_FORMATION, LOGISTICS_FORMATION -> true;
            default -> false;
        };
        return fieldedAsFormations ? supportFormationSize(faction) : 0;
    }

    /**
     * The name pattern for a support sub-formation, carrying {@code {0}} for its number.
     *
     * <p>Taken from the faction's own smallest formation rather than from a Clan-or-not test, so a ComStar or Word
     * of Blake command files Level IIs, a Clan command Stars, and everyone else lances.</p>
     *
     * @param faction the faction of the command being supported
     *
     * @return the localised name pattern
     */
    static String subFormationPattern(Faction faction) {
        return subFormationPattern(baseFormationLevel(faction));
    }

    /**
     * The smallest formation the faction fields: a Star for the Clans, a Level II for ComStar and the Word of Blake,
     * a lance for everyone else.
     *
     * <p>This mirrors {@link FormationLevel#parseFromDepth} at depth {@value #SUPPORT_SUB_FORMATION_DEPTH}, which
     * cannot be used directly because it reads the campaign's own faction. A command generated for a faction other
     * than the campaign's - a mercenary campaign generating a ComStar command, say - must be organised as the
     * faction it is generated for, exactly as its ranks already are.</p>
     *
     * @param faction the faction of the command being supported
     *
     * @return the faction's smallest formation
     */
    static FormationLevel baseFormationLevel(Faction faction) {
        if (faction.isClan()) {
            return FormationLevel.STAR_OR_NOVA;
        }
        if (faction.isComStarOrWoB()) {
            return FormationLevel.LEVEL_II_OR_CHOIR;
        }
        return FormationLevel.LANCE;
    }

    /**
     * The name pattern for a support sub-formation at {@code level}.
     *
     * <p>The level's own name is not used directly because several read as alternatives - "Star or Nova", "Level II
     * or Choir" - which suits a dropdown but not the name of a formation in the TOE. A level with no name of its own
     * here falls back on the level's name, so a faction family added later still produces something readable.</p>
     *
     * @param level the faction's smallest formation
     *
     * @return the localised name pattern, carrying {@code {0}} for the sub-formation's number
     */
    static String subFormationPattern(FormationLevel level) {
        String pattern = getTextAt(SUPPORT_FORMATION_RESOURCE_BUNDLE,
              "SupportTOEFormationTypes.subFormation." + level.name() + ".label");
        // A missing key comes back as a marker rather than a pattern, and a usable pattern must carry the number.
        if (!pattern.contains("{0}")) {
            return level + " {0}";
        }
        return pattern;
    }

    /**
     * Vehicles in one support formation: a lance for an Inner Sphere command, a vehicle Star for a Clan one. A Clan
     * vehicle Point is {@value #CLAN_VEHICLES_PER_POINT} vehicles, so a Star of five Points is ten vehicles.
     *
     * @param faction the faction of the command being supported
     *
     * @return the number of vehicles in one formation
     */
    static int supportFormationSize(Faction faction) {
        int baseSize = faction.getFormationBaseSize();
        return faction.isClan() ? baseSize * CLAN_VEHICLES_PER_POINT : baseSize;
    }

    /**
     * Rounds a vehicle requirement up to whole formations, because support vehicles are fielded as lances or Stars of
     * one vehicle type rather than as a loose count. A command that needs one truck still gets a full formation, and
     * one that needs five gets two.
     *
     * @param vehiclesNeeded how many vehicles the command's own need works out to
     * @param formationSize  vehicles in one formation, from {@link #supportFormationSize}
     *
     * @return the vehicle count rounded up to whole formations, never fewer than one formation
     */
    static int roundUpToWholeFormations(int vehiclesNeeded, int formationSize) {
        if (formationSize <= 0) {
            return Math.max(1, vehiclesNeeded);
        }
        int formations = Math.max(1, (int) Math.ceil((double) vehiclesNeeded / formationSize));
        return formations * formationSize;
    }

    /**
     * What a command's fighting strength adds up to: the units it fields and what they weigh.
     *
     * @param units   combat units in the command
     * @param tonnage their combined tonnage
     */
    record CombatForceTally(int units, double tonnage) {
    }

    /**
     * Tallies the command's combat units, which is what both the convoy and the salvage formation are sized against.
     * Large craft and conventional infantry are left out on the same terms {@link Resupply} uses.
     *
     * <p>Anything already filed into a support formation is left out too, and that exclusion carries the weight
     * here. Most support vehicles are not support vehicles by construction: a BattleMek Recovery Vehicle is an
     * ordinary fifty-ton Tank, so {@link Entity#isSupportVehicle()} is {@code false} for it. Since the capabilities
     * are generated one after another, counting them would let each one inflate the next: a command whose twelve
     * recovery vehicles had already been built would size its convoy against six hundred tons of its own support.</p>
     *
     * @param campaign the campaign to tally
     *
     * @return the combat unit count and tonnage
     */
    static CombatForceTally tallyCombatForce(Campaign campaign) {
        int units = 0;
        double tonnage = 0;
        for (Unit unit : campaign.getUnits()) {
            Entity entity = unit.getEntity();
            if ((entity == null) || entity.isSupportVehicle() || Resupply.isProhibitedUnitType(entity, false, false)) {
                continue;
            }
            if (isInSupportFormation(campaign, unit)) {
                continue;
            }
            units++;
            tonnage += entity.getWeight();
        }
        return new CombatForceTally(units, tonnage);
    }

    /**
     * Whether the unit sits in one of the command's support formations - its convoy, salvage, medical, commissary or
     * security formation - rather than in the fighting force those exist to support. A unit in no formation at all
     * counts as part of the force, so a tally taken before the TOE is built is never silently emptied.
     *
     * @param campaign the campaign holding the formations
     * @param unit     the unit to place
     *
     * @return {@code true} when the unit belongs to a support formation
     */
    private static boolean isInSupportFormation(Campaign campaign, Unit unit) {
        Formation formation = campaign.getPlayerForce().getFormation(unit.getFormationId());
        return (formation != null) && !formation.isFormationType(FormationType.STANDARD);
    }

    /**
     * Number of cargo trucks the command's own convoy needs to haul a resupply drop sized to its combat tonnage,
     * rounded up to whole formations.
     *
     * <p>The tonnage uses two pieces of {@link Resupply}'s model: combat tonnage over
     * {@value #COMBAT_TONNAGE_PER_CARGO_TON} gives the drop, and a player convoy hauls
     * {@link Resupply#CARGO_MULTIPLIER} times that. A battalion of thirty-six Meks needs roughly sixty-three tons
     * hauled, which is eleven Flatbed Trucks and so three lances.</p>
     *
     * <p>Three things {@link Resupply#calculateTargetCargoTonnage} does are deliberately left out. Its contract cap
     * is the employer's willingness to supply, and a command being generated holds no contract. Its
     * {@link Resupply#CARGO_MINIMUM_WEIGHT} floor is a floor on an employer's drop, and applying it here would hand
     * a small command a convoy sized for someone else; the one-formation minimum already covers the bottom end. Its
     * rounding to whole tons is skipped because this figure is divided again by the truck's capacity, so rounding
     * first would only lose precision.</p>
     *
     * @param campaign the campaign whose combat tonnage drives the count
     *
     * @return the truck count, at least one formation
     */
    static int logisticsUnitCount(Campaign campaign, Faction faction) {
        CombatForceTally tally = tallyCombatForce(campaign);
        double cargoToHaul = (tally.tonnage() / COMBAT_TONNAGE_PER_CARGO_TON) * Resupply.CARGO_MULTIPLIER;
        double cargoPerTruck = SupportVehicleSelector.typicalCapacity(SupportCapability.LOGISTICS,
              faction.getShortName(), campaign.getLocalDate().getYear(), null,
              SupportVehicleSelector.Candidate::cargoTons);
        if (cargoPerTruck <= 0) {
            cargoPerTruck = FALLBACK_CARGO_TONS_PER_TRUCK;
        }
        int trucksNeeded = (cargoPerTruck > 0) ? (int) Math.ceil(cargoToHaul / cargoPerTruck) : 1;
        int count = roundUpToWholeFormations(trucksNeeded, supportFormationSize(faction));
        LOGGER.info("[CompanyGen][SupportUnits] logistics: {} combat tons -> {} tons to haul, {} tons per truck -> "
                    + "{} truck(s) -> {} after rounding to formations of {}",
              tally.tonnage(), cargoToHaul, cargoPerTruck, trucksNeeded, count, supportFormationSize(faction));
        return count;
    }

    /**
     * Number of recovery vehicles the command needs to bring its wrecks home, rounded up to whole formations.
     *
     * <p>The opposing force is generated to the player's own budget, so a command meets roughly its own number of
     * units and about a quarter of them can be recovered. A battalion of thirty-six therefore wants nine recovery
     * vehicles, which is three lances.</p>
     *
     * @param campaign the campaign whose combat units drive the count
     *
     * @return the recovery vehicle count, at least one formation
     */
    static int salvageUnitCount(Campaign campaign, Faction faction) {
        CombatForceTally tally = tallyCombatForce(campaign);
        int wrecksToRecover = (int) Math.ceil((double) tally.units() / COMBAT_UNITS_PER_RECOVERY_VEHICLE);
        int count = roundUpToWholeFormations(wrecksToRecover, supportFormationSize(faction));
        LOGGER.info("[CompanyGen][SupportUnits] salvage: {} combat units -> {} recovery vehicle(s) -> {} after "
                    + "rounding to formations of {}",
              tally.units(), wrecksToRecover, count, supportFormationSize(faction));
        return count;
    }

    /**
     * Cargo a support unit can carry, in tons. A missing entry is logged and treated as no capacity, which the
     * callers then floor at a single formation.
     *
     * @param unitName the unit to look up in the unit cache
     *
     * @return the unit's cargo bay capacity in tons, or {@code 0} when it cannot be resolved
     */
    static double cargoCapacity(String unitName) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        if (mekSummary == null) {
            LOGGER.warn("[CompanyGen][SupportUnits] no unit entry for '{}', treating its cargo capacity as zero",
                  unitName);
            return 0;
        }
        return mekSummary.getCargoBayUnits();
    }

    /**
     * Number of mobile canteens required to feed the command. Each canteen counts as a single field
     * kitchen worth of coverage ({@link CampaignOptions#getFieldKitchenCapacity()} personnel),
     * regardless of how many kitchen items the unit model happens to carry, so the count reads as
     * roughly one canteen per kitchen's worth of personnel. The personnel that need feeding are
     * counted exactly as {@link Fatigue#checkFieldKitchenUsage} counts them (honouring the
     * ignore-non-combatants option). Never returns fewer than one so an enabled commissary always
     * fields at least one canteen.
     *
     * @param campaign the campaign whose roster and options drive the count
     *
     * @return the canteen count, at least {@code 1}
     */
    static int commissaryUnitCount(Campaign campaign, Faction faction) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int personnelNeedingKitchen = Fatigue.checkFieldKitchenUsage(campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false),
              campaignOptions.get(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS), campaign);
        int coveragePerCanteen = campaignOptions.get(CampaignOption.FIELD_KITCHEN_CAPACITY);
        int count = vehiclesForCoverage(personnelNeedingKitchen, coveragePerCanteen);
        LOGGER.info("[CompanyGen][SupportUnits] commissary: {} personnel need feeding, {} fed per canteen -> {} canteen(s)",
              personnelNeedingKitchen, coveragePerCanteen, count);
        return count;
    }

    /**
     * Number of MASH trucks required to treat the command's combatants as potential patients. Each
     * truck's MASH theatres cover ({@link CampaignOptions#getMASHTheatreCapacity()}) patients apiece,
     * matching the theatre capacity used by the in-play medical check. Never returns fewer than one
     * so an enabled medical capability always fields at least one truck.
     *
     * <p>Note that in the generation pipeline the trucks are crewed from the generated medical staff,
     * so the number actually fielded is additionally capped at the staff available to crew them.</p>
     *
     * @param campaign the campaign whose roster and options drive the count
     *
     * @return the MASH truck count, at least {@code 1}
     */
    static int medicalUnitCount(Campaign campaign, Faction faction) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int patientsToCover = combatPersonnelCount(campaign);
        double theatresPerTruck = SupportVehicleSelector.typicalCapacity(SupportCapability.MEDICAL,
              faction.getShortName(), campaign.getLocalDate().getYear(), null,
              SupportVehicleSelector.Candidate::mashTheatres);
        if (theatresPerTruck <= 0) {
            theatresPerTruck = 1;
        }
        int coveragePerTruck = (int) Math.round(theatresPerTruck
              * campaignOptions.get(CampaignOption.MASH_THEATRE_CAPACITY));
        int count = vehiclesForCoverage(patientsToCover, coveragePerTruck);
        LOGGER.info("[CompanyGen][SupportUnits] medical: {} combatants to cover, {} patients per MASH truck -> {} truck(s)",
              patientsToCover, coveragePerTruck, count);
        return count;
    }

    /**
     * Chooses the security detail size from the force echelon, measured by combatant headcount: a
     * company-sized force (at or below {@value #COMPANY_COMBATANT_CEILING} combatants) gets a
     * {@link SecurityTier#SQUAD}, a battalion (up to {@value #BATTALION_COMBATANT_CEILING}) a
     * {@link SecurityTier#PLATOON}, and a regiment or larger a {@link SecurityTier#COMPANY}.
     *
     * @param campaign the campaign whose roster drives the echelon
     *
     * @return the security tier appropriate to the force size
     */
    static SecurityTier securityTier(Campaign campaign) {
        int combatants = combatPersonnelCount(campaign);
        SecurityTier tier;
        if (combatants <= COMPANY_COMBATANT_CEILING) {
            tier = SecurityTier.SQUAD;
        } else if (combatants <= BATTALION_COMBATANT_CEILING) {
            tier = SecurityTier.PLATOON;
        } else {
            tier = SecurityTier.COMPANY;
        }
        LOGGER.info("[CompanyGen][SupportUnits] security: {} combatants -> {} detail", combatants, tier);
        return tier;
    }

    /**
     * Resolves the infantry unit name for a security tier, faction-appropriate. The
     * {@link SecurityTier#COMPANY} tier reuses the platoon unit (fielded {@value #PLATOONS_PER_COMPANY}
     * times to make up a company).
     *
     * @param tier   the security detail size
     * @param isClan {@code true} for a Clan command (rifle Points/Squads), {@code false} for Inner Sphere
     *
     * @return the infantry unit name to generate
     */
    static String securityUnitName(SecurityTier tier, boolean isClan) {
        if (tier == SecurityTier.SQUAD) {
            return isClan ? SECURITY_SQUAD_CLAN : SECURITY_SQUAD_INNER_SPHERE;
        }
        return isClan ? SECURITY_PLATOON_CLAN : SECURITY_PLATOON_INNER_SPHERE;
    }

    /**
     * Counts the active combat personnel on the campaign roster, using the same combat/non-combat
     * split ({@link Person#isCombat()}) as the daily fatigue check. Prisoners and camp followers are
     * excluded, matching the roster the field-kitchen usage count is taken over.
     *
     * @param campaign the campaign to inspect
     *
     * @return the number of active combat personnel
     */
    static int combatPersonnelCount(Campaign campaign) {
        int combatPersonnel = 0;
        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.isCombat()) {
                combatPersonnel++;
            }
        }
        return combatPersonnel;
    }

    /**
     * Number of support vehicles needed to cover {@code requiredCoverage} using vehicles that each
     * provide {@code coveragePerVehicle}, rounded up and floored at one. Returns {@code 1} when
     * {@code coveragePerVehicle} is not positive so a command with an unresolved capacity still
     * fields a single vehicle rather than none. Shared by the capability generators that scale to
     * the size of the generated force.
     *
     * @param requiredCoverage   total coverage the command needs (for example, personnel to feed)
     * @param coveragePerVehicle coverage a single vehicle provides
     *
     * @return the vehicle count, at least {@code 1}
     */
    static int vehiclesForCoverage(int requiredCoverage, int coveragePerVehicle) {
        if (coveragePerVehicle <= 0) {
            return 1;
        }
        int vehiclesNeeded = (int) Math.ceil((double) requiredCoverage / coveragePerVehicle);
        return Math.max(1, vehiclesNeeded);
    }

    /**
     * Counts the equipment items on {@code unitName} carrying {@code flag}, mirroring the per-item
     * capacity counting in {@link Fatigue#checkFieldKitchenCapacity} (field kitchens) and the medical
     * MASH-theatre check ({@link MiscType#F_MASH}). A missing entry or an unloadable entity is logged
     * and treated as zero items, which {@link #vehiclesForCoverage} then floors at a single vehicle.
     *
     * @param unitName the unit to load and inspect
     * @param flag     the equipment flag to count (for example {@link MiscType#F_FIELD_KITCHEN})
     *
     * @return the number of matching items on the unit, or {@code 0} on any load failure
     */
    static int countEquipment(String unitName, MiscTypeFlag flag) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        if (mekSummary == null) {
            LOGGER.error("Cannot find entry for {}", unitName);
            return 0;
        }

        try {
            Entity entity = mekSummary.loadEntity();
            int matchingItems = 0;
            for (MiscMounted item : entity.getMisc()) {
                if (item.getType().hasFlag(flag)) {
                    matchingItems++;
                }
            }
            return matchingItems;
        } catch (Exception exception) {
            LOGGER.error(exception, "Unable to load entity {} to count equipment: {}", unitName,
                  mekSummary.getSourceFile());
            return 0;
        }
    }

    /**
     * Counts the campaign units whose entity matches {@code unitName}. Used to reconcile support-vehicle
     * generation against what a command already fields, so re-running support (for example after adding
     * combat forces) tops up only the shortfall instead of duplicating existing support.
     *
     * @param campaign the campaign to inspect
     * @param unitName the support unit name to match (as loaded from the unit cache)
     *
     * @return the number of matching units currently in the campaign
     */
    static int countGeneratedUnitsNamed(Campaign campaign, @Nullable String unitName) {
        if (unitName == null) {
            return 0;
        }
        int matches = 0;
        for (Unit unit : campaign.getUnits()) {
            Entity entity = unit.getEntity();
            if (entity != null && unitName.equals(entity.getShortNameRaw())) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * Ensures the campaign fields {@code targetCount} of {@code unitName}, generating only the shortfall
     * beyond what already exists (see {@link #countGeneratedUnitsNamed}). Each fresh copy is loaded
     * crewed, optionally rank-assigned, and filed into {@code formationType}. A missing unit entry or an
     * unloadable entity is logged and skipped rather than aborting the whole batch.
     */
    private static void generate(SupportCapability capability, Campaign campaign, Faction faction,
          boolean autoAssignRanks, @Nullable String unitName, int targetCount,
          SupportTOEFormationTypes formationType, @Nullable VehicleCrewSource crewSource) {
        // A named capability is infantry, counted by name as before. A rolled one has no single name to count, so
        // what it already fields is read off its formation instead.
        int existing = (unitName != null)
              ? countGeneratedUnitsNamed(campaign, unitName)
              : countVehiclesInFormation(campaign, formationType);
        int count = Math.max(0, targetCount - existing);
        if (count <= 0) {
            LOGGER.info("[CompanyGen][SupportUnits] {}: target {} already met ({} present) -> generating 0",
                  formationType.name(), targetCount, existing);
            return;
        }

        List<RolledBatch> batches;
        if (unitName != null) {
            MekSummary named = MekSummaryCache.getInstance().getMek(unitName);
            if (named == null) {
                LOGGER.error("Cannot find entry for {}", unitName);
                return;
            }
            batches = List.of(new RolledBatch(unitName, named, count));
        } else {
            batches = rollBatches(capability, campaign, faction, count,
                  subFormationSize(faction, formationType));
            if (batches.isEmpty()) {
                return;
            }
        }

        boolean useRandomQuality = campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_UNIT_QUALITIES);
        List<Unit> units = new ArrayList<>();
        Set<PersonnelRole> pooledRoles = new HashSet<>();
        for (RolledBatch batch : batches) {
            for (int index = 0; index < batch.count(); index++) {
            try {
                PartQuality quality = useRandomQuality ? UnitOrder.getRandomUnitQuality(0) : PartQuality.QUALITY_D;
                // Built crewless, then crewed to match how the campaign crews everything else.
                Unit unit = campaign.addNewUnit(batch.summary().loadEntity(), false, 0, quality);
                if (unit != null) {
                    PersonnelRole pooledRole = crewSupportUnit(campaign, unit, faction, crewSource);
                    if (pooledRole != null) {
                        pooledRoles.add(pooledRole);
                    }
                    if (autoAssignRanks) {
                        AutomaticRankAssigner.assignRanks(campaign, unit, faction);
                    }
                    units.add(unit);
                }
            } catch (Exception exception) {
                LOGGER.error(exception, "Unable to load entity {}: {}", batch.unitName(),
                      batch.summary().getSourceFile());
            }
            }
        }

        if (!units.isEmpty()) {
            AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, units, formationType,
                  subFormationSize(faction, formationType), subFormationPattern(faction));
        }
        // The daily pool fill is off by default, so whatever was crewed from the pool is filled here and now.
        for (PersonnelRole pooledRole : pooledRoles) {
            campaign.resetTempCrewPoolForRole(pooledRole);
            campaign.getPlayerForce()
                  .getHumanResources()
                  .distributeTempCrewPoolToUnits(campaign, campaign.getCampaignOptions(), pooledRole);
        }
        StringBuilder built = new StringBuilder();
        for (RolledBatch batch : batches) {
            if (!built.isEmpty()) {
                built.append(" + ");
            }
            built.append(batch.count()).append(" x '").append(batch.unitName()).append("'");
        }
        LOGGER.info("[CompanyGen][SupportUnits] {}: generated {}/{} new ({}) ({} already present, target {})",
              formationType.name(), units.size(), count, built, existing, targetCount);
    }
}
