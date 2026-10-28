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
package mekhq.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import megamek.client.ui.preferences.PreferenceElement;
import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import mekhq.gui.enums.MHQTabType;

/**
 * Persists the user's preferred order of the main {@link MHQTabType} tabs. The order is stored by enum name rather than
 * by tab title, so it survives a change of language.
 * <p>
 * The managed list is owned by the caller and is read live when preferences are saved. It always holds every
 * {@link MHQTabType} exactly once, including tabs that are currently hidden or detached, so they keep their place.
 */
public class TabOrderPreference extends PreferenceElement {
    private static final MMLogger logger = MMLogger.create(TabOrderPreference.class);

    private static final String DELIMITER = ",";

    private final List<MHQTabType> tabOrder;

    public TabOrderPreference(final String name, final List<MHQTabType> tabOrder) throws Exception {
        super(name);
        this.tabOrder = tabOrder;
    }

    @Override
    protected String getValue() {
        return tabOrder.stream().map(MHQTabType::name).collect(Collectors.joining(DELIMITER));
    }

    @Override
    protected void initialize(final String value) {
        if (StringUtility.isNullOrBlank(value)) {
            return;
        }

        final List<MHQTabType> savedOrder = new ArrayList<>();
        for (final String name : value.split(DELIMITER)) {
            try {
                final MHQTabType tabType = MHQTabType.valueOf(name.trim());
                if (!savedOrder.contains(tabType)) {
                    savedOrder.add(tabType);
                }
            } catch (IllegalArgumentException ex) {
                logger.warn("Ignoring unknown tab type {} in saved tab order", name);
            }
        }

        // Tabs missing from the saved order (such as ones added in a newer version) go after their default predecessor
        for (final MHQTabType tabType : MHQTabType.values()) {
            if (!savedOrder.contains(tabType)) {
                final int predecessorIndex = (tabType.ordinal() == 0) ? -1
                      : savedOrder.indexOf(MHQTabType.values()[tabType.ordinal() - 1]);
                savedOrder.add(predecessorIndex + 1, tabType);
            }
        }

        tabOrder.clear();
        tabOrder.addAll(savedOrder);
    }

    @Override
    protected void dispose() {
    }
}
