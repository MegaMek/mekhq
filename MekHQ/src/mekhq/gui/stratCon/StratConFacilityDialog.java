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
package mekhq.gui.stratCon;

import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.handleImmersiveHyperlinkClick;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.*;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAdvisor.OrderOption;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * Shows everything about one StratCon facility in one place: who holds it and its state, as far as the player knows
 * it; what it does; what is happening there and what the player should do next; and every order that makes sense
 * there. An order that can be given has a button; one that cannot says why, and what to do about it.
 *
 * <p>It also serves an empty hex a formation stands on, offering Build and Interdict Supply.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";
    private static final String STRATCON_BUNDLE = "mekhq.resources.AtBStratCon";

    private final Campaign campaign;
    private final AbstractContract contract;
    private final StratConTrackState track;
    private final StratConCoords coords;
    private final Runnable onChange;
    private final JPanel content = new WidthTrackingPanel();

    /**
     * Opens the dialog and waits until the player closes it.
     *
     * @param owner    the window to centre on
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param coords   the hex
     * @param onChange run after each order the player gives here, so the map can refresh
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityDialog(@Nullable Frame owner, Campaign campaign, AbstractContract contract,
          StratConTrackState track, StratConCoords coords, Runnable onChange) {
        super(owner, true);
        this.campaign = campaign;
        this.contract = contract;
        this.track = track;
        this.coords = coords;
        this.onChange = onChange;

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8)));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(UIUtil.scaleForGUI(16));

        RoundedJButton closeButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "dialog.close"));
        closeButton.addActionListener(evt -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        rebuild();
        setPreferredSize(new Dimension(UIUtil.scaleForGUI(620), UIUtil.scaleForGUI(700)));
        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void rebuild() {
        content.removeAll();

        StratConFacility facility = track.getFacility(coords);
        setTitle((facility == null) ?
                       getTextAt(RESOURCE_BUNDLE, "dialog.title.hex") :
                       getFormattedTextAt(RESOURCE_BUNDLE, "dialog.title", facility.getDisplayableName()));

        content.add(createHtmlPane(buildInformation(facility)));
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));
        content.add(createOrdersPanel());

        JComponent underwayPanel = createUnderwayPanel();
        if (underwayPanel != null) {
            content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));
            content.add(underwayPanel);
        }

        content.revalidate();
        content.repaint();
    }

    private String buildInformation(@Nullable StratConFacility facility) {
        StringBuilder html = new StringBuilder("<html><body>");
        if (facility != null) {
            appendFacilityInformation(html, facility);
        }

        List<String> hints = StratConFacilityAdvisor.getHints(campaign, contract, track, coords);
        if (!hints.isEmpty()) {
            html.append("<h3>").append(getTextAt(RESOURCE_BUNDLE, "dialog.nextSteps")).append("</h3><ul>");
            for (String hint : hints) {
                html.append("<li>").append(hint).append("</li>");
            }
            html.append("</ul>");
        }

        html.append("<p>").append(getTextAt(RESOURCE_BUNDLE, "dialog.glossary")).append("</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private void appendFacilityInformation(StringBuilder html, StratConFacility facility) {
        String color = facility.isOwnerAlliedToPlayer() ? getPositiveColor() : getNegativeColor();
        html.append("<h2><span style='color:").append(color).append("'>")
              .append(facility.getDisplayableName())
              .append("</span></h2>");
        html.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "dialog.heldBy",
              getTextAt(RESOURCE_BUNDLE, "dialog.owner." + getOwnerKey(facility.getOwner()))));
        if (facility.isStrategicObjective()) {
            html.append("<br>").append(getTextAt(RESOURCE_BUNDLE, "dialog.strategicObjective"));
        }

        FacilityIntel intel = facility.getIntel();
        if (!facility.isOwnerAlliedToPlayer()) {
            html.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "dialog.intel",
                  getTextAt(STRATCON_BUNDLE, "stratConTab.facilityIntel." + intel.name())));
        }
        if (intel.isAtLeast(FacilityIntel.SCOUTED)) {
            html.append(getFormattedTextAt(STRATCON_BUNDLE,
                  "stratConTab.hexInfo.facilityStatus",
                  getTextAt(STRATCON_BUNDLE, "stratConTab.facilityTier." + facility.getTier().name()),
                  getTextAt(STRATCON_BUNDLE, "stratConTab.facilityCondition." + facility.getCondition().name())));
            html.append(intel.isAtLeast(FacilityIntel.DETAILED) ?
                              getFormattedTextAt(STRATCON_BUNDLE,
                                    "stratConTab.hexInfo.facilityGarrison",
                                    facility.getGarrison(),
                                    facility.getGarrisonMaximum()) :
                              getTextAt(STRATCON_BUNDLE, "stratConTab.hexInfo.facilityGarrisonUnknown"));
        } else {
            html.append(getTextAt(STRATCON_BUNDLE, "stratConTab.hexInfo.facilityUnscouted"));
        }

        if (StratConFacilitySupply.isSupplyLinesActive(campaign)) {
            html.append(getTextAt(STRATCON_BUNDLE,
                  StratConFacilitySupply.isCutOff(track, coords) ?
                        "stratConTab.hexInfo.supplyCut" :
                        "stratConTab.hexInfo.supplyConnected"));
        }
        int besiegerCount = StratConFacilitySiege.getSieges(track, coords).size();
        if (besiegerCount > 0) {
            html.append(getFormattedTextAt(STRATCON_BUNDLE, "stratConTab.hexInfo.besieged", besiegerCount));
        }
        List<StratConFacility> partners = StratConFacilitySynergies.getPartners(track, coords);
        if (!partners.isEmpty()) {
            List<String> partnerNames = new ArrayList<>();
            for (StratConFacility partner : partners) {
                partnerNames.add(partner.getDisplayableName());
            }
            html.append(getFormattedTextAt(STRATCON_BUNDLE,
                  "stratConTab.hexInfo.synergy",
                  String.join(", ", partnerNames)));
        }

        appendEffects(html, facility);
    }

    /**
     * What the facility does for whoever holds it now, and, for an enemy facility, what it would do for the player if
     * they took and held it.
     */
    private void appendEffects(StringBuilder html, StratConFacility facility) {
        String currentDescription = facility.getUserDescription();
        if (currentDescription != null) {
            html.append("<h3>").append(getTextAt(RESOURCE_BUNDLE, "dialog.effects")).append("</h3>")
                  .append("<p>").append(currentDescription).append("</p>");
        }

        if (facility.isOwnerAlliedToPlayer() || facility.isDefinitionMissing()) {
            return;
        }
        StratConFacilityDefinition definition = facility.getDefinition();
        if (!definition.hasProfileFor(ForceAlignment.Player)) {
            return;
        }
        StratConFacilityProfile capturedProfile = definition.getProfileFor(ForceAlignment.Player);
        if (capturedProfile.getDescription() != null) {
            html.append("<h3>").append(getTextAt(RESOURCE_BUNDLE, "dialog.effectsIfHeld")).append("</h3>")
                  .append("<p>").append(capturedProfile.getDescription()).append("</p>");
        }
    }

    /**
     * @return the resource key suffix naming who holds a facility: the player, their employer or the enemy
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String getOwnerKey(@Nullable ForceAlignment owner) {
        if (owner == ForceAlignment.Player) {
            return "player";
        }
        return StratConFacilityDefinition.isAlliedToPlayer(owner) ? "employer" : "enemy";
    }

    private JComponent createOrdersPanel() {
        JPanel ordersPanel = new JPanel();
        ordersPanel.setLayout(new BoxLayout(ordersPanel, BoxLayout.Y_AXIS));
        ordersPanel.setBorder(BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "dialog.orders")));
        ordersPanel.setAlignmentX(LEFT_ALIGNMENT);

        if (!StratConFacilityOperations.isEnabled(campaign)) {
            ordersPanel.add(createHtmlPane(getTextAt(RESOURCE_BUNDLE, "dialog.ordersDisabled")));
            return ordersPanel;
        }

        List<OrderOption> options = StratConFacilityAdvisor.getOrderOptions(campaign, contract, track, coords);
        if (options.isEmpty()) {
            ordersPanel.add(createHtmlPane(getTextAt(RESOURCE_BUNDLE, "dialog.noOrders")));
        }
        for (OrderOption option : options) {
            ordersPanel.add(createOrderRow(option));
        }
        return ordersPanel;
    }

    private JComponent createOrderRow(OrderOption option) {
        FacilityOperation operation = option.operation();
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), 0, UIUtil.scaleForGUI(4), 0));

        StringBuilder html = new StringBuilder("<html><body><b>")
                                   .append(getFormattedTextAt(RESOURCE_BUNDLE,
                                         "operation.label",
                                         getTextAt(RESOURCE_BUNDLE, "operation." + operation.name()),
                                         option.supportPointCost()))
                                   .append("</b><br>")
                                   .append(getTextAt(RESOURCE_BUNDLE, "operation.tooltip." + operation.name()));
        if (!option.isAvailable()) {
            html.append("<br><span style='color:").append(getNegativeColor()).append("'>")
                  .append(getTextAt(RESOURCE_BUNDLE, option.reasonKey()))
                  .append("</span>");
            if (option.guidanceKey() != null) {
                html.append(" <i>").append(getTextAt(RESOURCE_BUNDLE, option.guidanceKey())).append("</i>");
            }
        }
        html.append("</body></html>");
        row.add(createHtmlPane(html.toString()));

        if (option.isAvailable()) {
            row.add(createIssuePanel(option));
        }
        return row;
    }

    private JComponent createIssuePanel(OrderOption option) {
        JPanel issuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        issuePanel.setAlignmentX(LEFT_ALIGNMENT);

        JComboBox<FormationChoice> formationBox = new JComboBox<>();
        for (int formationId : option.eligibleFormationIds()) {
            formationBox.addItem(new FormationChoice(formationId,
                  StratConFacilityAdvisor.getFormationName(campaign, formationId)));
        }
        issuePanel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "dialog.formation")));
        issuePanel.add(formationBox);

        JComboBox<DefinitionChoice> definitionBox = null;
        if (option.operation() == FacilityOperation.BUILD) {
            definitionBox = new JComboBox<>();
            for (StratConFacilityDefinition definition : StratConFacilityOperations.getBuildableDefinitions()) {
                definitionBox.addItem(new DefinitionChoice(definition));
            }
            issuePanel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "dialog.build")));
            issuePanel.add(definitionBox);
        }

        RoundedJButton issueButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "dialog.giveOrder"));
        JComboBox<DefinitionChoice> chosenDefinitionBox = definitionBox;
        issueButton.addActionListener(evt -> {
            FormationChoice formation = (FormationChoice) formationBox.getSelectedItem();
            if (formation == null) {
                return;
            }
            StratConFacilityDefinition definition = null;
            if (chosenDefinitionBox != null) {
                DefinitionChoice definitionChoice = (DefinitionChoice) chosenDefinitionBox.getSelectedItem();
                if (definitionChoice == null) {
                    return;
                }
                definition = definitionChoice.definition();
            }
            StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  coords,
                  formation.formationId(),
                  option.operation(),
                  definition);
            changed();
        });
        issuePanel.add(issueButton);
        return issuePanel;
    }

    private @Nullable JComponent createUnderwayPanel() {
        List<StratConFacilityOrder> ordersHere = new ArrayList<>();
        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            if (order.getTargetCoords().equals(coords)) {
                ordersHere.add(order);
            }
        }
        if (ordersHere.isEmpty()) {
            return null;
        }

        JPanel underwayPanel = new JPanel();
        underwayPanel.setLayout(new BoxLayout(underwayPanel, BoxLayout.Y_AXIS));
        underwayPanel.setBorder(BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "dialog.underway")));
        underwayPanel.setAlignmentX(LEFT_ALIGNMENT);
        for (StratConFacilityOrder order : ordersHere) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.setAlignmentX(LEFT_ALIGNMENT);
            String formationName = StratConFacilityAdvisor.getFormationName(campaign, order.getFormationId());
            String operationName = getTextAt(RESOURCE_BUNDLE, "operation." + order.getOperation().name());
            if (order.getOperation() == FacilityOperation.SIEGE) {
                row.add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                      "dialog.underway.siege",
                      formationName,
                      order.getCompletionDate())));
                RoundedJButton liftButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "contextMenu.liftSiege"));
                int formationId = order.getFormationId();
                liftButton.addActionListener(evt -> {
                    StratConFacilitySiege.liftSiege(campaign, track, formationId);
                    changed();
                });
                row.add(liftButton);
            } else {
                row.add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                      "dialog.underway.order",
                      formationName,
                      operationName,
                      order.getCompletionDate())));
            }
            underwayPanel.add(row);
        }
        return underwayPanel;
    }

    private void changed() {
        onChange.run();
        rebuild();
    }

    private JEditorPane createHtmlPane(String html) {
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setAlignmentX(LEFT_ALIGNMENT);
        pane.addHyperlinkListener(evt -> {
            if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                handleImmersiveHyperlinkClick(this, campaign, evt.getDescription());
            }
        });
        return pane;
    }

    /**
     * A panel that always takes the scroll pane's width, so the HTML panes inside it wrap their text instead of
     * stretching the dialog sideways.
     */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return UIUtil.scaleForGUI(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private record FormationChoice(int formationId, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record DefinitionChoice(StratConFacilityDefinition definition) {
        @Override
        public String toString() {
            return definition.getDisplayableName();
        }
    }
}
