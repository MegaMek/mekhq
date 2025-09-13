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

import static mekhq.campaign.universe.factionStanding.FactionCensureAction.*;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;

/**
 * Enumerates the possible types of disciplinary actions (censures) that can be imposed by a faction due to low Faction
 * Standing or disciplinary issues.
 *
 * <p>These censures range from fines and forced retirements to more severe actions such as forced retirement or
 * replacement. This enumeration is used to represent outcomes resulting from faction standing events or penalties.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionCensureLevel {
    CENSURE_LEVEL_0(0, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION),
    CENSURE_LEVEL_1(1, FORMAL_WARNING, CLAN_TRIAL_OF_GRIEVANCE_UNSUCCESSFUL, LEGAL_CHALLENGE, BRIBE_OFFICIALS),
    CENSURE_LEVEL_2(2, NEWS_ARTICLE, CHATTER_WEB_DISCUSSION, NEWS_ARTICLE, NEWS_ARTICLE),
    CENSURE_LEVEL_3(3,
          COMMANDER_RETIREMENT,
          CLAN_TRIAL_OF_GRIEVANCE_SUCCESSFUL,
          FORMAL_WARNING,
          COMMANDER_IMPRISONMENT),
    CENSURE_LEVEL_4(4, LEADERSHIP_REPLACEMENT, LEADERSHIP_REPLACEMENT, FINE, COMMANDER_MURDERED),
    CENSURE_LEVEL_5(5, DISBAND, DISBAND, BARRED, LEADERSHIP_IMPRISONED);

    public static final int MIN_CENSURE_SEVERITY = CENSURE_LEVEL_0.getSeverity();
    public static final int MAX_CENSURE_SEVERITY = CENSURE_LEVEL_5.getSeverity();

    /** The severity level of this censure. Higher values indicate more severe censures. */
    private final int severity;
    private final FactionCensureAction innerSphereAction;
    private final FactionCensureAction clanAction;
    private final FactionCensureAction mercenaryAction;
    private final FactionCensureAction pirateAction;

    /**
     * Constructs a {@link FactionCensureLevel} with the specified severity.
     *
     * @param severity          the numeric severity level of this censure
     * @param innerSphereAction the censure action taken at this level for normal Inner Sphere factions
     * @param clanAction        the censure action taken at this level for normal Clan factions
     * @param mercenaryAction   the censure action taken at this level for Mercenary factions
     * @param pirateAction      the censure action taken at this level for the pirate factions
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionCensureLevel(int severity, FactionCensureAction innerSphereAction, FactionCensureAction clanAction,
          FactionCensureAction mercenaryAction, FactionCensureAction pirateAction) {
        this.severity = severity;
        this.innerSphereAction = innerSphereAction;
        this.clanAction = clanAction;
        this.mercenaryAction = mercenaryAction;
        this.pirateAction = pirateAction;
    }

    /**
     * Returns the severity level associated with this censure.
     *
     * @return the severity as an integer
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Determines the {@link FactionCensureAction} appropriate for the given faction.
     *
     * <p>The type of action is selected based on special logic for mercenary, pirate, clan, or inner sphere
     * factions:</p>
     * <ul>
     *   <li>If the faction is mercenary, returns the action designated for mercenaries.</li>
     *   <li>If the faction has the short name {@code PIR}, returns the pirate-specific action.</li>
     *   <li>If the faction is a Clan faction, returns the action specified for clans.</li>
     *   <li>Otherwise, returns the default action for inner sphere factions.</li>
     * </ul>
     *
     * @param censuringFaction the {@link Faction} issuing the censure
     *
     * @return the appropriate {@link FactionCensureAction} for the given faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureAction getFactionAppropriateAction(Faction censuringFaction) {
        if (censuringFaction.isMercenaryOrganization()) {
            return mercenaryAction;
        }

        if (censuringFaction.getShortName().equals(PIRACY_SUCCESS_INDEX_FACTION_CODE)) {
            return pirateAction;
        }

        if (censuringFaction.isClan()) {
            return clanAction;
        }

        return innerSphereAction;
    }

    /**
     * Determines if this censure is the same as the provided censure.
     *
     * @param other the censure to compare with
     *
     * @return {@code true} if this censure and the provided censure are the same; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean is(FactionCensureLevel other) {
        return this == other;
    }

    /**
     * Retrieves the {@link FactionCensureLevel} corresponding to the specified severity value.
     * <p>
     * Iterates through all available censure levels and returns the one whose severity matches the provided value. If
     * no match is found, returns {@code NONE}.
     * </p>
     *
     * @param severity the severity level to search for
     *
     * @return the matching {@link FactionCensureLevel}, or {@code NONE} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionCensureLevel getCensureLevelFromSeverity(int severity) {
        for (FactionCensureLevel censureLevel : FactionCensureLevel.values()) {
            if (censureLevel.getSeverity() == severity) {
                return censureLevel;
            }
        }
        return CENSURE_LEVEL_0;
    }

    /**
     * Parses the specified censure {@link String} into a {@link FactionCensureLevel} value.
     *
     * <p>The method first attempts to parse the text as an {@link Integer}, returning the corresponding ordinal
     * value from the {@link FactionCensureLevel} enum. If that fails, it then attempts to parse the text by its
     * name.</p>
     *
     * <p>If neither parsing attempt succeeds, it logs a warning and returns {@link #CENSURE_LEVEL_0}.</p>
     *
     * @param text the {@link String} to parse, representing either an enum ordinal or name
     *
     * @return the matching {@link FactionCensureLevel}, or {@link #CENSURE_LEVEL_0} if parsing fails
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionCensureLevel getCensureLevelFromCensureString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {
        }

        try {
            int severity = MathUtility.parseInt(text, CENSURE_LEVEL_0.getSeverity());
            return getCensureLevelFromSeverity(severity);
        } catch (Exception ignored) {
        }

        MMLogger.create(FactionCensureLevel.class)
              .warn("Unable to parse {} into an FactionCensureLevel. Returning CENSURE_LEVEL_0.",
                    text);
        return CENSURE_LEVEL_0;
    }
}
