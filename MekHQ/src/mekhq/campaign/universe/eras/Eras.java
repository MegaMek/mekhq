/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.eras;

import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.MekHqXmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Eras {
    //region Variable Declarations
    private static Eras instance;

    private TreeMap<LocalDate, Era> eras;
    //endregion Variable Declarations

    //region Constructors
    private Eras() {
        setEras(new TreeMap<>(LocalDate::compareTo));
    }
    //endregion Constructors

    //region Getters/Setters
    public static Eras getInstance() {
        if (instance == null) {
            setInstance(new Eras());
        }

        return instance;
    }

    public static void setInstance(final @Nullable Eras instance) {
        Eras.instance = instance;
    }

    public TreeMap<LocalDate, Era> getEras() {
        return eras;
    }

    private void setEras(final TreeMap<LocalDate, Era> eras) {
        this.eras = eras;
    }

    public Era getEra(final LocalDate today) {
        return getEras().ceilingEntry(today).getValue();
    }
    //endregion Getters/Setters

    //region File I/O
    public static void initializeEras() throws Exception {
        final Eras eras = new Eras();

        final File file = new MegaMekFile(MekHqConstants.ERAS_FILE_PATH).getFile();
        if ((file == null) || !file.exists()) {
            throw new IOException("The eras file does not exist.");
        }

        final Document xmlDoc;

        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MekHqXmlUtil.newSafeDocumentBuilder().parse(is);
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();
        final NodeList nl = element.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn = nl.item(x);

            if (!wn.getParentNode().equals(element) || (wn.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }

            if (wn.getNodeName().equalsIgnoreCase("era") && wn.hasChildNodes()) {
                final Era era = Era.generateInstanceFromXML(wn.getChildNodes());
                if (era != null) {
                    eras.getEras().put(era.getEnd(), era);
                }
            }
        }

        if (eras.getEras().isEmpty()) {
            throw new IOException("Failed to parse any eras");
        }

        setInstance(eras);
    }
    //endregion File I/O
}
