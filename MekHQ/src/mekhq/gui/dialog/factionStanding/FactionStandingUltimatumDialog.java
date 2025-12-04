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
package mekhq.gui.dialog.factionStanding;

import static mekhq.MHQConstants.CONFIRMATION_FACTION_STANDINGS_ULTIMATUM;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getInCharacterText;
import static mekhq.campaign.universe.factionStanding.GoingRogue.processGoingRogue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionJudgmentSceneType;
import mekhq.campaign.universe.factionStanding.GoingRogue;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.NewsDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentSceneDialog;

/**
 * Dialog logic for resolving a Faction Standing ultimatum event.
 *
 * <p>This class orchestrates a branching narrative event where a challenger issues an ultimatum to the player. The
 * dialog presents a sequence of immersive dialogs to the player, gathering confirmation and support, and administering
 * the aftermath: such as handling faction support, personnel status changes, and generating an in-character news
 * bulletin.</p>
 *
 * <p>Constructors and utility methods are included to facilitate these dialogs and manage campaign personnel
 * interactions.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingUltimatumDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingUltimatumDialog";

    private static final String KEY_ROOT = "FactionStandingUltimatumDialog.";
    private static final String KEY_INITIAL_OFFER = "initialOffer";
    private static final String KEY_SUPPORT_FOR = "for";
    private static final String KEY_SUPPORT_AGAINST = "against";
    private static final String KEY_NEWS_FOR = "newsFor";
    private static final String KEY_NEWS_AGAINST = "newsAgainst";

    private static final int CHOICE_INDEX_CHALLENGER = 0;
    private static final int CHOICE_INDEX_GO_MERCENARY = 2;
    private static final int CHOICE_INDEX_GO_PIRATE = 3;

    private final Campaign campaign;

    /**
     * Constructs and immediately executes a dialog sequence for an ultimatum event.
     *
     * <p>This involves presenting the player with a series of narrative and confirmation dialogs regarding support
     * for the challenger or incumbent, then resolves the outcome (including personnel departure, violent/desertion
     * status, and news generation).</p>
     *
     * @param campaign            the campaign context for the ultimatum event
     * @param challenger          the person challenging the incumbent
     * @param incumbent           the person being challenged
     * @param isViolentTransition {@code true} if the leadership change is violent
     * @param ultimatumName       the unique ultimatum name
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingUltimatumDialog(Campaign campaign, Person challenger, Person incumbent,
          boolean isViolentTransition, String ultimatumName) {
        this.campaign = campaign;
        Person commander = campaign.getCommander();
        String commanderAddress = campaign.getCommanderAddress(false);
        Person secondInCommand = campaign.getSecondInCommand();
        Person thirdInCommand = getThirdInCommand(commander, secondInCommand);
        String campaignName = campaign.getName();

        // Helper to show an ImmersiveDialogSimple with i18n text key
        showDialog(ultimatumName, KEY_INITIAL_OFFER, commander, secondInCommand, challenger, null, campaignName,
              commanderAddress);
        showDialog(ultimatumName, KEY_SUPPORT_FOR, commander, secondInCommand, thirdInCommand, null, campaignName,
              commanderAddress);
        showDialog(ultimatumName, KEY_SUPPORT_AGAINST, commander, secondInCommand, null, secondInCommand,
              campaignName, commanderAddress);

        // Ultimatum decision dialog loop
        boolean overallConfirmed = false;
        ImmersiveDialogSimple ultimatumDialog = null;
        while (!overallConfirmed) {
            List<String> buttons = List.of(
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "FactionStandingUltimatumDialog.support",
                        challenger.getGivenName()),
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "FactionStandingUltimatumDialog.support",
                        incumbent.getGivenName()),
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "FactionStandingUltimatumDialog.goRogue.mercenary"),
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "FactionStandingUltimatumDialog.goRogue.pirate")
            );
            ultimatumDialog = new ImmersiveDialogSimple(
                  campaign, challenger, incumbent,
                  getTextAt(RESOURCE_BUNDLE, "FactionStandingUltimatumDialog.ultimatum"),
                  buttons, null, null, true, ImmersiveDialogWidth.SMALL
            );

            if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_FACTION_STANDINGS_ULTIMATUM)) {
                overallConfirmed = new ImmersiveDialogConfirmation(campaign,
                      CONFIRMATION_FACTION_STANDINGS_ULTIMATUM).wasConfirmed();
            } else {
                overallConfirmed = true;
            }
        }

        int dialogChoice = ultimatumDialog.getDialogChoice();
        boolean mercenaryChoice = dialogChoice == CHOICE_INDEX_GO_MERCENARY;
        boolean pirateChoice = dialogChoice == CHOICE_INDEX_GO_PIRATE;
        if (mercenaryChoice || pirateChoice) {
            processBecomingMercenaryOrPirate(campaign, isViolentTransition, commander, secondInCommand,
                  thirdInCommand, mercenaryChoice);
            return;
        }

        processChoosingAnUltimatum(campaign,
              challenger,
              incumbent,
              isViolentTransition,
              ultimatumDialog,
              thirdInCommand,
              secondInCommand,
              commander,
              campaignName,
              commanderAddress,
              ultimatumName);
    }

    /**
     * Processes the decision made during an ultimatum scenario, updating news, personnel statuses, and faction
     * standings based on the selected outcome.
     *
     * @param campaign            the current {@link Campaign} instance
     * @param challenger          the {@link Person} issuing the challenge
     * @param incumbent           the {@link Person} currently in command
     * @param isViolentTransition {@code true} if the leadership transition involves violence, {@code false} otherwise
     * @param ultimatumDialog     the {@link ImmersiveDialogSimple} capturing the user's choice
     * @param thirdInCommand      the third-in-command {@link Person}, may be {@code null}
     * @param secondInCommand     the second-in-command {@link Person}, may be {@code null}
     * @param commander           the current commanding {@link Person}
     * @param campaignName        the name of the campaign
     * @param commanderAddress    the address or form of address for the commander
     * @param ultimatumName       the unique identifier for the ultimatum
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processChoosingAnUltimatum(Campaign campaign, Person challenger, Person incumbent,
          boolean isViolentTransition, ImmersiveDialogSimple ultimatumDialog, Person thirdInCommand,
          Person secondInCommand, Person commander, String campaignName, String commanderAddress,
          String ultimatumName) {
        boolean choseChallenger = ultimatumDialog.getDialogChoice() == CHOICE_INDEX_CHALLENGER;
        String newsKey = choseChallenger ? KEY_NEWS_FOR : KEY_NEWS_AGAINST;
        Faction chosenFaction = choseChallenger ? challenger.getOriginFaction() : incumbent.getOriginFaction();
        Faction otherFaction = choseChallenger ? incumbent.getOriginFaction() : challenger.getOriginFaction();
        Person supporter = choseChallenger ? thirdInCommand : secondInCommand;
        Person rival = choseChallenger ? secondInCommand : thirdInCommand;

        // In-character news bulletin
        String newsDialogKey = getDialogKey(ultimatumName, newsKey);
        String newsText = getInCharacterText(RESOURCE_BUNDLE,
              newsDialogKey,
              commander,
              secondInCommand,
              "",
              campaignName,
              "",
              null,
              commanderAddress);
        new NewsDialog(campaign, newsText);

        // Process outcome
        processGoingRogue(campaign, chosenFaction, commander, supporter, isViolentTransition, true);

        if (rival != null && !(rival.getStatus().isDepartedUnit() || rival.getStatus().isDead())) {
            rival.changeStatus(
                  campaign,
                  campaign.getLocalDate(),
                  isViolentTransition ? PersonnelStatus.HOMICIDE : PersonnelStatus.DESERTED
            );
        }

        GoingRogue.processFactionStandingChangeForOldFaction(campaign, otherFaction);
    }

    /**
     * Handles the process of a unit and its commanders becoming mercenaries, including updating personnel statuses,
     * displaying judgment scenes, and adjusting faction standings.
     *
     * @param campaign            the current {@link Campaign} instance
     * @param isViolentTransition {@code true} if the transition involves violence, {@code false} otherwise
     * @param commander           the current commanding {@link Person}
     * @param secondInCommand     the second-in-command {@link Person}, may be {@code null}
     * @param thirdInCommand      the third-in-command {@link Person}, may be {@code null}
     * @param isMercenary         {@code true} if the campaign is changing to the mercenary faction; {@code false} if
     *                            the campaign is changing to the pirate faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processBecomingMercenaryOrPirate(Campaign campaign, boolean isViolentTransition,
          Person commander, Person secondInCommand, Person thirdInCommand, boolean isMercenary) {
        new FactionJudgmentSceneDialog(campaign,
              commander,
              secondInCommand,
              FactionJudgmentSceneType.GO_ROGUE,
              campaign.getFaction());

        Faction oldFaction = campaign.getFaction();
        Faction newFaction = Factions.getInstance()
                                   .getFaction(isMercenary ? MERCENARY_FACTION_CODE : PIRATE_FACTION_CODE);
        processGoingRogue(campaign, newFaction, commander, secondInCommand,
              isViolentTransition, true, campaign.getCampaignOptions().isTrackFactionStanding());

        if (secondInCommand != null &&
                  !(secondInCommand.getStatus().isDepartedUnit() || secondInCommand.getStatus().isDead())) {
            secondInCommand.changeStatus(
                  campaign,
                  campaign.getLocalDate(),
                  isViolentTransition ? PersonnelStatus.HOMICIDE : PersonnelStatus.DESERTED
            );
        }

        if (thirdInCommand != null &&
                  !(thirdInCommand.getStatus().isDepartedUnit() || thirdInCommand.getStatus().isDead())) {
            thirdInCommand.changeStatus(
                  campaign,
                  campaign.getLocalDate(),
                  isViolentTransition ? PersonnelStatus.HOMICIDE : PersonnelStatus.DESERTED
            );
        }

        GoingRogue.processFactionStandingChangeForOldFaction(campaign, oldFaction);
    }

    /**
     * Displays an immersive dialog with campaign and personnel data, using a resource key for text localization.
     *
     * @param ultimatumName    the unique ultimatum name
     * @param key              the dialog key suffix for localization
     * @param commander        the commander character
     * @param second           the second-in-command character
     * @param leftPerson       (optional) the left-side dialog character
     * @param rightPerson      (optional) the right-side dialog character
     * @param campaignName     the campaign's name
     * @param commanderAddress how to address the commander
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void showDialog(String ultimatumName, String key, Person commander, Person second,
          @Nullable Person leftPerson, @Nullable Person rightPerson, String campaignName, String commanderAddress) {
        String dialogKey = getDialogKey(ultimatumName, key);
        String text = getInCharacterText(RESOURCE_BUNDLE, dialogKey, commander, second, "", campaignName, "",
              null, commanderAddress);
        String buttonLabel = getTextAt(RESOURCE_BUNDLE, "FactionStandingUltimatumDialog.continue");
        new ImmersiveDialogSimple(campaign, leftPerson, rightPerson, text, List.of(buttonLabel), null, null, false,
              ImmersiveDialogWidth.LARGE);
    }

    /**
     * Constructs a localization key for dialogs based on the affix and faction.
     *
     * @param ultimatumName the unique ultimatum name
     * @param affix         the key affix indicating dialog type
     *
     * @return the constructed resource localization key
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getDialogKey(String ultimatumName, String affix) {
        return KEY_ROOT + ultimatumName + '.' + affix;
    }

    /**
     * Selects the third-in-command person in the campaign .
     * <p>This is the highest-ranking unit member not already serving as commander or second-in-command.</p>
     *
     * @param commander       the current commander
     * @param secondInCommand the second-in-command individual
     *
     * @return the personnel member ranked third, or {@code null} if none exists
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable Person getThirdInCommand(Person commander, Person secondInCommand) {
        Person thirdInCommand = null;

        for (Person person : campaign.getActivePersonnel(false, false)) {
            if (person.equals(commander) || person.equals(secondInCommand)) {
                continue;
            }

            if (thirdInCommand == null || person.outRanksUsingSkillTiebreaker(campaign, thirdInCommand)) {
                thirdInCommand = person;
            }
        }

        return thirdInCommand;
    }

}
