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

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog for managing the replacement of limbs in a campaign. This immersive dialog provides an in-character message
 * and appropriate buttons based on the scenario, such as availability of doctors and sufficient funds.
 *
 * <p>The dialog uses localized resources to display messages and creates responses based
 * on campaign and patient details.</p>
 */
public class ReplacementLimbDialog {
    final private static String RESOURCE_BUNDLE = "mekhq.resources.ReplacementLimbDialog";

    final private Campaign campaign;
    private int choiceIndex = 0;

    public int getChoiceIndex() {
        return choiceIndex;
    }

    /**
     * Constructor to create a Replacement Limb Dialog.
     *
     * @param campaign        The associated {@link Campaign} object, holding the state of the game.
     * @param suitableDoctors A list of {@link Person} objects representing doctors qualified to perform the replacement
     *                        procedure. May be empty if no suitable doctors are available.
     * @param patient         The {@link Person} who is receiving the limb replacement.
     * @param cost            The {@link Money} cost of the limb replacement procedure.
     */
    public ReplacementLimbDialog(Campaign campaign, List<Person> suitableDoctors, Person patient, Money cost) {
        this.campaign = campaign;

        final boolean isPlanetside = campaign.getLocation().isOnPlanet();
        final boolean hasQualifiedDoctors = !suitableDoctors.isEmpty();
        final boolean hasSufficientFunds = campaign.getFunds().isGreaterOrEqualThan(cost);

        String inCharacterMessage = createInCharacterMessage(isPlanetside,
              hasQualifiedDoctors,
              campaign.getCommanderAddress(),
              patient,
              cost,
              hasSufficientFunds);

        List<String> buttons = createButtons(hasQualifiedDoctors, isPlanetside, hasSufficientFunds);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              inCharacterMessage,
              buttons,
              null,
              null,
              false);

        choiceIndex = dialog.getDialogChoice();
    }

    /**
     * Creates the buttons for the dialog based on the availability of qualified doctors, whether the procedure is
     * planetside, and if sufficient funds are available.
     *
     * <p>The behavior of the button creation is determined by the following conditions:</p>
     * <ul>
     *     <li>If sufficient funds are unavailable, or if there is no qualified doctor and it is not planetside,
     *         only an "Understood" button is created.</li>
     *     <li>Otherwise, both "Decline" and "Accept" buttons are created.</li>
     * </ul>
     *
     * @param hasQualifiedDoctor {@code true} if a qualified doctor is available to perform the procedure, {@code false}
     *                           otherwise.
     * @param isPlanetside       {@code true} if the campaign is currently planetside, {@code false} otherwise.
     * @param hasSufficientFunds {@code true} if the campaign has enough funds to cover the procedure, {@code false}
     *                           otherwise.
     *
     * @return A {@link List} of {@link String} objects representing the dialog buttons.
     */
    private static List<String> createButtons(boolean hasQualifiedDoctor, boolean isPlanetside,
          boolean hasSufficientFunds) {
        List<String> buttons = new ArrayList<>();

        if (!hasSufficientFunds || (!hasQualifiedDoctor && !isPlanetside)) {
            buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "understood.button"));
            return buttons;
        }

        buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "decline.button"));
        buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "accept.button"));

        return buttons;
    }

    /**
     * Creates an in-character message for the dialog based on various conditions related to planet location, doctor
     * availability, and funds.
     *
     * @param isPlanetside        {@code true} if the campaign is on a planet, {@code false} otherwise.
     * @param hasQualifiedDoctors {@code true} if there are qualified doctors available, {@code false} otherwise.
     * @param commanderAddress    The address for the campaign commander.
     * @param patient             The {@link Person} receiving the procedure.
     * @param cost                The {@link Money} cost of the procedure.
     * @param hasSufficientFunds  {@code true} if enough funds are available, {@code false} otherwise.
     *
     * @return A {@link String} containing the localized in-character message for the dialog.
     */
    private static String createInCharacterMessage(boolean isPlanetside, boolean hasQualifiedDoctors,
          String commanderAddress, Person patient, Money cost,
          boolean hasSufficientFunds) {
        String keyAddendum = "normal";

        if (!hasQualifiedDoctors && hasSufficientFunds) {
            if (isPlanetside) {
                keyAddendum = "noSurgeonOnPlanet";
            } else {
                keyAddendum = "noSurgeonInTravel";
            }
        }

        if (!hasSufficientFunds) {
            keyAddendum = "insufficientFunds";
        }

        final String KEY = "message.inCharacter." + keyAddendum;

        return getFormattedTextAt(RESOURCE_BUNDLE,
              KEY,
              commanderAddress,
              patient.getFullTitle(),
              cost.toAmountString());
    }

    /**
     * Determines the speaker for the dialog, prioritizing the senior doctor among the active personnel. If no doctors
     * are available, the campaign's senior HR administrator is selected as the speaker.
     *
     * @return The speaker for the dialog. This is the most senior doctor, if available; otherwise, it is the campaign's
     *       senior HR administrator.
     */
    private Person getSpeaker() {
        Person seniorDoctor = null;

        for (Person person : campaign.getActivePersonnel(false)) {
            if (person.isDoctor()) {
                if (person.outRanksUsingSkillTiebreaker(campaign, seniorDoctor)) {
                    seniorDoctor = person;
                }
            }
        }

        if (seniorDoctor != null) {
            return seniorDoctor;
        } else {
            return campaign.getSeniorAdminPerson(HR);
        }
    }
}
