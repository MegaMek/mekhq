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
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.roleplay.CheckDifficulty;
import mekhq.campaign.roleplay.CheckTarget;
import mekhq.campaign.roleplay.CheckTrait;
import mekhq.campaign.roleplay.NpcRating;
import mekhq.campaign.roleplay.OracleCharacter;
import mekhq.campaign.roleplay.RoleplayChecks;
import mekhq.campaign.roleplay.RoleplayChecks.Opponent;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCheckBox;

/**
 * The controls for one side of an opposed check on the {@link ChecksPage}: who takes part, what they roll or how good
 * they are, how hard the situation is for them and whether they may spend Edge. The person and trait pickers can be
 * searched by typing, as the company can run to hundreds of people and there are about a hundred skills.
 */
final class OpposedSidePanel {
    /** Someone who can take part in an opposed check: a cast member, someone in Personnel, or both. */
    record Participant(String label, @Nullable Person person, @Nullable OracleCharacter character) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final ChecksPage page;
    private final RoleplayChecks checks;
    private final boolean isDefender;
    private final JPanel panel = column();
    private final SearchablePicker<Participant> who;
    private final SearchablePicker<CheckTrait> trait;
    private final JComboBox<NpcRating> rating = new JComboBox<>(NpcRating.values());
    private final JComboBox<CheckDifficulty> sideDifficulty = new JComboBox<>(CheckDifficulty.values());
    private final HudCheckBox sideEdge = new HudCheckBox(ChecksPage.text("ChecksPage.opposed.edge"));
    private final JLabel note = ChecksPage.hint("");
    private UUID lastPerson;
    private Person traitPerson;

    /**
     * @param page       the page the side belongs to, for the people it lists and to refresh its preview
     * @param checks     works out targets
     * @param isDefender {@code true} for the side being acted against
     */
    OpposedSidePanel(final ChecksPage page, final RoleplayChecks checks, final boolean isDefender) {
        this.page = page;
        this.checks = checks;
        this.isDefender = isDefender;
        who = new SearchablePicker<>("opposedWho", Participant::label, this::changedWho);
        trait = new SearchablePicker<>("opposedTrait", this::describeTrait, page::refreshPreview);
        buildRatingAndDifficulty();
        layOut();
    }

    private void buildRatingAndDifficulty() {
        Hud.styleComboBox(rating);
        Hud.styleComboBox(sideDifficulty);
        rating.setRenderer(ChecksPage.labelRenderer());
        sideDifficulty.setRenderer((list, value, index, isSelected, cellHasFocus) ->
                                         Hud.comboRenderer().getListCellRendererComponent(list,
                                               (value == null) ? "" : value.getLabel() + "  "
                                                                            + value.getSignedModifier(),
                                               index, isSelected, cellHasFocus));
        sideDifficulty.setSelectedItem(CheckDifficulty.NORMAL);
        rating.setSelectedItem(NpcRating.REGULAR);
        sideEdge.setSelected(false);
        rating.addActionListener(event -> page.refreshPreview());
        sideDifficulty.addActionListener(event -> page.refreshPreview());
        sideEdge.addActionListener(event -> page.refreshPreview());
    }

    private void layOut() {
        panel.add(leftAligned(who.getComponent()));
        panel.add(Box.createVerticalStrut(scaleForGUI(6)));
        panel.add(leftAligned(trait.getComponent()));
        panel.add(leftAligned(rating));
        panel.add(Box.createVerticalStrut(scaleForGUI(6)));
        panel.add(leftAligned(sideDifficulty));
        panel.add(Box.createVerticalStrut(scaleForGUI(6)));
        panel.add(leftAligned(sideEdge));
        panel.add(leftAligned(note));
    }

    /** @return the side's controls */
    JPanel getPanel() {
        return panel;
    }

    // region Pickers

    private String describeTrait(final @Nullable CheckTrait value) {
        if (value == null) {
            return "";
        }
        if (traitPerson == null) {
            return value.getLabel();
        }
        String label = OracleConsole.joined(value.getLabel(), checks.target(traitPerson, value, 0).describe());
        return RoleplayChecks.isTrained(traitPerson, value) ? label
                     : label + "  " + ChecksPage.text("ChecksPage.skill.untrained");
    }

    // endregion Pickers

    // region Filling

    /** Rebuilds the list of who can take part, keeping the current choice if they are still listed. */
    void fill() {
        Participant previous = who.getSelected();
        List<Participant> participants = new ArrayList<>();
        Set<UUID> inCast = new LinkedHashSet<>();
        for (OracleCharacter character : page.console().roleplay().getActiveCharacters()) {
            Person person = page.personOf(character);
            if (person != null) {
                inCast.add(person.getId());
            }
            String kind = ChecksPage.text(person != null ? "ChecksPage.opposed.kind.linked"
                                                : "ChecksPage.opposed.kind.rated");
            participants.add(new Participant(OracleConsole.joined(character.getName(), kind), person, character));
        }
        for (Person person : page.companyPeople()) {
            if (!inCast.contains(person.getId())) {
                participants.add(new Participant(OracleConsole.joined(person.getFullName(),
                      ChecksPage.text("ChecksPage.opposed.kind.company")), person, null));
            }
        }
        Participant selected = null;
        if (previous != null) {
            selected = find(participants, previous.character() == null ? null : previous.character().getId(),
                  previous.person() == null ? null : previous.person().getId());
        } else if (!participants.isEmpty()) {
            // Start the two sides on different people.
            selected = participants.get(Math.min(isDefender ? 1 : 0, participants.size() - 1));
        }
        who.setItems(participants, selected);
        changedWho();
    }

    /**
     * Chooses a participant by cast member, or by person when there is no cast member.
     *
     * @param characterId the cast member, or {@code null}
     * @param personId    the person, or {@code null}
     */
    void choose(final @Nullable UUID characterId, final @Nullable UUID personId) {
        who.setSelected(find(who.getItems(), characterId, personId));
    }

    /**
     * Moves this side off the other side's participant, if both have the same one.
     *
     * @param other the other side
     */
    void chooseOtherThan(final OpposedSidePanel other) {
        if (!isSameAs(other)) {
            return;
        }
        Participant theirs = other.who.getSelected();
        for (Participant participant : who.getItems()) {
            if (!participant.equals(theirs) && !sameParticipant(participant, theirs)) {
                who.setSelected(participant);
                return;
            }
        }
    }

    private static @Nullable Participant find(final List<Participant> participants,
          final @Nullable UUID characterId, final @Nullable UUID personId) {
        for (Participant participant : participants) {
            boolean sameCharacter = characterId != null && participant.character() != null
                                          && characterId.equals(participant.character().getId());
            boolean samePerson = characterId == null && personId != null && participant.person() != null
                                       && personId.equals(participant.person().getId());
            if (sameCharacter || samePerson) {
                return participant;
            }
        }
        return null;
    }

    private void changedWho() {
        Participant participant = who.getSelected();
        Person person = (participant == null) ? null : participant.person();
        boolean rated = participant != null && person == null;
        trait.getComponent().setVisible(person != null);
        rating.setVisible(rated);
        sideEdge.setVisible(person != null && checks.isEdgeAllowed());
        sideEdge.setEnabled(checks.canUseEdge(person));
        if (person != null && !person.getId().equals(lastPerson)) {
            fillTraits(person);
        }
        lastPerson = (person == null) ? null : person.getId();
        if (rated && participant.character() != null && participant.character().getRating() != null) {
            rating.setSelectedItem(participant.character().getRating());
        }
        note.setText(ChecksPage.wrap(rated ? ChecksPage.text("ChecksPage.opposed.ratedNote")
                                           : (person != null && checks.isEdgeAllowed())
                                                   ? getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.edge",
                                                         person.getCurrentEdge()) : ""));
        panel.revalidate();
        page.refreshPreview();
    }

    /** Lists the person's trained skills, then the attributes, then their untrained skills. */
    private void fillTraits(final Person person) {
        CheckTrait previous = trait.getSelected();
        List<CheckTrait> trained = new ArrayList<>();
        List<CheckTrait> untrained = new ArrayList<>();
        for (String name : SkillType.getSkillList()) {
            (person.hasSkill(name) ? trained : untrained).add(CheckTrait.skill(name));
        }
        Comparator<CheckTrait> byName = Comparator.comparing(CheckTrait::getLabel, String.CASE_INSENSITIVE_ORDER);
        trained.sort(byName);
        untrained.sort(byName);
        List<CheckTrait> traits = new ArrayList<>(trained);
        ChecksPage.checkableAttributes().forEach(attribute -> traits.add(CheckTrait.attributes(attribute, null)));
        traits.addAll(untrained);
        // The display text includes this person's targets, so set them first.
        traitPerson = person;
        trait.setItems(traits, previous);
    }

    // endregion Filling

    // region Reading

    /**
     * @param other the other side
     *
     * @return {@code true} if both sides are the same person or cast member
     */
    boolean isSameAs(final OpposedSidePanel other) {
        return sameParticipant(who.getSelected(), other.who.getSelected());
    }

    private static boolean sameParticipant(final @Nullable Participant mine, final @Nullable Participant theirs) {
        if (mine == null || theirs == null) {
            return false;
        }
        boolean samePerson = mine.person() != null && theirs.person() != null
                                   && mine.person().getId().equals(theirs.person().getId());
        boolean sameCharacter = mine.character() != null && mine.character() == theirs.character();
        return samePerson || sameCharacter;
    }

    /** @return {@code true} once someone is chosen and, for a person, what they roll */
    boolean isReady() {
        Participant participant = who.getSelected();
        return participant != null && (participant.person() == null || trait.getSelected() != null);
    }

    /** @return the chosen participant's name, or an empty string */
    String name() {
        Participant participant = who.getSelected();
        if (participant == null) {
            return "";
        }
        return (participant.person() != null) ? participant.person().getFullName()
                     : Objects.requireNonNull(participant.character()).getName();
    }

    private CheckDifficulty difficulty() {
        return Objects.requireNonNullElse((CheckDifficulty) sideDifficulty.getSelectedItem(),
              CheckDifficulty.NORMAL);
    }

    /** @return {@code true} if this side will re-roll a loss with Edge */
    boolean rerolls() {
        Participant participant = who.getSelected();
        return participant != null && sideEdge.isSelected() && checks.canUseEdge(participant.person());
    }

    /** @return the number this side needs; only call when {@link #isReady()} */
    CheckTarget target() {
        Participant participant = Objects.requireNonNull(who.getSelected());
        int modifier = difficulty().getModifier();
        if (participant.person() != null) {
            return checks.target(participant.person(), Objects.requireNonNull(trait.getSelected()), modifier);
        }
        return RoleplayChecks.target((NpcRating) Objects.requireNonNull(rating.getSelectedItem()), modifier);
    }

    /** @return this side as the check rolls it; only call when {@link #isReady()} */
    Opponent opponent() {
        Participant participant = Objects.requireNonNull(who.getSelected());
        if (participant.person() != null) {
            return Opponent.person(participant.person(), participant.character(),
                  Objects.requireNonNull(trait.getSelected()), difficulty(), 0, sideEdge.isSelected());
        }
        return Opponent.rated(participant.character(), (NpcRating) Objects.requireNonNull(rating.getSelectedItem()),
              difficulty(), 0);
    }

    // endregion Reading
}
