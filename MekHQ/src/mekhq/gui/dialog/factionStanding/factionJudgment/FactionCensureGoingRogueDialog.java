/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.factionStanding.factionJudgment;

import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.ADOPTION_OR_MEKS;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.NO_ACCOLADE;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.CENSURE_LEVEL_0;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.campaign.universe.factionStanding.FactionCensureLevel;
import mekhq.campaign.universe.factionStanding.FactionJudgment;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * Displays a dialog allowing the user to choose a new faction for their force when going rogue.
 *
 * <p>This dialog presents a list of possible factions the campaign can join, based on campaign state, and records
 * whether the user confirmed the change, as well as which faction was selected. The options are presented within an
 * immersive dialog with both in-character and out-of-character text.</p>
 *
 * <p>Intended to be used when a player-controlled unit has chosen to go rogue and thus must select a new faction to
 * affiliate with.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionCensureGoingRogueDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingJudgments";

    /** Button index returned by the dialog when the 'confirm' action is selected. */
    private static final int CONFIRMED_DIALOG_INDEX = 1;

    /** The campaign context for which the dialog is being shown. */
    private final Campaign campaign;
    /** List of possible factions that the player can choose to join. */
    private final List<Faction> possibleFactions = new ArrayList<>();
    /** {@code true} if the user confirmed their choice in the dialog. */
    private final boolean wasConfirmed;
    /** The faction chosen by the user, or null if the dialog was canceled. */
    private final Faction chosenFaction;

    /**
     * Returns true if the user confirmed their choice in the dialog.
     *
     * @return true if the player confirmed; false if canceled.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    /**
     * Gets the faction the user selected to join, or {@code null} if none was selected.
     *
     * @return the chosen {@link Faction}, or {@code null} if no selection was made.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable Faction getChosenFaction() {
        return chosenFaction;
    }

    /**
     * Constructs and displays the dialog for selecting a new affiliation when going rogue. The dialog is modal and
     * completes upon construction.
     *
     * @param campaign                the campaign context to use for available factions and dialog content.
     * @param isUsingFactionStandings {@code true} if the campaign has faction standings enabled.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureGoingRogueDialog(Campaign campaign, boolean isUsingFactionStandings) {
        this.campaign = campaign;

        getPossibleFactions(isUsingFactionStandings);

        ImmersiveDialogCore dialog = new ImmersiveDialogCore(campaign,
              getSpeaker(),
              null,
              getInCharacterText(),
              getButtons(),
              isUsingFactionStandings ? getOutOfCharacterText() : null,
              null,
              false,
              getFactionPanel(),
              null,
              true);

        wasConfirmed = dialog.getDialogChoice() == CONFIRMED_DIALOG_INDEX;
        chosenFaction = possibleFactions.get(dialog.getComboBoxChoiceIndex());
    }

    /**
     * Returns the {@link Person} to serve as the in-character speaker for the dialog.
     *
     * @return the campaign's senior administrator with a command role.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Person getSpeaker() {
        return campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);
    }

    /**
     * Retrieves the localized in-character dialog text, including the commander's address.
     *
     * @return in-character dialog text.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getInCharacterText() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "FactionCensureGoingRogueDialog.inCharacter",
              campaign.getCommanderAddress());
    }

    /**
     * Provides the button options for the dialog, typically Cancel and Confirm.
     *
     * @return a list of button label and tooltip pairs.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons() {
        return List.of(
              new ImmersiveDialogCore.ButtonLabelTooltipPair(
                    getTextAt(RESOURCE_BUNDLE, "FactionCensureDialog.button.cancel"), null),
              new ImmersiveDialogCore.ButtonLabelTooltipPair(
                    getTextAt(RESOURCE_BUNDLE, "FactionCensureDialog.button.confirm"), null)
        );
    }

    /**
     * Retrieves the out-of-character help or explanation text for the dialog.
     *
     * @return localized out-of-character text.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getOutOfCharacterText() {
        return getTextAt(RESOURCE_BUNDLE, "FactionCensureGoingRogueDialog.outOfCharacter");
    }

    /**
     * Determines and populates the list of factions the player may join after leaving their current faction.
     *
     * <p>The method collects all factions active on the current in-game date and filters them based on eligibility,
     * including campaign-specific restrictions, such as Inner Sphere and Clan alignment rules. Factions representing
     * mercenaries and pirates are added to the top of the list if eligible, followed by other active factions.
     * Aggregate and mercenary organization factions are excluded.</p>
     *
     * <p>This process ensures that only appropriate and permissible choices are presented to the player, with special
     * factions given priority in the list.</p>
     *
     * @param isUsingFactionStandings {@code true} if the campaign has faction standings enabled.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void getPossibleFactions(boolean isUsingFactionStandings) {
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();
        Factions factions = Factions.getInstance();

        // Clear previous results (shouldn't be necessary but doesn't hurt)
        possibleFactions.clear();

        Faction mercenaries = factions.getFaction(MERCENARY_FACTION_CODE);
        Faction pirates = factions.getFaction(PIRATE_FACTION_CODE);

        boolean isMerc = campaignFaction.equals(mercenaries);
        boolean isPirate = campaignFaction.equals(pirates);

        List<Faction> activeFactions = new ArrayList<>(factions.getActiveFactions(today));
        activeFactions.remove(campaignFaction);

        boolean isBeforeTukayyid = today.isBefore(BATTLE_OF_TUKAYYID);
        boolean removeClanFactions = isBeforeTukayyid && !campaignFaction.isClan();
        boolean removeInnerSphereFactions = isBeforeTukayyid && campaignFaction.isClan();

        activeFactions.removeIf(faction -> !faction.isPlayable() ||
                                                 (faction.isClan() && removeClanFactions) ||
                                                 (!faction.isClan() && removeInnerSphereFactions) ||
                                                 faction.equals(mercenaries) ||
                                                 faction.equals(pirates)
        );

        if (isMerc || isPirate) {
            FactionStandings factionStandings = campaign.getFactionStandings();
            FactionJudgment factionJudgments = factionStandings.getFactionJudgments();
            for (Faction faction : new ArrayList<>(activeFactions)) {
                String factionShortName = faction.getShortName();

                FactionAccoladeLevel currentAccoladeLevel = factionJudgments.getAccoladeForFaction(factionShortName);
                if (NO_ACCOLADE.equals(currentAccoladeLevel)) {
                    activeFactions.remove(faction);
                    continue;
                }

                FactionCensureLevel currentCensureLevel = factionJudgments.getCensureLevelForFaction(factionShortName);
                if (!CENSURE_LEVEL_0.equals(currentCensureLevel)) {
                    activeFactions.remove(faction);
                    continue;
                }

                if (factionStandings.getRegardForFaction(factionShortName, false) < 0) {
                    activeFactions.remove(faction);
                    continue;
                }

                int recognition = currentAccoladeLevel.getRecognition();
                if (recognition < ADOPTION_OR_MEKS.getRecognition()) {
                    activeFactions.remove(faction);
                }
            }
        }

        // Add in order: mercenaries, pirates, then other valid factions
        if (!isMerc) {
            possibleFactions.add(mercenaries);
        }
        if (!isPirate) {
            possibleFactions.add(pirates);
        }
        possibleFactions.addAll(activeFactions);
    }

    /**
     * Constructs a Swing panel containing the UI for selecting a faction from the available options.
     *
     * @return a {@link JPanel} containing the faction drop-down selection.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel getFactionPanel() {
        JPanel factionPanel = new JPanel();
        JLabel lblFactions = new JLabel(getTextAt(RESOURCE_BUNDLE, "FactionCensureGoingRogueDialog.possibleFactions"));
        MMComboBox<String> comboFactions = new MMComboBox<>("choicePerson", createPersonGroupModel());

        factionPanel.add(lblFactions);
        factionPanel.add(comboFactions);

        return factionPanel;
    }

    /**
     * Builds the combo box model for displaying possible faction options by their full names for the current game
     * year.
     *
     * @return a {@link DefaultComboBoxModel} containing faction names.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private DefaultComboBoxModel<String> createPersonGroupModel() {
        final int gameYear = campaign.getGameYear();
        final DefaultComboBoxModel<String> factionModel = new DefaultComboBoxModel<>();
        for (Faction faction : possibleFactions) {
            factionModel.addElement(faction.getFullName(gameYear));
        }
        return factionModel;
    }
}
