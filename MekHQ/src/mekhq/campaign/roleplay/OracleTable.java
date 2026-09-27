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
package mekhq.campaign.roleplay;

import java.nio.file.Paths;

import mekhq.MHQConstants;

/**
 * Every oracle table available to the solo-roleplay oracle system. Each table maps to a {@code meaning,weight} CSV
 * file located beneath {@link MHQConstants#ORACLE_DIRECTORY} (with an optional override beneath
 * {@link MHQConstants#ORACLE_DIRECTORY_USER}).
 */
public enum OracleTable {
    ADVENTURE_AFTERMATH("adventure/aftermath"),
    ADVENTURE_ALLY("adventure/ally"),
    ADVENTURE_ASSET("adventure/asset"),
    ADVENTURE_BETRAYAL("adventure/betrayal"),
    ADVENTURE_CLUE("adventure/clue"),
    ADVENTURE_COMPLICATION("adventure/complication"),
    ADVENTURE_CONFLICT("adventure/conflict"),
    ADVENTURE_COST("adventure/cost"),
    ADVENTURE_DILEMMA("adventure/dilema"),
    ADVENTURE_ESCALATION("adventure/escalation"),
    ADVENTURE_INFORMATION("adventure/information"),
    ADVENTURE_LOCATION("adventure/location"),
    ADVENTURE_MISSION("adventure/mission"),
    ADVENTURE_OBJECTIVE("adventure/objective"),
    ADVENTURE_PRESSURE("adventure/pressure"),
    ADVENTURE_RESOURCES("adventure/resources"),
    ADVENTURE_REVELATION("adventure/revelation"),
    ADVENTURE_REWARD("adventure/reward"),
    ADVENTURE_SECRET("adventure/secret"),
    ADVENTURE_STAKES("adventure/stakes"),
    ADVENTURE_THREAT("adventure/threat"),
    ADVENTURE_TWIST("adventure/twist"),
    CHARACTERS_ACTION("characters/action"),
    CHARACTERS_APPEARANCE("characters/appearance"),
    CHARACTERS_DEMEANOR("characters/demeanor"),
    CHARACTERS_MOTIVE("characters/motive"),
    CHARACTERS_PERSONALITY("characters/personality"),
    CHARACTERS_RELATIONSHIP("characters/relationship"),
    ENVIRONMENTS_ARTIFICIAL_ACTIVITY("environments/artificial/activity"),
    ENVIRONMENTS_ARTIFICIAL_ATMOSPHERE("environments/artificial/atmosphere"),
    ENVIRONMENTS_ARTIFICIAL_CONDITION("environments/artificial/condition"),
    ENVIRONMENTS_ARTIFICIAL_SMELLS("environments/artificial/smells"),
    ENVIRONMENTS_ARTIFICIAL_SOUNDS("environments/artificial/sounds"),
    ENVIRONMENTS_NATURAL_ATMOSPHERE("environments/natural/atmosphere"),
    ENVIRONMENTS_NATURAL_ECOLOGY("environments/natural/ecology"),
    ENVIRONMENTS_NATURAL_SMELLS("environments/natural/smells"),
    ENVIRONMENTS_NATURAL_SOUNDS("environments/natural/sounds"),
    ENVIRONMENTS_NATURAL_WEATHER("environments/natural/weather"),
    THEMES_AMBITION("themes/ambition"),
    THEMES_CHANGE("themes/change"),
    THEMES_CONFLICT("themes/conflict"),
    THEMES_FAMILY("themes/family"),
    THEMES_FEAR("themes/fear"),
    THEMES_FREEDOM("themes/freedom"),
    THEMES_GREED("themes/greed"),
    THEMES_HONOR("themes/honor"),
    THEMES_IDENTITY("themes/identity"),
    THEMES_JUSTICE("themes/justice"),
    THEMES_LEGACY("themes/legacy"),
    THEMES_LOYALTY("themes/loyalty"),
    THEMES_OBLIGATION("themes/obligation"),
    THEMES_POWER("themes/power"),
    THEMES_REVENGE("themes/revenge"),
    THEMES_SACRIFICE("themes/sacrifice"),
    THEMES_SECRETS("themes/secrets"),
    THEMES_SURVIVAL("themes/survival"),
    THEMES_TRADITION("themes/tradition"),
    THEMES_TRUST("themes/trust");

    /** The table's path relative to the oracle directory, without the {@code .csv} extension. */
    private final String relativePath;

    OracleTable(final String relativePath) {
        this.relativePath = relativePath;
    }

    /**
     * Builds a display label from the constant's name: the category, then the rest of the name. For example,
     * {@code ENVIRONMENTS_NATURAL_WEATHER} becomes "Environments: Natural Weather".
     *
     * @return the display label
     */
    public String getLabel() {
        final String[] words = name().split("_");
        final StringBuilder label = new StringBuilder(capitalize(words[0])).append(':');
        for (int i = 1; i < words.length; i++) {
            label.append(' ').append(capitalize(words[i]));
        }
        return label.toString();
    }

    private static String capitalize(final String word) {
        return word.charAt(0) + word.substring(1).toLowerCase();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * @return the table's path relative to the oracle directory, without the {@code .csv} extension
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * @return the path of the base data file for this table
     */
    public String getFilePath() {
        return Paths.get(MHQConstants.ORACLE_DIRECTORY, relativePath + ".csv").toString();
    }

    /**
     * @return the path of the optional {@code userdata/} override file for this table
     */
    public String getUserFilePath() {
        return Paths.get(MHQConstants.ORACLE_DIRECTORY_USER, relativePath + ".csv").toString();
    }
}
