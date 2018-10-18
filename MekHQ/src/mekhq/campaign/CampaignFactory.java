/*
 * Copyright (c) 2018 - The MegaMek Team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.io.CampaignXmlParseException;
import mekhq.campaign.io.CampaignXmlParser;

/**
 * Defines a factory API that enables {@link Campaign} instances to be created
 * from its detected format.
 */
public class CampaignFactory {

    private MekHQ app;

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
    public Campaign createCampaign(InputStream is) 
        throws CampaignXmlParseException, IOException, NullEntityException {
        if (!is.markSupported()) {
            is = new BufferedInputStream(is);
        }

        byte[] header = readHeader(is);

        Boolean isGzipped = false;
        // Check if the first two bytes are the GZIP magic bytes...
        if (header.length >= 2 && header[0] == (byte) 0x1f
            && header[1] == (byte) 0x8b) {
            // ..if so, assume campaign is in a gzip file
            isGzipped = true;
            is = new GZIPInputStream(is);
        }
        // ...otherwise, assume we're an XML file.

        CampaignXmlParser parser = new CampaignXmlParser(is, this.app);

        Campaign c = parser.parse();
        c.setPreferGzippedOutput(isGzipped);

        return c;
    }

    private byte[] readHeader(InputStream is) throws IOException {
        is.mark(4);
        byte[] header = new byte[2];
        is.read(header);
        is.reset();

        return header;
    }
}
