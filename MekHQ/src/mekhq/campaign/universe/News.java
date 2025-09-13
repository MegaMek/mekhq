/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Instead of making this a static like Planets, we are just going to reload a years worth of news items at the start of
 * every year, to cut down on memory usage. If this slows things down too much on year turn over we can reconsider
 *
 * @author Jay Lawson
 */
public class News {
    private static final MMLogger logger = MMLogger.create(News.class);

    private final static Object LOADING_LOCK = new Object[0];

    private static Unmarshaller unmarshaller;

    static {
        try {
            JAXBContext context = JAXBContext.newInstance(NewsItem.class);
            // Marshaller / unmarshaller instances
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            // unmarshaller.setEventHandler(new
            // javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch (JAXBException e) {
            logger.error("", e);
        }
    }

    // we need two hashes - one to access by date and the other by an id
    private Map<LocalDate, List<NewsItem>> archive;
    private Map<Integer, NewsItem> news;

    public News(int year, long seed) {
        loadNewsFor(year, seed);
    }

    public NewsItem getNewsItem(int id) {
        synchronized (LOADING_LOCK) {
            return news.get(id);
        }
    }

    public List<NewsItem> fetchNewsFor(LocalDate d) {
        synchronized (LOADING_LOCK) {
            if (archive.containsKey(d)) {
                return archive.get(d);
            }
            return new ArrayList<>();
        }
    }

    public void loadNewsFor(int year, long seed) {
        synchronized (LOADING_LOCK) {
            archive = new HashMap<>();
            news = new HashMap<>();
            int id = 0;
            logger.debug("Starting load of news data for {} from XML...", year);

            // Initialize variables.
            Document xmlDoc;

            try (FileInputStream fis = new FileInputStream("data/universe/news.xml")) {
                // Using factory get an instance of document builder
                DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                logger.error("", ex);
                return;
            }

            Element newsEle = xmlDoc.getDocumentElement();
            NodeList nl = newsEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            newsEle.normalize();

            // Okay, lets iterate through the children, eh?
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn = nl.item(x);

                if (!wn.getParentNode().equals(newsEle)) {
                    continue;
                }

                int xc = wn.getNodeType();

                if (xc == Node.ELEMENT_NODE) {
                    // This is what we really care about.
                    // All the meat of our document is in this node type, at this
                    // level.
                    // Okay, so what element is it?
                    String xn = wn.getNodeName();

                    if (xn.equalsIgnoreCase("newsItem")) {
                        NewsItem newsItem;
                        try {
                            newsItem = (NewsItem) unmarshaller.unmarshal(wn);
                        } catch (JAXBException e) {
                            logger.error("", e);
                            continue;
                        }
                        if (StringUtility.isNullOrBlank(newsItem.getHeadline())) {
                            logger.error("Null or empty headline for a news item");
                            continue;
                        } else if (null == newsItem.getDate()) {
                            logger.error("The date is null for news Item {}", newsItem.getHeadline());
                            continue;
                        } else if (StringUtility.isNullOrBlank(newsItem.getDescription())) {
                            logger.error("Null or empty headline for a news item");
                            continue;
                        } else if (!newsItem.isInYear(year)) {
                            continue;
                        }
                        List<NewsItem> items;
                        newsItem.finalizeDate();
                        if (null == archive.get(newsItem.getDate())) {
                            items = new ArrayList<>();
                        } else {
                            items = archive.get(newsItem.getDate());
                        }
                        items.add(newsItem);
                        archive.put(newsItem.getDate(), items);
                        newsItem.setId(id);
                        news.put(id++, newsItem);
                    }
                }
            }
            logger.debug("Loaded {} days of news items for {}", archive.size(), year);
        }
    }
}
