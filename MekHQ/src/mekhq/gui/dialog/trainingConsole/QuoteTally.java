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
package mekhq.gui.dialog.trainingConsole;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingRefusal;

/**
 * How a purchase prices out across a group of people: who can make it now, what that costs in XP and fees, and why
 * the rest can't. Every catalog card on the Skills, Abilities and Attributes pages is drawn from one.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class QuoteTally {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;

    private int xp;
    private int minXp = Integer.MAX_VALUE;
    private int maxXp = Integer.MIN_VALUE;
    private Money fees = Money.zero();
    private final List<TrainingPurchase> purchases = new ArrayList<>();
    private final List<Person> readyPeople = new ArrayList<>();
    private final Map<TrainingRefusal, List<Person>> cannot = new EnumMap<>(TrainingRefusal.class);

    /**
     * Prices one purchase per person against the plan.
     *
     * @param plan        the plan
     * @param people      the people
     * @param purchaseFor the purchase for each person
     *
     * @return the tally
     */
    static QuoteTally of(TrainingPlan plan, List<Person> people,
          Function<Person, ? extends TrainingPurchase> purchaseFor) {
        QuoteTally tally = new QuoteTally();
        for (Person person : people) {
            TrainingPurchase purchase = purchaseFor.apply(person);
            tally.add(purchase, plan.quote(purchase));
        }
        return tally;
    }

    /** Counts one priced purchase. */
    void add(TrainingPurchase purchase, Quote quote) {
        if (quote.isAllowed()) {
            xp += quote.xp();
            minXp = Math.min(minXp, quote.xp());
            maxXp = Math.max(maxXp, quote.xp());
            fees = fees.plus(quote.fee());
            purchases.add(purchase);
            readyPeople.add(purchase.person());
        } else {
            addCannot(quote.refusal(), purchase.person());
        }
    }

    /** Counts someone who can't, without pricing anything. */
    void addCannot(TrainingRefusal refusal, Person person) {
        cannot.computeIfAbsent(refusal, key -> new ArrayList<>()).add(person);
    }

    /** @return how many can make the purchase now */
    int ready() {
        return readyPeople.size();
    }

    /** @return the XP everyone who can would spend, less any they would get back */
    int xp() {
        return xp;
    }

    /** @return the training fees of everyone who can */
    Money fees() {
        return fees;
    }

    /** @return the allowed purchases, in order */
    List<TrainingPurchase> purchases() {
        return purchases;
    }

    /** @return the people who can make the purchase now */
    List<Person> readyPeople() {
        return readyPeople;
    }

    /** @return the people who can't, by why not */
    Map<TrainingRefusal, List<Person>> cannot() {
        return cannot;
    }

    /** @return the people refused for one reason */
    List<Person> cannot(TrainingRefusal refusal) {
        return cannot.getOrDefault(refusal, List.of());
    }

    /** @return how many were refused for one reason */
    int count(TrainingRefusal refusal) {
        return cannot(refusal).size();
    }

    /** @return how many were refused, for any reason */
    int cannotCount() {
        return cannot.values().stream().mapToInt(List::size).sum();
    }

    /** @return the XP line under a card's button, such as "120 XP" or "+40 XP back", or empty if no one can */
    String xpLine() {
        return (ready() == 0) ? "" : ConsoleWidgets.xpText(xp);
    }

    /**
     * @param singleKey the line when everyone pays the same, with the XP as {0}
     * @param rangeKey  the line when they don't, with the lowest and highest as {0} and {1}
     *
     * @return what each person pays, or empty if no one can
     */
    String costText(String singleKey, String rangeKey) {
        if (ready() == 0) {
            return "";
        }
        return (minXp == maxXp) ? getFormattedTextAt(RESOURCE_BUNDLE, singleKey, minXp) :
                     getFormattedTextAt(RESOURCE_BUNDLE, rangeKey, minXp, maxXp);
    }

    /**
     * @param isAffordable whether the campaign can pay everyone's fees
     *
     * @return why the card's button can't be used, shown under it: the fees, or the most common reason no one can;
     *       {@code null} if it can be used
     */
    @Nullable String unavailableReason(boolean isAffordable) {
        if (!isAffordable) {
            return getTextAt(RESOURCE_BUNDLE, "fees.cannotAfford.short");
        }
        if (ready() > 0) {
            return null;
        }
        Map.Entry<TrainingRefusal, List<Person>> top = null;
        for (Map.Entry<TrainingRefusal, List<Person>> entry : cannot.entrySet()) {
            if ((top == null) || (entry.getValue().size() > top.getValue().size())) {
                top = entry;
            }
        }
        return (top == null) ? null : ConsoleWidgets.reason(top.getValue().size(), top.getKey());
    }
}
