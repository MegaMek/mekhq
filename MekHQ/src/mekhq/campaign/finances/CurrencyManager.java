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
import org.joda.money.CurrencyUnit;
import org.joda.money.CurrencyUnitDataProvider;
import org.joda.money.format.MoneyFormatter;
import org.joda.money.format.MoneyFormatterBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

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
    private class DefaultCurrency {
        CurrencyUnit currency;
        int start;
        int end;

        DefaultCurrency(CurrencyUnit currency, int start, int end) {
            this.currency = currency;
            this.start = start;
            this.end = end;
        }
    }

    private static final CurrencyManager instance = new CurrencyManager();
    private boolean initialized;

    private Campaign campaign;
    private HashMap<String, String> currencyNames;
    private HashMap<String, String> currencySymbols;
    private ArrayList<DefaultCurrency> defaultCurrencies;

    private MoneyFormatter xmlMoneyFormatter;
    private MoneyFormatter uiAmountPrinter;
    private MoneyFormatter uiAmountAndSymbolPrinter;
    private MoneyFormatter uiAmountAndNamePrinter;

    private CurrencyManager() {
        this.initialized = false;
    }

    public void initialize(Campaign campaign) {
        assert campaign != null;

        this.campaign = campaign;
        this.currencyNames = new HashMap<>();
        this.currencySymbols = new HashMap<>();
        this.defaultCurrencies = new ArrayList<>();

        this.registerCurrencies();

        this.uiAmountPrinter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .toFormatter();

        this.xmlMoneyFormatter = new MoneyFormatterBuilder()
                .append(new XmlMoneyWriter(), new XmlMoneyParser())
                .toFormatter();

        this.uiAmountAndSymbolPrinter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .appendLiteral(" ")
                .append(new CurrencyDataLookupWriter(this.currencySymbols), null)
                .toFormatter();

        this.uiAmountAndNamePrinter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .appendLiteral(" ")
                .append(new CurrencyDataLookupWriter(this.currencyNames), null)
                .toFormatter();

        this.initialized = true;
    }

    public static CurrencyManager getInstance() {
        return instance;
    }

    MoneyFormatter getXmlMoneyFormatter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "GetXmlMoneyFormatter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.xmlMoneyFormatter;
    }

    MoneyFormatter getUiAmountPrinter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "getUiAmountPrinter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.uiAmountPrinter;
    }

    MoneyFormatter getUiAmountAndSymbolPrinter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "GetShortUiMoneyFormatter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.uiAmountAndSymbolPrinter;
    }

    MoneyFormatter getUiAmountAndNamePrinter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "GetLongUiMoneyFormatter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.uiAmountAndNamePrinter;
    }

    CurrencyUnit getDefaultCurrency() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "getDefaultCurrency",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        for (DefaultCurrency defaultCurrency : this.defaultCurrencies) {
            if ((this.campaign.getGameYear() >= defaultCurrency.start) &&
                    (this.campaign.getGameYear() <= defaultCurrency.end )) {
                return defaultCurrency.currency;
            }
        }

        // No default currency, get the main currency of the first faction of the planet
        //this.campaign.getCurrentPlanet().getFactions(campaign.getDate())

        MekHQ.getLogger().log(
                CurrencyManager.class,
                "getDefaultCurrency",
                LogLevel.FATAL,
                "No default currently defined for year " + this.campaign.getGameYear()); //$NON-NLS-1$

        return null;
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
            Document xmlDoc = db.parse(new FileInputStream("data/universe/currencies.xml"));

            Element root = xmlDoc.getDocumentElement();
            root.normalize();
            NodeList currencies = root.getElementsByTagName("currency");


            for (int i = 0; i < currencies.getLength(); i++) {
                String name = "", code = "", symbol = "";
                int decimalPlaces = 0, defaultStart = -1, defaultEnd = -1;

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
                        case "defaultStart":
                            defaultStart = Integer.parseInt(currencyField.getTextContent());
                            break;
                        case "defaultEnd":
                            defaultEnd = Integer.parseInt(currencyField.getTextContent());
                            break;
                    }
                }

                CurrencyUnit currency = CurrencyUnit.registerCurrency(code, -1, decimalPlaces, true);
                this.currencyNames.put(code, name);
                this.currencySymbols.put(code, symbol);

                if (defaultStart != -1 && defaultEnd != -1) {
                    this.defaultCurrencies.add(new DefaultCurrency(currency, defaultStart, defaultEnd));
                }
            }

            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    METHOD_NAME,
                    LogLevel.INFO,
                    "Load of currency information complete!"); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().error(CurrencyManager.class, METHOD_NAME, ex);

            // There was an error loading the currencies data, create a default currency
            CurrencyUnit defaultCurrency = CurrencyUnit.registerCurrency("CSB", -1, 0, true);
            this.currencyNames.put("CSB", "ComStar bill");
            this.currencySymbols.put("CSB", "C-Bill");
            this.defaultCurrencies.add(new DefaultCurrency(defaultCurrency, 0, 9999));
        }
    }
}
