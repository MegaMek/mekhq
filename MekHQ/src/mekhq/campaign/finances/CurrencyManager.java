/*
 * CurrencyManager.java
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

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import org.joda.money.CurrencyUnitDataProvider;
import org.joda.money.format.MoneyFormatter;
import org.joda.money.format.MoneyFormatterBuilder;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Main class used to handle all money and currency information.
 * Currency information is loaded when this class is first constructed.
 *
 * There should be only one instance of this class.
 *
 * @author Vicente Cartas Espinel <vicente.cartas at outlook.com>
 *
 */
public class CurrencyManager extends CurrencyUnitDataProvider {
    private static final CurrencyManager instance = new CurrencyManager();

    private Campaign campaign;
    private ArrayList<Currency> currencies;
    private HashMap<String, String> currencyCodeToNameMap;
    private HashMap<String, String> currencyCodeToSymbolMap;
    private Currency backupCurrency;

    private MoneyFormatter xmlMoneyFormatter;
    private MoneyFormatter uiAmountPrinter;
    private MoneyFormatter uiAmountAndSymbolPrinter;
    private MoneyFormatter uiAmountAndNamePrinter;

    private CurrencyManager() {
        this.currencies = new ArrayList<>();
        this.currencyCodeToNameMap = new HashMap<>();
        this.currencyCodeToSymbolMap = new HashMap<>();

        this.backupCurrency = new Currency(
                "CSB",
                -1,
                0,
                "ComStar bill",
                "C-Bill",
                2835,
                999999,
                true,
                true);

        this.createFormatters();
    }

    public static CurrencyManager getInstance() {
        return instance;
    }

    public void loadCurrencies() {
        this.registerCurrencies();
    }

    public void setCampaign(Campaign campaign) {
        assert campaign != null;
        this.campaign = campaign;
    }

    MoneyFormatter getXmlMoneyFormatter() {
        return this.xmlMoneyFormatter;
    }

    MoneyFormatter getUiAmountPrinter() {
        return this.uiAmountPrinter;
    }

    MoneyFormatter getUiAmountAndSymbolPrinter() {
        return this.uiAmountAndSymbolPrinter;
    }

    MoneyFormatter getUiAmountAndNamePrinter() {
        return this.uiAmountAndNamePrinter;
    }

    Currency getDefaultCurrency() {
        if (campaign != null) {
            HashMap<String, Currency> possibleCurrencies = new HashMap<>();

            // Use the default currency in this time period, if it exists
            for (Currency currency : this.currencies) {
                if ((this.campaign.getGameYear() >= currency.getStartYear()) &&
                        (this.campaign.getGameYear() <= currency.getEndYear())) {

                    if (currency.getIsDefault()) {
                        return currency;
                    }

                    possibleCurrencies.put(currency.getCode(), currency);
                }
            }

            // Use the currency of the Faction in any of our contracts, if it exists
            for (Contract contract : this.campaign.getActiveContracts()) {
                if (contract instanceof AtBContract) {
                    Currency currency = possibleCurrencies.getOrDefault(
                            Faction.getFaction(((AtBContract)contract).getEmployerCode()).getCurrencyCode(),
                            null);

                    if (currency != null) {
                        return currency;
                    }
                }
            }

            // Use the currency of one of the factions in the planet where the unit is deployed, if it exists
            Planet planet = campaign.getCurrentPlanet();
            if (planet != null) {
                Set<Faction> factions = planet.getFactionSet(new DateTime(campaign.getDate()));
                for (Faction faction : factions) {
                    Currency currency = possibleCurrencies.getOrDefault(faction.getCurrencyCode(), null);
                    if (currency != null) {
                        return currency;
                    }
                }
            }
        }

        // No currency found, return the backup
        return this.backupCurrency;
    }

    @Override
    protected void registerCurrencies() {
        final String METHOD_NAME = "registerCurrencies()"; //$NON-NLS-1$

        MekHQ.getLogger().log(
                CurrencyManager.class,
                METHOD_NAME,
                LogLevel.INFO,
                "Starting load currency information from XML..."); //$NON-NLS-1$

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            try (FileInputStream xmlFile = new FileInputStream("data/universe/currencies.xml")){
                Document xmlDoc = db.parse(xmlFile);

                Element root = xmlDoc.getDocumentElement();
                root.normalize();
                NodeList currencies = root.getElementsByTagName("currency");


                for (int i = 0; i < currencies.getLength(); i++) {
                    String name = "", code = "", symbol = "";
                    int numericCurrencyCode = -1, decimalPlaces = 0, startYear = Integer.MAX_VALUE, endYear = Integer.MIN_VALUE;
                    boolean isDefault = false, isBackup = false;

                    NodeList currencyData = currencies.item(i).getChildNodes();
                    for (int j = 0; j < currencyData.getLength(); j++) {
                        Node currencyField = currencyData.item(j);

                        switch (currencyField.getNodeName()) {
                            case "name":
                                name = currencyField.getTextContent();
                                break;
                            case "code":
                                code = currencyField.getTextContent();
                                break;
                            case "symbol":
                                symbol = currencyField.getTextContent();
                                break;
                            case "decimalPlaces":
                                decimalPlaces = Integer.parseInt(currencyField.getTextContent());
                                break;
                            case "numericCurrencyCode":
                                numericCurrencyCode = Integer.parseInt(currencyField.getTextContent());
                                break;
                            case "startYear":
                                startYear = Integer.parseInt(currencyField.getTextContent());
                                break;
                            case "endYear":
                                endYear = Integer.parseInt(currencyField.getTextContent());
                                break;
                            case "isDefault":
                                isDefault = Boolean.parseBoolean(currencyField.getTextContent());
                                break;
                            case "isBackup":
                                isBackup = Boolean.parseBoolean(currencyField.getTextContent());
                                break;
                        }
                    }

                    // Adjust the currency start and end dates if needed by the
                    // start/end dates of the factions that use it
                    for (Faction faction : Faction.getFactions()) {
                        if (faction.getCurrencyCode().equals(code)) {
                            if (faction.getStartYear() < startYear) {
                                startYear = faction.getStartYear();
                            }

                            if (faction.getEndYear() > endYear) {
                                endYear = faction.getEndYear();
                            }
                        }
                    }

                    // Sanity check for dates in case we are still
                    // using the initial values (MAX_VALUE, MIN_VALUE)
                    if (startYear > endYear) {
                        startYear = endYear;
                    }

                    Currency currency = new Currency(
                            code,
                            numericCurrencyCode,
                            decimalPlaces,
                            name,
                            symbol,
                            startYear,
                            endYear,
                            isDefault,
                            isBackup);
                    this.currencies.add(currency);
                    this.currencyCodeToNameMap.put(code, name);
                    this.currencyCodeToSymbolMap.put(code, symbol);

                    if (isBackup) {
                        this.backupCurrency = currency;
                    }
                }
            }

            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    METHOD_NAME,
                    LogLevel.INFO,
                    "Load of currency information complete!"); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().error(CurrencyManager.class, METHOD_NAME, ex);
        }
    }

    private void createFormatters() {
        this.uiAmountPrinter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .toFormatter();

        this.xmlMoneyFormatter = new MoneyFormatterBuilder()
                .append(new XmlMoneyWriter(), new XmlMoneyParser())
                .toFormatter();

        this.uiAmountAndSymbolPrinter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .appendLiteral(" ")
                .append(new CurrencyDataLookupWriter(this.currencyCodeToSymbolMap), null)
                .toFormatter();

        this.uiAmountAndNamePrinter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .appendLiteral(" ")
                .append(new CurrencyDataLookupWriter(this.currencyCodeToNameMap), null)
                .toFormatter();
    }
}
