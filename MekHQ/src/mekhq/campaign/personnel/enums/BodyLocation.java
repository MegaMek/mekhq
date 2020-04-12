/*
 * Copyright (C) 2016, 2020 - The MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;

import java.util.*;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(BodyLocation.XMLAdapter.class)
public enum BodyLocation {
    //region Enum Declarations
    HEAD(0, "BodyLocation.HEAD.text"),
    CHEST(3, "BodyLocation.CHEST.text"),
    ABDOMEN(4, "BodyLocation.ABDOMEN.text"),
    RIGHT_ARM(5, "BodyLocation.RIGHT_ARM.text", true),
    LEFT_ARM(2, "BodyLocation.LEFT_ARM.text", true),
    RIGHT_LEG(6, "BodyLocation.RIGHT_LEG.text", true),
    LEFT_LEG(1, "BodyLocation.Left_LEG.text", true),
    RIGHT_HAND(9, "BodyLocation.RIGHT_HAND.text", true, RIGHT_ARM),
    LEFT_HAND(8, "BodyLocation.LEFT_HAND.text", true, LEFT_ARM),
    RIGHT_FOOT(11, "BodyLocation.RIGHT_FOOT.text", true, RIGHT_LEG),
    LEFT_FOOT(10, "BodyLocation.LEFT_FOOT.text", true, LEFT_LEG),
    INTERNAL(7, "BodyLocation.INTERNAL.text"),
    GENERIC(-1, "BodyLocation.GENERIC.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final int id;
    private final boolean limb; // Includes everything attached to a limb
    private final String locationName;
    private final BodyLocation parent;

    /**
     * We can't use an EnumSet here because it requires the whole enum to be initialised. We
     * fix it later, in the static code block.
     */
    private Set<BodyLocation> children = new HashSet<>();

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Static Initialization
    // Initialize by-id array lookup table
    private static BodyLocation[] idMap;
    static {
        int maxId = 0;
        for (BodyLocation workTime : values()) {
            maxId = Math.max(maxId, workTime.id);
        }
        idMap = new BodyLocation[maxId + 1];
        Arrays.fill(idMap, GENERIC);
        for (BodyLocation workTime : values()) {
            if (workTime.id > 0) {
                idMap[workTime.id] = workTime;
            }
            // Optimise the children sets (we can't do that in the constructor, since
            // the EnumSet static methods require this enum to be fully initialized first).
            if (workTime.children.isEmpty()) {
                workTime.children = EnumSet.noneOf(BodyLocation.class);
            } else {
                workTime.children = EnumSet.copyOf(workTime.children);
            }
        }
    }
    //endregion Static Initialization

    //region Constructors
    BodyLocation(int id, String localizationString) {
        this(id, localizationString, false, null);
    }

    BodyLocation(int id, String localizationString, boolean limb) {
        this(id, localizationString, limb, null);
    }

    BodyLocation(int id, String localizationString, boolean limb, BodyLocation parent) {
        this.id = id;
        this.locationName = resources.getString(localizationString);
        this.limb = limb;
        this.parent = parent;
        if (parent != null) {
            parent.addChildLocation(this);
        }
    }
    //endregion Constructors

    //region Getters
    public boolean isLimb() {
        return limb;
    }

    public String locationName() {
        return locationName;
    }

    public BodyLocation Parent() {
        return parent;
    }
    //endregion Getters

    /**
     * @return the body location corresponding to the (old) ID
     */
    public static BodyLocation of(int id) {
        return ((id > 0) && (id < idMap.length)) ? idMap[id] : GENERIC;
    }

    /**
     * @return the body location corresponding to the given string
     */
    public static BodyLocation of(String str) {
        try {
            return of(Integer.parseInt(str));
        } catch(NumberFormatException ignored) {
            return valueOf(str.toUpperCase(Locale.ROOT));
        }
    }

    private void addChildLocation(BodyLocation child) {
        children.add(child);
    }

    public boolean isParentOf(BodyLocation child) {
        if (children.contains(child)) {
            return true;
        }
        for (BodyLocation myChild : children) {
            if (myChild.isParentOf(child)) {
                return true;
            }
        }
        return false;
    }

    public boolean isChildOf(BodyLocation parent) {
        return ((null != this.parent) && ((this.parent == parent) || this.parent.isChildOf(parent)));
    }

    public static final class XMLAdapter extends XmlAdapter<String, BodyLocation> {
        @Override
        public BodyLocation unmarshal(String v) {
            return (null == v) ? null : BodyLocation.of(v);
        }

        @Override
        public String marshal(BodyLocation v) {
            return (null == v) ? null : v.toString();
        }
    }
}

