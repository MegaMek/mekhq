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
package mekhq.campaign.personnel.familiarity;

import static mekhq.utilities.MHQInternationalization.getTextAt;

public enum FamiliarityMode {
    DISABLED("DISABLED",
          0,
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0)),
    NORMAL("NORMAL",
          200,
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(1, 0),
          new FamiliarityLevel(1, 1),
          new FamiliarityLevel(1, 1)),
    HARD("HARD",
          300,
          new FamiliarityLevel(-1, -1),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(1, 0),
          new FamiliarityLevel(1, 1)),
    ROLEPLAY("ROLEPLAY",
          300,
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0));

    private final static String RESOURCE_BUNDLE = "mekhq.resources.FamiliarityMode";

    private final static int FAMILIARITY_ZERO = 0;
    private final static int FAMILIARITY_ONE_HUNDRED = 100;
    private final static int FAMILIARITY_TWO_HUNDRED = 200;
    public final static int FAMILIARITY_THREE_HUNDRED = 300;

    private final String lookUpName;
    private final int familiarityCap;
    private final String label;
    private final String tooltip;
    private final FamiliarityLevel zero;
    private final FamiliarityLevel oneHundred;
    private final FamiliarityLevel twoHundred;
    private final FamiliarityLevel threeHundred;

    FamiliarityMode(final String lookUpName, final int familiarityCap, final FamiliarityLevel zero,
          final FamiliarityLevel oneHundred, final FamiliarityLevel twoHundred, final FamiliarityLevel threeHundred) {
        this.lookUpName = lookUpName;
        this.familiarityCap = familiarityCap;
        this.label = getLabel(lookUpName);
        this.tooltip = getTooltip(lookUpName);
        this.zero = zero;
        this.oneHundred = oneHundred;
        this.twoHundred = twoHundred;
        this.threeHundred = threeHundred;
    }

    private static String getLabel(String lookUpName) {
        return getTextAt(RESOURCE_BUNDLE, "FamiliarityMode." + lookUpName + ".label");
    }

    private static String getTooltip(String lookUpName) {
        return getTextAt(RESOURCE_BUNDLE, "FamiliarityMode." + lookUpName + ".tooltip");
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }

    public int getFamiliarityCap() {
        return familiarityCap;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    public int getPilotingMaintenanceBonus(final int familiarity) {
        int level = getFamiliarityLevel(familiarity);

        return switch (level) {
            case FAMILIARITY_ZERO -> zero.pilotingMaintenance();
            case FAMILIARITY_ONE_HUNDRED -> oneHundred.pilotingMaintenance();
            case FAMILIARITY_TWO_HUNDRED -> twoHundred.pilotingMaintenance();
            case FAMILIARITY_THREE_HUNDRED -> threeHundred.pilotingMaintenance();
            default -> throw new IllegalStateException("Unexpected value: " + level);
        };
    }

    public int getGunneryRepairBonus(final int familiarity) {
        int level = getFamiliarityLevel(familiarity);

        return switch (level) {
            case FAMILIARITY_ZERO -> zero.gunneryRepairs();
            case FAMILIARITY_ONE_HUNDRED -> oneHundred.gunneryRepairs();
            case FAMILIARITY_TWO_HUNDRED -> twoHundred.gunneryRepairs();
            case FAMILIARITY_THREE_HUNDRED -> threeHundred.gunneryRepairs();
            default -> throw new IllegalStateException("Unexpected value: " + level);
        };
    }

    private static int getFamiliarityLevel(int familiarity) {
        if (familiarity < FAMILIARITY_ONE_HUNDRED) {
            return FAMILIARITY_ZERO;
        }

        if (familiarity < FAMILIARITY_TWO_HUNDRED) {
            return FAMILIARITY_ONE_HUNDRED;
        }

        if (familiarity < FAMILIARITY_THREE_HUNDRED) {
            return FAMILIARITY_TWO_HUNDRED;
        }

        return FAMILIARITY_THREE_HUNDRED;
    }

    @Override
    public String toString() {
        return label;
    }
}
