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
package mekhq.campaign.personnel.training;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.utilities.spaUtilities.SpaUtilities;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

/**
 * The campaign settings that price training, read once, and the per-person facts that every price needs, worked out
 * once per person. Pricing a purchase for a thousand people would otherwise read the same options and role skills a
 * thousand times.
 *
 * <p>A context is a snapshot: build a new one if the campaign's options might have changed.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TrainingContext {
    private final Campaign campaign;
    private final CampaignOptions options;
    private final LocalDate today;
    private final boolean isUseReasoning;
    private final double xpCostMultiplier;
    private final boolean isAdminsHaveNegotiation;
    private final boolean isDoctorsUseAdministration;
    private final boolean isTechsUseAdministration;
    private final boolean isUseArtillery;
    private final boolean isUseAbilities;
    private final boolean isCapTotalSpas;
    private final boolean isUseSpaTrainingCosts;
    private final boolean isUseEdge;
    private final boolean isGM;
    private final boolean isLogSkillGain;
    private final int attributeCost;
    private final int edgeCost;
    private final int maximumEdge;
    private final int edgeRefreshCost;

    private final Map<Person, Set<String>> roleSkills = new HashMap<>();
    private final Map<String, AbilityCategory> abilityCategories = new HashMap<>();

    /**
     * @param campaign the campaign whose options price the training
     */
    public TrainingContext(Campaign campaign) {
        this.campaign = campaign;
        options = campaign.getCampaignOptions();
        today = campaign.getLocalDate();
        isUseReasoning = options.get(CampaignOption.USE_REASONING_XP_MULTIPLIER);
        xpCostMultiplier = options.get(CampaignOption.XP_COST_MULTIPLIER);
        isAdminsHaveNegotiation = options.get(CampaignOption.ADMINS_HAVE_NEGOTIATION);
        isDoctorsUseAdministration = options.get(CampaignOption.DOCTORS_USE_ADMINISTRATION);
        isTechsUseAdministration = options.get(CampaignOption.TECHS_USE_ADMINISTRATION);
        isUseArtillery = options.get(CampaignOption.USE_ARTILLERY);
        isUseAbilities = options.get(CampaignOption.USE_ABILITIES);
        isCapTotalSpas = options.get(CampaignOption.CAP_TOTAL_SPAS);
        isUseSpaTrainingCosts = options.get(CampaignOption.USE_SPA_TRAINING_COSTS);
        isUseEdge = options.get(CampaignOption.USE_EDGE);
        isGM = campaign.isGM();
        isLogSkillGain = options.get(CampaignOption.PERSONNEL_LOG_SKILL_GAIN);
        attributeCost = options.get(CampaignOption.ATTRIBUTE_COST);
        edgeCost = options.get(CampaignOption.EDGE_COST);
        maximumEdge = options.get(CampaignOption.MAXIMUM_EDGE);
        edgeRefreshCost = options.get(CampaignOption.EDGE_REFRESH_COST);
    }

    public Campaign campaign() {
        return campaign;
    }

    public CampaignOptions options() {
        return options;
    }

    public LocalDate today() {
        return today;
    }

    public boolean isUseReasoning() {
        return isUseReasoning;
    }

    public double xpCostMultiplier() {
        return xpCostMultiplier;
    }

    public boolean isUseArtillery() {
        return isUseArtillery;
    }

    public boolean isUseAbilities() {
        return isUseAbilities;
    }

    public boolean isCapTotalSpas() {
        return isCapTotalSpas;
    }

    public boolean isUseSpaTrainingCosts() {
        return isUseSpaTrainingCosts;
    }

    public boolean isUseSkillTrainingCosts() {
        return options.get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS);
    }

    public boolean isUseEdge() {
        return isUseEdge;
    }

    public boolean isGM() {
        return isGM;
    }

    public boolean isLogSkillGain() {
        return isLogSkillGain;
    }

    public int attributeCost() {
        return attributeCost;
    }

    public int edgeCost() {
        return edgeCost;
    }

    public int maximumEdge() {
        return maximumEdge;
    }

    public int edgeRefreshCost() {
        return edgeRefreshCost;
    }

    /**
     * @param person a person
     *
     * @return the skills of the person's primary and secondary roles, as the Spend XP menu marked them
     */
    public Set<String> roleSkillsOf(Person person) {
        return roleSkills.computeIfAbsent(person, key -> {
            Set<String> skills = new HashSet<>();
            for (PersonnelRole role : new PersonnelRole[] { key.getPrimaryRole(), key.getSecondaryRole() }) {
                skills.addAll(role.getSkillsForProfession(isAdminsHaveNegotiation, isDoctorsUseAdministration,
                      isTechsUseAdministration, isUseArtillery, true));
            }
            return skills;
        });
    }

    /**
     * @param ability an ability
     *
     * @return the ability's category, worked out once, as working it out reads every prerequisite
     */
    public AbilityCategory categoryOf(SpecialAbility ability) {
        return abilityCategories.computeIfAbsent(ability.getName(), name -> SpaUtilities.getSpaCategory(ability));
    }
}
