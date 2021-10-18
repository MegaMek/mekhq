/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.enums;

import mekhq.campaign.universe.generators.companyGenerators.CompanyGenerationOptions;

public enum CompanyGenerationPanelType {
    OPTIONS,
    PERSONNEL,
    UNITS,
    UNIT,
    SPARES,
    CONTRACTS,
    FINANCES,
    OVERVIEW;

    public CompanyGenerationPanelType getNextPanelType(final CompanyGenerationOptions options) {
        switch (this) {
            case OPTIONS:
                return PERSONNEL;
            case PERSONNEL:
                return UNITS;
            case UNITS:
                return UNIT;
            case UNIT:
                return SPARES;
            case SPARES:
                return options.isSelectStartingContract() ? CONTRACTS
                        : (options.isRandomizeStartingCash() || options.isPayForSetup()) ? FINANCES
                        : OVERVIEW;
            case CONTRACTS:
                return (options.isRandomizeStartingCash() || options.isPayForSetup()) ? FINANCES
                        : OVERVIEW;
            case FINANCES:
            case OVERVIEW:
            default:
                return OVERVIEW;
        }
    }

    public CompanyGenerationPanelType getPreviousPanelType(final CompanyGenerationOptions options) {
        switch (this) {
            case OPTIONS:
            case PERSONNEL:
                return OPTIONS;
            case UNITS:
                return PERSONNEL;
            case UNIT:
                return UNITS;
            case SPARES:
                return UNIT;
            case CONTRACTS:
                return SPARES;
            case FINANCES:
                return options.isSelectStartingContract() ? CONTRACTS : SPARES;
            case OVERVIEW:
            default:
                return (options.isRandomizeStartingCash() || options.isPayForSetup()) ? FINANCES
                        : options.isSelectStartingContract() ? CONTRACTS : SPARES;
        }
    }
}
