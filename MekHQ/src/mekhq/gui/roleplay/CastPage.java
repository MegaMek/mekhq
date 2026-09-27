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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.AskPage.column;
import static mekhq.gui.roleplay.AskPage.rightButtonRow;
import static mekhq.gui.roleplay.AskPage.scroll;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.JournalFilter;
import mekhq.campaign.roleplay.OracleCharacter;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudConfirmButton;

/**
 * The console's Cast page: the characters the Oracle can pick when a random event involves an NPC. The list can be
 * reordered by dragging; removed characters move to their own filter and can be restored. The dossier shows a linked
 * person's portrait and role, and every journal entry the character appears in.
 */
class CastPage implements ConsoleSection {
    private static final int PORTRAIT_SIZE = 64;

    private final OracleConsole console;
    private final JPanel root = new JPanel(new BorderLayout());

    private boolean showingRemoved;
    private final HudChip inCastChip;
    private final HudChip removedChip;
    private final JTextField addField = new JTextField();
    private final DefaultListModel<OracleCharacter> model = new DefaultListModel<>();
    private final JList<OracleCharacter> list = new JList<>(model);
    private final JLabel listHint;

    private final JPanel dossier = column();
    private final JLabel portrait = new JLabel();
    private final JLabel name = new JLabel();
    private final JLabel about = new JLabel();
    private final JPanel renameRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
    private final JTextField renameField = new JTextField();
    private final JPanel appearances = column();
    private final HudButton rename;
    private final HudConfirmButton remove;
    private final HudButton restore;
    private final JLabel empty = Hud.notice("");

    /** The selected id, or {@code null} if nothing is selected. */
    private UUID selectedId;

    CastPage(final OracleConsole console) {
        this.console = console;
        root.setOpaque(true);
        root.setBackground(GROUND);

        Hud.styleField(addField);
        addField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.add.toolTipText"));
        addField.addActionListener(event -> add());
        HudButton addButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.add")
                                                  .toUpperCase(Locale.ROOT), true, true);
        addButton.addActionListener(event -> add());
        JPanel addRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
        addRow.add(addField, BorderLayout.CENTER);
        addRow.add(addButton, BorderLayout.EAST);

        inCastChip = new HudChip(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.filter.active"), () -> setRemoved(false));
        removedChip = new HudChip(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.filter.removed"), () -> setRemoved(true));
        JPanel chips = Hud.transparentPanel(null);
        chips.setLayout(new BoxLayout(chips, BoxLayout.X_AXIS));
        chips.add(inCastChip);
        chips.add(Box.createHorizontalStrut(scaleForGUI(6)));
        chips.add(removedChip);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.setCellRenderer(new CastRenderer());
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && list.getSelectedValue() != null) {
                selectedId = list.getSelectedValue().getId();
                renameRow.setVisible(false);
                refreshDossier();
            }
        });
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ReorderHandler());
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, true);
        listHint = Hud.hint(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.hint"));

        JPanel top = column();
        top.add(leftAligned(addRow));
        top.add(Box.createVerticalStrut(scaleForGUI(8)));
        top.add(leftAligned(chips));
        top.add(Box.createVerticalStrut(scaleForGUI(8)));
        JPanel bottom = column();
        bottom.add(Box.createVerticalStrut(scaleForGUI(8)));
        bottom.add(leftAligned(wrappedHint(listHint)));

        JPanel board = new JPanel(new BorderLayout());
        board.setOpaque(true);
        board.setBackground(GROUND);
        board.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        board.setPreferredSize(new Dimension(scaleForGUI(340), 0));
        board.add(top, BorderLayout.NORTH);
        board.add(listScroll, BorderLayout.CENTER);
        board.add(bottom, BorderLayout.SOUTH);

        // Dossier
        portrait.setPreferredSize(new Dimension(scaleForGUI(PORTRAIT_SIZE), scaleForGUI(PORTRAIT_SIZE)));
        portrait.setHorizontalAlignment(SwingConstants.CENTER);
        portrait.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)));
        name.setForeground(ACCENT_BRIGHT);
        name.setFont(hudFont(Font.BOLD, 1.2f, 0.12f));
        about.setForeground(TEXT_MUTED);
        about.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        JPanel identity = column();
        identity.add(leftAligned(name));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(about));
        JPanel header = Hud.transparentPanel(new BorderLayout(scaleForGUI(14), 0));
        header.add(portrait, BorderLayout.WEST);
        header.add(identity, BorderLayout.CENTER);

        Hud.styleField(renameField);
        renameField.addActionListener(event -> finishRename(true));
        HudButton saveName = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.save").toUpperCase(Locale.ROOT),
              true, true);
        saveName.addActionListener(event -> finishRename(true));
        HudButton cancelName = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cancel")
                                                   .toUpperCase(Locale.ROOT), false, true);
        cancelName.addActionListener(event -> finishRename(false));
        renameRow.add(renameField, BorderLayout.CENTER);
        renameRow.add(rightButtonRow(null, cancelName, saveName), BorderLayout.EAST);
        renameRow.setVisible(false);

        rename = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.rename").toUpperCase(Locale.ROOT), false,
              true);
        rename.addActionListener(event -> startRename());
        remove = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.remove").toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.confirmRemove").toUpperCase(Locale.ROOT), true);
        remove.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.remove.toolTipText"));
        remove.addActionListener(event -> setActive(false));
        restore = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.restore").toUpperCase(Locale.ROOT),
              false, true);
        restore.addActionListener(event -> setActive(true));
        HudButton storyline = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.readStoryline")
                                                  .toUpperCase(Locale.ROOT), true);
        storyline.addActionListener(event -> console.readStoryline(null, selectedId));

        dossier.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        dossier.add(leftAligned(header));
        dossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        dossier.add(leftAligned(renameRow));
        dossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        dossier.add(leftAligned(rightButtonRow(null, rename, remove, restore, storyline)));
        dossier.add(Box.createVerticalStrut(scaleForGUI(14)));
        dossier.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.appearances"))));
        dossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        dossier.add(leftAligned(appearances));

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(true);
        right.setBackground(SURFACE_DEEP);
        right.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        right.add(scroll(dossier, SURFACE_DEEP), BorderLayout.CENTER);
        right.add(empty, BorderLayout.NORTH);

        root.add(board, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setRemoved(false);
    }

    private static JComponent wrappedHint(final JLabel hint) {
        hint.setText("<html><div style='width:" + scaleForGUI(230) + "px'>" + hint.getText() + "</div></html>");
        return hint;
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    /**
     * @return the id of the character on screen, or {@code null} if none is selected
     */
    @Nullable UUID getSelectedCharacterId() {
        return selectedId;
    }

    /**
     * Selects a character, switching to the removed list if that is where they are.
     *
     * @param characterId the character's id
     */
    void select(final UUID characterId) {
        selectedId = characterId;
        OracleCharacter character = console.roleplay().getCharacter(characterId);
        setRemoved(character != null && !character.isActive());
    }

    private void setRemoved(final boolean removed) {
        showingRemoved = removed;
        inCastChip.setActive(!removed);
        removedChip.setActive(removed);
        list.setDragEnabled(!removed);
        listHint.setVisible(!removed);
        refresh();
    }

    @Override
    public void refresh() {
        List<OracleCharacter> shown = console.roleplay().getCharacters().stream()
                                            .filter(character -> character.isActive() != showingRemoved).toList();
        model.clear();
        model.addAll(shown);
        OracleCharacter selected = console.roleplay().getCharacter(selectedId);
        if (selected == null || !shown.contains(selected)) {
            selected = shown.isEmpty() ? null : shown.get(0);
            selectedId = (selected == null) ? null : selected.getId();
        }
        if (selected != null) {
            list.setSelectedValue(selected, true);
        } else {
            list.clearSelection();
        }
        removedChip.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.filter.removedCount",
              console.roleplay().getCharacters().size() - console.roleplay().getActiveCharacters().size()));
        refreshDossier();
    }

    private List<JournalEntry> appearancesOf(final OracleCharacter character) {
        List<JournalEntry> entries = new ArrayList<>(console.roleplay().getTimeline(
              new JournalFilter(null, null, null, Set.of(), null, character.getId())));
        Collections.reverse(entries);
        return entries;
    }

    private final class CastRenderer implements ListCellRenderer<OracleCharacter> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends OracleCharacter> source,
              OracleCharacter character, int index, boolean isSelected, boolean cellHasFocus) {
            List<JournalEntry> seen = appearancesOf(character);
            String sub = seen.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.notSeen")
                               : getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.lastSeen",
                                     OracleConsole.formatDate(seen.get(0).getDate()));
            List<Tag> tags = character.isLinked()
                                   ? List.of(new Tag(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.linked"),
                                         ACCENT_BRIGHT)) : List.of();
            return card.show(character.isActive() ? ACCENT : TEXT_FAINT, false, character.getName(), null, tags, sub,
                  Integer.toString(seen.size()), getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.entries"),
                  isSelected);
        }
    }

    private void refreshDossier() {
        OracleCharacter character = console.roleplay().getCharacter(selectedId);
        dossier.setVisible(character != null);
        empty.setVisible(character == null);
        empty.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(60), scaleForGUI(20), 0, scaleForGUI(20)));
        empty.setText(getTextAt(RESOURCE_BUNDLE, showingRemoved ? "OracleConsole.cast.noneRemoved"
                                                       : "OracleConsole.cast.empty"));
        if (character == null) {
            return;
        }

        Person person = character.isLinked()
                              ? console.campaign().getPlayerForce().getHumanResources()
                                      .getPerson(character.getPersonId()) : null;
        ImageIcon icon = (person == null) ? null : person.getPortrait().getImageIcon(scaleForGUI(PORTRAIT_SIZE));
        portrait.setIcon(icon);
        portrait.setText(icon == null ? initials(character.getName()) : null);
        portrait.setForeground(ACCENT);
        portrait.setFont(hudFont(Font.BOLD, 1.3f, 0.06f));

        name.setText(character.getName().toUpperCase(Locale.ROOT));
        List<JournalEntry> seen = appearancesOf(character);
        String linked = (person != null) ? getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.about.linked",
              person.getPrimaryRoleDesc()) : character.isLinked()
                                                   ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.about.gone")
                                                   : getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.about.typed");
        about.setText(linked + "  ·  " + getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.about.count",
              seen.size()));

        rename.setArmed(!character.isLinked());
        rename.setToolTipText(character.isLinked() ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.rename.linked")
                                    : null);
        remove.setVisible(character.isActive());
        remove.reset();
        restore.setVisible(!character.isActive());

        appearances.removeAll();
        if (seen.isEmpty()) {
            appearances.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cast.noAppearances"))));
        }
        for (JournalEntry entry : seen) {
            appearances.add(leftAligned(JournalPage.entryCard(entry, false)));
        }
        dossier.revalidate();
        dossier.repaint();
    }

    private static String initials(final String name) {
        StringBuilder initials = new StringBuilder();
        for (String word : name.strip().split("\\s+")) {
            if (!word.isEmpty() && Character.isLetter(word.charAt(0)) && initials.length() < 2) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.toString();
    }

    private void add() {
        OracleCharacter character = console.roleplay().addCharacter(addField.getText());
        addField.setText("");
        if (character != null) {
            selectedId = character.getId();
            setRemoved(false);
            console.changed();
        }
    }

    private void setActive(final boolean active) {
        OracleCharacter character = console.roleplay().getCharacter(selectedId);
        if (character == null) {
            return;
        }
        if (active) {
            console.roleplay().restoreCharacter(character);
        } else {
            console.roleplay().removeCharacter(character);
            selectedId = null;
        }
        console.changed();
    }

    private void startRename() {
        OracleCharacter character = console.roleplay().getCharacter(selectedId);
        if (character == null || character.isLinked()) {
            return;
        }
        renameField.setText(character.getName());
        renameRow.setVisible(true);
        renameField.requestFocusInWindow();
        renameField.selectAll();
        dossier.revalidate();
    }

    private void finishRename(final boolean save) {
        OracleCharacter character = console.roleplay().getCharacter(selectedId);
        if (save && character != null) {
            console.roleplay().renameCharacter(character, renameField.getText());
        }
        renameRow.setVisible(false);
        console.changed();
    }

    /** Moves a character to where it is dropped in the list. */
    private final class ReorderHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            OracleCharacter character = list.getSelectedValue();
            return (character == null) ? null : new StringSelection(character.getId().toString());
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return !showingRemoved && support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                UUID id = UUID.fromString((String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                OracleCharacter character = console.roleplay().getCharacter(id);
                int dropIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
                int currentIndex = console.roleplay().getActiveCharacters().indexOf(character);
                if (character == null || currentIndex < 0) {
                    return false;
                }
                // Removing the character first shifts everything after it up by one.
                console.roleplay().moveCharacter(character, dropIndex > currentIndex ? dropIndex - 1 : dropIndex);
                selectedId = id;
                console.changed();
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
}
