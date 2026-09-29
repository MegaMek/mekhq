/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.annotation.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionJudgmentSceneType;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumData;
import mekhq.campaign.universe.factionStanding.GoingRogue;
import mekhq.campaign.universe.factionStanding.UltimatumUnitDamage;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.NewsDialog;
import mekhq.gui.dialog.factionStanding.UltimatumSidePickerDialog.ChoiceKind;
import mekhq.gui.dialog.factionStanding.UltimatumSidePickerDialog.PickerChoice;
import mekhq.gui.dialog.factionStanding.UltimatumSidePickerDialog.PickerContent;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentSceneDialog;

/**
 * Dialog logic for resolving a Faction Standing ultimatum event.
 *
 * <p>The ultimatum opens with a short sequence of immersive scenes: the first side's offer, the third-in-command
 * arguing for it, and the second-in-command, the ultimatum's single dissenting voice, arguing against. The player then
 * picks a side, or goes rogue, in the {@link UltimatumSidePickerDialog} carousel. The aftermath covers the news
 * bulletin, the dissenter leaving if the choice goes against them, the loyalty checks of everyone else, standing
 * changes with every faction involved, and, for violent ultimatums, damage to the campaign's units.</p>
 *
 * <p>The picker's "Ignore Ultimatum (GM)" button, always offered, ends the ultimatum with no consequences at
 * all.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingUltimatumDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingUltimatumDialog";

    static final String KEY_ROOT = "FactionStandingUltimatumDialog.";
    static final String KEY_INITIAL_OFFER = "initialOffer";
    static final String KEY_SUPPORT_FOR = "for";
    static final String KEY_SUPPORT_AGAINST = "against";
    static final String KEY_SIDE_PITCH = "pitch";
    static final String KEY_SIDE_NEWS = "news";
    static final String KEY_ROGUE_MERCENARY = "mercenary";
    static final String KEY_ROGUE_PIRATE = "pirate";

    private static final String PICKER_PREFIX = KEY_ROOT + "picker.";

    /**
     * One side of an ultimatum, ready to show and resolve.
     *
     * @param id       the side's ID, used for its text keys and the dissenter's preference
     * @param agitator the generated person asking for the campaign's support
     * @param faction  the faction the campaign joins if it chooses this side
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record UltimatumSide(String id, Person agitator, Faction faction) {}

    private final Campaign campaign;

    /**
     * Constructs and immediately runs the dialog sequence for an ultimatum event, then resolves the player's choice.
     *
     * @param campaign            the campaign context for the ultimatum event
     * @param sides               the sides the player can choose between, in picker order; the first side delivers
     *                            the opening offer
     * @param dissenterPreference the ID of the side the dissenting officer wants, or
     *                            {@link FactionStandingUltimatumData#ROGUE_PREFERENCE}
     * @param isViolentTransition {@code true} if the leadership change is violent
     * @param divisiveness        added to the loyalty check personnel make to follow the player's decision
     * @param ultimatumName       the unique ultimatum name
     * @param date                the ultimatum's date
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingUltimatumDialog(Campaign campaign, List<UltimatumSide> sides, String dissenterPreference,
          boolean isViolentTransition, int divisiveness, String ultimatumName, LocalDate date) {
        this.campaign = campaign;
        PlayerForce playerForce = campaign.getPlayerForce();
        ForceHumanResources humanResources = playerForce.getHumanResources();
        boolean isClanForce = playerForce.isClanForce();
        Person commander = humanResources.getCommander(campaign.getCampaignOptions(), isClanForce,
              campaign.getLocalDate());
        String commanderAddress = campaign.getCommanderAddress(false);
        Person secondInCommand = humanResources.getSecondInCommand(campaign.getCampaignOptions(), isClanForce,
              campaign.getLocalDate());
        Person thirdInCommand = getThirdInCommand(campaign, commander, secondInCommand);
        String campaignName = playerForce.getName();
        Faction originalFaction = playerForce.getFaction();
        boolean isTrackingFactionStanding = campaign.getCampaignOptions().get(CampaignOption.TRACK_FACTION_STANDING);

        showDialog(ultimatumName, KEY_INITIAL_OFFER, commander, secondInCommand, sides.get(0).agitator(), null,
              campaignName, commanderAddress);
        showDialog(ultimatumName, KEY_SUPPORT_FOR, commander, secondInCommand, thirdInCommand, null, campaignName,
              commanderAddress);
        showDialog(ultimatumName, KEY_SUPPORT_AGAINST, commander, secondInCommand, null, secondInCommand,
              campaignName, commanderAddress);

        List<PickerChoice> choices = buildChoices(sides, ultimatumName, originalFaction, isViolentTransition,
              isTrackingFactionStanding, dissenterPreference, campaign.getGameYear(),
              pitchKey -> getInCharacterText(RESOURCE_BUNDLE, pitchKey, commander, secondInCommand, "",
                    campaignName, "", null, commanderAddress));
        PickerContent content = new PickerContent(choices,
              date.toString(),
              getTextAt(RESOURCE_BUNDLE, KEY_ROOT + "ultimatum"),
              secondInCommand == null ? null : getFormattedTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "dissenter",
                    secondInCommand.getFullTitle()),
              isViolentTransition,
              buildGameInformation(sides.size(), isViolentTransition, isTrackingFactionStanding));

        PickerChoice choice = askForDecision(content);
        if (choice == null) {
            campaign.addReport(DailyReportType.POLITICS, getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "ignored.report"));
            return;
        }

        // Captured before anyone leaves, so a violent ultimatum can scale its unit damage to the departures
        List<Person> personnelBeforeDecision = UltimatumUnitDamage.snapshotPersonnel(campaign);

        if (choice.kind().isGoingRogue()) {
            processBecomingMercenaryOrPirate(campaign, sides, choice, originalFaction, isViolentTransition,
                  divisiveness, commander, secondInCommand, isTrackingFactionStanding);
        } else {
            processChoosingSide(campaign, sides, choice, originalFaction, isViolentTransition, divisiveness,
                  secondInCommand, commander, campaignName, commanderAddress, ultimatumName,
                  isTrackingFactionStanding);
        }

        if (isViolentTransition) {
            UltimatumUnitDamage.damageUnits(campaign,
                  UltimatumUnitDamage.getDepartedShare(personnelBeforeDecision));
        }
    }

    /**
     * Shows the picker until the player commits to a choice and confirms it, or ignores the ultimatum.
     *
     * <p>Closing the picker from its window frame doesn't count as a decision; it is shown again.</p>
     *
     * @param content what the picker shows
     *
     * @return the player's confirmed choice, or {@code null} if the ultimatum was ignored
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable PickerChoice askForDecision(PickerContent content) {
        while (true) {
            UltimatumSidePickerDialog picker = new UltimatumSidePickerDialog(null, content);
            if (picker.wasIgnored()) {
                return null;
            }

            PickerChoice choice = picker.getSelectedChoice();
            if (choice == null) {
                continue;
            }

            boolean isConfirmed = MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_FACTION_STANDINGS_ULTIMATUM)
                                        || new ImmersiveDialogConfirmation(campaign,
                  CONFIRMATION_FACTION_STANDINGS_ULTIMATUM).wasConfirmed();
            if (isConfirmed) {
                return choice;
            }
        }
    }

    /**
     * Builds the picker's cards: one per side, in order, then Mercenary and Pirate.
     *
     * @param sides                     the ultimatum's sides
     * @param ultimatumName             the ultimatum's name, for its text keys
     * @param originalFaction           the campaign's faction before the ultimatum
     * @param isViolentTransition       {@code true} if the ultimatum is violent
     * @param isTrackingFactionStanding {@code true} if standing tags should be shown
     * @param dissenterPreference       the ID of the side the dissenter wants, or
     *                                  {@link FactionStandingUltimatumData#ROGUE_PREFERENCE}
     * @param gameYear                  the campaign's year, for faction names
     * @param pitchResolver             turns a side's pitch text key into its displayed text
     *
     * @return the cards, in carousel order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<PickerChoice> buildChoices(List<UltimatumSide> sides, String ultimatumName, Faction originalFaction,
          boolean isViolentTransition, boolean isTrackingFactionStanding, String dissenterPreference, int gameYear,
          Function<String, String> pitchResolver) {
        List<PickerChoice> choices = new ArrayList<>();
        for (int sideIndex = 0; sideIndex < sides.size(); sideIndex++) {
            UltimatumSide side = sides.get(sideIndex);
            Faction faction = side.faction();
            boolean isStaying = faction.equals(originalFaction);

            List<Tag> placementTags = new ArrayList<>();
            String placementKey = PICKER_PREFIX + (isStaying ? "tag.stays" : "tag.joins");
            placementTags.add(new Tag(getFormattedTextAt(RESOURCE_BUNDLE, placementKey, faction.getShortName()),
                  HudStyle.TEXT_MUTED));
            if (isDefection(isViolentTransition, faction, originalFaction)) {
                placementTags.add(new Tag(getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "tag.defection"),
                      HudStyle.DANGER));
            }
            List<Tag> standingTags = new ArrayList<>();
            if (isTrackingFactionStanding) {
                standingTags.add(new Tag(getFormattedTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "tag.standingUp",
                      faction.getShortName()), HudStyle.READY));
                addStandingLossTag(standingTags, getStandingLossCodes(sides, faction, originalFaction));
            }

            Person agitator = side.agitator();
            choices.add(new PickerChoice(ChoiceKind.SIDE,
                  sideIndex,
                  faction.getShortName(),
                  faction.getFullName(gameYear),
                  agitator.getGivenName() + " · " + agitator.getPrimaryRole().getLabel(false),
                  pitchResolver.apply(getSideKey(ultimatumName, side.id(), KEY_SIDE_PITCH)),
                  toTagRows(placementTags, standingTags),
                  dissenterStays(dissenterPreference, ChoiceKind.SIDE, side.id())));
        }

        for (ChoiceKind rogueKind : List.of(ChoiceKind.MERCENARY, ChoiceKind.PIRATE)) {
            boolean isMercenary = rogueKind == ChoiceKind.MERCENARY;
            String rogueKey = PICKER_PREFIX + (isMercenary ? "mercenary." : "pirate.");
            List<Tag> placementTags = new ArrayList<>();
            placementTags.add(new Tag(getTextAt(RESOURCE_BUNDLE, rogueKey + "tag"),
                  isMercenary ? HudStyle.CAUTION : HudStyle.DANGER));
            if (isViolentTransition) {
                placementTags.add(new Tag(getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "tag.defection"),
                      HudStyle.DANGER));
            }
            List<Tag> standingTags = new ArrayList<>();
            if (isTrackingFactionStanding) {
                addStandingLossTag(standingTags, getStandingLossCodes(sides, null, originalFaction));
            }

            choices.add(new PickerChoice(rogueKind,
                  -1,
                  isMercenary ? MERCENARY_FACTION_CODE : PIRATE_FACTION_CODE,
                  getTextAt(RESOURCE_BUNDLE, rogueKey + "title"),
                  getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "rogue.subtitle"),
                  resolveRoguePitch(ultimatumName, isMercenary ? KEY_ROGUE_MERCENARY : KEY_ROGUE_PIRATE,
                        rogueKey + "pitch", pitchResolver),
                  toTagRows(placementTags, standingTags),
                  dissenterStays(dissenterPreference, rogueKind, null)));
        }

        return choices;
    }

    /**
     * Resolves a rogue card's appeal: the ultimatum's own text when the writers have supplied it, otherwise the
     * shared default, so a new ultimatum still shows something before its text is written.
     *
     * @param ultimatumName the ultimatum's name
     * @param rogueKind     {@link #KEY_ROGUE_MERCENARY} or {@link #KEY_ROGUE_PIRATE}
     * @param fallbackKey   the shared default text key
     * @param pitchResolver turns a text key into its displayed text
     *
     * @return the appeal to show on the card
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String resolveRoguePitch(String ultimatumName, String rogueKind, String fallbackKey,
          Function<String, String> pitchResolver) {
        String ultimatumKey = getRogueKey(ultimatumName, rogueKind);
        if (isResourceKeyValid(getTextAt(RESOURCE_BUNDLE, ultimatumKey))) {
            return pitchResolver.apply(ultimatumKey);
        }
        return getTextAt(RESOURCE_BUNDLE, fallbackKey);
    }

    /**
     * Groups a card's tags into rows by what they describe: where the unit ends up, then standing changes. Empty rows
     * are left out.
     *
     * @param placementTags the tags describing where the unit ends up, including defection
     * @param standingTags  the tags describing standing changes
     *
     * @return the non-empty rows, in display order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<List<Tag>> toTagRows(List<Tag> placementTags, List<Tag> standingTags) {
        List<List<Tag>> tagRows = new ArrayList<>();
        if (!placementTags.isEmpty()) {
            tagRows.add(placementTags);
        }
        if (!standingTags.isEmpty()) {
            tagRows.add(standingTags);
        }
        return tagRows;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static void addStandingLossTag(List<Tag> tags, List<String> lostFactionCodes) {
        if (!lostFactionCodes.isEmpty()) {
            tags.add(new Tag(getFormattedTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "tag.standingDown",
                  String.join(", ", lostFactionCodes)), HudStyle.CAUTION));
        }
    }

    /**
     * Lists the factions whose standing drops for a choice: the campaign's original faction if it leaves, then every
     * other side's faction. Each faction appears once.
     *
     * @param sides           the ultimatum's sides
     * @param chosenFaction   the faction chosen, or {@code null} when going rogue
     * @param originalFaction the campaign's faction before the ultimatum
     *
     * @return the faction codes, in order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<String> getStandingLossCodes(List<UltimatumSide> sides, @Nullable Faction chosenFaction,
          Faction originalFaction) {
        List<String> codes = new ArrayList<>();
        if (!originalFaction.equals(chosenFaction) && !originalFaction.isAggregate()) {
            codes.add(originalFaction.getShortName());
        }
        for (Faction faction : getOtherSideFactions(sides, chosenFaction, originalFaction)) {
            codes.add(faction.getShortName());
        }
        return codes;
    }

    /**
     * Lists the factions of the sides the player didn't choose, leaving out the campaign's original faction (which
     * {@link GoingRogue} already handles) and repeats.
     *
     * @param sides           the ultimatum's sides
     * @param chosenFaction   the faction chosen, or {@code null} when going rogue
     * @param originalFaction the campaign's faction before the ultimatum
     *
     * @return the factions whose standing should drop, in side order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<Faction> getOtherSideFactions(List<UltimatumSide> sides, @Nullable Faction chosenFaction,
          Faction originalFaction) {
        List<Faction> factions = new ArrayList<>();
        for (UltimatumSide side : sides) {
            Faction faction = side.faction();
            if (faction.equals(chosenFaction) || faction.equals(originalFaction) || faction.isAggregate()
                      || factions.contains(faction)) {
                continue;
            }
            factions.add(faction);
        }
        return factions;
    }

    /**
     * Determines whether the dissenting officer stays with the unit after a choice.
     *
     * @param dissenterPreference the ID of the side the dissenter wants, or
     *                            {@link FactionStandingUltimatumData#ROGUE_PREFERENCE}
     * @param kind                what the player chose
     * @param sideId              the chosen side's ID, or {@code null} when going rogue
     *
     * @return {@code true} if the dissenter agrees with the choice and stays
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean dissenterStays(@Nullable String dissenterPreference, ChoiceKind kind, @Nullable String sideId) {
        if (FactionStandingUltimatumData.ROGUE_PREFERENCE.equals(dissenterPreference)) {
            return kind.isGoingRogue();
        }
        return (kind == ChoiceKind.SIDE) && (sideId != null) && sideId.equals(dissenterPreference);
    }

    /**
     * @return the out-of-character rules summary for the picker
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String buildGameInformation(int sideCount, boolean isViolentTransition,
          boolean isTrackingFactionStanding) {
        StringBuilder information = new StringBuilder();
        if (isTrackingFactionStanding) {
            information.append(getTextAt(RESOURCE_BUNDLE,
                  PICKER_PREFIX + (sideCount == 1 ? "info.standing.single" : "info.standing.multiple")));
            information.append(' ');
        }
        information.append(getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "info.dissenter"));
        // The loyalty check only runs alongside Faction Standing
        if (isTrackingFactionStanding) {
            information.append(' ').append(getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "info.loyalty"));
        }
        if (isViolentTransition) {
            information.append(' ').append(getTextAt(RESOURCE_BUNDLE, PICKER_PREFIX + "info.violent"));
        }
        return information.toString();
    }

    /**
     * Resolves choosing one of the ultimatum's sides: the news bulletin, joining or staying with the side's faction,
     * the dissenter leaving if they disagree, and standing changes with every faction involved.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void processChoosingSide(Campaign campaign, List<UltimatumSide> sides, PickerChoice choice,
          Faction originalFaction, boolean isViolentTransition, int divisiveness, @Nullable Person secondInCommand,
          Person commander, String campaignName, String commanderAddress, String ultimatumName,
          boolean isTrackingFactionStanding) {
        UltimatumSide side = sides.get(choice.sideIndex());
        Faction chosenFaction = side.faction();

        // In-character news bulletin. The news text is written around the second-in-command, the dissenting voice.
        String newsText = getInCharacterText(RESOURCE_BUNDLE,
              getSideKey(ultimatumName, side.id(), KEY_SIDE_NEWS),
              commander,
              secondInCommand,
              "",
              campaignName,
              "",
              null,
              commanderAddress);
        new NewsDialog(campaign, newsText);

        // The second-in-command is exempt from the loyalty check; whether they stay is the dissenter's decision below
        processGoingRogue(campaign,
              chosenFaction,
              commander,
              secondInCommand,
              isDefection(isViolentTransition, chosenFaction, originalFaction),
              true,
              isTrackingFactionStanding,
              divisiveness);

        if (!choice.dissenterStays()) {
            departIfPresent(campaign, secondInCommand, isViolentTransition);
        }

        // processGoingRogue has already lowered standing with the original faction, if the campaign left it
        if (isTrackingFactionStanding) {
            for (Faction otherFaction : getOtherSideFactions(sides, chosenFaction, originalFaction)) {
                GoingRogue.processFactionStandingChangeForOldFaction(campaign, otherFaction);
            }
        }
    }

    /**
     * Resolves going rogue as a Mercenary or Pirate instead of choosing a side.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processBecomingMercenaryOrPirate(Campaign campaign, List<UltimatumSide> sides,
          PickerChoice choice, Faction originalFaction, boolean isViolentTransition, int divisiveness,
          Person commander, @Nullable Person secondInCommand, boolean isTrackingFactionStanding) {
        new FactionJudgmentSceneDialog(campaign,
              commander,
              secondInCommand,
              FactionJudgmentSceneType.GO_ROGUE,
              originalFaction);

        Faction newFaction = Factions.getInstance()
                                   .getFaction(choice.kind() == ChoiceKind.MERCENARY ? MERCENARY_FACTION_CODE
                                                     : PIRATE_FACTION_CODE);
        // A violent transition is treated as a defection. processGoingRogue also lowers standing with the old faction.
        processGoingRogue(campaign, newFaction, commander, secondInCommand, isViolentTransition, true,
              isTrackingFactionStanding, divisiveness);

        if (!choice.dissenterStays()) {
            departIfPresent(campaign, secondInCommand, isViolentTransition);
        }

        if (isTrackingFactionStanding) {
            for (Faction sideFaction : getOtherSideFactions(sides, null, originalFaction)) {
                GoingRogue.processFactionStandingChangeForOldFaction(campaign, sideFaction);
            }
        }
    }

    /**
     * Determines whether siding with {@code chosenFaction} counts as a defection.
     *
     * <p>A violent transition is treated as a defection, unless the campaign is staying loyal to its current
     * faction.</p>
     *
     * @param isViolentTransition {@code true} if the leadership change is violent
     * @param chosenFaction       the faction the player sided with
     * @param currentFaction      the campaign's faction before the ultimatum resolved
     *
     * @return {@code true} if the choice is a defection
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isDefection(boolean isViolentTransition, Faction chosenFaction, Faction currentFaction) {
        return isViolentTransition && !chosenFaction.equals(currentFaction);
    }

    /**
     * Removes a person who refuses to follow the player's decision, if they are still with the unit.
     *
     * @param campaign            the current campaign
     * @param person              the departing person; may be {@code null}
     * @param isViolentTransition {@code true} if the person is killed rather than deserting
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void departIfPresent(Campaign campaign, @Nullable Person person, boolean isViolentTransition) {
        if (person == null) {
            return;
        }

        PersonnelStatus status = person.getStatus();
        if (status.isDepartedUnit() || status.isDead()) {
            return;
        }

        person.changeStatus(campaign,
              campaign.getLocalDate(),
              isViolentTransition ? PersonnelStatus.HOMICIDE : PersonnelStatus.DESERTED);
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
    private void showDialog(String ultimatumName, String key, Person commander, @Nullable Person second,
          @Nullable Person leftPerson, @Nullable Person rightPerson, String campaignName, String commanderAddress) {
        String dialogKey = getDialogKey(ultimatumName, key);
        String text = getInCharacterText(RESOURCE_BUNDLE, dialogKey, commander, second, "", campaignName, "",
              null, commanderAddress);
        String buttonLabel = getTextAt(RESOURCE_BUNDLE, "FactionStandingUltimatumDialog.continue");
        new ImmersiveDialogSimple(campaign, leftPerson, rightPerson, text, List.of(buttonLabel), null, null, false,
              ImmersiveDialogWidth.LARGE);
    }

    /**
     * Constructs a localization key for one of the ultimatum's scenes.
     *
     * @param ultimatumName the unique ultimatum name
     * @param affix         the key affix indicating dialog type
     *
     * @return the constructed resource localization key
     *
     * @author Illiani
     * @since 0.50.07
     */
    static String getDialogKey(String ultimatumName, String affix) {
        return KEY_ROOT + ultimatumName + '.' + affix;
    }

    /**
     * Constructs a localization key for one of a side's texts.
     *
     * @param ultimatumName the unique ultimatum name
     * @param sideId        the side's ID
     * @param affix         {@link #KEY_SIDE_PITCH} or {@link #KEY_SIDE_NEWS}
     *
     * @return the constructed resource localization key
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getSideKey(String ultimatumName, String sideId, String affix) {
        return KEY_ROOT + ultimatumName + ".side." + sideId + '.' + affix;
    }

    /**
     * Lists every text key an ultimatum needs: its three opening scenes, a pitch and a news bulletin for each
     * side, and the appeals on its Mercenary and Pirate cards.
     *
     * @param ultimatumName the unique ultimatum name
     * @param sideIds       the IDs of the ultimatum's sides
     *
     * @return the required keys, scenes first, then each side's keys in side order, then Mercenary and Pirate
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getRequiredTextKeys(String ultimatumName, List<String> sideIds) {
        List<String> keys = new ArrayList<>();
        keys.add(getDialogKey(ultimatumName, KEY_INITIAL_OFFER));
        keys.add(getDialogKey(ultimatumName, KEY_SUPPORT_FOR));
        keys.add(getDialogKey(ultimatumName, KEY_SUPPORT_AGAINST));
        for (String sideId : sideIds) {
            keys.add(getSideKey(ultimatumName, sideId, KEY_SIDE_PITCH));
            keys.add(getSideKey(ultimatumName, sideId, KEY_SIDE_NEWS));
        }
        keys.add(getRogueKey(ultimatumName, KEY_ROGUE_MERCENARY));
        keys.add(getRogueKey(ultimatumName, KEY_ROGUE_PIRATE));
        return keys;
    }

    /**
     * Constructs the localization key for one of the rogue cards' appeals.
     *
     * @param ultimatumName the unique ultimatum name
     * @param rogueKind     {@link #KEY_ROGUE_MERCENARY} or {@link #KEY_ROGUE_PIRATE}
     *
     * @return the constructed resource localization key
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getRogueKey(String ultimatumName, String rogueKind) {
        return KEY_ROOT + ultimatumName + ".rogue." + rogueKind + '.' + KEY_SIDE_PITCH;
    }

    /**
     * Selects the third-in-command person in the campaign.
     *
     * <p>This is the highest-ranking unit member not already serving as commander or second-in-command.</p>
     *
     * @param campaign        the current campaign
     * @param commander       the current commander
     * @param secondInCommand the second-in-command individual
     *
     * @return the personnel member ranked third, or {@code null} if none exists
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable Person getThirdInCommand(Campaign campaign, @Nullable Person commander,
          @Nullable Person secondInCommand) {
        Person thirdInCommand = null;

        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
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
