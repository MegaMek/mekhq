/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.resupplyAndCaches;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static mekhq.campaign.market.procurement.Procurement.getFactionTechCode;

/**
 * Utility class for managing and facilitating dialog-related operations
 * in resupply missions within MekHQ campaigns.
 *
 * <p>This class includes methods for:</p>
 * <ul>
 *     <li>Selecting appropriate logistics representatives based on rank and skill.</li>
 *     <li>Fetching icons for dialog speakers or factions.</li>
 *     <li>Creating detailed parts reports from resupply convoy contents.</li>
 *     <li>Formatting data for column-based UI display.</li>
 *     <li>Generating references to enemy factions in the campaign.</li>
 * </ul>
 *
 * <p>Primarily used for managing UI-related elements in the resupply dialogs, including
 * presentation and interaction contexts tied to resupply scenarios.</p>
 */
public class ResupplyDialogUtilities {
    /**
     * Retrieves the speaker's icon for dialogs. If no speaker is supplied, the faction icon
     * for the campaign is returned instead.
     *
     * @param campaign the {@link Campaign} instance containing the faction icon.
     * @param speaker  the {@link Person} serving as the speaker for the dialog; can be {@code null}.
     * @return an {@link ImageIcon} for the speaker's portrait, or the faction icon if the speaker is {@code null}.
     */
    public static @Nullable ImageIcon getSpeakerIcon(Campaign campaign, @Nullable Person speaker) {
        if (speaker == null) {
            return campaign.getCampaignFactionIcon();
        }

        return speaker.getPortrait().getImageIcon();
    }

    /**
     * Generates a detailed report of the parts available in the convoy contents of the given resupply mission.
     *
     * <p>The report lists parts with their names, qualities, and additional properties such as technology type
     * (Clan or Mixed), potential extinction status, tonnage, and ammunition shot counts.</p>
     *
     * @param resupply the {@link Resupply} instance defining the convoy's context.
     * @return a {@link List} of formatted strings representing the parts report, sorted alphabetically.
     */
    static List<String> createPartsReport(Resupply resupply) {
        final Campaign campaign = resupply.getCampaign();
        Faction originFaction = campaign.getFaction();
        int year = campaign.getGameYear();

        final List<Part> convoyContents = resupply.getConvoyContents();

        Map<String, Integer> entries = convoyContents.stream().collect(Collectors.toMap(
            part -> {
                String name = part.getName();
                String quality = part.getQualityName();

                String append = part.isClan() ? " (Clan)" : "";
                append = part.isMixedTech() ? " (Mixed)" : append;
                append += " (" + quality + ')';
                append += part.isExtinct(year, originFaction.isClan(), getFactionTechCode(originFaction)) ?
                    " (<b>EXTINCT!</b>)" : "";

                if (part instanceof AmmoBin) {
                    return ((AmmoBin) part).getType().getName() + append;
                } else if (part instanceof MekLocation || part instanceof MekActuator) {
                    return name + " (" + part.getUnitTonnage() + "t)" + append;
                } else {
                    return name + append;
                }
            },
            part -> {
                if (part instanceof AmmoBin) {
                    return ((AmmoBin) part).getFullShots() * 5;
                } else if (part instanceof Armor) {
                    return (int) Math.ceil(((Armor) part).getArmorPointsPerTon() * 5);
                } else {
                    return 1;
                }
            },
            Integer::sum));

        return entries.keySet().stream()
            .map(item -> item + " x" + entries.get(item))
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Formats a list of part reports into an array of three columns for visual representation,
     * ensuring that the parts are distributed across the columns evenly.
     *
     * <p>Each column entry is prepended with a bullet point and presented as an HTML string for rendering.</p>
     *
     * @param partsReport the {@link List} of part report entries to be formatted.
     * @return a {@link String} array containing three HTML-formatted columns.
     */
    public static String[] formatColumnData(List<String> partsReport) {
        String[] columns = new String[3];
        Arrays.fill(columns, "");

        int i = 0;
        for (String entry : partsReport) {
            columns[i % 3] += "<br> - " + entry;
            i++;
        }

        return columns;
    }

    /**
     * Retrieves a formatted string referencing the enemy faction in the context of the current resupply mission.
     *
     * <p>If the faction is not a Clan, it adds the prefix "the" to the faction name. The full faction
     * name is resolved for the campaign's current game year.</p>
     *
     * @param resupply the {@link Resupply} instance defining the mission's context.
     * @return a {@link String} containing the enemy faction reference.
     */
    public static String getEnemyFactionReference(Resupply resupply) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

        String enemyFactionReference = contract.getEnemy().getFullName(campaign.getGameYear());
        if (!enemyFactionReference.contains("Clan")) {
            enemyFactionReference = "the " + enemyFactionReference;
        }

        return enemyFactionReference;
    }
}
