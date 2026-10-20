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
package mekhq.gui.dialog.markets.contractMarket;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import megamek.client.ui.preferences.PreferenceElement;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.gui.dialog.markets.contractMarket.ChaosContractMarketDialog.MarketPage;

/**
 * Which of the contract market's page guides the player has seen. Each page's guide opens by itself the first time the
 * player visits that page, and only from its button after that.
 *
 * <p>The flags are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's
 * node, so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while
 * MekHQ is closed) shows the guides again.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractMarketGuidePreferences {
    private static final MMLogger LOGGER = MMLogger.create(ContractMarketGuidePreferences.class);

    static final String GUIDE_SEEN_PREFIX = "guideSeen.";

    private static ContractMarketGuidePreferences instance;

    private final Map<MarketPage, Flag> seen = new EnumMap<>(MarketPage.class);

    /**
     * @param node the preferences node to store the settings under
     */
    ContractMarketGuidePreferences(final PreferencesNode node) {
        for (MarketPage page : MarketPage.values()) {
            String name = GUIDE_SEEN_PREFIX + page.name().toLowerCase(Locale.ROOT);
            try {
                Flag flag = new Flag(name);
                // Managing the flag loads any value saved in an earlier session and saves it again on exit.
                node.manage(flag);
                seen.put(page, flag);
            } catch (Exception exception) {
                LOGGER.error(exception, "Failed to register contract market guide preference {}", name);
            }
        }
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    public static synchronized ContractMarketGuidePreferences getInstance() {
        if (instance == null) {
            instance = new ContractMarketGuidePreferences(MekHQ.getMHQPreferences()
                                                                .forClass(ContractMarketGuidePreferences.class));
        }
        return instance;
    }

    /**
     * @param page a market page
     *
     * @return {@code true} once the player has seen that page's guide
     */
    boolean isGuideSeen(final MarketPage page) {
        Flag flag = seen.get(page);
        // If the flag could not be registered, treat the guide as seen rather than opening it every time.
        return (flag == null) || flag.value;
    }

    void setGuideSeen(final MarketPage page, final boolean isSeen) {
        Flag flag = seen.get(page);
        if (flag != null) {
            flag.value = isSeen;
        }
    }

    /** One on/off setting, saved in the preferences file as {@code true} or {@code false}. */
    private static final class Flag extends PreferenceElement {
        private boolean value;

        private Flag(final String name) throws Exception {
            super(name);
        }

        @Override
        protected String getValue() {
            return Boolean.toString(value);
        }

        @Override
        protected void initialize(final String saved) {
            value = MathUtility.parseBoolean(saved.strip());
        }

        @Override
        protected void dispose() {
            // Nothing to release: the flag is not tied to a component.
        }
    }
}
