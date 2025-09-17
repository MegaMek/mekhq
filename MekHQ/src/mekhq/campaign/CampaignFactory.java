/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import megamek.Version;
import megamek.common.annotations.Nullable;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.io.CampaignXmlParseException;
import mekhq.campaign.io.CampaignXmlParser;
import mekhq.gui.dialog.CampaignHasProblemOnLoad;

/**
 * Defines a factory API that enables {@link Campaign} instances to be created from its detected format.
 */
public class CampaignFactory {
    private MekHQ app;

    public enum CampaignProblemType {
        NONE,
        CANT_LOAD_FROM_NEWER_VERSION,
        /**
         * No longer in use
         */
        @Deprecated(since = "0.50.07", forRemoval = true)
        CANT_LOAD_FROM_OLDER_VERSION,
        /**
         * No longer in use
         */
        @Deprecated(since = "0.50.07", forRemoval = true)
        ACTIVE_OR_FUTURE_CONTRACT,
        /**
         * No longer in use
         */
        @Deprecated(since = "0.50.07", forRemoval = true)
        NEW_VERSION_WITH_OLD_DATA
    }

    /**
     * Protected constructor to prevent instantiation.
     */
    protected CampaignFactory() {
    }

    /**
     * Obtain a new instance of a CampaignFactory.
     *
     * @return New instance of a CampaignFactory.
     */
    public static CampaignFactory newInstance(MekHQ app) {
        CampaignFactory factory = new CampaignFactory();
        factory.app = app;
        return factory;
    }

    /**
     * Creates a new instance of a {@link Campaign} from the input stream using the currently configured parameters.
     *
     * @param is The {@link InputStream} to create the {@link Campaign} from.
     *
     * @return A new instance of a {@link Campaign}.
     *
     * @throws CampaignXmlParseException if the XML for the campaign cannot be parsed.
     * @throws IOException               if an IO error is encountered reading the input stream.
     * @throws NullEntityException       if the campaign contains a null entity
     */
    public @Nullable Campaign createCampaign(InputStream is)
          throws CampaignXmlParseException, IOException, NullEntityException {
        if (!is.markSupported()) {
            is = new BufferedInputStream(is);
        }

        byte[] header = readHeader(is);

        // Check if the first two bytes are the GZIP magic bytes...
        if ((header.length >= 2) && (header[0] == (byte) 0x1f) && (header[1] == (byte) 0x8b)) {
            is = new GZIPInputStream(is);
        }
        // ...otherwise, assume we're an XML file.

        CampaignXmlParser parser = new CampaignXmlParser(is, this.app);
        Campaign campaign = parser.parse();

        return checkForLoadProblems(campaign);
    }

    /**
     * Validates the campaign for loading issues and presents the user with dialogs for each problem encountered.
     *
     * <p>This method sequentially checks for three potential problems while loading the campaign:</p>
     * <ul>
     *   <li>If the campaign version is newer than the application's version.</li>
     *   <li>If the campaign version is older than the last supported milestone version.</li>
     *   <li>If the campaign has active or future AtB contracts.</li>
     * </ul>
     *
     * <p>For each issue encountered, a dialog is displayed to the user using {@link CampaignHasProblemOnLoad}.
     * The user can either cancel or proceed with loading. If the user cancels at any point, the method
     * returns {@code null}. Otherwise, if no problems remain or the user chooses to proceed for all
     * issues, the method returns the given {@code Campaign} object.</p>
     *
     * @param campaign the {@link Campaign} object to validate and load
     *
     * @return the {@link Campaign} object if the user chooses to proceed with all problems or if no problems are
     *       detected; {@code null} if the user chooses to cancel
     */
    private static Campaign checkForLoadProblems(Campaign campaign) {
        final Version campaignVersion = campaign.getVersion();

        // Check if the campaign is from a newer version
        if (campaignVersion.isHigherThan(MHQConstants.VERSION)) {
            if (triggerProblemDialog(campaign, CampaignProblemType.CANT_LOAD_FROM_NEWER_VERSION)) {
                return null;
            }
        }

        // All checks passed, return the campaign
        return campaign;
    }

    /**
     * Displays the {@link CampaignHasProblemOnLoad} dialog for a given problem type and returns whether the user
     * cancelled the loading process.
     *
     * <p>The dialog informs the user about the specific problem and allows them to either
     * cancel the loading process or continue despite the problem. If the user selects "Cancel," the method returns
     * {@code true}. Otherwise, it returns {@code false}.</p>
     *
     * @param campaign    the {@link Campaign} object associated with the problem
     * @param problemType the {@link CampaignProblemType} specifying the current issue
     *
     * @return {@code true} if the user chose to cancel loading, {@code false} otherwise
     */
    private static boolean triggerProblemDialog(Campaign campaign, CampaignProblemType problemType) {
        CampaignHasProblemOnLoad problemDialog = new CampaignHasProblemOnLoad(campaign, problemType);
        return problemDialog.wasCanceled();
    }

    private byte[] readHeader(InputStream is) throws IOException {
        is.mark(4);
        byte[] header = new byte[2];
        is.read(header);
        is.reset();

        return header;
    }

}
