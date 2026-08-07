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
package mekhq.gui.dialog;

import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.WarriorsAlmanac.AlmanacTechAdvancementPhase;
import mekhq.campaign.universe.WarriorsAlmanac.WarriorsAlmanacData;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;

public class WarriorsAlmanacDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.WarriorsAlmanacDialog";

    private record TechBaseColumn(String headerKey, @Nullable WarriorsAlmanacData data) {}

    private boolean isEmpty = true;

    public WarriorsAlmanacDialog(final Campaign campaign, final boolean isAutomaticDisplay) {
        final LocalDate currentDate = campaign.getLocalDate();
        final int gameYear = currentDate.getYear();

        boolean isClanForce = campaign.getPlayerForce().getFaction().isClan();
        boolean hideClanAndMixed = !currentDate.isAfter(BATTLE_OF_TUKAYYID) && !isClanForce;
        boolean hideInnerSphereAndMixed = !currentDate.isAfter(CLAN_INVASION_FIRST_WAVE_BEGINS) && isClanForce;

        final StringBuilder report = new StringBuilder();

        report.append(getFormattedTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.title", String.valueOf(gameYear)));
        report.append(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.blurb"));

        appendSections(campaign, hideClanAndMixed, report, gameYear, hideInnerSphereAndMixed);

        if (isEmpty) {
            report.append(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.nothing"));

            if (isAutomaticDisplay) {
                return;
            }
        }

        new ImmersiveDialogNotification(campaign, report.toString(), true);
    }

    private void appendSections(Campaign campaign, boolean hideClanAndMixed, StringBuilder report, int gameYear,
          boolean hideInnerSphereAndMixed) {
        if (hideClanAndMixed) {
            appendSection(report, "WarriorsAlmanacDialog.parts", List.of(
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.innerSphere",
                        campaign.getPartsAlmanacIS().get(gameYear))));

            appendSection(report, "WarriorsAlmanacDialog.units", List.of(
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.innerSphere",
                        campaign.getUnitsAlmanacIS().get(gameYear)),
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.unknown",
                        campaign.getUnitsAlmanacUnknown().get(gameYear))));
        } else if (hideInnerSphereAndMixed) {
            appendSection(report, "WarriorsAlmanacDialog.parts", List.of(
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.clan",
                        campaign.getPartsAlmanacClan().get(gameYear))));

            appendSection(report, "WarriorsAlmanacDialog.units", List.of(
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.clan",
                        campaign.getUnitsAlmanacClan().get(gameYear)),
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.unknown",
                        campaign.getUnitsAlmanacUnknown().get(gameYear))));
        } else {
            appendSection(report, "WarriorsAlmanacDialog.parts", List.of(
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.innerSphere",
                        campaign.getPartsAlmanacIS().get(gameYear)),
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.clan",
                        campaign.getPartsAlmanacClan().get(gameYear))));

            appendSection(report, "WarriorsAlmanacDialog.units", List.of(
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.innerSphere",
                        campaign.getUnitsAlmanacIS().get(gameYear)),
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.clan",
                        campaign.getUnitsAlmanacClan().get(gameYear)),
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.mixed",
                        campaign.getUnitsAlmanacMixed().get(gameYear)),
                  new TechBaseColumn("WarriorsAlmanacDialog.techBase.unknown",
                        campaign.getUnitsAlmanacUnknown().get(gameYear))));
        }
    }

    private void appendSection(final StringBuilder report, final String sectionHeaderKey,
          final List<TechBaseColumn> columns) {
        final List<Map<AlmanacTechAdvancementPhase, List<String>>> groupedByColumn = new ArrayList<>();
        for (TechBaseColumn column : columns) {
            groupedByColumn.add(groupNamesByPhase(column.data()));
        }

        boolean sectionHeaderWritten = false;
        for (AlmanacTechAdvancementPhase AlmanacTechAdvancementPhase : AlmanacTechAdvancementPhase.values()) {
            boolean phaseHeaderWritten = false;
            for (int i = 0; i < columns.size(); i++) {
                final List<String> names = groupedByColumn.get(i).get(AlmanacTechAdvancementPhase);
                if (names == null) {
                    continue;
                }
                if (!sectionHeaderWritten) {
                    report.append(getTextAt(RESOURCE_BUNDLE, sectionHeaderKey));
                    sectionHeaderWritten = true;
                    isEmpty = false;
                }
                if (!phaseHeaderWritten) {
                    report.append(getTextAt(RESOURCE_BUNDLE, AlmanacTechAdvancementPhase.getHeaderKey()));
                    phaseHeaderWritten = true;
                }
                report.append(getTextAt(RESOURCE_BUNDLE, columns.get(i).headerKey()));
                appendNames(report, names);
            }
        }
    }

    private static Map<AlmanacTechAdvancementPhase, List<String>> groupNamesByPhase(
          final @Nullable WarriorsAlmanacData data) {
        final Map<AlmanacTechAdvancementPhase, List<String>> grouped = new EnumMap<>(AlmanacTechAdvancementPhase.class);
        if (data == null) {
            return grouped;
        }

        final Set<String> alreadyGrouped = new HashSet<>();
        for (AlmanacTechAdvancementPhase AlmanacTechAdvancementPhase : AlmanacTechAdvancementPhase.values()) {
            final List<String> names = new ArrayList<>();
            for (String name : AlmanacTechAdvancementPhase.getNames().apply(data)) {
                if (alreadyGrouped.add(name)) {
                    names.add(name);
                }
            }
            if (!names.isEmpty()) {
                grouped.put(AlmanacTechAdvancementPhase, names);
            }
        }
        return grouped;
    }

    private static void appendNames(final StringBuilder report, final List<String> names) {
        report.append("<ul>");
        names.stream()
              .sorted(String.CASE_INSENSITIVE_ORDER)
              .forEach(name -> report.append("<li>").append(name).append("</li>"));
        report.append("</ul>");
    }
}
