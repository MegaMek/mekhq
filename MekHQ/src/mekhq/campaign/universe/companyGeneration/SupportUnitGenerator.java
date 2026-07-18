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

import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.ranks.AutoAssignRankForCompanyGenerator;
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
 *   <li>Medical - a MASH truck</li>
 *   <li>Commissary - a mobile canteen</li>
 *   <li>Security - a rifle infantry platoon (Clan: a rifle Point)</li>
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
    private static final String SECURITY_UNIT_INNER_SPHERE = "Foot Platoon (Rifle)";
    private static final String SECURITY_UNIT_CLAN = "Clan Foot Point (Rifle Light)";

    /** Vehicles per point for the count-scaled capabilities (salvage, logistics) in a Clan command. */
    private static final int CLAN_VEHICLES_PER_POINT = 2;

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

    /** A single MASH truck for the medical formation. */
    public static void generateMedicalUnit(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        generate(campaign, faction, autoAssignRanks, MEDICAL_UNIT, 1,
              SupportTOEFormationTypes.MEDICAL_FORMATION);
    }

    /** A single mobile canteen for the commissary formation. */
    public static void generateCommissaryUnit(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        generate(campaign, faction, autoAssignRanks, COMMISSARY_UNIT, 1,
              SupportTOEFormationTypes.COMMISSARY_FORMATION);
    }

    /** A single rifle infantry unit (Clan Point or Inner Sphere platoon) for the security formation. */
    public static void generateSecurityUnit(Campaign campaign, Faction faction, boolean autoAssignRanks) {
        String unitName = campaign.isClanCampaign() ? SECURITY_UNIT_CLAN : SECURITY_UNIT_INNER_SPHERE;
        generate(campaign, faction, autoAssignRanks, unitName, 1, SupportTOEFormationTypes.SECURITY_FORMATION);
    }

    /** Formation base size, doubled for a Clan command. */
    static int scaledCount(Campaign campaign) {
        int count = campaign.getFaction().getFormationBaseSize();
        return campaign.isClanCampaign() ? count * CLAN_VEHICLES_PER_POINT : count;
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
