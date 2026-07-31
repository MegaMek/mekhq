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

import static java.awt.Color.BLACK;
import static megamek.utilities.ImageUtilities.addTintToImageIcon;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jakarta.annotation.Nonnull;
import megamek.client.ui.settings.SettingsHeaderPanel;

/** Campaign-specific image and resource-key adapter over {@link SettingsHeaderPanel}. */
public class CampaignOptionsHeaderPanel extends SettingsHeaderPanel {
    private static final int DEFAULT_IMAGE_SIZE = 80;
    private static final int DEFAULT_BODY_TEXT_WIDTH = 750;
    private static final Map<String, Icon> HEADER_IMAGE_CACHE = new HashMap<>();

    public CampaignOptionsHeaderPanel(@Nonnull String name, @Nonnull String imageAddress) {
        this(name, imageAddress, false, DEFAULT_IMAGE_SIZE, true);
    }

    public CampaignOptionsHeaderPanel(@Nonnull String name, @Nonnull String imageAddress, boolean includeBodyText,
          int imageSize) {
        this(name, imageAddress, includeBodyText, imageSize, true);
    }

    public CampaignOptionsHeaderPanel(@Nonnull String name, @Nonnull String imageAddress, boolean includeBodyText,
          int imageSize, boolean tintImage) {
        this(name, imageAddress, includeBodyText, imageSize, tintImage, getCampaignOptionsResourceBundle());
    }

    public CampaignOptionsHeaderPanel(@Nonnull String name, @Nonnull String imageAddress, boolean includeBodyText,
          int imageSize, boolean tintImage, @Nonnull String resourceBundleName) {
        super(name,
              getTextAt(resourceBundleName, "lbl" + name + ".text"),
              headerIcon(imageAddress, imageSize, tintImage),
              includeBodyText ? getTextAt(resourceBundleName, "lbl" + name + "Body.text") : null,
              DEFAULT_BODY_TEXT_WIDTH);
    }

    private static Icon headerIcon(String imageAddress, int imageSize, boolean tintImage) {
        String cacheKey = imageAddress + '|' + imageSize + '|' + tintImage;
        return HEADER_IMAGE_CACHE.computeIfAbsent(cacheKey, ignored -> {
            ImageIcon icon = scaleImageIcon(new ImageIcon(imageAddress), imageSize, true);
            return tintImage ? addTintToImageIcon(icon.getImage(), BLACK) : icon;
        });
    }
}
