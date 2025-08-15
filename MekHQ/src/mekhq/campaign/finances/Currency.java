/*
 * Copyright (c) 2019 Vicente Cartas Espinel (vicente.cartas at outlook.com). All rights reserved.
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances;

import org.joda.money.CurrencyUnit;

/**
 * This class represents a currency that will be associated with monetary amounts.
 *
 * @author Vicente Cartas Espinel (vicente.cartas at outlook.com)
 */
public class Currency {
    private CurrencyUnit wrapped;

    private String code; // COMES FROM CURRENCIES.XML
    private int decimalCode; // COMES FROM CURRENCIES.XML
    private int decimalPlaces; // COMES FROM CURRENCIES.XML

    private String name; // COMES FROM CURRENCIES.XML
    private String symbol; // COMES FROM CURRENCIES.XML
    private boolean isDefault; // COMES FROM CURRENCIES.XML
    private boolean isBackup; // COMES FROM CURRENCIES.XML

    private int startYear; // COMES FROM FACTIONS.XML
    private int endYear; // COMES FROM FACTIONS.XML

    public Currency(String code, int numericCurrencyCode, int decimalPlaces, String name,
          String symbol, int startYear, int endYear, boolean isDefault,
          boolean isBackup) {
        this.wrapped = CurrencyUnit.registerCurrency(code, numericCurrencyCode, decimalPlaces, true);
        this.name = name.trim();
        this.symbol = symbol.trim();
        this.startYear = startYear;
        this.endYear = endYear;
        this.isDefault = isDefault;
        this.isBackup = isBackup;
    }

    CurrencyUnit getCurrencyUnit() {
        return this.wrapped;
    }

    String getCode() {
        return this.code;
    }

    int getStartYear() {
        return this.startYear;
    }

    int getEndYear() {
        return this.endYear;
    }

    boolean getIsDefault() {
        return this.isDefault;
    }

    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public String toString() {
        return this.wrapped.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Currency) {
            return this.wrapped.equals(((Currency) obj).wrapped);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }
}
