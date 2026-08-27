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

import megamek.common.options.IOption;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
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
    private static final int DETAIL_PORTRAIT_SIZE = scaleForGUI(72);
    private static final int DETAIL_WIDTH = 340;
    /** Unscaled width of the header text beside the portrait: the pane width less the portrait and its gap. */
    private static final int DETAIL_HEADER_WIDTH = 260;

    /**
     * The negotiator SPAs and Flaws whose effects the negotiation table actually reads, in the order they are shown in
     * the detail pane. Kept in sync with {@code ContractNegotiationDialog}: manual-haggle caps, the two re-negotiation
     * options, the general penalty, the extra attempt, and the Edge trigger.
     */
    private static final String[] RELEVANT_TRAITS = {
          PersonnelOptions.HARD_BARGAINER, PersonnelOptions.PUSHOVER,
          PersonnelOptions.SHREWD_TRADER, PersonnelOptions.INFLEXIBLE,
          PersonnelOptions.FINE_PRINT_READER, PersonnelOptions.EASILY_FOOLED,
          PersonnelOptions.LOOPHOLE_FINDER, PersonnelOptions.BLACKLISTED,
          PersonnelOptions.RELENTLESS_BARGAINER, PersonnelOptions.ABRASIVE,
          PersonnelOptions.EDGE_COMMANDER_NEGOTIATION };

    private static final String MUTED_HEX = "#888888";

    private final transient Campaign campaign;
    private final transient List<Person> candidates;
    private final JList<Person> candidateList;

    private JLabel detailPortrait;
    private JLabel detailHeader;
    private JLabel detailTraits;

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

        this.campaign = campaign;
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
        candidateList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetail(candidateList.getSelectedValue());
            }
        });

        if (candidates.isEmpty()) {
            content.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.empty")),
                  BorderLayout.CENTER);
        } else {
            content.add(buildCandidateAndDetail(), BorderLayout.CENTER);
        }

        content.add(buildButtons(), BorderLayout.SOUTH);

        // Selection is set after the detail pane exists, so the listener paints the initial candidate's details.
        if (currentSelection != null && candidates.contains(currentSelection)) {
            candidateList.setSelectedValue(currentSelection, true);
        } else if (!candidates.isEmpty()) {
            candidateList.setSelectedIndex(0);
        }

        getContentPane().add(content);
        setMinimumSize(scaleForGUI(720, 420));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /**
     * Builds the candidate list (left) beside the detail pane (right) that describes the highlighted candidate's
     * negotiation skill and negotiation-relevant traits.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildCandidateAndDetail() {
        final int pad = scaleForGUI(8);

        megamek.common.ui.FastJScrollPane listScroll = new megamek.common.ui.FastJScrollPane(candidateList);
        listScroll.setPreferredSize(new Dimension(scaleForGUI(260), scaleForGUI(320)));

        detailPortrait = new JLabel();
        detailPortrait.setVerticalAlignment(JLabel.TOP);
        detailHeader = new JLabel();
        detailHeader.setVerticalAlignment(JLabel.TOP);
        detailTraits = new JLabel();
        detailTraits.setVerticalAlignment(JLabel.TOP);
        detailTraits.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Portrait on the left, the candidate's name/role/skill/edge to its right, so the top of the pane uses its
        // width instead of stacking a wide portrait above a mostly empty column. The traits list sits below, full width.
        JPanel topRow = new JPanel(new BorderLayout(pad, 0));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.add(detailPortrait, BorderLayout.WEST);
        topRow.add(detailHeader, BorderLayout.CENTER);

        // Stack the top row and the traits, then anchor the whole stack to the top (BorderLayout NORTH) so any spare
        // height falls below the traits rather than stretching the gap between them.
        JPanel stack = new JPanel();
        stack.setLayout(new javax.swing.BoxLayout(stack, javax.swing.BoxLayout.Y_AXIS));
        stack.add(topRow);
        stack.add(javax.swing.Box.createVerticalStrut(pad));
        stack.add(detailTraits);

        JPanel detail = new JPanel(new BorderLayout());
        detail.setBorder(BorderFactory.createEmptyBorder(0, pad, 0, 0));
        detail.add(stack, BorderLayout.NORTH);

        megamek.common.ui.FastJScrollPane detailScroll = new megamek.common.ui.FastJScrollPane(detail);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));

        JPanel panel = new JPanel(new BorderLayout(pad, 0));
        panel.add(listScroll, BorderLayout.WEST);
        panel.add(detailScroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Repaints the detail pane for the given candidate: their portrait, title, negotiation skill and 2d6 target number,
     * and every negotiation-relevant SPA or Flaw they carry, each with the effect it has at the table. Blank when no
     * candidate is highlighted.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void updateDetail(Person person) {
        if (detailHeader == null) {
            return;
        }
        if (person == null) {
            detailPortrait.setIcon(null);
            detailHeader.setText("<html><span style='color:" +
                                       MUTED_HEX +
                                       "'>"
                                       +
                                       getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.none") +
                                       "</span></html>");
            detailTraits.setText("");
            return;
        }

        ImageIcon portrait = person.getPortraitImageIconWithFallback(true);
        if (portrait != null) {
            detailPortrait.setIcon(new ImageIcon(portrait.getImage()
                                                       .getScaledInstance(DETAIL_PORTRAIT_SIZE,
                                                             DETAIL_PORTRAIT_SIZE,
                                                             Image.SCALE_SMOOTH)));
        } else {
            detailPortrait.setIcon(null);
        }

        boolean useEdge = campaign.getCampaignOptions().get(CampaignOption.USE_EDGE);

        // Header: name, role, negotiation skill, and Edge - sits to the right of the portrait, so it wraps at the
        // narrower width left beside it.
        StringBuilder header = new StringBuilder("<html><div style='width:")
                                     .append(scaleForGUI(DETAIL_HEADER_WIDTH)).append("px'>");
        header.append("<b style='font-size:larger'>").append(person.getFullTitle()).append("</b><br>");
        header.append("<span style='color:").append(MUTED_HEX).append("'>")
              .append(person.getPrimaryRole().toString()).append("</span><br><br>");
        header.append(skillDetail(person));
        // Edge is only meaningful - and only spent by the Edge negotiation trigger below - when the campaign uses it.
        if (useEdge) {
            header.append("<br><b>").append(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.edge"))
                  .append("</b> ").append(person.getAdjustedEdge());
        }
        detailHeader.setText(header.append("</div></html>").toString());

        // Traits: full-width list below the portrait/header row.
        StringBuilder traits = new StringBuilder("<html><div style='width:").append(scaleForGUI(DETAIL_WIDTH))
                                     .append("px'>");
        traits.append("<b>").append(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.traits"))
              .append("</b><br>");
        boolean anyTrait = false;
        for (String trait : RELEVANT_TRAITS) {
            // The Edge negotiation trigger has no effect when Edge is disabled, so don't list it then.
            if (trait.equals(PersonnelOptions.EDGE_COMMANDER_NEGOTIATION) && !useEdge) {
                continue;
            }
            if (!person.getOptions().booleanOption(trait)) {
                continue;
            }
            IOption option = person.getOptions().getOption(trait);
            if (option == null) {
                continue;
            }
            anyTrait = true;
            String name = option.getDisplayableName().replaceAll("\\s*\\([^)]*\\)", "");
            String description = option.getDescription() == null ? "" : option.getDescription();
            traits.append("&bull; <b>").append(name).append("</b><br><span style='color:").append(MUTED_HEX)
                  .append("'>").append(description.replace("\n\n", "<br>").replace("\n", "<br>"))
                  .append("</span><br><br>");
        }
        if (!anyTrait) {
            traits.append("<span style='color:").append(MUTED_HEX).append("'>")
                  .append(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.noTraits"))
                  .append("</span>");
        }
        detailTraits.setText(traits.append("</div></html>").toString());
    }

    /** The negotiation skill line for the detail pane: skill level and the 2d6 target number, or an untrained note. */
    private String skillDetail(Person person) {
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        if (skill == null) {
            return "<b>" +
                         getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.skill.label")
                         +
                         "</b> " +
                         getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.skill.untrained");
        }
        int target = person.checkSkill(SkillType.S_NEGOTIATION, campaign).getTargetNumber().getValue();
        return "<b>" + getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.skill.label") + "</b> "
                     + getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.skill.value",
              skill.getFinalSkillValue(person.getSkillModifierData()), target);
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
        return getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.skill",
              skill.getFinalSkillValue(person.getSkillModifierData()));
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
