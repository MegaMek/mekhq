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
package mekhq.gui.campaignOptions.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;

/**
 * Enumeration representing the categories of personnel allowed to make procurement checks in a campaign.
 *
 * <p>This enum defines specific options for filtering or restricting the types of personnel
 * eligible to perform procurement-related tasks:</p>
 * <ul>
 *   <li><b>NONE</b>: No personnel can make procurement checks.</li>
 *   <li><b>ALL</b>: Any personnel, regardless of their role, can make procurement checks.</li>
 *   <li><b>SUPPORT</b>: Only personnel with non-combat support roles (e.g., Admin, Techs) can make
 *       procurement checks.</li>
 *   <li><b>LOGISTICS</b>: Only personnel with the Admin/Logistics role are allowed to make
 *       procurement checks.</li>
 * </ul>
 */
public enum ProcurementPersonnelPick {
    NONE, ALL, SUPPORT, LOGISTICS;

    final private String RESOURCE_BUNDLE = "mekhq.resources.ProcurementPersonnelPick";

    private final String label;
    private final String description;

    ProcurementPersonnelPick() {
        this.label = this.generateLabel();
        this.description = this.generateDescription();
    }

    public String getLabel() {
        return this.label;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * Retrieves the label associated with the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the relevant localized string.</p>
     *
     * @return the localized label string corresponding to the enumeration value.
     */
    private String generateLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the description associated with the current enum value.
     *
     * <p>This method constructs a resource key by appending the enum's name with the suffix
     * {@code .description} and uses this key to fetch a formatted description from the specified resource bundle.</p>
     *
     * @return The formatted description text for the current enum value, or a fallback value if the key is not found.
     */
    private String generateDescription() {
        final String RESOURCE_KEY = name() + ".description";
        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Determines if a person is ineligible to perform procurement activities based on their role and the specified
     * acquisition category.
     *
     * <p>This method evaluates the provided {@link ProcurementPersonnelPick} category to filter out
     * individuals who do not meet the requirements for procurement. It uses the following criteria:</p>
     * <ul>
     *   <li>{@code NONE}: The person is always ineligible.</li>
     *   <li>{@code ALL}: The person is always eligible, and no filtering is applied.</li>
     *   <li>{@code SUPPORT}: The person must have a support role to be considered eligible.</li>
     *   <li>{@code LOGISTICS}: The person must be a logistics administrator, either through
     *       their primary or secondary role, to be eligible.</li>
     * </ul>
     *
     * @param person              The {@link Person} whose eligibility for procurement is to be determined.
     * @param acquisitionCategory The {@link ProcurementPersonnelPick} category specifying the procurement eligibility
     *                            requirements.
     *
     * @return {@code true} if the person is ineligible to perform procurement based on the specified acquisition
     *       category, {@code false} otherwise.
     */
    public static boolean isIneligibleToPerformProcurement(Person person,
          ProcurementPersonnelPick acquisitionCategory) {
        switch (acquisitionCategory) {
            case NONE -> {
                return true;
            }
            case ALL -> {
                return false;
            }
            case SUPPORT -> {
                if (!person.hasSupportRole(true)) {
                    return true;
                }
            }
            case LOGISTICS -> {
                if (!person.getPrimaryRole().isAdministratorLogistics() &&
                          !person.getSecondaryRole().isAdministratorLogistics()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts the specified string into its corresponding {@link ProcurementPersonnelPick} enum value. The method
     * attempts to interpret the string as either the name of an enum constant or an ordinal value of the enum. If the
     * conversion fails, the method logs an error and returns the default value {@code NONE}.
     *
     * @param text the string to be converted into an {@link ProcurementPersonnelPick} enum value. It can be the name of
     *             the enum constant or its ordinal value as a string.
     *
     * @return the corresponding {@link ProcurementPersonnelPick} enum constant if the string matches a name or ordinal
     *       value, otherwise {@code NONE}.
     */
    public static ProcurementPersonnelPick fromString(String text) {
        try {
            return ProcurementPersonnelPick.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            return ProcurementPersonnelPick.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {
        }


        MMLogger logger = MMLogger.create(ProcurementPersonnelPick.class);
        logger.error("Unknown ProcurementPersonnelPick ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
