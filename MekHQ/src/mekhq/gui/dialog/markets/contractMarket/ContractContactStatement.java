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
package mekhq.gui.dialog.markets.contractMarket;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.Locale;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingLevel;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Composes the flavor statement the employer's contact makes when pitching a contract in the dossier.
 *
 * <p>The statement is built from two halves, each adapted from {@code FactionStandingGreeting} to read as an offer
 * being made rather than an assignment handed down:</p>
 * <ul>
 *     <li>a <b>greeting</b> that carries the hiring faction's voice and how it regards the player, keyed by the
 *     faction and by the campaign's standing with it (banded positive, neutral, or negative); and</li>
 *     <li>an <b>ask</b> that describes the actual job, keyed by the contract objective type and the same standing
 *     band, so a warm employer frames the work as trust and a hostile one frames it as a warning.</li>
 * </ul>
 *
 * <p>Both halves are written to work whether the player is a mercenary hired by the faction or a member of it.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractContactStatement {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int NEUTRAL_BAND_FLOOR = 3;
    private static final int POSITIVE_BAND_FLOOR = 6;

    private ContractContactStatement() {
    }

    /**
     * Returns the contact's pitch for the given offer: a faction-and-standing greeting followed by an objective-and-
     * standing ask.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String forContract(Campaign campaign, AbstractContract contract) {
        String faction = factionKey(contract);
        String band = standingBand(campaign, contract);
        String objective = objectiveKey(contract);
        String baseName = contract.getEmployerDisplayName();

        String nameCapitalized = FactionStandingUtilities.addNamePrefix(baseName, true);
        String nameLowercase = FactionStandingUtilities.addNamePrefix(baseName, false);

        String greeting = getFormattedTextAt(RESOURCE_BUNDLE,
              "greeting.contractMarket." + faction + "." + band, nameCapitalized, nameLowercase);
        String ask = getFormattedTextAt(RESOURCE_BUNDLE,
              "ask.contractMarket." + objective + "." + band, nameCapitalized, nameLowercase);

        return greeting + " " + ask;
    }

    /**
     * Bands the campaign's standing with the hiring faction into positive, neutral, or negative.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String standingBand(Campaign campaign, AbstractContract contract) {
        FactionStandings standings = campaign.getPlayerForce().getFactionStandings();
        double regard = standings.getRegardForFaction(contract.getEmployerFactionCode(), true);
        FactionStandingLevel standingLevel = FactionStandingUtilities.calculateFactionStandingLevel(regard);
        int level = standingLevel.getStandingLevel();

        if (level >= POSITIVE_BAND_FLOOR) {
            return "positive";
        }
        if (level >= NEUTRAL_BAND_FLOOR) {
            return "neutral";
        }
        return "negative";
    }

    /**
     * Maps the hiring faction to a greeting voice. The Clan, ComStar, and Word of Blake registers are chosen by
     * predicate; the major Inner Sphere and Periphery states by faction code; everything else falls back to the generic
     * Inner Sphere or Periphery voice.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String factionKey(AbstractContract contract) {
        Faction faction = contract.getEmployerFaction();
        if (faction == null) {
            return "innersphere";
        }
        if (faction.isClan()) {
            return "clan";
        }
        if (faction.isWoB()) {
            return "wordofblake";
        }
        if (faction.isComStar()) {
            return "comstar";
        }

        String voice = switch (faction.getShortName()) {
            case "CC" -> "capellan";
            case "DC" -> "draconis";
            case "LA", "LC" -> "lyran";
            case "FS" -> "federatedsuns";
            case "FWL" -> "freeworlds";
            case "TH" -> "terran";
            case "ROS" -> "republic";
            case "SL" -> "starleague";
            case "FC" -> "fedcom";
            case "RWR" -> "rimworlds";
            case "TC" -> "taurian";
            case "MOC" -> "canopus";
            case "OA" -> "outworlds";
            case "MH" -> "marian";
            default -> null;
        };
        if (voice != null) {
            return voice;
        }

        return faction.isPeriphery() ? "periphery" : "innersphere";
    }

    /**
     * Maps the contract objective type to its ask key. Falls back to the undefined ask if the objective is somehow
     * missing.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String objectiveKey(AbstractContract contract) {
        ContractObjectiveType objectiveType = contract.getObjectiveType();
        if (objectiveType == null) {
            return "undefined";
        }
        return objectiveType.name().toLowerCase(Locale.ROOT);
    }
}
