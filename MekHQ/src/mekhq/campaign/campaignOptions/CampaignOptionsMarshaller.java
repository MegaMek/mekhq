/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.campaignOptions;

import java.io.PrintWriter;

import mekhq.utilities.MHQXMLUtility;

/**
 * Writes {@link CampaignOptions} to XML. Serialization is data-driven: every option is emitted by its
 * {@link CampaignOptionCodec} (see {@link CampaignOptionCodecs}) keyed by {@link CampaignOption#xmlTag()}, in
 * {@link CampaignOption#values()} declaration order.
 *
 * @author natit
 */
public class CampaignOptionsMarshaller {
    public static void writeCampaignOptionsToXML(final CampaignOptions campaignOptions, final PrintWriter pw,
          int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "campaignOptions");
        CampaignOptionCodecs.writeAll(campaignOptions, pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "campaignOptions");
    }
}
