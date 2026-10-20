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
package mekhq.campaign.parts;

import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.unit.UnitTechProgression;

/**
 * Finds the designs a unit can be refitted to with a refit kit: the same chassis and unit type, a matching weight, and
 * allowed by the campaign's tech rules. It also counts the designs it passed over, so the player can be told why the
 * list is empty.
 */
public final class RefitDesignSearch {
    private static final MMLogger LOGGER = MMLogger.create(RefitDesignSearch.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChooseRefitDialog";

    /**
     * The designs found, and how many of the same chassis were passed over and why.
     *
     * @param models              the model names of the designs that may be offered
     * @param skippedForWeight    designs of the same chassis whose weight does not match
     * @param skippedForTechRules designs the campaign's tech rules do not allow
     */
    public record Result(List<String> models, int skippedForWeight, int skippedForTechRules) {
        /**
         * Explains to the player why no refit could be offered.
         *
         * @param unitName         the unit being refitted
         * @param failedRefitCheck designs found here that the refit check then refused
         *
         * @return one or more sentences, as HTML paragraphs
         */
        public String describeEmptyList(String unitName, int failedRefitCheck) {
            List<String> sentences = new ArrayList<>();
            sentences.add(getFormattedTextAt(RESOURCE_BUNDLE, "noDesigns.text", unitName));
            if (skippedForWeight > 0) {
                sentences.add(getFormattedTextAt(RESOURCE_BUNDLE, "noDesigns.weight", skippedForWeight));
            }
            if (skippedForTechRules > 0) {
                sentences.add(getFormattedTextAt(RESOURCE_BUNDLE, "noDesigns.techRules", skippedForTechRules));
            }
            if (failedRefitCheck > 0) {
                sentences.add(getFormattedTextAt(RESOURCE_BUNDLE, "noDesigns.refitCheck", failedRefitCheck));
            }
            if (sentences.size() == 1) {
                sentences.add(getFormattedTextAt(RESOURCE_BUNDLE, "noDesigns.noneFound"));
            }

            StringBuilder text = new StringBuilder();
            for (String sentence : sentences) {
                text.append("<p>").append(sentence).append("</p>");
            }
            return text.toString();
        }
    }

    private RefitDesignSearch() {}

    /**
     * Finds the designs this unit could be refitted to.
     *
     * @param entity   the unit's current design
     * @param campaign the campaign, for its tech rules and date
     *
     * @return the designs found, with counts of those passed over
     */
    public static Result findDesigns(Entity entity, Campaign campaign) {
        List<String> models = new ArrayList<>();
        int skippedForWeight = 0;
        int skippedForTechRules = 0;
        // Refits may fit Clan and Inner Sphere parts already owned, so the variant list allows mixed tech
        RefitTechManager refitTechManager = new RefitTechManager(campaign);

        for (MekSummary summary : MekSummaryCache.getInstance().getAllMeks()) {
            if (!isOtherDesignOfSameChassis(entity, summary)) {
                continue;
            }
            if (!isWeightMatch(entity, summary)) {
                LOGGER.trace("[RefitDesigns] {}: weight does not match", summary.getName());
                skippedForWeight++;
                continue;
            }
            if (!isAllowedByTechRules(summary, campaign, refitTechManager)) {
                LOGGER.trace("[RefitDesigns] {}: not allowed by the tech rules", summary.getName());
                skippedForTechRules++;
                continue;
            }
            models.add(summary.getModel());
        }

        LOGGER.debug("[RefitDesigns] {}: {} designs found, {} skipped for weight, {} skipped for tech rules",
              entity.getShortName(), models.size(), skippedForWeight, skippedForTechRules);
        return new Result(models, skippedForWeight, skippedForTechRules);
    }

    private static boolean isOtherDesignOfSameChassis(Entity entity, MekSummary summary) {
        boolean isSameChassis = entity.getChassis().equalsIgnoreCase(summary.getChassis());
        boolean isCurrentModel = entity.getModel().equalsIgnoreCase(summary.getModel());
        boolean isSameUnitType = summary.getUnitType().equals(UnitType.getTypeName(entity.getUnitType()));
        return isSameChassis && !isCurrentModel && isSameUnitType;
    }

    /**
     * Whether a design's weight lets it be a refit of this unit. Battle armor matches on squad size, which the summary
     * holds as its weight. Conventional infantry always matches: a platoon's weight follows from its troopers and
     * their weapons, so any change to either gives a different weight.
     *
     * @param entity  the unit's current design
     * @param summary the candidate design
     *
     * @return {@code true} if the weights are compatible
     */
    static boolean isWeightMatch(Entity entity, MekSummary summary) {
        if (entity instanceof BattleArmor battleArmor) {
            return battleArmor.getSquadSize() == (int) summary.getTWWeight();
        }
        if (entity instanceof ConvInfantry) {
            return true;
        }
        return summary.getTons() == entity.getWeight();
    }

    private static boolean isAllowedByTechRules(MekSummary summary, Campaign campaign,
          RefitTechManager refitTechManager) {
        ITechnology techProgression = UnitTechProgression.getProgression(summary, campaign.getTechFaction(), true);
        if (techProgression == null) {
            // This should never happen unless there was an exception thrown when calculating the progression. In
            // such a case we log it and take the least restrictive action, which is to let it through.
            LOGGER.warn("Could not determine tech progression for {}, including among available refits.",
                  summary.getName());
        } else if (!refitTechManager.isLegal(techProgression)) {
            return false;
        }

        boolean campaignIsClan = campaign.getPlayerForce().getFaction().isClan();
        String techBase = summary.getTechBase().toLowerCase();
        boolean modelIsClan = summary.isClan() || techBase.contains("clan") || techBase.contains("mixed");
        boolean isLimitClanTech = campaign.getCampaignOptions().get(CampaignOption.LIMIT_CLAN_TECH);
        LocalDate today = campaign.getLocalDate();
        boolean isBeforeTukayyid = !today.isAfter(BATTLE_OF_TUKAYYID);
        boolean isBlockedClanDesign = !campaignIsClan && modelIsClan && isLimitClanTech && isBeforeTukayyid;
        return !isBlockedClanDesign;
    }
}
