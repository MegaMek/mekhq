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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.alliedCommand;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.assessmentDifficulty;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.assessmentLabel;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.salvage;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.support;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.threat;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.transport;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

import jakarta.annotation.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ObfuscatableIntel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.gui.view.PlanetViewPanel;
import org.apache.commons.lang3.StringUtils;

/**
 * The full "briefing dossier" for a single selected {@link AbstractContract}.
 *
 * <p>This is the commitment view: the employer's contact gets a face and a name, the flavor description reads as an
 * in-universe quote, and the terms and intel sit in two aligned columns so the trade-off is legible before the player
 * signs. The Accept button forwards the offer to the owning dialog.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractDossierPanel extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int PORTRAIT_SIZE = scaleForGUI(48);
    private static final Dimension MINIMUM_SIZE = scaleForGUI(420, 400);

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final LocalDate currentDate;
    private final Color accent;

    /**
     * Builds a dossier for the given offer.
     *
     * @param campaign    the active campaign, used to estimate transit and running costs
     * @param contract    the selected offer
     * @param currentDate campaign date, for time-sensitive planet names
     * @param actions     the per-offer actions the footer buttons invoke
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractDossierPanel(Campaign campaign, AbstractContract contract, LocalDate currentDate,
          ContractMarketActions actions) {
        this.campaign = campaign;
        this.contract = contract;
        this.currentDate = currentDate;
        this.accent = contract.getEmployerColor().getColour();

        setLayout(new BorderLayout());
        setMinimumSize(MINIMUM_SIZE);
        setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(actions), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        final int pad = scaleForGUI(12);
        header.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        Color onAccent = contrastingText(accent);
        // The title names the enemy, so when the opposition is hidden the title reads as unknown too.
        boolean oppositionHidden = contract.isIntelObfuscated(ObfuscatableIntel.OPPOSITION);
        String titleText = oppositionHidden
                                 ? getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.title.obfuscated")
                                 : contract.getName();
        JLabel title = new JLabel("<html><span style='font-size:smaller'>"
                                        +
                                        getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.eyebrow").toUpperCase() +
                                        "</span><br>"
                                        +
                                        "<b style='font-size:larger'>" +
                                        escape(titleText) +
                                        "</b></html>");
        title.setForeground(onAccent);
        header.add(title, BorderLayout.WEST);

        JPanel badges = new JPanel();
        badges.setLayout(new BoxLayout(badges, BoxLayout.Y_AXIS));
        badges.setOpaque(false);

        // A batchall badge would reveal the enemy is a Clan, so suppress it while the opposition is hidden.
        Faction enemyFaction = contract.getEnemyFaction();
        boolean hasBatchall = !oppositionHidden && (enemyFaction != null) && enemyFaction.isClan();
        if (hasBatchall) {
            badges.add(headerBadge(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.batchall"), onAccent));
        }
        if (contract.isProvingGround()) {
            if (hasBatchall) {
                badges.add(Box.createVerticalStrut(scaleForGUI(4)));
            }
            badges.add(headerBadge(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.provingGround"), onAccent));
        }

        if (badges.getComponentCount() > 0) {
            JPanel badgeWrapper = new JPanel(new BorderLayout());
            badgeWrapper.setOpaque(false);
            badgeWrapper.add(badges, BorderLayout.NORTH);
            header.add(badgeWrapper, BorderLayout.EAST);
        }

        return header;
    }

    /** A small pill-shaped header badge (e.g. Batchall, Proving Ground) drawn in the accent-contrasting colour. */
    private JLabel headerBadge(String text, Color onAccent) {
        JLabel badge = new JLabel(text);
        badge.setForeground(onAccent);
        badge.setAlignmentX(Component.RIGHT_ALIGNMENT);
        badge.setBorder(BorderFactory.createCompoundBorder(
              new RoundedLineBorder(onAccent, scaleForGUI(1), scaleForGUI(16)),
              BorderFactory.createEmptyBorder(scaleForGUI(2), scaleForGUI(8), scaleForGUI(2), scaleForGUI(8))));
        return badge;
    }

    private JPanel buildBody(ContractMarketActions actions) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        final int pad = scaleForGUI(12);
        body.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        body.add(leftAligned(buildContactRow()));
        body.add(Box.createVerticalStrut(pad));
        body.add(buildTabs());
        body.add(Box.createVerticalStrut(pad));
        body.add(buildFooter(actions));

        return body;
    }

    /**
     * Builds the tabbed area: a Details tab with the terms, intel, pay, and profit estimate, and a Target planet tab
     * showing the destination system.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JTabbedPane buildTabs() {
        final int pad = scaleForGUI(12);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        JPanel briefing = buildBriefing();
        if (briefing != null) {
            details.add(briefing);
            details.add(Box.createVerticalStrut(pad));
        }
        details.add(buildColumns());
        details.add(Box.createVerticalStrut(pad));
        details.add(buildMetrics());
        details.add(Box.createVerticalStrut(pad));
        details.add(buildProfitEstimate());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "tab.contractMarket.details"), details);
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "tab.contractMarket.targetPlanet"),
              buildTargetPlanetTab(details.getPreferredSize()));
        return tabs;
    }

    private JScrollPane buildTargetPlanetTab(Dimension preferredSize) {
        Component view;
        PlanetarySystem system = contract.getTargetSystem();
        if (system == null) {
            JLabel empty = new JLabel(getTextAt(RESOURCE_BUNDLE, "tab.contractMarket.targetPlanet.none"),
                  SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(24), scaleForGUI(12),
                  scaleForGUI(24), scaleForGUI(12)));
            view = empty;
        } else {
            Planet planet = contract.getTargetPlanet();
            Integer position = planet == null ? null : planet.getSystemPosition();
            view = new PlanetViewPanel(system, campaign, position == null ? 0 : position);
        }

        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.setPreferredSize(preferredSize);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        return scroll;
    }

    /**
     * Lays the contact block (portrait and name) on the left and the contact's spoken pitch on the right, on one row.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildContactRow() {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(16), 0));

        // Pin the contact block to the top so its portrait lines up with the first line of the speech.
        JPanel contactWrapper = new JPanel(new BorderLayout());
        contactWrapper.add(buildNegotiatorRow(), BorderLayout.NORTH);
        row.add(contactWrapper, BorderLayout.WEST);

        row.add(buildQuote(), BorderLayout.CENTER);
        return row;
    }

    private JTextArea buildQuote() {
        // A JTextArea wraps to whatever width the CENTER region gives it, so the pitch expands to fill the space to the
        // right of the contact block instead of being pinned to a fixed width on the left.
        JTextArea quote = new JTextArea(ContractContactStatement.forContract(campaign, contract));
        quote.setEditable(false);
        quote.setFocusable(false);
        quote.setLineWrap(true);
        quote.setWrapStyleWord(true);
        quote.setOpaque(false);
        Font base = new JLabel().getFont();
        quote.setFont(base.deriveFont(Font.ITALIC));
        quote.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, accent),
              BorderFactory.createEmptyBorder(0, scaleForGUI(10), 0, 0)));
        return quote;
    }

    private JPanel buildNegotiatorRow() {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));

        Person negotiator = contract.getEmployerNegotiator();

        JLabel portrait = new JLabel();
        if (negotiator != null) {
            ImageIcon icon = negotiator.getPortraitImageIconWithFallback(true);
            if (icon != null) {
                Image scaled = icon.getImage().getScaledInstance(PORTRAIT_SIZE, PORTRAIT_SIZE, Image.SCALE_SMOOTH);
                portrait.setIcon(new ImageIcon(scaled));
            }
        }
        portrait.setPreferredSize(new Dimension(PORTRAIT_SIZE, PORTRAIT_SIZE));
        row.add(portrait, BorderLayout.WEST);

        String name = negotiator == null ? contract.getEmployerMarketDisplayName() : negotiator.getFullTitle();
        JLabel details = new JLabel("<html><b>" +
                                          escape(name) +
                                          "</b><br>"
                                          +
                                          "<span style='font-size:smaller'>" +
                                          escape(contract.getEmployerMarketDisplayName()) +
                                          "</span></html>");
        row.add(details, BorderLayout.CENTER);

        return row;
    }

    private JPanel buildColumns() {
        JPanel columns = new JPanel(new GridLayout(1, 2, scaleForGUI(16), 0));

        JPanel terms = section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.terms"));
        addDetailRow(terms,
              "dossier.contractMarket.terms.command",
              contract.getCommandRights().toString(),
              null);
        addDetailRow(terms, "dossier.contractMarket.terms.salvage", salvage(contract), null);
        addDetailRow(terms, "dossier.contractMarket.terms.support", support(contract), null);
        addDetailRow(terms, "dossier.contractMarket.terms.transport", transport(contract), null);
        columns.add(terms);

        JPanel intel = section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.intel"));
        addDetailRow(intel, "dossier.contractMarket.intel.alliedCommand",
              intelValue(ObfuscatableIntel.ALLIED_COMMAND, alliedCommand(contract)), null);
        addDetailRow(intel,
              "dossier.contractMarket.intel.opposition",
              intelValue(ObfuscatableIntel.OPPOSITION,
                    contract.getEnemyDisplayNameIncludingFaction(currentDate.getYear())),
              null);
        addDetailRow(intel, "dossier.contractMarket.intel.threat",
              intelValue(ObfuscatableIntel.THREAT, threat(contract)), null);
        addDetailRow(intel, "dossier.contractMarket.intel.morale",
              intelValue(ObfuscatableIntel.MORALE, contract.getMoraleLevel().toString()), null);
        // Assessment is never obfuscated.
        int difficulty = assessmentDifficulty(contract);
        Color assessmentColor = difficulty >= 8 ? dangerColor() : (difficulty <= 2 ? positiveColor() : null);
        addDetailRow(intel, "dossier.contractMarket.intel.assessment", assessmentLabel(contract), assessmentColor);
        columns.add(intel);

        return columns;
    }

    /**
     * Builds the briefing card holding the contract's (Markdown-rendered) description, or {@code null} when the
     * contract has no description so no empty card is shown.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable JPanel buildBriefing() {
        String description = contract.getDescription();
        if (StringUtils.isBlank(description)) {
            return null;
        }

        final int pad = scaleForGUI(10);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(pad, pad, pad, pad)));

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.briefing").toUpperCase());
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() - 2f));
        title.setForeground(mutedColor());
        panel.add(title, BorderLayout.NORTH);

        JTextPane descriptionPane = new JTextPane();
        descriptionPane.setEditable(false);
        descriptionPane.setFocusable(false);
        descriptionPane.setOpaque(false);
        descriptionPane.setContentType("text/html");
        descriptionPane.setText(MarkdownRenderer.getRenderedHtml(description));
        descriptionPane.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), 0, 0, 0));
        panel.add(descriptionPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel section(String heading) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel title = new JLabel(heading.toUpperCase());
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() - 2f));
        title.setForeground(mutedColor());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, scaleForGUI(4), 0);
        panel.add(title, gbc);
        return panel;
    }

    /**
     * The intel value to display: the real {@code value}, or a "No Intel" placeholder when the contract hides that
     * field from the player in the market.
     */
    private String intelValue(ObfuscatableIntel field, String value) {
        return contract.isIntelObfuscated(field)
                     ? getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.intel.obfuscated")
                     : value;
    }

    private void addDetailRow(JPanel section, String labelKey, String value, Color valueColor) {
        int row = section.getComponentCount();

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(scaleForGUI(3), 0, scaleForGUI(3), scaleForGUI(12));
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        label.setForeground(mutedColor());
        section.add(label, labelGbc);

        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.gridy = row;
        valueGbc.weightx = 1.0;
        valueGbc.anchor = GridBagConstraints.EAST;
        valueGbc.fill = GridBagConstraints.HORIZONTAL;
        valueGbc.insets = new Insets(scaleForGUI(3), 0, scaleForGUI(3), 0);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        if (valueColor != null) {
            valueLabel.setForeground(valueColor);
        }
        section.add(valueLabel, valueGbc);
    }

    private JPanel buildMetrics() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, scaleForGUI(10), 0));
        metrics.add(metricTile("dossier.contractMarket.metric.total",
              contract.getTotalPay().toAmountAndSymbolString()));
        metrics.add(metricTile("dossier.contractMarket.metric.monthly",
              contract.getMonthlyPayOut().toAmountAndSymbolString()));
        metrics.add(metricTile("dossier.contractMarket.metric.transport",
              contract.getTransportPayment().toAmountAndSymbolString()));
        metrics.add(metricTile("dossier.contractMarket.metric.combat",
              contract.getContractFinanceData().combatPay().toAmountAndSymbolString()));
        return metrics;
    }

    private JPanel buildProfitEstimate() {
        ContractProfitEstimate estimate = ContractProfitEstimate.estimate(campaign, contract);
        final int pad = scaleForGUI(10);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(pad, pad, pad, pad)));

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.title").toUpperCase());
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() - 2f));
        title.setForeground(mutedColor());

        GridBagConstraints titleGbc = new GridBagConstraints();
        titleGbc.gridx = 0;
        titleGbc.gridy = 0;
        titleGbc.gridwidth = 2;
        titleGbc.anchor = GridBagConstraints.WEST;
        titleGbc.insets = new Insets(0, 0, scaleForGUI(6), 0);
        panel.add(title, titleGbc);

        addProfitRow(panel, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.income"),
              "+" + estimate.guaranteedIncome().toAmountAndSymbolString(), positiveColor(), false);
        addProfitRow(panel, getFormattedTextAt(RESOURCE_BUNDLE,
                    "dossier.contractMarket.profit.expenses",
                    contract.getLengthInMonths(),
                    estimate.transitDays()),
              "-" + estimate.operatingCosts().toAmountAndSymbolString(), dangerColor(), false);
        addProfitRow(panel, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.net"),
              estimate.estimatedNet().toAmountAndSymbolString(),
              estimate.estimatedNet().isPositive() ? positiveColor() : dangerColor(), true);

        // A JTextArea wraps to whatever width the layout gives it, so the caveat spans the full panel width instead of
        // being confined to a fixed HTML body width like a JLabel would be.
        JTextArea caveat = new JTextArea(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.caveat"));
        caveat.setEditable(false);
        caveat.setFocusable(false);
        caveat.setLineWrap(true);
        caveat.setWrapStyleWord(true);
        caveat.setOpaque(false);
        caveat.setBorder(null);
        caveat.setForeground(mutedColor());
        Font caveatFont = new JLabel().getFont();
        caveat.setFont(caveatFont.deriveFont(caveatFont.getSize2D() - 2f));

        GridBagConstraints caveatGbc = new GridBagConstraints();
        caveatGbc.gridx = 0;
        caveatGbc.gridy = GridBagConstraints.RELATIVE;
        caveatGbc.gridwidth = 2;
        caveatGbc.weightx = 1.0;
        caveatGbc.fill = GridBagConstraints.HORIZONTAL;
        caveatGbc.anchor = GridBagConstraints.WEST;
        caveatGbc.insets = new Insets(scaleForGUI(6), 0, 0, 0);
        panel.add(caveat, caveatGbc);

        return panel;
    }

    private void addProfitRow(JPanel panel, String label, String value, Color valueColor, boolean emphasize) {
        int row = panel.getComponentCount();

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(scaleForGUI(3), 0, scaleForGUI(3), scaleForGUI(24));
        JLabel labelComponent = new JLabel(label);
        if (emphasize) {
            labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD));
        } else {
            labelComponent.setForeground(mutedColor());
        }
        panel.add(labelComponent, labelGbc);

        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.gridy = row;
        valueGbc.weightx = 1.0;
        valueGbc.anchor = GridBagConstraints.EAST;
        valueGbc.fill = GridBagConstraints.HORIZONTAL;
        valueGbc.insets = new Insets(scaleForGUI(3), 0, scaleForGUI(3), 0);
        JLabel valueComponent = new JLabel(value);
        valueComponent.setHorizontalAlignment(SwingConstants.RIGHT);
        valueComponent.setForeground(valueColor);
        if (emphasize) {
            valueComponent.setFont(valueComponent.getFont().deriveFont(Font.BOLD, valueComponent.getFont().getSize2D()
                                                                                        + 1f));
        }
        panel.add(valueComponent, valueGbc);
    }

    private JPanel metricTile(String labelKey, String value) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(8), scaleForGUI(10))));

        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        label.setForeground(mutedColor());
        label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() - 1f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, valueLabel.getFont().getSize2D() + 3f));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        tile.add(label);
        tile.add(valueLabel);
        return tile;
    }

    private JPanel buildFooter(ContractMarketActions actions) {
        JPanel footer = new JPanel(new BorderLayout());

        // Render the destination as the system's report hyperlink so clicking it focuses the interstellar map.
        PlanetarySystem targetSystem = contract.getTargetSystem();
        String destination;
        if (targetSystem != null) {
            destination = targetSystem.getHyperlinkedName(currentDate);
        } else {
            destination = contract.getTargetPlanetName(currentDate);
            if (destination == null) {
                destination = contract.getTargetSystemName(currentDate);
            }
        }
        String travelText = getFormattedTextAt(RESOURCE_BUNDLE,
              "dossier.contractMarket.footer.travel",
              destination,
              contract.getLengthInMonths());

        JEditorPane travel = new JEditorPane("text/html",
              "<html><body style='margin:0'>" + travelText + "</body></html>");
        travel.setEditable(false);
        travel.setOpaque(false);
        travel.setBorder(null);
        travel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        travel.setFont(new JLabel().getFont());
        travel.setForeground(mutedColor());
        travel.addHyperlinkListener(campaign.getGUI().getReportHLL());
        footer.add(travel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(6), 0));

        JButton negotiate = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate"));
        negotiate.addActionListener(e -> actions.negotiate(contract));
        buttons.add(negotiate);

        JButton delete = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.delete"));
        delete.addActionListener(e -> actions.delete(contract));
        buttons.add(delete);

        JButton edit = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit"));
        edit.addActionListener(e -> actions.edit(contract));
        edit.setEnabled(campaign.isGM());
        buttons.add(edit);

        JButton accept = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.accept"));
        accept.addActionListener(e -> actions.accept(contract));
        buttons.add(accept);

        footer.add(buttons, BorderLayout.EAST);

        return footer;
    }

    private JPanel leftAligned(Component component) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        wrapper.add(component, BorderLayout.CENTER);
        return wrapper;
    }

    private Color mutedColor() {
        Color base = getForeground();
        Color background = getBackground();
        if (base == null || background == null) {
            return Color.GRAY;
        }
        return blend(base, background, 0.45f);
    }

    private static Color blend(Color foreground, Color background, float backgroundWeight) {
        float fw = 1f - backgroundWeight;
        return new Color(
              Math.round(foreground.getRed() * fw + background.getRed() * backgroundWeight),
              Math.round(foreground.getGreen() * fw + background.getGreen() * backgroundWeight),
              Math.round(foreground.getBlue() * fw + background.getBlue() * backgroundWeight));
    }

    private static Color dangerColor() {
        return decodeHex(MekHQ.getMHQOptions().getFontColorNegativeHexColor(), Color.RED);
    }

    private static Color positiveColor() {
        return decodeHex(MekHQ.getMHQOptions().getFontColorPositiveHexColor(), new Color(0, 128, 0));
    }

    private static Color decodeHex(String hex, Color fallback) {
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static Color contrastingText(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen()
                                  + 0.114 * background.getBlue()) / 255.0;
        return luminance > 0.6 ? Color.BLACK : Color.WHITE;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
