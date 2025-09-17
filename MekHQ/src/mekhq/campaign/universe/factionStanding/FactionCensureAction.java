/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

/**
 * An enumeration representing various types of censure actions that can be applied by a faction. Each enum constant
 * represents a specific faction-related action or event associated with disciplinary, leadership, or administrative
 * measures.
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionCensureAction {
    BARRED("BARRED", true, true),
    CHATTER_WEB_DISCUSSION("CHATTER_WEB_DISCUSSION", true, true),
    CLAN_TRIAL_OF_GRIEVANCE_SUCCESSFUL("CLAN_TRIAL_OF_GRIEVANCE_SUCCESSFUL", false, true),
    CLAN_TRIAL_OF_GRIEVANCE_UNSUCCESSFUL("CLAN_TRIAL_OF_GRIEVANCE_UNSUCCESSFUL", false, true),
    COMMANDER_IMPRISONMENT("COMMANDER_IMPRISONMENT", false, false),
    COMMANDER_MURDERED("COMMANDER_MURDERED", false, true),
    COMMANDER_RETIREMENT("COMMANDER_RETIREMENT", false, true),
    DISBAND("DISBAND", false, true),
    FINE("FINE", true, true),
    BRIBE_OFFICIALS("BRIBE_OFFICIALS", true, true),
    FORMAL_WARNING("FORMAL_WARNING", true, true),
    LEADERSHIP_IMPRISONED("LEADERSHIP_IMPRISONED", false, false),
    LEADERSHIP_REPLACEMENT("LEADERSHIP_REPLACEMENT", false, false),
    LEGAL_CHALLENGE("LEGAL_CHALLENGE", true, true),
    NEWS_ARTICLE("NEWS_ARTICLE", true, true),
    NO_ACTION("NO_ACTION", true, true);

    /** String representation for lookup purposes (usually the enum name itself). */
    private final String lookupName;
    private final boolean validOnContract;
    private final boolean validInTransit;

    /**
     * Constructs a new {@link FactionCensureAction} with the specified lookup name.
     *
     * @param lookupName      the string representation used for lookup purposes, typically corresponding to the enum
     *                        name
     * @param validOnContract {@code true} if the action can be performed while the campaign is on-contract
     * @param validInTransit  {@code true} if the action can be performed while the campaign is in transit
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionCensureAction(String lookupName, boolean validOnContract, boolean validInTransit) {
        this.lookupName = lookupName;
        this.validOnContract = validOnContract;
        this.validInTransit = validInTransit;
    }

    /**
     * Returns the string lookup name of this censure action.
     *
     * @return the lookup name, typically matching the enum name
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookupName() {
        return lookupName;
    }

    /**
     * Checks if the faction censure action is valid while the campaign is on-contract.
     *
     * @return {@code true} if the action can be performed while the campaign is on-contract, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isValidOnContract() {
        return validOnContract;
    }

    /**
     * Checks if the faction censure action is valid while the campaign is in transit.
     *
     * @return {@code true} if the action can be performed while the campaign is in transit, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isValidInTransit() {
        return validInTransit;
    }

    @Override
    public String toString() {
        return this.getLookupName().replace("_", " ");
    }
}
