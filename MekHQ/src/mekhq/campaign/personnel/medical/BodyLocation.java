/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mekhq.MekHQ;
import mekhq.campaign.personnel.medical.BodyLocation.XMLAdapter;

@XmlJavaTypeAdapter(value = XMLAdapter.class)
public enum BodyLocation {
    //region Enum Declarations
    HEAD(0, "BodyLocation.HEAD.text"),
    SKULL(12, "BodyLocation.SKULL.text", false, HEAD),
    EARS(13, "BodyLocation.EARS.text", false, HEAD),
    EYES(14, "BodyLocation.EYES.text", false, HEAD),
    JAW(15, "BodyLocation.JAW.text", false, HEAD),
    CHEST(3, "BodyLocation.CHEST.text"),
    RIBS(16, "BodyLocation.RIBS.text", false, CHEST),
    LUNGS(17, "BodyLocation.LUNGS.text", false, CHEST),
    HEART(18, "BodyLocation.HEART.text", false, CHEST),
    ORGANS(44, "BodyLocation.ORGANS.text", false, CHEST),
    ABDOMEN(4, "BodyLocation.ABDOMEN.text"),
    GROIN(19, "BodyLocation.GROIN.text", false, ABDOMEN),
    RIGHT_ARM(5, "BodyLocation.RIGHT_ARM.text", true),
    UPPER_RIGHT_ARM(23, "BodyLocation.UPPER_RIGHT_ARM.text", true, RIGHT_ARM),
    RIGHT_ELBOW(24, "BodyLocation.RIGHT_ELBOW.text", true, RIGHT_ARM),
    RIGHT_SHOULDER(25, "BodyLocation.RIGHT_SHOULDER.text", true, RIGHT_ARM),
    LEFT_ARM(2, "BodyLocation.LEFT_ARM.text", true),
    UPPER_LEFT_ARM(20, "BodyLocation.UPPER_LEFT_ARM.text", true, LEFT_ARM),
    LEFT_ELBOW(21, "BodyLocation.LEFT_ELBOW.text", true, LEFT_ARM),
    LEFT_SHOULDER(22, "BodyLocation.LEFT_SHOULDER.text", true, LEFT_ARM),
    RIGHT_LEG(6, "BodyLocation.RIGHT_LEG.text", true),
    RIGHT_THIGH(33, "BodyLocation.RIGHT_THIGH.text", true, RIGHT_LEG),
    RIGHT_FEMUR(34, "BodyLocation.RIGHT_FEMUR.text", true, RIGHT_LEG),
    RIGHT_HIP(35, "BodyLocation.RIGHT_HIP.text", true, RIGHT_LEG),
    LEFT_LEG(1, "BodyLocation.LEFT_LEG.text", true),
    LEFT_THIGH(30, "BodyLocation.LEFT_THIGH.text", true, LEFT_LEG),
    LEFT_FEMUR(31, "BodyLocation.LEFT_FEMUR.text", true, LEFT_LEG),
    LEFT_HIP(32, "BodyLocation.LEFT_HIP.text", true, LEFT_LEG),
    RIGHT_HAND(9, "BodyLocation.RIGHT_HAND.text", true, RIGHT_ARM),
    RIGHT_WRIST(28, "BodyLocation.RIGHT_WRIST.text", true, RIGHT_HAND),
    RIGHT_FOREARM(29, "BodyLocation.RIGHT_FOREARM.text", true, RIGHT_HAND),
    LEFT_HAND(8, "BodyLocation.LEFT_HAND.text", true, LEFT_ARM),
    LEFT_WRIST(26, "BodyLocation.LEFT_WRIST.text", true, LEFT_HAND),
    LEFT_FOREARM(27, "BodyLocation.LEFT_FOREARM.text", true, LEFT_HAND),
    RIGHT_FOOT(11, "BodyLocation.RIGHT_FOOT.text", true, RIGHT_LEG),
    LEFT_FOOT(10, "BodyLocation.LEFT_FOOT.text", true, LEFT_LEG),
    LEFT_CALF(36, "BodyLocation.LEFT_CALF.text", true, LEFT_FOOT),
    LEFT_ANKLE(37, "BodyLocation.LEFT_ANKLE.text", true, LEFT_FOOT),
    LEFT_KNEE(38, "BodyLocation.LEFT_KNEE.text", true, LEFT_FOOT),
    LEFT_SHIN(39, "BodyLocation.LEFT_SHIN.text", true, LEFT_FOOT),
    RIGHT_CALF(40, "BodyLocation.RIGHT_CALF.text", true, RIGHT_FOOT),
    RIGHT_ANKLE(41, "BodyLocation.RIGHT_ANKLE.text", true, RIGHT_FOOT),
    RIGHT_KNEE(42, "BodyLocation.RIGHT_KNEE.text", true, RIGHT_FOOT),
    RIGHT_SHIN(43, "BodyLocation.RIGHT_SHIN.text", true, RIGHT_FOOT),
    INTERNAL(7, "BodyLocation.INTERNAL.text"),
    GENERIC(-1, "BodyLocation.GENERIC.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final int id;
    private final boolean limb; // Includes everything attached to a limb
    private final String locationName;
    private final BodyLocation parent;

    /**
     * We can't use an EnumSet here because it requires the whole enum to be initialised. We fix it later, in the static
     * code block.
     */
    private Set<BodyLocation> children = new HashSet<>();
    //endregion Variable Declarations

    //region Static Initialization
    // Initialize by-id array lookup table
    private static final BodyLocation[] idMap;

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
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
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
        } catch (NumberFormatException ignored) {
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
