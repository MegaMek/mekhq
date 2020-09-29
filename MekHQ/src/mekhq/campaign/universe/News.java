/*
 * Copyright (C) 2013-2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

/**
 * Instead of making this a static like Planets, we are just going to reload a years
 * worth of news items at the start of every year, to cut down on memory usage. If this
 * slows things down too much on year turn over we can reconsider
 * @author Jay Lawson
 */
public class News {
    private final static Object LOADING_LOCK = new Object[0];

    // Marshaller / unmarshaller instances
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(NewsItem.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch (JAXBException e) {
            MekHQ.getLogger().error(News.class, e);
        }
    }

    //we need two hashes - one to access by date and the other by an id
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
            MekHQ.getLogger().info(this, "Starting load of news data for " + year + " from XML...");

            // Initialize variables.
            Document xmlDoc;

            try (FileInputStream fis = new FileInputStream("data/universe/news.xml")) {
                // Using factory get an instance of document builder
                DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().error(this, ex);
                return;
            }

            Element newsEle = xmlDoc.getDocumentElement();
            NodeList nl = newsEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML.  At least this cleans it up.
            newsEle.normalize();

            // Okay, lets iterate through the children, eh?
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn = nl.item(x);

                if (wn.getParentNode() != newsEle)
                    continue;

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
                            MekHQ.getLogger().error(this, e);
                            continue;
                        }
                        if (null == newsItem.getDate()) {
                            MekHQ.getLogger().error(this, "The date is null for news Item " + newsItem.getHeadline());
                        }
                        if (!newsItem.isInYear(year)) {
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
            MekHQ.getLogger().info(this, "loaded " + archive.size() + " days of news items for " + year);
        }
    }
}
