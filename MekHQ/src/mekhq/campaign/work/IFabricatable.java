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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nonnull;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechRating;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.unit.Unit;

/**
 * Shared fabrication behavior for work items that can be manufactured from scratch (for money) rather than drawn from
 * warehouse stock. Both {@link mekhq.campaign.parts.missing.MissingPart} (replacing a missing component) and
 * {@link mekhq.campaign.parts.equipment.AmmoBin} (reloading ammunition) implement this so the cost, tech-rating
 * eligibility, time, and target-number rules live in one place.
 *
 * <p>The fabrication state itself ({@link Part#isFabricating()} / {@link Part#isFabricateUntilSuccess()}) is stored on
 * {@link Part} and persists with the save; this interface only supplies the behavior derived from it. What a successful
 * fabrication actually produces is left to each implementing type.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IFabricatable extends IPartWork {
    String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /** Fabrication takes ten times the normal replacement time and cost (Campaign Ops, p.202 rev 5th printing). */
    int FABRICATION_MULTIPLIER = 10;
    /** A fabrication attempt receives an additional +2 to its target number. */
    int FABRICATION_MODIFIER = 2;
    /** The Fabricator Special Ability reduces the fabrication target number by 2. */
    int FABRICATOR_ABILITY_MODIFIER = -2;
    /** The Jury-Rigger Special Ability reduces all fabrication costs by 25% (i.e. to 75% of normal). */
    double JURY_RIGGER_COST_MULTIPLIER = 0.75;
    /** The Wasteful flaw increases all fabrication (and repair) costs by 25% (i.e. to 125% of normal). */
    double WASTEFUL_COST_MULTIPLIER = 1.25;
    /** Fraction of a part's undamaged value charged as the cost of a normal repair when 'Pay For Repairs' is enabled. */
    double REPAIR_COST_FRACTION = 0.2;
    /** The purchase price of a fabricated part is half the sale price of a new component. */
    double FABRICATED_PART_PRICE_FRACTION = 0.5;

    // region Accessors supplied by the implementing Part
    Campaign getCampaign();

    @Override
    @Nullable
    Person getTech();

    @Override
    @Nullable
    Unit getUnit();

    @Nonnull
    TechRating getTechRating();

    String getName();

    boolean isFabricating();

    void setFabricating(boolean isFabricating);

    boolean isFabricateUntilSuccess();

    void setFabricateUntilSuccess(boolean fabricateUntilSuccess);

    /**
     * @return a brand-new instance of the component this work item would install (used to derive fabrication cost and
     *       eligibility)
     */
    Part getNewPart();
    // endregion

    /**
     * The undamaged value used for the 'Pay For Repairs' component of the fabrication cost. Defaults to the value of a
     * new part; types that fabricate only a portion of a component (e.g. a partial ammo reload) may override this.
     *
     * @return the value basis for the repair-cost component of a fabrication attempt
     */
    default Money getFabricationRepairBasis() {
        return getNewPart().getUndamagedValue();
    }

    /**
     * The part value used for the 'Pay For Parts' component of the fabrication cost. Defaults to the value of a new
     * part; types that fabricate only a portion of a component (e.g. a partial ammo reload) may override this.
     *
     * @return the value basis for the part-cost component of a fabrication attempt
     */
    default Money getFabricationPartBasis() {
        return getNewPart().getActualValue();
    }

    /**
     * @return the time multiplier applied while fabricating (ten times the normal work time), or 1 when not fabricating
     */
    default int fabricationTimeMultiplier() {
        return isFabricating() ? FABRICATION_MULTIPLIER : 1;
    }

    /**
     * Adds the fabrication target-number modifiers to the supplied roll when this work item is being fabricated: the
     * standard +2 fabrication penalty, offset by the Fabricator Special Ability if the tech has it.
     *
     * @param mods the target roll to adjust in place
     * @param tech the tech attempting the fabrication, or {@code null}
     */
    default void addFabricationMods(TargetRoll mods, final @Nullable Person tech) {
        if (isFabricating()) {
            mods.addModifier(FABRICATION_MODIFIER, "fabricating");
            if ((tech != null) && tech.getOptions().booleanOption(PersonnelOptions.TECH_FABRICATOR)) {
                mods.addModifier(FABRICATOR_ABILITY_MODIFIER, "Fabricator");
            }
        }
    }

    /**
     * Cancels an in-progress fabrication, reverting this task to a normal replacement/reload. Clears the fabrication
     * flags and unassigns any tech, resetting accumulated overtime and time spent (spent minutes are not refunded as
     * money).
     */
    default void cancelFabrication() {
        setFabricating(false);
        setFabricateUntilSuccess(false);
        cancelAssignment(true);
    }

    /**
     * @return {@code true} if the item being fabricated is ammunition, which enables the ammunition-only Munitioneer
     *       Special Ability in {@link #canFabricate(Person)}. Defaults to {@code false}; ammo bins override it.
     */
    default boolean isAmmunitionFabrication() {
        return false;
    }

    /**
     * Determines whether this may be fabricated from scratch by its assigned tech.
     *
     * @return an empty string if it can be fabricated, otherwise an explanation of why it cannot
     *
     * @see #canFabricate(Person)
     */
    default String canFabricate() {
        return canFabricate(getTech());
    }

    /**
     * Determines whether this may be fabricated from scratch by the given tech. Without factory-grade facilities,
     * fabrication is limited to components with a base Tech Rating of A, B, or C (Campaign Ops, p.202 rev 5th
     * printing); a factory-conditions site removes that restriction, and an optional rule additionally permits Tech
     * Rating D at a maintenance facility. The MacGyver Special Ability, if the tech has it, treats the Tech Rating as
     * one lower for this determination; the Munitioneer Special Ability does the same for ammunition only, and the two
     * stack.
     *
     * @param tech the tech who would perform the fabrication, or {@code null} to judge eligibility without any tech
     *             ability
     *
     * @return an empty string if it can be fabricated, otherwise an explanation of why it cannot (and, for a Tech
     *       Rating that is too high, what site would fix that)
     */
    default String canFabricate(final @Nullable Person tech) {
        final Unit unit = getUnit();
        if (unit == null) {
            return getTextAt(RESOURCE_BUNDLE, "MissingPart.noUnit");
        }

        // A factory-grade installation lifts the tech-rating restriction.
        if (unit.getSite() >= Unit.SITE_FACTORY_CONDITIONS) {
            return "";
        }

        TechRating rating = getTechRating();

        int effectiveRating = rating.ordinal();
        if (tech != null) {
            // MacGyver treats any part's base Tech Rating as one lower.
            if (tech.getOptions().booleanOption(PersonnelOptions.TECH_MACGYVER)) {
                effectiveRating--;
            }
            // Munitioneer is the ammunition-only counterpart, and stacks with MacGyver.
            if (isAmmunitionFabrication() && tech.getOptions().booleanOption(PersonnelOptions.TECH_MUNITIONEER)) {
                effectiveRating--;
            }
            effectiveRating = Math.max(0, effectiveRating);
        }

        // Base limit is Tech Rating C. An optional rule additionally permits Tech Rating D at a maintenance facility
        // (or better).
        boolean maintenanceFacilityAllowsD = getCampaign().getCampaignOptions()
                                                   .get(CampaignOption.FABRICATE_D_IN_MAINTENANCE_FACILITY);
        int maxRating = TechRating.C.ordinal();
        if (maintenanceFacilityAllowsD && (unit.getSite() >= Unit.SITE_FACILITY_MAINTENANCE)) {
            maxRating = TechRating.D.ordinal();
        }

        if (effectiveRating <= maxRating) {
            return "";
        }

        // Tech Rating D can be unlocked at a maintenance facility when the optional rule is on; anything higher (or
        // Tech Rating D without the optional rule) needs factory conditions.
        if (maintenanceFacilityAllowsD && (effectiveRating == TechRating.D.ordinal())) {
            return getTextAt(RESOURCE_BUNDLE, "MissingPart.complex.maintenance");
        }

        return getTextAt(RESOURCE_BUNDLE, "MissingPart.complex.factory");
    }

    /**
     * Computes the cost of a single fabrication attempt (charged per attempt, not only on success).
     *
     * @return the money cost of one fabrication attempt for the assigned tech (can be {@link Money#zero()})
     */
    default Money getFabricationCost() {
        return getFabricationCost(getTech());
    }

    /**
     * Computes the cost of a single fabrication attempt performed by the given tech.
     *
     * <p>Both cost profiles charge ten times the normal repair/replacement cost (when the campaign pays for repairs).
     * They differ only in the part-price component, charged when the campaign pays for parts:</p>
     * <ul>
     *     <li><b>Balanced fabrication</b> (default): ten times the new part's price.</li>
     *     <li><b>Rules-accurate</b>: half the new part's price.</li>
     * </ul>
     *
     * <p>The Jury-Rigger Special Ability, if the tech has it, reduces the total by 25%, or the Wasteful flaw increases
     * it by 25%.</p>
     *
     * @param tech the tech who would perform the fabrication, or {@code null} to price it without any tech ability
     *
     * @return the money cost of one fabrication attempt (may be {@link Money#zero()})
     */
    default Money getFabricationCost(final @Nullable Person tech) {
        final CampaignOptions options = getCampaign().getCampaignOptions();

        Money cost = Money.zero();
        if (options.get(CampaignOption.PAY_FOR_REPAIRS)) {
            // Ten times the cost of a normal repair.
            cost = cost.plus(getFabricationRepairBasis()
                                   .multipliedBy(REPAIR_COST_FRACTION)
                                   .multipliedBy(FABRICATION_MULTIPLIER));
        }
        if (options.get(CampaignOption.PAY_FOR_PARTS)) {
            // Part price: ten times under the balanced profile, half under the rules-accurate profile.
            double partFraction = options.get(CampaignOption.USE_BALANCED_FABRICATION)
                                        ? FABRICATION_MULTIPLIER
                                        : FABRICATED_PART_PRICE_FRACTION;
            cost = cost.plus(getFabricationPartBasis().multipliedBy(partFraction));
        }

        // The Jury-Rigger and Wasteful Special Abilities adjust all fabrication costs (they are mutually exclusive).
        if (tech != null) {
            if (tech.getOptions().booleanOption(PersonnelOptions.TECH_JURY_RIGGER)) {
                cost = cost.multipliedBy(JURY_RIGGER_COST_MULTIPLIER);
            } else if (tech.getOptions().booleanOption(PersonnelOptions.TECH_WASTEFUL)) {
                cost = cost.multipliedBy(WASTEFUL_COST_MULTIPLIER);
            }
        }
        return cost;
    }

    /**
     * Debits the cost of a single fabrication attempt (per attempt, regardless of success or failure).
     *
     * @param finances the finances to debit
     */
    default void chargeFabricationAttempt(Finances finances) {
        Money cost = getFabricationCost();
        if (!cost.isZero()) {
            finances.debit(TransactionType.EQUIPMENT_PURCHASE,
                  getCampaign().getLocalDate(),
                  cost,
                  getFormattedTextAt(RESOURCE_BUNDLE, "Fabricatable.transaction.description", getName()));
        }
    }
}
