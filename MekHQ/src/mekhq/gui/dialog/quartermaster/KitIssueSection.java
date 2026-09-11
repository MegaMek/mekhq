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
package mekhq.gui.dialog.quartermaster;

import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;

import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;

/**
 * One family of quartermaster kit that the shared issue dialog can order and equip - for example armor kits or
 * technician tool kits. Each family supplies its own tab UI and knows how to price its pending selections and commit
 * them, so the dialog stays generic: it lays the sections out as tabs, aggregates their cost tallies into the shared
 * footer, and commits them together when the player confirms.
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface KitIssueSection {
    /** The tab title for this kit family. */
    String getTitle();

    /** The tab body for this kit family. */
    JComponent getComponent();

    /** Whether the player has made any selection in this section that a commit would act on. */
    boolean hasPendingChanges();

    /** The cost/stock tally of this section's pending selections, for the shared footer. */
    Tally computeTally();

    /** Applies this section's pending selections, accumulating counts into {@code totals} for the shared report. */
    void commit(CommitTotals totals);

    /** A section's contribution to the footer tally: how many kits come from stores, how many are ordered, and cost. */
    record Tally(int fromStores, int toProcure, Money cost) {
        public static Tally empty() {
            return new Tally(0, 0, Money.zero());
        }

        public Tally plus(Tally other) {
            return new Tally(fromStores + other.fromStores,
                  toProcure + other.toProcure,
                  cost.plus(other.cost));
        }
    }

    /** Running totals accumulated across all sections during a commit, used to write the summary report. */
    final class CommitTotals {
        public int issued;
        public int ordered;
        public int removed;
        public int platoons;
        public final Set<Person> changed = new HashSet<>();
    }
}
