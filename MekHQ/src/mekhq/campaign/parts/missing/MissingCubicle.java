/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.missing;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import megamek.common.bays.BayType;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Cubicle;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Neoancient
 */
public class MissingCubicle extends MissingPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingCubicle.class);

    private BayType bayType;

    public MissingCubicle() {
        this(0, null, null);
    }

    public MissingCubicle(int tonnage, BayType bayType, Campaign c) {
        super(tonnage, false, c);
        this.bayType = bayType;
        if (null != bayType) {
            name = bayType.getDisplayName() + " Cubicle";
        }
    }

    @Override
    public String getName() {
        if (null != parentPart) {
            return parentPart.getName() + " Cubicle";
        }
        return super.getName();
    }

    @Override
    public int getBaseTime() {
        return 3360; // one week
    }

    @Override
    public void updateConditionFromPart() {
        // TODO Auto-generated method stub
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0, false);
            replacement.changeQuantity(-1);
            Part parentReference = parentPart;
            remove(false);
            if (null != parentReference) {
                parentReference.addChildPart(actualReplacement);
                parentReference.updateConditionFromPart();
            }
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof Cubicle) && (((Cubicle) part).getBayType() == bayType);
    }

    @Override
    public Part getNewPart() {
        return new Cubicle(getUnitTonnage(), bayType, campaign);
    }

    @Override
    public double getTonnage() {
        return bayType.getWeight();
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("bayType")) {
                String bayRawValue = wn2.getTextContent();
                bayType = BayType.parse(bayRawValue);
                if (null == bayType) {
                    LOGGER.error("Could not parse bay type {} treating as BayType.Mek", wn2.getTextContent());
                    bayType = BayType.MEK;
                }
                name = bayType.getDisplayName() + " Cubicle";
            }
        }
    }

    @Override
    public int writeToXMLBegin(final PrintWriter pw, int indent) {
        indent = super.writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bayType", bayType.name());
        return indent;
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public ITechnology getTechAdvancement() {
        return bayType;
    }

}
