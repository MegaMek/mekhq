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
package mekhq.campaign.universe.factionHints;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Processes and evaluates faction relationship changes ("war and peace" events) for the player faction on the current
 * day.
 *
 * <p>Gathers all relevant {@link FactionHints} (wars, rivalries, alliances) from the campaign's FactionHints data,
 * identifies which start or end today for each faction active on the current game date, and triggers dialog messages
 * for each category as appropriate.</p>
 *
 * <p>Used to notify the player of major diplomatic events in the campaign.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class WarAndPeaceProcessor {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.WarAndPeaceProcessor";

    private final String KEY_WAR_START = "warStartFactions";
    private final String KEY_WAR_END = "warEndFactions";
    private final String KEY_RIVALRY_START = "rivalryStartFactions";
    private final String KEY_RIVALRY_END = "rivalryEndFactions";
    private final String KEY_NEUTRAL_START = "neutralStartFactions";
    private final String KEY_NEUTRAL_END = "neutralEndFactions";
    private final String KEY_ALLIANCE_START = "allianceStartFactions";
    private final String KEY_ALLIANCE_END = "allianceEndFactions";

    private final Campaign campaign;
    private final LocalDate today;

    private final Set<Faction> warStartFactions = new HashSet<>();
    private final Set<Faction> warEndFactions = new HashSet<>();

    private final Set<Faction> rivalStartFactions = new HashSet<>();
    private final Set<Faction> rivalEndFactions = new HashSet<>();

    private final Set<Faction> neutralStartFactions = new HashSet<>();
    private final Set<Faction> neutralEndFactions = new HashSet<>();

    private final Set<Faction> allianceStartFactions = new HashSet<>();
    private final Set<Faction> allianceEndFactions = new HashSet<>();

    /**
     * Constructs a {@link WarAndPeaceProcessor}, collecting and processing all faction-related relationship changes for
     * today.
     *
     * <p>Triggers notifications to the player if relevant changes are found.</p>
     *
     * @param campaign   The campaign context in which to operate.
     * @param activeOnly If {@code true}, only check whether the hint is active today not if it started today
     *
     * @author Illiani
     * @since 0.50.10
     */
    public WarAndPeaceProcessor(final Campaign campaign, final boolean activeOnly) {
        this.campaign = campaign;
        this.today = campaign.getLocalDate();
        final Faction campaignFaction = campaign.getFaction();

        processFactionHints(campaignFaction, activeOnly);
        triggerAllMessages(campaignFaction);
    }

    /**
     * Triggers notification dialogs for all newly started or ending relationships today.
     *
     * @param campaignFaction The player's campaign faction for which to display events.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void triggerAllMessages(Faction campaignFaction) {
        Map<String, Set<Faction>> factionArrays = Map.of(
              KEY_WAR_START, warStartFactions,
              KEY_WAR_END, warEndFactions,
              KEY_RIVALRY_START, rivalStartFactions,
              KEY_RIVALRY_END, rivalEndFactions,
              KEY_NEUTRAL_START, neutralStartFactions,
              KEY_NEUTRAL_END, neutralEndFactions,
              KEY_ALLIANCE_START, allianceStartFactions,
              KEY_ALLIANCE_END, allianceEndFactions
        );

        final String oocMessage = getTextAt(RESOURCE_BUNDLE, "WarAndPeaceProcessor.ooc");
        final String buttonLabel = getTextAt(RESOURCE_BUNDLE, "WarAndPeaceProcessor.button");
        for (Map.Entry<String, Set<Faction>> entry : factionArrays.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                triggerMessage(campaignFaction, entry.getKey(), entry.getValue(), oocMessage, buttonLabel);
            }
        }
    }

    /**
     * Composes and shows a notification dialog about one type of relationship change (war/rivalry/alliance start/end).
     *
     * @param campaignFaction The player's campaign faction.
     * @param keyAffix        String key indicating the relationship type ("warStartFactions", etc.)
     * @param factions        The factions involved in the relationship change.
     * @param oocMessage      Supplemental message to show in the dialog.
     * @param buttonLabel     Label for the confirmation button.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void triggerMessage(Faction campaignFaction, String keyAffix, Set<Faction> factions, String oocMessage,
          String buttonLabel) {
        StringBuilder message = new StringBuilder(getTextAt(RESOURCE_BUNDLE, "WarAndPeaceProcessor." + keyAffix));
        for (Faction faction : factions) {
            String name = FactionStandingUtilities.getFactionName(faction, today.getYear());
            message.append("<br> - <b>").append(name).append("<b>");
        }

        // We use a proxy speaker to allow us to leverage a mechanic in ImmersiveDialogSimple that shows the faction
        // icon in the event the speaker has no portrait.
        Person proxySpeaker = new Person(campaign);
        proxySpeaker.setOriginFaction(campaignFaction);
        proxySpeaker.setGivenName("");
        proxySpeaker.setSurname("");

        new ImmersiveDialogSimple(campaign,
              proxySpeaker,
              null,
              message.toString(),
              List.of(buttonLabel),
              oocMessage,
              null,
              false);
    }

    /**
     * Populates the internal sets for started and ended wars, rivalries, and alliances for today.
     *
     * @param campaignFaction The player's campaign faction.
     * @param activeOnly      If {@code true}, only check whether the hint is active today not if it started today
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void processFactionHints(final Faction campaignFaction, boolean activeOnly) {
        FactionHints factionHints = FactionHints.getInstance();
        Collection<Faction> activeFactions = Factions.getInstance().getActiveFactions(today);

        final Map<Faction, List<FactionHint>> wars = factionHints.getWars()
                                                           .getOrDefault(campaignFaction, Map.of());
        collectionFactions(activeFactions, wars, warStartFactions, warEndFactions, activeOnly);

        final Map<Faction, List<FactionHint>> rivalries = factionHints.getRivals()
                                                                .getOrDefault(campaignFaction, Map.of());
        collectionFactions(activeFactions, rivalries, rivalStartFactions, rivalEndFactions, activeOnly);

        final Map<Faction, List<FactionHint>> neutrals = factionHints.getNeutralExceptions()
                                                               .getOrDefault(campaignFaction, Map.of());
        collectionFactions(activeFactions, neutrals, neutralStartFactions, neutralEndFactions, activeOnly);

        final Map<Faction, List<FactionHint>> alliances = factionHints.getAlliances()
                                                                .getOrDefault(campaignFaction, Map.of());
        collectionFactions(activeFactions, alliances, allianceStartFactions, allianceEndFactions, activeOnly);
    }

    /**
     * Examines faction hint lists and adds each relevant faction to either the "starts today" or "ends today" sets.
     *
     * @param activeFactions List of candidate factions.
     * @param factionHints   Map of factions to their relationship hints.
     * @param startFactions  Output set: factions whose relationship starts today.
     * @param endFactions    Output set: factions whose relationship ends today.
     * @param activeOnly     If {@code true}, only check whether the hint is active today not if it started today
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void collectionFactions(final Collection<Faction> activeFactions,
          final Map<Faction, List<FactionHint>> factionHints, final Set<Faction> startFactions,
          final Set<Faction> endFactions, boolean activeOnly) {
        for (Faction faction : activeFactions) {
            List<FactionHint> hints = factionHints.getOrDefault(faction, List.of());
            for (FactionHint hint : hints) {
                if (hint.hintStartsToday(today)) {
                    startFactions.add(faction);
                } else if (activeOnly && hint.isInDateRange(today)) {
                    startFactions.add(faction);
                } else if (hint.hintEndsToday(today)) {
                    endFactions.add(faction);
                }

                // At this point we have processed all hints for the faction
                if (startFactions.contains(faction) && endFactions.contains(faction)) {
                    break;
                }
            }
        }
    }
}
