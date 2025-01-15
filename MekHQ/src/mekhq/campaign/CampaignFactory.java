/*
 * Copyright (c) 2018 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import megamek.Version;
import megamek.common.annotations.Nullable;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.io.CampaignXmlParseException;
import mekhq.campaign.io.CampaignXmlParser;
import mekhq.gui.dialog.CampaignHasProblemOnLoad;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static mekhq.campaign.CampaignFactory.CampaignProblemType.NONE;

/**
 * Defines a factory API that enables {@link Campaign} instances to be created
 * from its detected format.
 */
public class CampaignFactory {
    private MekHQ app;

    public enum CampaignProblemType {
        NONE,
        CANT_LOAD_FROM_NEWER_VERSION,
        CANT_LOAD_FROM_OLDER_VERSION,
        ACTIVE_OR_FUTURE_CONTRACT
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
     * Creates a new instance of a {@link Campaign} from the input stream using
     * the currently configured parameters.
     *
     * @param is The {@link InputStream} to create the {@link Campaign} from.
     * @return A new instance of a {@link Campaign}.
     * @throws CampaignXmlParseException if the XML for the campaign cannot be
     *                                   parsed.
     * @throws IOException               if an IO error is encountered reading
     *                                   the input stream.
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

        if (campaign == null) {
            return null;
        }

        return checkForLoadProblems(campaign);
    }

    /**
     * Validates a given campaign for potential loading problems and handles user choices based on
     * detected issues.
     *
     * <p>This method performs the following checks:</p>
     * <ul>
     *   <li>Ensures the campaign version is compatible with the current application version.</li>
     *   <li>Checks whether the campaign version is supported compared to the last milestone version.</li>
     *   <li>Checks whether the campaign has active or future AtB (Against the Bot) contracts.</li>
     * </ul>
     *
     * <p>If any issues are found, a dialog prompts the user to decide whether to proceed or not.
     * If the user chooses to abort, the method returns {@code null}; otherwise, it returns the
     * original campaign object.</p>
     *
     * @param campaign the {@link Campaign} object to validate for loading issues
     * @return the given {@link Campaign} if no critical issues are detected or the user chooses to proceed;
     *         {@code null} if the user opts to abort loading due to critical issues
     */
    private static Campaign checkForLoadProblems(Campaign campaign) {
        CampaignProblemType problemType = CampaignProblemType.NONE;

        final Version mhqVersion = MHQConstants.VERSION;
        final Version lastMilestone = MHQConstants.LAST_MILESTONE;
        final Version campaignVersion = campaign.getVersion();

        if (campaignVersion.isHigherThan(mhqVersion)) {
            problemType = CampaignProblemType.CANT_LOAD_FROM_NEWER_VERSION;
        }

        if (campaignVersion.isLowerThan(lastMilestone)) {
            problemType = CampaignProblemType.CANT_LOAD_FROM_OLDER_VERSION;
        }

        if (!campaign.hasActiveAtBContract(true)) {
            problemType = CampaignProblemType.ACTIVE_OR_FUTURE_CONTRACT;
        }

        if (problemType != NONE) {
            CampaignHasProblemOnLoad problemDialog = new CampaignHasProblemOnLoad(campaign, problemType);

            if (problemDialog.getDialogChoice() == 0) {
                return null;
            }
        }

        return campaign;
    }

    private byte[] readHeader(InputStream is) throws IOException {
        is.mark(4);
        byte[] header = new byte[2];
        is.read(header);
        is.reset();

        return header;
    }

}
