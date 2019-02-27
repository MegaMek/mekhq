/*
 * Currency.java
 *
 * Copyright (c) 2019 Vicente Cartas Espinel <vicente.cartas at outlook.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.finances;

import org.joda.money.CurrencyUnit;

/**
 * This class represents a currency that will be associated
 * with monetary amounts.
 *
 * @author Vicente Cartas Espinel <vicente.cartas at outlook.com>
 *
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

    public Currency(
            String code,
            int numericCurrencyCode,
            int decimalPlaces,
            String name,
            String symbol,
            int startYear,
            int endYear,
            boolean isDefault,
            boolean isBackup) {
        assert code != null &&
                code.length() == 3 &&
                Character.isUpperCase(code.charAt(0)) &&
                Character.isUpperCase(code.charAt(1)) &&
                Character.isUpperCase(code.charAt(2));
        assert numericCurrencyCode >= -1 && numericCurrencyCode <= 999;
        assert decimalPlaces >=0 && decimalPlaces <=30;
        assert name != null && !name.trim().isEmpty();
        assert symbol != null && !symbol.trim().isEmpty();
        assert startYear <= endYear;

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

    @Override
    public String toString() {
        return this.wrapped.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Currency) {
            return this.wrapped.equals(((Currency)obj).wrapped);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }
}
