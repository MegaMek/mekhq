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
package mekhq.campaign.market.personnelMarket.records;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.RecruitmentCosts.TrainingCostBreakdown;

/**
 * The cost of hiring an applicant from the personnel market, split by part.
 *
 * @param salaryCost       the salary-based hiring cost, zero if the applicant is priced by their training
 * @param trainingCost     the training-based hiring cost, or {@code null} if the applicant is priced by their salary
 * @param goldenHelloExtra the extra cost of a Golden Hello, zero if none is offered
 *
 * @author Illiani
 * @since 0.51.01
 */
public record HiringCostBreakdown(Money salaryCost, @Nullable TrainingCostBreakdown trainingCost,
      Money goldenHelloExtra) {
    /**
     * @return a breakdown for an applicant who costs nothing to hire
     */
    public static HiringCostBreakdown free() {
        return new HiringCostBreakdown(Money.zero(), null, Money.zero());
    }

    /**
     * @return the combined cost of every part
     */
    public Money total() {
        Money total = salaryCost.plus(goldenHelloExtra);
        return trainingCost == null ? total : total.plus(trainingCost.total());
    }
}
