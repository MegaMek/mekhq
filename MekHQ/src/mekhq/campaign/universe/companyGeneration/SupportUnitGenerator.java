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
package mekhq.campaign.universe.companyGeneration;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.AutoAssignRankForCompanyGenerator;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.universe.Faction;

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
    private static final String LOGISTICS_UNIT = "Flatbed Truck";
    static final String MEDICAL_UNIT = "MASH Truck (Small)";
    private static final String COMMISSARY_UNIT = "Sherpa Armored Truck (Mobile Canteen)";
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

    /** Recovery vehicles: one per formation base size, doubled for a Clan command. */
    public static void generateSalvageUnits(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        generate(campaign, faction, autoAssignRanks, SALVAGE_UNIT, scaledCount(campaign),
              SupportTOEFormationTypes.SALVAGE_FORMATION);
    }

    /** Flatbed logistics trucks: one per formation base size, doubled for a Clan command. */
    public static void generateLogisticsUnits(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        generate(campaign, faction, autoAssignRanks, LOGISTICS_UNIT, scaledCount(campaign),
              SupportTOEFormationTypes.LOGISTICS_FORMATION);
    }

    /** MASH trucks for the medical formation, scaled to treat the command's combatants. */
    public static void generateMedicalUnits(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        generate(campaign, faction, autoAssignRanks, MEDICAL_UNIT, medicalUnitCount(campaign),
              SupportTOEFormationTypes.MEDICAL_FORMATION);
    }

    /** Mobile canteens for the commissary formation, scaled to feed the command's personnel. */
    public static void generateCommissaryUnits(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        generate(campaign, faction, autoAssignRanks, COMMISSARY_UNIT, commissaryUnitCount(campaign),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);
    }

    /**
     * Rifle infantry for the security formation, sized to the force it protects: a company-sized
     * force gets a single squad, a battalion a single platoon, and a regiment or larger a full
     * company (fielded as {@value #PLATOONS_PER_COMPANY} platoons).
     */
    public static void generateSecurityUnits(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        boolean isClan = campaign.isClanCampaign();
        SecurityTier tier = securityTier(campaign);
        String unitName = securityUnitName(tier, isClan);
        int count = tier == SecurityTier.COMPANY ? PLATOONS_PER_COMPANY : 1;
        generate(campaign, faction, autoAssignRanks, unitName, count, SupportTOEFormationTypes.SECURITY_FORMATION);
    }

    /** Formation base size, doubled for a Clan command. */
    static int scaledCount(Campaign campaign) {
        int count = campaign.getFaction().getFormationBaseSize();
        return campaign.isClanCampaign() ? count * CLAN_VEHICLES_PER_POINT : count;
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
        int personnelNeedingKitchen = Fatigue.checkFieldKitchenUsage(campaign.getActivePersonnel(false, false),
              campaignOptions.isUseFieldKitchenIgnoreNonCombatants(), campaign);
        int coveragePerCanteen = campaignOptions.getFieldKitchenCapacity();
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
        int coveragePerTruck = countEquipment(MEDICAL_UNIT, MiscType.F_MASH) * campaignOptions.getMASHTheatreCapacity();
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
        for (Person person : campaign.getActivePersonnel(false, false)) {
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
     * Loads {@code unitName} once, creates {@code count} crewed copies, optionally assigns ranks, and
     * files the batch into {@code formationType}. A missing unit entry or an unloadable entity is
     * logged and skipped rather than aborting the whole batch.
     */
    private static void generate(Campaign campaign, Faction faction, boolean autoAssignRanks, String unitName,
          int count, SupportTOEFormationTypes formationType) {
        if (count <= 0) {
            return;
        }

        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        if (mekSummary == null) {
            LOGGER.error("Cannot find entry for {}", unitName);
            return;
        }

        boolean useRandomQuality = campaign.getCampaignOptions().isUseRandomUnitQualities();
        List<Unit> units = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            try {
                PartQuality quality = useRandomQuality ? UnitOrder.getRandomUnitQuality(0) : PartQuality.QUALITY_D;
                Unit unit = campaign.addNewUnit(mekSummary.loadEntity(), true, 0, quality);
                if (unit != null) {
                    if (autoAssignRanks) {
                        AutoAssignRankForCompanyGenerator.assignRanks(campaign, unit, faction);
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
        LOGGER.info("[CompanyGen][SupportUnits] {}: generated {}/{} x '{}'",
              formationType.name(), units.size(), count, unitName);
    }
}
