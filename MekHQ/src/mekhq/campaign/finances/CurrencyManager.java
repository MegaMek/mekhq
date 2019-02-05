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
import mekhq.campaign.io.CampaignXmlParseException;
import org.joda.money.CurrencyUnit;
import org.joda.money.CurrencyUnitDataProvider;
import org.joda.money.format.MoneyFormatter;
import org.joda.money.format.MoneyFormatterBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    private MoneyFormatter shortUiMoneyFormatter;
    private MoneyFormatter longUiMoneyFormatter;

    private CurrencyManager() {
        this.initialized = false;
    }

    public void initialize(Campaign campaign) throws Exception {
        assert campaign != null;

        this.campaign = campaign;
        this.currencyNames = new HashMap<>();
        this.currencySymbols = new HashMap<>();
        this.defaultCurrencies = new ArrayList<>();

        this.registerCurrencies();

        this.xmlMoneyFormatter = new MoneyFormatterBuilder()
                .append(new XmlMoneyWriter(), new XmlMoneyParser())
                .toFormatter();

        this.shortUiMoneyFormatter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .appendLiteral(" ")
                .append(new CurrencyDataLookupWriter(this.currencySymbols), null)
                .toFormatter();

        this.longUiMoneyFormatter = new MoneyFormatterBuilder()
                .appendAmountLocalized()
                .appendLiteral(" ")
                .append(new CurrencyDataLookupWriter(this.currencyNames), null)
                .toFormatter();

        this.initialized = true;
    }

    public static CurrencyManager getInstance() {
        return instance;
    }

    public MoneyFormatter getXmlMoneyFormatter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "GetXmlMoneyFormatter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.xmlMoneyFormatter;
    }

    public MoneyFormatter getShortUiMoneyFormatter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "GetShortUiMoneyFormatter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.shortUiMoneyFormatter;
    }

    public MoneyFormatter getLongUiMoneyFormatter() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "GetLongUiMoneyFormatter",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        return this.longUiMoneyFormatter;
    }

    public CurrencyUnit getDefaultCurrency() {
        if (!this.initialized) {
            MekHQ.getLogger().log(
                    CurrencyManager.class,
                    "getDefaultCurrency",
                    LogLevel.FATAL,
                    "Attempted to use CurrencyManager before calling initialize"); //$NON-NLS-1$
        }

        for (DefaultCurrency defaultCurrency : this.defaultCurrencies) {
            if ((defaultCurrency.start >= this.campaign.getGameYear()) &&
                    (defaultCurrency.end <= this.campaign.getGameYear())) {
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
    protected void registerCurrencies() throws Exception {
        this.loadFromXmlFile("/build/data/currencies.xml");
    }

    private void loadFromXmlFile(String xmlFile) throws CampaignXmlParseException {
        final String METHOD_NAME = "loadFromXmlFile(String xmlFile)"; //$NON-NLS-1$

        MekHQ.getLogger().log(
                CurrencyManager.class,
                METHOD_NAME,
                LogLevel.INFO,
                "Starting load currency information from XML..."); //$NON-NLS-1$

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            Document xmlDoc = db.parse(new FileInputStream(xmlFile));

            Element root = xmlDoc.getDocumentElement();
            root.normalize();
            NodeList currencies = root.getChildNodes();

            for (int i = 0; i < currencies.getLength(); i++) {
                NodeList currencyData = currencies.item(i).getChildNodes();

                String name = currencyData.item(0).getNodeValue();
                String code = currencyData.item(1).getNodeValue();
                int decimalPlaces = Integer.parseInt(currencyData.item(2).getNodeValue());
                String symbol = currencyData.item(3).getNodeValue();

                CurrencyUnit currency = CurrencyUnit.registerCurrency(code, -1, decimalPlaces, true);
                this.currencyNames.put(code, name);
                this.currencySymbols.put(code, symbol);

                // Check if the currency contains default information
                if (currencyData.getLength() > 4) {
                    int defaultStart = Integer.parseInt(currencyData.item(4).getNodeValue());
                    int defaultEnd = Integer.parseInt(currencyData.item(5).getNodeValue());
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
            throw new CampaignXmlParseException(ex);
        }
    }
}
