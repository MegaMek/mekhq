package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

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

    private String reportAsString;
    private boolean isEmpty = true;

    public boolean isEmpty() {
        return isEmpty;
    }

    public WarriorsAlmanacDialog(final Campaign campaign) {
        final int gameYear = campaign.getGameYear();

        final StringBuilder report = new StringBuilder();

        report.append(getFormattedTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.title", gameYear));
        report.append(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.blurb"));

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

        // Both sections were empty for this year, so there's nothing worth showing.
        if (isEmpty) {
            return;
        }

        new ImmersiveDialogNotification(campaign, report.toString(), true);
    }

    private void appendSection(final StringBuilder report, final String sectionHeaderKey,
          final List<TechBaseColumn> columns) {
        // Resolve each column's names into a single phase up front, honoring the phase priority.
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
