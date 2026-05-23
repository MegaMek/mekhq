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
package mekhq.campaign.base;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Personnel;
import mekhq.campaign.Warehouse;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;

/**
 * Abstract base class for player-owned bases that exist at fixed locations in the universe.
 *
 * <p>Every {@code AbstractBase} is an {@link ILocation} in the location tree. A parent
 * {@link ILocation} (typically a {@link mekhq.campaign.FixedLocation}) is required at
 * construction time to ensure the base is always anchored to a real place in the universe.</p>
 */
public abstract class AbstractBase implements ILocation {
    protected static final MMLogger logger = MMLogger.create(AbstractBase.class);

    private UUID id;
    private String displayName;
    private String displayType;
    private String planetId;
    private final LocationNode locationNode;
    private final Personnel basePersonnel = new Personnel();
    private final Warehouse baseWarehouse = new Warehouse();
    private final Hangar baseHangar = new Hangar();

    /**
     * Creates a new base anchored under {@code parentLocation}.
     *
     * @param parentLocation the location this base lives under; must not be {@code null}
     */
    protected AbstractBase(ILocation parentLocation) {
        Objects.requireNonNull(parentLocation, "parentLocation must not be null");
        this.id = UUID.randomUUID();
        this.locationNode = new LocationNode(this);
        LocationNode.LocationManager.setLocation(basePersonnel, this);
        LocationNode.LocationManager.setLocation(baseWarehouse, this);
        LocationNode.LocationManager.setLocation(baseHangar, this);
        this.setParent(parentLocation);
    }

    /** No-arg constructor for XML deserialization only. */
    protected AbstractBase() {
        this.id = UUID.randomUUID();
        this.locationNode = new LocationNode(this);
        LocationNode.LocationManager.setLocation(basePersonnel, this);
        LocationNode.LocationManager.setLocation(baseWarehouse, this);
        LocationNode.LocationManager.setLocation(baseHangar, this);
    }

    @Override
    public LocationNode getLocationNode() {
        return locationNode;
    }

    /** Returns the {@link Personnel} node that holds persons who have arrived at this base. */
    public Personnel getBasePersonnel() {
        return basePersonnel;
    }

    /** Returns the {@link Warehouse} that holds spare parts stored at this base. */
    public Warehouse getBaseWarehouse() {
        return baseWarehouse;
    }

    /** Returns the {@link Hangar} that holds units stationed at this base. */
    public Hangar getBaseHangar() {
        return baseHangar;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public @Nullable String getDisplayType() {
        return displayType;
    }

    public void setDisplayType(@Nullable String displayType) {
        this.displayType = displayType;
    }

    public @Nullable String getPlanetId() {
        return planetId;
    }

    public void setPlanetId(@Nullable String planetId) {
        this.planetId = planetId;
    }

    /**
     * Returns the immediate parent of this base in the location tree, or {@code null} if unparented.
     */
    public @Nullable ILocation getParent() {
        LocationNode parent = locationNode.getParent();
        return parent != null ? parent.getLocatable() : null;
    }

    /**
     * Writes the common base fields to XML. Subclasses should call this inside their own
     * open/close tags.
     */
    protected void writeBaseFieldsToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayName", displayName);
        if (displayType != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayType", displayType);
        }
        if (planetId != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetId", planetId);
        }
    }

    /**
     * Reads a single child node into the common base fields. Subclasses call this inside their
     * XML parsing loop.
     *
     * @return {@code true} if the node was consumed
     */
    protected static boolean readBaseFieldFromXML(AbstractBase base, Node wn2) {
        switch (wn2.getNodeName().toLowerCase()) {
            case "id" -> {
                base.id = UUID.fromString(wn2.getTextContent().trim());
                return true;
            }
            case "displayname" -> {
                base.displayName = wn2.getTextContent().trim();
                return true;
            }
            case "displaytype" -> {
                base.displayType = wn2.getTextContent().trim();
                return true;
            }
            case "planetid" -> {
                base.planetId = wn2.getTextContent().trim();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public abstract void writeToXML(PrintWriter pw, int indent);

    /**
     * Dispatches XML deserialization to the correct {@link AbstractBase} subclass based on the
     * element name of {@code wn}.
     *
     * @return the deserialized base, or {@code null} if the node name is unrecognized
     */
    public static @Nullable AbstractBase generateInstanceFromXML(Node wn, Campaign campaign,
          megamek.Version version) {
        return switch (wn.getNodeName().toLowerCase()) {
            case "playerbase" -> PlayerBase.generateInstanceFromXML(wn, campaign, version);
            default -> {
                logger.warn("Unrecognized base node '{}' — skipping", wn.getNodeName());
                yield null;
            }
        };
    }
}
