/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.eras;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.TreeMap;

import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

        final File file = new MegaMekFile(MHQConstants.ERAS_FILE_PATH).getFile();
        if ((file == null) || !file.exists()) {
            throw new IOException("The eras file does not exist.");
        }

        final Document xmlDoc;

        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MHQXMLUtility.newSafeDocumentBuilder().parse(is);
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
