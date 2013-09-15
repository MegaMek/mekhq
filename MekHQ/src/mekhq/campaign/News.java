package mekhq.campaign;

import java.io.FileInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.MekHQ;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Instead of making this a static like Planets, wer are just going to reload a years
 * worth of news items at the start of every year, to cut down on memory usage. If this 
 * slows things down too much on year turn over we can reconsider
 * @author Jay Lawson
 *
 */
public class News {
    
    private Hashtable<Date, ArrayList<NewsItem>> archive;

    public News(int year) {
        loadNewsFor(year);
    }
    
    public ArrayList<NewsItem> fetchNewsFor(Date d) {
        if(null != archive.get(d)) {
            return archive.get(d);
        }
        return new ArrayList<NewsItem>();
    }
    
    
    public void loadNewsFor(int year) {
        archive = new Hashtable<Date, ArrayList<NewsItem>>();
        MekHQ.logMessage("Starting load of news data for " + year + " from XML...");
        // Initialize variables.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;
    
        
        try {
            FileInputStream fis = new FileInputStream("data/universe/news.xml");
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
    
            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.logError(ex);
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
                        newsItem = NewsItem.getNewsItemFromXML(wn);
                    } catch (DOMException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        continue;
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        continue;
                    }
                    if(null == newsItem.getDate()) {
                        MekHQ.logMessage("The date is null for news Item " + newsItem.getHeadline());
                    }
                    int y = newsItem.getYear();
                    if(y != year) {
                        continue;
                    }
                    ArrayList<NewsItem> items;
                    if(null == archive.get(newsItem.getDate())) {
                        items = new ArrayList<NewsItem>();
                        items.add(newsItem);
                        archive.put(newsItem.getDate(), items);
                    } else {
                        items = archive.get(newsItem.getDate());
                        items.add(newsItem);
                        archive.put(newsItem.getDate(), items);
                    }
                    
                }
            }
        }   
        MekHQ.logMessage("loaded " + archive.size() + " days of news items for " + year);
    }
    
}