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

    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    static final String SALVAGE_UNIT = "BattleMech Recovery Vehicle";
    static final String LOGISTICS_UNIT = "Flatbed Truck";
    static final String MEDICAL_UNIT = "MASH Truck (Small)";
    static final String COMMISSARY_UNIT = "Sherpa Armored Truck (Mobile Canteen)";
    private static final String SECURITY_SQUAD_INNER_SPHERE = "Foot Squad (Rifle)";
    private static final String SECURITY_SQUAD_CLAN = "Clan Foot Squad (Rifle)";
    private static final String SECURITY_PLATOON_INNER_SPHERE = "Foot Platoon (Rifle)";
    private static final String SECURITY_PLATOON_CLAN = "Clan Foot Point (Rifle Light)";

    /** Vehicles per point for the count-scaled capabilities (salvage, logistics) in a Clan command. */
    private static final int CLAN_VEHICLES_PER_POINT = 2;

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
        generate(campaign, faction, autoAssignRanks, capability.unitName(campaign), capability.targetCount(campaign),
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
    public static int vehiclesStillToGenerate(Campaign campaign) {
        int planned = 0;
        for (SupportCapability capability : SupportCapability.values()) {
            if (!capability.needsMechanics() || !capability.isEnabled(campaign)) {
                continue;
            }
            planned += shortfall(campaign, capability.unitName(campaign), capability.targetCount(campaign));
        }
        return planned;
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
    static int shortfall(Campaign campaign, String unitName, int targetCount) {
        return Math.max(0, targetCount - countGeneratedUnitsNamed(campaign, unitName));
    }

    /** Formation base size, doubled for a Clan command. */
    static int scaledCount(Campaign campaign) {
        int count = campaign.getPlayerForce().getFaction().getFormationBaseSize();
        return campaign.getPlayerForce().isClanForce() ? count * CLAN_VEHICLES_PER_POINT : count;
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
    static int commissaryUnitCount(Campaign campaign) {
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
    static int medicalUnitCount(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int patientsToCover = combatPersonnelCount(campaign);
        int coveragePerTruck = countEquipment(MEDICAL_UNIT, MiscType.F_MASH) * campaignOptions.get(CampaignOption.MASH_THEATRE_CAPACITY);
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
    static int countGeneratedUnitsNamed(Campaign campaign, String unitName) {
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
    private static void generate(Campaign campaign, Faction faction, boolean autoAssignRanks, String unitName,
          int targetCount, SupportTOEFormationTypes formationType, @Nullable VehicleCrewSource crewSource) {
        int existing = countGeneratedUnitsNamed(campaign, unitName);
        int count = Math.max(0, targetCount - existing);
        if (count <= 0) {
            LOGGER.info("[CompanyGen][SupportUnits] {}: target {} x '{}' already met ({} present) -> generating 0",
                  formationType.name(), targetCount, unitName, existing);
            return;
        }

        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        if (mekSummary == null) {
            LOGGER.error("Cannot find entry for {}", unitName);
            return;
        }

        boolean useRandomQuality = campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_UNIT_QUALITIES);
        List<Unit> units = new ArrayList<>();
        Set<PersonnelRole> pooledRoles = new HashSet<>();
        for (int index = 0; index < count; index++) {
            try {
                PartQuality quality = useRandomQuality ? UnitOrder.getRandomUnitQuality(0) : PartQuality.QUALITY_D;
                // Built crewless, then crewed to match how the campaign crews everything else.
                Unit unit = campaign.addNewUnit(mekSummary.loadEntity(), false, 0, quality);
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
                LOGGER.error(exception, "Unable to load entity {}: {}", unitName, mekSummary.getSourceFile());
            }
        }

        if (!units.isEmpty()) {
            AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, units, formationType);
        }
        // The daily pool fill is off by default, so whatever was crewed from the pool is filled here and now.
        for (PersonnelRole pooledRole : pooledRoles) {
            campaign.resetTempCrewPoolForRole(pooledRole);
            campaign.getPlayerForce()
                  .getHumanResources()
                  .distributeTempCrewPoolToUnits(campaign, campaign.getCampaignOptions(), pooledRole);
        }
        LOGGER.info("[CompanyGen][SupportUnits] {}: generated {}/{} new x '{}' ({} already present, target {})",
              formationType.name(), units.size(), count, unitName, existing, targetCount);
    }
}
