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
package mekhq.gui.dialog;

import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog that displays a notification to the commander about personnel who have advanced via vocational experience
 * points (XP).
 *
 * <p>This dialog is primarily used to recognize individuals who have gained XP
 * as part of the campaign's vocational experience system. It notifies the user, displays relevant information in
 * character, and allows quick navigation to the personnel records via hyperlinks.</p>
 */
public class VocationalExperienceAwardDialog extends ImmersiveDialogSimple {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.VocationalExperienceAwardDialog";

    /**
     * Constructs the {@link VocationalExperienceAwardDialog}.
     *
     * <p>This dialog leverages the superclass {@link ImmersiveDialogSimple} to provide
     * a visually immersive and interactive interface. It includes a left-side speaker and displays a message detailing
     * personnel advancements.</p>
     *
     * @param campaign the {@link Campaign} to which this dialog is tied
     */
    public VocationalExperienceAwardDialog(Campaign campaign) {
        super(campaign, campaign.getSeniorAdminPerson(HR),
              null,
              createInCharacterMessage(campaign), null,
              createOutOfCharacterMessage(campaign),
              null, false);

        setModal(false);
        setAlwaysOnTop(true);
    }

    /**
     * Constructs the in-character message to be displayed in the dialog.
     *
     * <p>This message addresses the commander and lists all personnel who have advanced
     * in XP. The list of personnel is displayed in an HTML-styled table, where each person's name is hyperlinked to
     * allow quick access to their record.</p>
     *
     * @param campaign the {@link Campaign} containing the data for the personnel
     *
     * @return a string representing the in-character message in HTML format
     */
    private static String createInCharacterMessage(Campaign campaign) {
        List<Person> personnelWhoAdvanced = campaign.getPersonnelWhoAdvancedInXP();

        String commanderAddress = campaign.getCommanderAddress();

        StringBuilder message = new StringBuilder();
        message.append(commanderAddress);
        message.append(getFormattedTextAt(RESOURCE_BUNDLE, "dialog.message"));

        // Create a table to hold the personnel
        message.append("<br><table style='width:100%; text-align:left;'>");

        for (int i = 0; i < personnelWhoAdvanced.size(); i++) {
            if (i % 2 == 0) {
                message.append("<tr>");
            }

            // Add the person in a column
            Person person = personnelWhoAdvanced.get(i);
            message.append("<td>- ").append(person.getHyperlinkedFullTitle()).append("</td>");

            if ((i + 1) % 2 == 0 || i == personnelWhoAdvanced.size() - 1) {
                message.append("</tr>");
            }
        }

        message.append("</table>");
        return message.toString();
    }

    /**
     * Constructs an out-of-character (OOC) message to provide context for XP advancements based on the campaign's
     * settings and current state.
     *
     * <p>The generated message includes details about the idle XP gained, as determined by
     * the campaign's configuration and active contracts. If the campaign has an active contract that is not a garrison
     * type (when using AtB settings), or simply has an active contract otherwise, the default XP advancement rate is
     * doubled.</p>
     *
     * <p>This method integrates campaign options such as:</p>
     * <ul>
     *     <li>The default vocational XP advancement rate ({@code VocationalXP})</li>
     *     <li>The status of whether the campaign is using the AtB (Against the Bot)
     *         system ({@code isUseAtB})</li>
     *     <li>The type of active employment contracts (e.g., garrison or non-garrison)</li>
     * </ul>
     *
     * <p>This information is formatted into a predefined message string using localized
     * resource strings.</p>
     *
     * @param campaign the {@link Campaign} containing the current campaign state, settings, and active contracts
     *
     * @return a string representing the out-of-character (OOC) message to be displayed
     */
    private static String createOutOfCharacterMessage(Campaign campaign) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        int advancement = campaignOptions.getVocationalXP();

        if (campaign.hasActiveContract()) {
            if (campaignOptions.isUseAtB()) {
                for (AtBContract contract : campaign.getActiveAtBContracts()) {
                    if (!contract.getContractType().isGarrisonType()) {
                        advancement *= 2;
                        break;
                    }
                }
            } else {
                advancement *= 2;
            }
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, "dialog.ooc", advancement);
    }
}
