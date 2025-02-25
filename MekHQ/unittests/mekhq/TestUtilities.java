/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

import mekhq.campaign.Campaign;

public final class TestUtilities {
    public static Campaign getTestCampaign() {
        return new Campaign();
    }

    public static InputStream ParseBase64XmlFile(String base64) {
        return new ByteArrayInputStream(Decode(base64));
    }

    public static byte[] Decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
