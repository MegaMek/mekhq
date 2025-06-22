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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionCensureLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.MHQInternationalization;

public class FactionCensureDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureDialog";

    private final static String BUTTON_KEY_POSITIVE = "FactionCensureEvent.button.positive.";
    private final static String BUTTON_KEY_NEUTRAL = "FactionCensureEvent.button.neutral.";
    private final static String BUTTON_KEY_NEGATIVE = "FactionCensureEvent.button.negative.";
    private final static String BUTTON_KEY_ROGUE = "FactionCensureEvent.button.rogue.";
    private final static String BUTTON_KEY_SEPPUKU = "FactionCensureEvent.button.seppuku.";

    private final static String DIALOG_KEY_IN_CHARACTER = "FactionCensureEvent.inCharacter.";
    private final static String DIALOG_KEY_AFFIX_CLAN = "clan";
    private final static String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";

    private final static String DIALOG_KEY_OUT_OF_CHARACTER = "FactionCensureEvent.outOfCharacter.";

    private final static String KEY_AFFIX_WARNING = "warning";
    private final static String KEY_AFFIX_COMMANDER_RETIREMENT = "retirement";
    private final static String KEY_AFFIX_COMMANDER_IMPRISONMENT = "imprisonment";
    private final static String KEY_AFFIX_LEADERSHIP_REPLACEMENT = "replacement";
    private final static String KEY_AFFIX_DISBAND = "disband";

    private final static String DRACONIS_COMBINE = "DC";


    private final Campaign campaign;
    private int dialogChoiceIndex = 0;

    public int getDialogChoiceIndex() {
        return dialogChoiceIndex;
    }

    public FactionCensureDialog(final Campaign campaign, final FactionCensureLevel censureLevel,
          Person mostSeniorCharacter) {
        this.campaign = campaign;

        String contextKey = switch (censureLevel) {
            case WARNING -> KEY_AFFIX_WARNING;
            case COMMANDER_RETIREMENT -> KEY_AFFIX_COMMANDER_RETIREMENT;
            case COMMANDER_IMPRISONMENT -> KEY_AFFIX_COMMANDER_IMPRISONMENT;
            case LEADERSHIP_REPLACEMENT -> KEY_AFFIX_LEADERSHIP_REPLACEMENT;
            case DISBAND -> KEY_AFFIX_DISBAND;
            default -> null;
        };

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(campaign),
              null,
              getInCharacterMessage(contextKey),
              getDialogOptions(contextKey, mostSeniorCharacter),
              getOutOfCharacterMessage(),
              null,
              true);
        dialogChoiceIndex = dialog.getDialogChoice();
    }

    public Person getSpeaker(Campaign campaign) {
        Faction faction = campaign.getFaction();
        String factionCode = faction.getShortName();

        Person speaker = campaign.newPerson(PersonnelRole.MEKWARRIOR, factionCode, Gender.RANDOMIZE);
        String rankSystemCode = "SLDF";
        if (faction.isClan()) {
            rankSystemCode = "CLAN";

            Bloodname bloodname = Bloodname.randomBloodname(factionCode, Phenotype.MEKWARRIOR, campaign.getGameYear());

            if (bloodname != null) {
                speaker.setBloodname(bloodname.getName());
            }
        }

        final RankSystem rankSystem = Ranks.getRankSystemFromCode(rankSystemCode);

        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return speaker;
        }

        speaker.setRankSystem(rankValidator, rankSystem);
        speaker.setRank(38);

        return speaker;
    }

    public String getInCharacterMessage(String contextKey) {
        String commanderAddress = campaign.getCommanderAddress(false);
        String campaignName = campaign.getName();
        Faction campaignFaction = campaign.getFaction();
        String campaignFactionCode = campaignFaction.getShortName();

        String dialog = getFormattedTextAt(RESOURCE_BUNDLE,
              DIALOG_KEY_IN_CHARACTER + contextKey + '.' + campaignFactionCode, commanderAddress, campaignName);

        if (MHQInternationalization.isResourceKeyValid(dialog)) {
            return dialog;
        }

        String affixKey;
        if (campaignFaction.isClan()) {
            affixKey = DIALOG_KEY_AFFIX_CLAN;
        } else if (campaignFaction.isPeriphery()) {
            affixKey = DIALOG_KEY_AFFIX_PERIPHERY;
        } else {
            affixKey = DIALOG_KEY_AFFIX_INNER_SPHERE;
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, DIALOG_KEY_IN_CHARACTER + contextKey + '.' + affixKey,
              commanderAddress, campaignName);
    }

    public List<String> getDialogOptions(String contextKey, Person mostSeniorCharacter) {
        List<String> options = new ArrayList<>();

        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_POSITIVE + contextKey));
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_NEUTRAL + contextKey));
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_NEGATIVE + contextKey));
        options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_ROGUE + contextKey));

        boolean isDraconisCombineCampaign = Objects.equals(campaign.getFaction().getShortName(), DRACONIS_COMBINE);
        if (mostSeniorCharacter != null && isDraconisCombineCampaign) {
            options.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY_SEPPUKU + KEY_AFFIX_WARNING));
        }

        return options;
    }

    public String getOutOfCharacterMessage() {
        return null;
    }
}
