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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.settings.SettingsCheckBox;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;

/** Campaign-specific resource-key and metadata adapter over {@link SettingsCheckBox}. */
public class CampaignOptionsCheckBox extends SettingsCheckBox {
    public CampaignOptionsCheckBox(@Nonnull String name) {
        this(name, (Integer) null, null);
    }

    public CampaignOptionsCheckBox(@Nonnull String name, @Nullable Integer customWrapSize) {
        this(name, customWrapSize, null);
    }

    public CampaignOptionsCheckBox(@Nonnull String name, @Nullable CampaignOptionsMetadata metadata) {
        this(name, (Integer) null, metadata);
    }

    public CampaignOptionsCheckBox(@Nonnull String name, @Nullable Integer customWrapSize,
          @Nullable CampaignOptionsMetadata metadata) {
        super(campaignTextProvider(), "lbl" + name, customWrapSize, settingsBadges(metadata));
        setName("chk" + name);
    }

    public CampaignOptionsCheckBox(@Nonnull String resourceBundleName, @Nonnull String name) {
        this(resourceBundleName, name, null);
    }

    public CampaignOptionsCheckBox(@Nonnull String resourceBundleName, @Nonnull String name,
          @Nullable CampaignOptionsMetadata metadata) {
        super(textProvider(resourceBundleName), name, settingsBadges(metadata));
        setName("chk" + name);
    }
}
