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
package mekhq.campaign.market.personnelMarket.enums;

import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the Personnel Market Style tooltip in
 * {@code CampaignOptionsDialog.properties}.
 *
 * <p>Guards against issue #8776, where the tooltip referred to the disabled state as
 * "None" while the actual dropdown option (sourced from
 * {@link PersonnelMarketStyle#PERSONNEL_MARKET_DISABLED}) renders as "Disabled". The two
 * must agree, otherwise users see a help text that names an option that does not exist.</p>
 */
class PersonnelMarketStyleTooltipTest {
    private static final String CAMPAIGN_OPTIONS_BUNDLE = "mekhq.resources.CampaignOptionsDialog";
    private static final String TOOLTIP_KEY = "lblPersonnelMarketStyle.tooltip";

    @Test
    void tooltipMentionsDisabledOptionByItsActualLabel() {
        String tooltip = getTextAt(CAMPAIGN_OPTIONS_BUNDLE, TOOLTIP_KEY);
        String expectedLabel = PERSONNEL_MARKET_DISABLED.toString();

        assertTrue(tooltip.contains(expectedLabel),
              "Personnel Market Style tooltip must reference the disabled-state option by its "
                    + "user-facing label (\"" + expectedLabel + "\"). Got: " + tooltip);
    }

    @Test
    void tooltipDoesNotReferToDisabledOptionAsNone() {
        // Specific regression guard for #8776: the bullet was originally "<b>None</b> means
        // the new personnel markets are disabled.", which did not match the dropdown label.
        String tooltip = getTextAt(CAMPAIGN_OPTIONS_BUNDLE, TOOLTIP_KEY);

        assertFalse(tooltip.contains("<b>None</b>"),
              "Personnel Market Style tooltip must not refer to the disabled state as <b>None</b>; "
                    + "the dropdown option is labelled \"" + PERSONNEL_MARKET_DISABLED.toString() + "\".");
    }
}
