/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.IArmorState;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.Turret;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.unit.VehicleLocations;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingTurret extends MissingPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingTurret.class);

    /** Stands for "not recorded": saves made before the turret's location was recorded. */
    private static final int UNKNOWN_LOCATION = -1;

    double weight;
    /** The location of the turret this part stands for. */
    private int turretLocation = UNKNOWN_LOCATION;

    @Deprecated(since = "0.51.0", forRemoval = true)
    public MissingTurret() {
        this(0, UNKNOWN_LOCATION, 0, null);
    }

    /**
     * @param tonnage        the vehicle's tonnage
     * @param turretLocation the location of the destroyed turret, which differs between tanks, superheavy tanks,
     *                       large support tanks and VTOLs, and between a dual-turret tank's two turrets
     * @param weight         the turret's weight
     * @param campaign       the campaign, or {@code null} while the part is being loaded from a save
     */
    public MissingTurret(int tonnage, int turretLocation, double weight, @Nullable Campaign campaign) {
        super(tonnage, campaign);
        this.turretLocation = turretLocation;
        this.weight = weight;
        this.name = "Turret";
    }

    /**
     * @return the location of the turret this part stands for. Saves made before the location was recorded fall back
     *       to the vehicle's destroyed turret, or to the standard turret location when the part is on no vehicle.
     */
    public int getTurretLocation() {
        if (turretLocation != UNKNOWN_LOCATION) {
            return turretLocation;
        }
        if ((unit != null) && (unit.getEntity() instanceof Tank tank)) {
            int destroyedTurret = VehicleLocations.destroyedTurretLocation(tank);
            LOGGER.debug("{}: destroyed turret saved without its location, taken as location {}", unit.getName(),
                  destroyedTurret);
            return destroyedTurret;
        }
        return Tank.LOC_TURRET;
    }

    @Override
    public int getBaseTime() {
        return 160;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public void writeToXML(final PrintWriter printWriter, int indent) {
        indent = writeToXMLBegin(printWriter, indent);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "weight", weight);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "turretLocation", getTurretLocation());
        writeToXMLEnd(printWriter, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        NodeList childNodes = node.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);

            try {
                if (childNode.getNodeName().equalsIgnoreCase("weight")) {
                    weight = Double.parseDouble(childNode.getTextContent());
                } else if (childNode.getNodeName().equalsIgnoreCase("turretLocation")) {
                    turretLocation = Integer.parseInt(childNode.getTextContent().trim());
                }
            } catch (Exception exception) {
                LOGGER.error("", exception);
            }
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        // Any spare turret of the same weight fits; it is moved to this turret's location when fitted
        return (part instanceof Turret) && (part.getTonnage() == weight);
    }

    @Override
    protected Part prepareReplacement(Part replacement) {
        if (replacement instanceof Turret turret) {
            turret.setTurretLocation(getTurretLocation());
        }
        return replacement;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new Turret(getTurretLocation(), getUnitTonnage(), weight, campaign);
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, getTurretLocation());
        }
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TankLocation.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }
}
