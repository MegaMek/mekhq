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

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ObfuscatableIntel;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A single selectable "job board" card summarizing one {@link AbstractContract}.
 *
 * <p>The card is intentionally scannable: an employer-colored band, an emblem, the contract and employer name, a
 * threat rating, and the three figures that drive the decision (total pay, destination, term length). Clicking it
 * notifies the owning dialog, which then repopulates the full {@link ContractDossierPanel}.</p>
 *
 * <p>The card sizes itself to its content so a long, wrapping contract name is never clipped. Because names vary in
 * length, the owning dialog equalizes every card in a row to a common height via {@link #setForcedHeight(int)}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractCardPanel extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    /** Matches RoundedLineBorder's own subtle padding so the selected and unselected borders reserve equal insets. */
    private static final int SUBTLE_BORDER_PADDING = 4;

    private static final int BAND_HEIGHT = scaleForGUI(6);
    private static final int EMBLEM_SIZE = scaleForGUI(34);
    static final int CARD_WIDTH = scaleForGUI(280);

    /** Character budgets for wordWrap, calibrated to the title area at CARD_WIDTH. */
    private static final int NAME_MAX_CHARS = 21;
    private static final int SUBTITLE_MAX_CHARS = 28;

    private final transient AbstractContract contract;
    private final Color accent;
    private boolean selected;
    private int forcedHeight = -1;
    private transient JLabel titlesLabel;

    /**
     * Creates a card for a single contract offer.
     *
     * @param contract    the offer to summarize
     * @param currentDate the campaign date, used to resolve time-sensitive planet names
     * @param onSelected  callback invoked with this card's contract when the card is clicked
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractCardPanel(AbstractContract contract, LocalDate currentDate,
          Consumer<AbstractContract> onSelected) {
        this.contract = contract;
        this.accent = contract.getEmployerColor().getColour();

        setLayout(new BorderLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setSelected(false);

        add(buildColorBand(), BorderLayout.NORTH);
        add(buildContent(currentDate), BorderLayout.CENTER);

        // Swing does not bubble mouse events to ancestors, so a click landing on a child label would otherwise be
        // swallowed. Install the same handler on the card and every descendant, so the whole card is clickable.
        MouseListener clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSelected.accept(contract);
            }
        };
        installClickListener(this, clickListener);
    }

    private static void installClickListener(Component component, MouseListener listener) {
        component.addMouseListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installClickListener(child, listener);
            }
        }
    }

    public AbstractContract getContract() {
        return contract;
    }

    /**
     * Updates the card's border to reflect whether it is the currently selected offer.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            // Wrap in the same 4px padding the subtle border uses so both states reserve identical insets, keeping the
            // title area the same width whether the card is selected.
            setBorder(BorderFactory.createCompoundBorder(
                  new RoundedLineBorder(accent, scaleForGUI(2), scaleForGUI(16)),
                  BorderFactory.createEmptyBorder(SUBTLE_BORDER_PADDING, SUBTLE_BORDER_PADDING,
                        SUBTLE_BORDER_PADDING, SUBTLE_BORDER_PADDING)));
        } else {
            setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
        }
    }

    private void applyTitleText() {
        // The name reveals the enemy, so a hidden opposition blanks the card title too, matching the dossier.
        String rawName = contract.isIntelObfuscated(ObfuscatableIntel.OPPOSITION)
                               ? getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.title.obfuscated")
                               : contract.getName().replace('_', ' ');
        String name = wrapInner(escape(rawName), NAME_MAX_CHARS);
        String subtitle = wrapInner(escape(contract.getEmployerMarketDisplayName()) + " &middot; "
                                          + escape(contract.getObjectiveType().toString()), SUBTITLE_MAX_CHARS);
        titlesLabel.setText("<html><b>" + name + "</b><br>"
                                  + "<span style='font-size:smaller'>" + subtitle + "</span></html>");
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Forces this card to a fixed height so every card in a row shares a common height. Pass a non-positive value to
     * revert to the card's natural, content-driven height.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setForcedHeight(int forcedHeight) {
        this.forcedHeight = forcedHeight;
        revalidate();
    }

    private JPanel buildColorBand() {
        JPanel band = new JPanel();
        band.setBackground(accent);
        band.setPreferredSize(new Dimension(1, BAND_HEIGHT));
        return band;
    }

    private JPanel buildContent(LocalDate currentDate) {
        JPanel content = new JPanel(new BorderLayout(0, scaleForGUI(4)));
        content.setOpaque(false);
        final int pad = scaleForGUI(8);
        content.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        content.add(buildTitleRow(currentDate), BorderLayout.CENTER);
        content.add(buildChipsRow(currentDate), BorderLayout.SOUTH);

        return content;
    }

    private JPanel buildTitleRow(LocalDate currentDate) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(false);

        // Left column: difficulty skulls stacked directly above the faction emblem, so the skulls sit in line with the
        // title's first line and there is no separate skull band leaving dead space at the top.
        JComponent skulls = ContractMarketFormatting.difficultySkulls(contract, scaleForGUI(14));
        skulls.setAlignmentX(Component.LEFT_ALIGNMENT);
        skulls.setMaximumSize(skulls.getPreferredSize());

        JComponent emblem = buildEmblemTile(currentDate);
        emblem.setAlignmentX(Component.LEFT_ALIGNMENT);
        emblem.setMaximumSize(emblem.getPreferredSize());

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(skulls);
        leftColumn.add(Box.createVerticalStrut(scaleForGUI(4)));
        leftColumn.add(emblem);

        JPanel westWrapper = new JPanel(new BorderLayout());
        westWrapper.setOpaque(false);
        westWrapper.add(leftColumn, BorderLayout.NORTH);
        row.add(westWrapper, BorderLayout.WEST);

        titlesLabel = new JLabel();
        titlesLabel.setVerticalAlignment(SwingConstants.TOP);
        applyTitleText();
        row.add(titlesLabel, BorderLayout.CENTER);

        return row;
    }

    /**
     * Wraps text with explicit line breaks by character count (via {@code WrapLayout.wordWrap}) rather than a CSS pixel
     * width, which Swing's HTML renderer honors reliably without clipping. The {@code <html>} wrapper and any trailing
     * break are stripped so the result can be embedded in a larger HTML label.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String wrapInner(String text, int maximumCharacters) {
        String wrapped = wordWrap(text, maximumCharacters).replace("<html>", "").replace("</html>", "");
        while (wrapped.endsWith("<br>")) {
            wrapped = wrapped.substring(0, wrapped.length() - "<br>".length());
        }
        return wrapped;
    }

    /**
     * Builds the faction-logo emblem on a rounded tile painted in the employer's accent colour (the same purple the
     * card band and selected border use), pinned to the top so it aligns with the first line of the title.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JComponent buildEmblemTile(LocalDate currentDate) {
        JLabel emblem = new JLabel(Factions.getFactionLogoWithScaling(currentDate.getYear(),
              contract.getEmployerFactionCode(), EMBLEM_SIZE));
        emblem.setOpaque(false);

        final int arc = scaleForGUI(10);
        final int pad = scaleForGUI(4);
        JPanel tile = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(accent);
                graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                graphics2D.dispose();
                super.paintComponent(graphics);
            }
        };
        tile.setOpaque(false);
        tile.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        tile.add(emblem, BorderLayout.CENTER);
        return tile;
    }

    private JPanel buildChipsRow(LocalDate currentDate) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, dividerColor()));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(scaleForGUI(6), 0, 0, scaleForGUI(12));

        gbc.gridx = 0;
        row.add(chip(contract.getTotalPay().toAmountAndSymbolString()), gbc);

        gbc.gridx = 1;
        String planet = contract.getTargetPlanetName(currentDate);
        row.add(chip(planet == null ? contract.getTargetSystemName(currentDate) : planet), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        row.add(chip(getFormattedTextAt(RESOURCE_BUNDLE,
              "card.contractMarket.months",
              contract.getLengthInMonths())), gbc);

        return row;
    }

    private JLabel chip(String value) {
        JLabel label = new JLabel(value);
        label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() - 1f));
        return label;
    }

    private Color dividerColor() {
        Color base = getBackground();
        return base == null ? Color.GRAY : base.darker();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        return new Dimension(CARD_WIDTH, forcedHeight > 0 ? forcedHeight : natural.height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
