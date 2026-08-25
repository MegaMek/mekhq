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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * A modal dialog for choosing the officer who will represent the player at a contract's negotiation table.
 *
 * <p>Candidates are the detachment's personnel trained in Negotiation, ranked best-first; if nobody is trained, the
 * detachment commander stands in. Each candidate is shown with their portrait, title, and negotiation skill so the
 * player can pick their strongest bargainer.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractNegotiatorPickerDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int PORTRAIT_SIZE = scaleForGUI(40);

    private final transient List<Person> candidates;
    private final JList<Person> candidateList;

    private transient Person selectedNegotiator;
    private boolean confirmed;

    /**
     * Opens the modal picker.
     *
     * @param campaign         the active campaign
     * @param currentSelection the currently chosen negotiator, preselected if still eligible
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractNegotiatorPickerDialog(Campaign campaign, Person currentSelection) {
        super(campaign.getGUI().getFrame(), getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.title"), true);

        this.candidates = eligibleNegotiators(campaign);
        this.candidateList = new JList<>();

        initializeComponents(currentSelection);
    }

    /** @return whether the player confirmed a selection. */
    public boolean wasConfirmed() {
        return confirmed;
    }

    /** @return the chosen negotiator, or {@code null} if the player cancelled. */
    public Person getSelectedNegotiator() {
        return selectedNegotiator;
    }

    /**
     * The people eligible to negotiate for the player: detachment personnel trained in Negotiation, ranked best-first,
     * or the detachment commander if nobody is trained.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Person> eligibleNegotiators(Campaign campaign) {
        Detachment detachment = campaign.getPlayerForce().getForceDetachment();

        List<Person> trained = new ArrayList<>();
        for (Person person : detachment.getPersonnel().values()) {
            if (person.hasSkill(SkillType.S_NEGOTIATION)) {
                trained.add(person);
            }
        }
        trained.sort(Comparator.comparingInt(ContractNegotiatorPickerDialog::negotiationLevel).reversed());
        if (!trained.isEmpty()) {
            return trained;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        Person commander = playerForce.getHumanResources()
                                 .getCommander(campaign.getCampaignOptions(),
                                       playerForce.isClanForce(),
                                       campaign.getLocalDate());
        return commander == null ? List.of() : List.of(commander);
    }

    private static int negotiationLevel(Person person) {
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        return skill == null ? -1 : skill.getLevel();
    }

    private void initializeComponents(Person currentSelection) {
        final int pad = scaleForGUI(8);

        JPanel content = new JPanel(new BorderLayout(pad, pad));
        content.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        JLabel intro = new JLabel("<html><body style='width:" +
                                        scaleForGUI(360) +
                                        "px'>"
                                        +
                                        getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.intro") +
                                        "</body></html>");
        content.add(intro, BorderLayout.NORTH);

        DefaultListModel<Person> model = new DefaultListModel<>();
        candidates.forEach(model::addElement);
        candidateList.setModel(model);
        candidateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        candidateList.setCellRenderer(new CandidateRenderer());
        if (currentSelection != null && candidates.contains(currentSelection)) {
            candidateList.setSelectedValue(currentSelection, true);
        } else if (!candidates.isEmpty()) {
            candidateList.setSelectedIndex(0);
        }

        if (candidates.isEmpty()) {
            content.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.empty")),
                  BorderLayout.CENTER);
        } else {
            content.add(new megamek.common.ui.FastJScrollPane(candidateList), BorderLayout.CENTER);
        }

        content.add(buildButtons(), BorderLayout.SOUTH);

        getContentPane().add(content);
        setMinimumSize(scaleForGUI(420, 360));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(6), 0));

        RoundedJButton cancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "picker.contractMarket.negotiator.cancel"));
        cancel.addActionListener(e -> dispose());
        buttons.add(cancel);

        RoundedJButton select = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "picker.contractMarket.negotiator.select"));
        select.addActionListener(e -> {
            selectedNegotiator = candidateList.getSelectedValue();
            confirmed = selectedNegotiator != null;
            dispose();
        });
        select.setEnabled(!candidates.isEmpty());
        buttons.add(select);

        return buttons;
    }

    private static String skillLine(Person person) {
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        if (skill == null) {
            return getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.skill.none");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.skill", skill.getLevel());
    }

    /** Renders each candidate with a portrait thumbnail, their title, and their negotiation skill. */
    private static class CandidateRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Person person) {
                setText("<html><b>" + person.getFullTitle() + "</b><br><span style='font-size:smaller'>"
                              + skillLine(person) + "</span></html>");
                ImageIcon icon = person.getPortraitImageIconWithFallback(true);
                if (icon != null) {
                    Image scaled = icon.getImage().getScaledInstance(PORTRAIT_SIZE, PORTRAIT_SIZE, Image.SCALE_SMOOTH);
                    setIcon(new ImageIcon(scaled));
                }
                setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(4), scaleForGUI(4),
                      scaleForGUI(4)));
            }
            return this;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(Math.max(preferred.width, scaleForGUI(420)), preferred.height);
    }
}
