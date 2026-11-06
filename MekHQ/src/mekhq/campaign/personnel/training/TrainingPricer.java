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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.ATOWTraits.TRAIT_MODIFICATION_COST;
import static mekhq.campaign.personnel.skills.SkillType.S_ARTILLERY;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillPrerequisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillTrainingCategory;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPurchase.AbilityPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.FlawBuyOff;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;
import mekhq.utilities.spaUtilities.SpaUtilities;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

/**
 * Prices a {@link TrainingPurchase} for a person as they will be once everything staged before it is applied, and
 * says whether they can make it. Prices follow the Spend XP menu exactly: the Reasoning and talent multipliers, the
 * campaign's XP cost multiplier, XP already put towards a skill, and the C-bill training fees for skills and
 * abilities when those options are on.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TrainingPricer {
    private TrainingPricer() {
    }

    /**
     * A purchase's price and whether it can be made.
     *
     * @param refusal         why it can't be made, or {@code null} if it can
     * @param xp              the XP it costs; negative if it gives XP, as taking a flaw does
     * @param fee             the C-bill training fee, zero if there is none
     * @param feeCategory     what the fee pays for, or {@code null} if there is no fee
     * @param reachedLevel    for a skill whose improvement is charged a fee, the experience level it reaches;
     *                        otherwise {@code -1}
     * @param projectedSkill  for a skill improvement, the skill at its new level; otherwise {@code null}
     */
    public record Quote(@Nullable TrainingRefusal refusal, int xp, Money fee,
          @Nullable TrainingFeeCategory feeCategory, int reachedLevel, @Nullable Skill projectedSkill) {
        /** @return {@code true} if the purchase can be made */
        public boolean isAllowed() {
            return refusal == null;
        }

        static Quote refused(TrainingRefusal refusal) {
            return new Quote(refusal, 0, Money.zero(), null, -1, null);
        }

        static Quote of(int xp, Money fee, @Nullable TrainingFeeCategory feeCategory) {
            return new Quote(null, xp, fee, feeCategory, -1, null);
        }

        Quote refusedFor(TrainingRefusal reason) {
            return new Quote(reason, xp, fee, feeCategory, reachedLevel, projectedSkill);
        }
    }

    /**
     * @param context    the campaign settings and per-person facts
     * @param projection the person as they will be once everything staged so far is applied
     * @param purchase   the purchase
     *
     * @return the purchase's price, and why it can't be made if it can't; a refused quote still carries the price
     *       where there is one, so the player can see how short they are
     */
    public static Quote quote(TrainingContext context, PersonProjection projection, TrainingPurchase purchase) {
        if (!projection.person().getStatus().isActiveFlexible()) {
            return Quote.refused(TrainingRefusal.NOT_ACTIVE);
        }
        Quote quote = switch (purchase) {
            case SkillImprovement improvement -> quoteSkill(context, projection, improvement.skillName());
            case NaturalAptitude aptitude -> quoteAptitude(context, projection, aptitude.skillName());
            case AbilityPurchase ability -> quoteAbility(context, projection, ability.abilityName(),
                  ability.choice());
            case FlawBuyOff buyOff -> quoteBuyOff(context, projection, buyOff.abilityName());
            case AttributeIncrease increase -> quoteAttribute(context, projection, increase.attribute());
            case TraitChange change -> quoteTrait(context, projection, change.trait(), change.delta());
            case EdgeRefill ignored -> quoteEdgeRefill(context, projection);
        };
        if (quote.isAllowed() && (quote.xp() > 0) && (quote.xp() > projection.availableXp())) {
            return quote.refusedFor(TrainingRefusal.NOT_ENOUGH_XP);
        }
        return quote;
    }

    // region Skills

    private static Quote quoteSkill(TrainingContext context, PersonProjection projection, String skillName) {
        Person person = projection.person();
        SkillType type = SkillType.getType(skillName);
        if ((type == null) || (S_ARTILLERY.equals(skillName) && !context.isUseArtillery())) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }

        Skill current = projection.skill(skillName);
        int baseCost;
        if (current == null) {
            baseCost = type.getCost(0);
            if (baseCost < 0) {
                return Quote.refused(TrainingRefusal.DISABLED);
            }
        } else {
            if (!current.isImprovementLegal()) {
                return Quote.refused(TrainingRefusal.AT_LIMIT);
            }
            baseCost = current.getCostToImprove();
            if (baseCost <= 0) {
                return Quote.refused(TrainingRefusal.AT_LIMIT);
            }
        }

        // As Person.getCostToImprove, then the campaign multiplier, as the Spend XP menu prices it
        int personCost = (int) round(baseCost * person.getTalentBasedXpCostMultiplier(context.isUseReasoning(),
              type));
        int xp = (int) round(personCost * context.xpCostMultiplier());

        // The training fee follows the skill's trained level alone; attributes and other modifiers don't change it
        Skill improved;
        int trainedLevel;
        if (current == null) {
            improved = new Skill(type, 0, 0);
            trainedLevel = -1;
        } else {
            // XP already put towards the skill comes off the price, and is used up by the improvement
            xp = max(0, xp - current.getXpProgress());
            improved = new Skill(type, current.getLevel(), current.getBonus(),
                  max(0, current.getXpProgress() - personCost), current.getHasNaturalAptitude(),
                  current.getNaturalAptitudeXpProgress());
            improved.improve();
            trainedLevel = current.getLevel();
        }

        boolean isRoleSkill = context.roleSkillsOf(person).contains(skillName);
        Money fee = SkillTrainingCosts.getSkillImprovementCost(context.options(), type, isRoleSkill, trainedLevel);
        TrainingFeeCategory category = null;
        int reachedLevel = -1;
        if (!fee.isZero()) {
            category = TrainingFeeCategory.of(SkillTrainingCategory.getCategory(isRoleSkill, type));
            reachedLevel = type.getExperienceLevel(trainedLevel + 1);
        }
        return new Quote(null, xp, fee, category, reachedLevel, improved);
    }

    private static Quote quoteAptitude(TrainingContext context, PersonProjection projection, String skillName) {
        Person person = projection.person();
        if (projection.skill(skillName) == null) {
            return Quote.refused(TrainingRefusal.NOT_HELD);
        }
        if (projection.hasAptitude(skillName)) {
            return Quote.refused(TrainingRefusal.ALREADY_HELD);
        }
        int cost = person.getCostToGainNaturalAptitude(skillName, context.isUseReasoning());
        if (cost == SkillType.DISABLED_SKILL_LEVEL) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        cost = (int) round(cost * context.xpCostMultiplier());
        Skill real = person.getSkill(skillName);
        if (real != null) {
            // As with improving skills, XP already put towards the aptitude comes off the price
            cost = max(0, cost - real.getNaturalAptitudeXpProgress());
        }
        return Quote.of(cost, Money.zero(), null);
    }

    // endregion Skills

    // region Abilities

    private static Quote quoteAbility(TrainingContext context, PersonProjection projection, String abilityName,
          @Nullable String choice) {
        if (!context.isUseAbilities()) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        SpecialAbility ability = SpecialAbility.getAbility(abilityName);
        if (ability == null) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        if (projection.hasAbility(abilityName)) {
            return Quote.refused(TrainingRefusal.ALREADY_HELD);
        }
        if ((context.categoryOf(ability) == AbilityCategory.CHARACTER_CREATION_ONLY) && !context.isGM()) {
            return Quote.refused(TrainingRefusal.GM_ONLY);
        }
        Person person = projection.person();
        if (!isEligible(ability, projection)) {
            return Quote.refused(TrainingRefusal.NOT_ELIGIBLE);
        }
        if (AbilityChoices.needsChoice(ability)
                  && ((choice == null) || !AbilityChoices.isValidFor(ability, person, choice))) {
            return Quote.refused(TrainingRefusal.NO_CHOICE);
        }

        // Reasoning only changes the cost of an ability that costs XP, and before the campaign multiplier
        int baseCost = ability.getCost();
        int xp = (int) round(baseCost > 0 ?
                                   baseCost * person.getReasoningXpCostMultiplier(context.isUseReasoning()) :
                                   baseCost);
        xp = (int) round(xp * context.xpCostMultiplier());

        Money fee = Money.zero();
        TrainingFeeCategory category = null;
        if (!SpaUtilities.isFlaw(ability)) {
            int held = projection.nonFlawAbilityCount();
            if (context.isCapTotalSpas() && (held >= SpaUtilities.SPA_CAP)) {
                return Quote.refused(TrainingRefusal.SPA_CAP);
            }
            if (context.isUseSpaTrainingCosts()) {
                fee = SpaUtilities.getSpaTrainingCost(held);
                category = TrainingFeeCategory.ABILITIES;
            }
        }
        return Quote.of(xp, fee, category);
    }

    /**
     * Checks an ability's prerequisites against the person's staged skills and abilities, so training a skill and
     * then buying the ability it unlocks can be staged together.
     */
    static boolean isEligible(SpecialAbility ability, PersonProjection projection) {
        Skills skills = projection.projectedSkills();
        for (SkillPrerequisite prerequisite : ability.getPrereqSkills()) {
            if (!prerequisite.qualifies(skills)) {
                return false;
            }
        }
        for (String required : ability.getPrereqAbilities()) {
            if (!projection.hasAbility(required)) {
                return false;
            }
        }
        for (String invalid : ability.getInvalidAbilities()) {
            if (projection.hasAbility(invalid)) {
                return false;
            }
        }
        return ability.isClanRequirementMet(projection.person().isClanPersonnel());
    }

    private static Quote quoteBuyOff(TrainingContext context, PersonProjection projection, String abilityName) {
        SpecialAbility ability = SpecialAbility.getAbility(abilityName);
        if (ability == null) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        if (!projection.hasFlawToBuyOff(abilityName) || !SpaUtilities.isFlaw(ability)) {
            return Quote.refused(TrainingRefusal.NOT_HELD);
        }
        // A flaw's cost is negative; paying it off costs that much, scaled only by the campaign multiplier
        int xp = -(int) round(ability.getCost() * context.xpCostMultiplier());
        return Quote.of(xp, Money.zero(), null);
    }

    // endregion Abilities

    // region Attributes, traits and Edge

    private static Quote quoteAttribute(TrainingContext context, PersonProjection projection,
          SkillAttribute attribute) {
        if (attribute.isNoAttribute()) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        Person person = projection.person();
        boolean isEdge = attribute == SkillAttribute.EDGE;
        if (isEdge && !context.isUseEdge()) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        int baseCost = (int) round((isEdge ? context.edgeCost() : context.attributeCost())
                                         * context.xpCostMultiplier());
        int xp = (int) round(baseCost * person.getReasoningXpCostMultiplier(context.isUseReasoning()));
        if (projection.attributeScore(attribute) + 1 > attributeCap(context, person, attribute)) {
            return Quote.refused(TrainingRefusal.AT_LIMIT);
        }
        return Quote.of(xp, Money.zero(), null);
    }

    /**
     * @param context   the campaign settings, for the campaign's Edge limit
     * @param person    a person
     * @param attribute an attribute, Edge included
     *
     * @return the highest score training can raise the attribute to
     */
    public static int attributeCap(TrainingContext context, Person person, SkillAttribute attribute) {
        int cap = person.getAttributeCap(attribute);
        return (attribute == SkillAttribute.EDGE) ? min(cap, context.maximumEdge()) : cap;
    }

    /**
     * @param trait a trait
     *
     * @return {@code true} if the Training Console offers this trait; the others are set at character creation
     */
    public static boolean isTrainableTrait(ATOWTraits trait) {
        return switch (trait) {
            case CONNECTIONS, FAME, WEALTH, EXTRA_INCOME, UNLUCKY, BLOODMARK -> true;
            default -> false;
        };
    }

    /**
     * @param trait a trait
     * @param delta {@code 1} or {@code -1}
     *
     * @return {@code true} if moving the trait this way costs XP; the other way gives XP back
     */
    public static boolean isCostlyDirection(ATOWTraits trait, int delta) {
        return switch (trait) {
            // Being less unlucky, or less hunted, is the improvement
            case UNLUCKY, BLOODMARK -> delta < 0;
            default -> delta > 0;
        };
    }

    /**
     * @param person a person
     * @param trait  a trait the console offers
     *
     * @return the person's current level in the trait
     */
    public static int traitLevel(Person person, ATOWTraits trait) {
        return switch (trait) {
            case CONNECTIONS -> person.getConnections();
            case FAME -> person.getFame();
            case WEALTH -> person.getWealth();
            case EXTRA_INCOME -> person.getExtraIncomeTraitLevel();
            case UNLUCKY -> person.getUnlucky();
            case BLOODMARK -> person.getBloodmark();
            default -> 0;
        };
    }

    /**
     * Sets a person's level in a trait the console offers.
     *
     * @param person a person
     * @param trait  a trait the console offers
     * @param level  the new level
     *
     * @return {@code false} if the console doesn't offer the trait, in which case nothing changed
     */
    public static boolean setTraitLevel(Person person, ATOWTraits trait, int level) {
        switch (trait) {
            case CONNECTIONS -> person.setConnections(level);
            case FAME -> person.setFame(level);
            case WEALTH -> person.setWealth(level);
            case EXTRA_INCOME -> person.setExtraIncomeFromTraitLevel(level);
            case UNLUCKY -> person.setUnlucky(level);
            case BLOODMARK -> person.setBloodmark(level);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Quote quoteTrait(TrainingContext context, PersonProjection projection, ATOWTraits trait,
          int delta) {
        // Connections can only be bought, never sold back, as in the Spend XP menu
        if (!isTrainableTrait(trait) || (Math.abs(delta) != 1) || ((trait == ATOWTraits.CONNECTIONS) && (delta < 0))) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        int target = projection.traitLevel(trait) + delta;
        if ((target < trait.getMinimum()) || (target > trait.getMaximum())) {
            return Quote.refused(TrainingRefusal.AT_LIMIT);
        }
        int traitCost = (int) round(TRAIT_MODIFICATION_COST * context.xpCostMultiplier());
        return Quote.of(isCostlyDirection(trait, delta) ? traitCost : -traitCost, Money.zero(), null);
    }

    private static Quote quoteEdgeRefill(TrainingContext context, PersonProjection projection) {
        if (!context.isUseEdge()) {
            return Quote.refused(TrainingRefusal.DISABLED);
        }
        Person person = projection.person();
        if ((person.getUsedEdge() <= 0) || projection.isEdgeRefilled()) {
            return Quote.refused(TrainingRefusal.NOT_NEEDED);
        }
        int xp = (int) round(context.edgeRefreshCost()
                                   * person.getTalentBasedXpCostMultiplier(context.isUseReasoning(), null));
        return Quote.of(xp, Money.zero(), null);
    }

    // endregion Attributes, traits and Edge

    /**
     * Records a purchase that has been quoted and allowed in the person's projection.
     *
     * @param projection the person's projection
     * @param purchase   the purchase
     * @param quote      its allowed quote
     */
    static void apply(PersonProjection projection, TrainingPurchase purchase, Quote quote) {
        switch (purchase) {
            case SkillImprovement improvement ->
                  projection.improveSkill(improvement.skillName(), quote.projectedSkill(), quote.xp());
            case NaturalAptitude aptitude -> projection.gainAptitude(aptitude.skillName(), quote.xp());
            case AbilityPurchase ability -> projection.gainAbility(ability.abilityName(), ability.choice(),
                  quote.xp());
            case FlawBuyOff buyOff -> projection.removeFlaw(buyOff.abilityName(), quote.xp());
            case AttributeIncrease increase -> projection.changeAttribute(increase.attribute(), quote.xp());
            case TraitChange change -> projection.changeTrait(change.trait(), change.delta(), quote.xp());
            case EdgeRefill ignored -> projection.refillEdge(quote.xp());
        }
    }
}
