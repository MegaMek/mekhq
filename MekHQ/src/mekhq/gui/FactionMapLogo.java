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
package mekhq.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.universe.Factions2;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Factions;

/** Resolves Java2D territory emblems without changing logos used by other screens or renderers. */
final class FactionMapLogo {
    private static final MMLogger LOGGER = MMLogger.create(FactionMapLogo.class);

    private FactionMapLogo() {
    }

    static boolean hasCustomLogo(int gameYear, String factionCode) {
        return Factions.getCanonicalFactionLogoAddress(gameYear, factionCode) == null
                     && getConfiguredLogo(gameYear, factionCode) != null;
    }

    static @Nullable BufferedImage load(int gameYear, String factionCode) {
        String canonicalAddress = Factions.getCanonicalFactionLogoAddress(gameYear, factionCode);
        if (canonicalAddress != null) {
            return readImage(new File(canonicalAddress));
        }

        String configuredLogo = getConfiguredLogo(gameYear, factionCode);
        if (configuredLogo != null) {
            File logoDirectory = new File(MHQConstants.FORCE_ICON_PATH, MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH);
            File logoFile = new File(logoDirectory, configuredLogo);
            String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
            if (!userDirectory.isBlank()) {
                File userLogo = new File(userDirectory, logoFile.toString());
                if (userLogo.isFile()) {
                    logoFile = userLogo;
                }
            }
            BufferedImage image = readImage(logoFile);
            if (image != null) {
                return image;
            }
        }
        return readImage(new File(Factions.getFactionLogoAddress(gameYear, factionCode)));
    }

    private static @Nullable String getConfiguredLogo(int gameYear, String factionCode) {
        return Factions2.getInstance().getFaction(factionCode)
                     .map(faction -> faction.getLogo(gameYear))
                     .filter(logo -> !logo.isBlank())
                     .orElse(null);
    }

    private static @Nullable BufferedImage readImage(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                LOGGER.warn("Unable to decode faction map logo {}", file);
            }
            return image;
        } catch (IOException exception) {
            LOGGER.warn(exception, "Unable to read faction map logo {}", file);
            return null;
        }
    }
}
