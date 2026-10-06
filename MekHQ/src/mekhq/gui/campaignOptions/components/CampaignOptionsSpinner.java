/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.components;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.settingsBadges;
import static mekhq.gui.campaignOptions.components.CampaignOptionsComponentSupport.campaignTextProvider;
import static mekhq.gui.campaignOptions.components.CampaignOptionsComponentSupport.textProvider;

import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.settings.SettingsSpinner;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;

/** Campaign-specific resource-key and metadata adapter over {@link SettingsSpinner}. */
public class CampaignOptionsSpinner extends SettingsSpinner {
    public CampaignOptionsSpinner(@Nonnull String name, Number defaultValue, Number minimum, Number maximum,
          Number stepSize, boolean noTooltip, @Nullable CampaignOptionsMetadata metadata) {
        super(campaignTextProvider(), "lbl" + name, defaultValue, minimum, maximum, stepSize, noTooltip,
              settingsBadges(metadata));
        setName("spn" + name);
    }

    public CampaignOptionsSpinner(@Nonnull String name, int defaultValue, int minimum, int maximum, int stepSize) {
        this(name, defaultValue, minimum, maximum, stepSize, false, null);
    }

    public CampaignOptionsSpinner(@Nonnull String name, double defaultValue, double minimum, double maximum,
          double stepSize) {
        this(name, defaultValue, minimum, maximum, stepSize, false, null);
    }

    public CampaignOptionsSpinner(@Nonnull String name, int defaultValue, int minimum, int maximum, int stepSize,
          @Nullable CampaignOptionsMetadata metadata) {
        this(name, defaultValue, minimum, maximum, stepSize, false, metadata);
    }

    public CampaignOptionsSpinner(@Nonnull String name, double defaultValue, double minimum, double maximum,
          double stepSize, @Nullable CampaignOptionsMetadata metadata) {
        this(name, defaultValue, minimum, maximum, stepSize, false, metadata);
    }

    public CampaignOptionsSpinner(@Nonnull String name, double defaultValue, double minimum, double maximum,
          double stepSize, boolean noTooltip) {
        this(name, defaultValue, minimum, maximum, stepSize, noTooltip, null);
    }

    public CampaignOptionsSpinner(@Nonnull String resourceBundleName, @Nonnull String name, Number defaultValue,
          Number minimum, Number maximum, Number stepSize) {
        super(textProvider(resourceBundleName), name, defaultValue, minimum, maximum, stepSize);
        setName("spn" + name);
    }

    public static void installSelectAllOnFocus(@Nonnull JSpinner spinner) {
        SettingsSpinner.installSelectAllOnFocus(spinner);
    }
}
