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
package mekhq.gui.dialog.glossary;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

import jakarta.annotation.Nullable;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.campaign.utilities.glossary.GlossaryIndex;
import mekhq.campaign.utilities.glossary.GlossaryIndex.Item;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.dialog.glossary.GlossaryPdfExporter.Options;
import mekhq.gui.dialog.glossary.GlossaryPdfExporter.PageSize;
import mekhq.gui.utilities.WrapLayout;

/**
 * Chooses which glossary entries to export to a PDF, and how the document is laid out.
 *
 * <p>A scope ticks a starting set: the entry being read, the entries the index lists, the entries changed since the
 * last core release, or the whole glossary. The ticks can then be changed one by one, so a player can add the few
 * entries they want to a list, or drop the ones they do not.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class GlossaryExportDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = GlossaryConsole.RESOURCE_BUNDLE;

    /** The starting sets of entries a player can choose from. */
    enum Scope {
        /** The entry being read. */
        ENTRY,
        /** The entries the index lists, after its search and filter. */
        LIST,
        /** The entries added or updated since the last core release. */
        SINCE_LAST_CORE,
        /** Every entry. */
        ALL
    }

    private final GlossaryIndex index;
    private final @Nullable GlossaryEntry currentEntry;
    private final List<GlossaryEntry> listedEntries;
    private final Map<GlossaryEntry, HudCheckBox> checkBoxes = new LinkedHashMap<>();
    private final HudSegmentedControl<Scope> scopeControl = new HudSegmentedControl<>(this::applyScope);
    private final HudSegmentedControl<PageSize> pageSizeControl = new HudSegmentedControl<>(pageSize -> {});
    private final HudCheckBox contentsBox;
    private final HudCheckBox bookmarksBox;
    private final HudCheckBox linksBox;
    private final HudCheckBox referencedByBox;
    private final JScrollPane entryScroll;
    private HudButton exportButton;
    private boolean confirmed;

    /**
     * Shows the export choices.
     *
     * @param owner         the glossary console
     * @param index         the glossary's entries
     * @param currentEntry  the entry being read, or {@code null} if none
     * @param listedEntries the entries the index lists after a search or filter, in its order; empty if it lists
     *                      everything
     *
     * @author Illiani
     * @since 0.51.01
     */
    GlossaryExportDialog(Window owner, GlossaryIndex index, @Nullable GlossaryEntry currentEntry,
          List<GlossaryEntry> listedEntries) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "export.glossary.title"), ModalityType.APPLICATION_MODAL);
        this.index = index;
        this.currentEntry = currentEntry;
        this.listedEntries = List.copyOf(listedEntries);
        for (Item item : index.items()) {
            HudCheckBox checkBox = new HudCheckBox(item.title());
            checkBox.addActionListener(event -> refreshExportButton());
            checkBoxes.put(item.entry(), checkBox);
        }
        contentsBox = optionBox("export.glossary.option.contents", true);
        bookmarksBox = optionBox("export.glossary.option.bookmarks", true);
        linksBox = optionBox("export.glossary.option.links", true);
        referencedByBox = optionBox("export.glossary.option.referencedBy", false);
        entryScroll = new JScrollPane();
        initializeComponents();
    }

    /** @return {@code true} if the player chose to export, rather than cancelling */
    boolean wasConfirmed() {
        return confirmed;
    }

    /**
     * @return the entries the player ticked. A single entry, or the index's list, keeps its own order; anything else
     *       is in title order.
     */
    List<GlossaryEntry> getSelectedEntries() {
        Set<GlossaryEntry> ticked = new HashSet<>();
        checkBoxes.forEach((entry, checkBox) -> {
            if (checkBox.isSelected()) {
                ticked.add(entry);
            }
        });
        List<GlossaryEntry> selected = new ArrayList<>();
        if (scopeControl.getSelected() == Scope.LIST) {
            // The list's own order first, such as best search match first, then anything ticked besides.
            listedEntries.stream().filter(ticked::contains).forEach(selected::add);
        }
        checkBoxes.keySet().stream().filter(ticked::contains).filter(entry -> !selected.contains(entry))
              .forEach(selected::add);
        return selected;
    }

    /** @return how the player wants the document laid out */
    Options getOptions() {
        PageSize pageSize = pageSizeControl.getSelected();
        return new Options(pageSize == null ? PageSize.LETTER : pageSize, contentsBox.isSelected(),
              bookmarksBox.isSelected(), linksBox.isSelected(), referencedByBox.isSelected());
    }

    // region Layout

    private void initializeComponents() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        content.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        Scope initial = currentEntry != null ? Scope.ENTRY : (!listedEntries.isEmpty() ? Scope.LIST : Scope.ALL);
        scopeControl.setSelected(initial);
        applyScope(initial);
        setMinimumSize(scaleForGUI(560, 460));
        pack();
        setSize(scaleForGUI(640, 620));
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private JComponent buildBody() {
        JPanel body = column();
        body.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(14), scaleForGUI(10),
              scaleForGUI(14)));

        body.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "export.glossary.scope"))));
        body.add(Box.createVerticalStrut(scaleForGUI(5)));
        long changedCount = index.items().stream().filter(item -> item.change().isChanged()).count();
        List<Segment<Scope>> scopes = new ArrayList<>();
        scopes.add(scopeSegment(Scope.ENTRY, currentEntry == null ? "" : currentEntry.getTitle(),
              currentEntry != null));
        scopes.add(scopeSegment(Scope.LIST, getFormattedTextAt(RESOURCE_BUNDLE, "export.glossary.count",
              listedEntries.size()), !listedEntries.isEmpty()));
        scopes.add(scopeSegment(Scope.SINCE_LAST_CORE, getFormattedTextAt(RESOURCE_BUNDLE, "export.glossary.count",
              changedCount), changedCount > 0));
        scopes.add(scopeSegment(Scope.ALL, getFormattedTextAt(RESOURCE_BUNDLE, "export.glossary.count",
              index.items().size()), true));
        // Two to a row, so the open entry's title has room to show under its scope.
        scopeControl.setColumns(2);
        scopeControl.setSegments(scopes);
        body.add(leftAligned(scopeControl));
        body.add(Box.createVerticalStrut(scaleForGUI(12)));

        JPanel layoutRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(14), 0));
        JPanel paper = column();
        paper.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "export.glossary.paper"))));
        paper.add(Box.createVerticalStrut(scaleForGUI(5)));
        List<Segment<PageSize>> sizes = new ArrayList<>();
        for (PageSize pageSize : PageSize.values()) {
            sizes.add(new Segment<>(pageSize, getTextAt(RESOURCE_BUNDLE, "export.glossary.paper." + pageSize.name()),
                  null, true));
        }
        pageSizeControl.setSegments(sizes);
        pageSizeControl.setSelected(defaultPageSize());
        paper.add(leftAligned(pageSizeControl));
        layoutRow.add(paper, BorderLayout.WEST);

        JPanel include = column();
        include.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "export.glossary.include"))));
        include.add(Box.createVerticalStrut(scaleForGUI(3)));
        JPanel options = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(2)));
        options.add(contentsBox);
        options.add(bookmarksBox);
        options.add(linksBox);
        options.add(referencedByBox);
        include.add(leftAligned(options));
        layoutRow.add(include, BorderLayout.CENTER);
        body.add(leftAligned(layoutRow));
        body.add(Box.createVerticalStrut(scaleForGUI(12)));

        JPanel entriesHeading = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        entriesHeading.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "export.glossary.entries")), BorderLayout.WEST);
        JPanel links = Hud.transparentPanel(null);
        links.setLayout(new BoxLayout(links, BoxLayout.X_AXIS));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "export.glossary.selectAll"), () -> setAll(true)));
        links.add(Box.createHorizontalStrut(scaleForGUI(10)));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "export.glossary.selectNone"), () -> setAll(false)));
        entriesHeading.add(links, BorderLayout.EAST);
        body.add(leftAligned(entriesHeading));
        body.add(Box.createVerticalStrut(scaleForGUI(5)));

        JPanel list = column();
        list.setOpaque(true);
        list.setBackground(SURFACE_DEEP);
        list.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(6),
              scaleForGUI(8)));
        for (HudCheckBox checkBox : checkBoxes.values()) {
            list.add(leftAligned(checkBox));
            list.add(Box.createVerticalStrut(scaleForGUI(3)));
        }
        JPanel listHolder = Hud.transparentPanel(new BorderLayout());
        listHolder.add(list, BorderLayout.NORTH);
        entryScroll.setViewportView(listHolder);
        Hud.styleScroll(entryScroll, SURFACE_DEEP, true);
        entryScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(body, BorderLayout.NORTH);
        JPanel padded = Hud.transparentPanel(new BorderLayout());
        padded.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(14), scaleForGUI(14), scaleForGUI(14)));
        padded.add(entryScroll, BorderLayout.CENTER);
        holder.add(padded, BorderLayout.CENTER);
        return holder;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        footer.add(Hud.hint(getTextAt(RESOURCE_BUNDLE, "export.glossary.hint")), BorderLayout.CENTER);

        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "export.glossary.cancel")
                                               .toUpperCase(Locale.ROOT), false);
        cancel.addActionListener(event -> dispose());
        exportButton = new HudButton("", true);
        exportButton.addActionListener(event -> {
            confirmed = true;
            dispose();
        });

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(exportButton);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private static Segment<Scope> scopeSegment(Scope scope, String sub, boolean enabled) {
        return new Segment<>(scope, getTextAt(RESOURCE_BUNDLE, "export.glossary.scope." + scope.name()), sub, enabled);
    }

    private HudCheckBox optionBox(String key, boolean selected) {
        HudCheckBox checkBox = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, key));
        checkBox.setToolTipText(getTextAt(RESOURCE_BUNDLE, key + ".toolTipText"));
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /** Letter paper where it is the usual size, A4 everywhere else. */
    private static PageSize defaultPageSize() {
        String country = Locale.getDefault().getCountry();
        return Set.of("US", "CA", "MX", "PH").contains(country) ? PageSize.LETTER : PageSize.A4;
    }

    // endregion Layout

    // region Choices

    /** Ticks the entries in a scope and clears the rest. */
    private void applyScope(Scope scope) {
        Set<GlossaryEntry> inScope = new HashSet<>();
        switch (scope) {
            case ENTRY -> {
                if (currentEntry != null) {
                    inScope.add(currentEntry);
                }
            }
            case LIST -> inScope.addAll(listedEntries);
            case SINCE_LAST_CORE -> index.items().stream()
                                          .filter(item -> item.change().isChanged())
                                          .forEach(item -> inScope.add(item.entry()));
            case ALL -> inScope.addAll(checkBoxes.keySet());
        }
        checkBoxes.forEach((entry, checkBox) -> checkBox.setSelected(inScope.contains(entry)));
        // A single entry needs no contents page; anything longer is easier to find your way round with one.
        contentsBox.setSelected(inScope.size() > 1);
        if (exportButton != null) {
            refreshExportButton();
        }
        scrollToFirstTicked();
    }

    private void setAll(boolean selected) {
        checkBoxes.values().forEach(checkBox -> checkBox.setSelected(selected));
        refreshExportButton();
    }

    private void refreshExportButton() {
        int count = (int) checkBoxes.values().stream().filter(HudCheckBox::isSelected).count();
        exportButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "export.glossary.confirm", count)
                                   .toUpperCase(Locale.ROOT));
        exportButton.setArmed(count > 0);
    }

    private void scrollToFirstTicked() {
        SwingUtilities.invokeLater(() -> checkBoxes.values().stream()
                                               .filter(HudCheckBox::isSelected)
                                               .findFirst()
                                               .ifPresent(checkBox -> checkBox.scrollRectToVisible(
                                                     new Rectangle(0, 0, checkBox.getWidth(),
                                                           checkBox.getHeight()))));
    }

    // endregion Choices
}
