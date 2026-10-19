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

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Factions;

/** Resolves Java2D territory emblems without changing logos used by other screens or renderers. */
final class FactionMapLogo {
    private static final MMLogger LOGGER = MMLogger.create(FactionMapLogo.class);

    private FactionMapLogo() {
    }

    /**
     * @return {@code true} if the faction (or, for a command, one of its parents) has an icon for the given year, or,
     *       for older faction files, a legacy logo
     */
    static boolean hasIcon(int gameYear, String factionCode) {
        return (RATGenerator.getCanonicalFactionLogoAddress(gameYear, factionCode) != null)
                     || (Factions.getLegacyFactionLogo(gameYear, factionCode) != null);
    }

    /**
     * Loads the faction's icon for the given year or, for older faction files without an icon, its legacy logo. When
     * neither exists or the image cannot be read, the generic Clan or mercenary logo is loaded instead.
     *
     * @return the emblem image, or {@code null} if not even the generic logo can be read
     */
    static @Nullable BufferedImage load(int gameYear, String factionCode) {
        File dedicatedLogo = (RATGenerator.getCanonicalFactionLogoAddress(gameYear, factionCode) != null)
              ? RATGenerator.getFactionLogoFile(gameYear, factionCode)
              : getLegacyLogoFile(gameYear, factionCode);
        if (dedicatedLogo != null) {
            BufferedImage image = readImage(dedicatedLogo);
            if (image != null) {
                return image;
            }
        }
        return readImage(RATGenerator.getGenericFactionLogoFile(factionCode));
    }

    /**
     * @return the legacy logo file from the formation icon logos, preferring the user directory over the installed
     *       one, or {@code null} if the faction has no legacy logo for the year
     */
    private static @Nullable File getLegacyLogoFile(int gameYear, String factionCode) {
        String legacyLogo = Factions.getLegacyFactionLogo(gameYear, factionCode);
        if (legacyLogo == null) {
            return null;
        }
        File logoDirectory = new File(MHQConstants.FORCE_ICON_PATH, MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH);
        File logoFile = new File(logoDirectory, legacyLogo);
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDirectory.isBlank()) {
            File userLogo = new File(userDirectory, logoFile.toString());
            if (userLogo.isFile()) {
                return userLogo;
            }
        }
        return logoFile;
    }

    private static @Nullable BufferedImage readImage(@Nullable File file) {
        if (file == null) {
            return null;
        }
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
