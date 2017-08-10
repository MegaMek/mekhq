package mekhq.campaign.universe;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;

/**
 * Instead of making this a static like Planets, we are just going to reload a years
 * worth of news items at the start of every year, to cut down on memory usage. If this 
 * slows things down too much on year turn over we can reconsider
 * @author Jay Lawson
 *
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
        } catch(JAXBException e) {
            MekHQ.getLogger().log(News.class, "<init>", e);
        }
    }

    //we need two hashes - one to access by date and the other by an id
    private Map<DateTime, List<NewsItem>> archive;
    private Map<Integer, NewsItem> news;
    
    public News(int year, long seed) {
        loadNewsFor(year, seed);
    }
    
    public NewsItem getNewsItem(int id) {
        synchronized(LOADING_LOCK) {
            return news.get(id);
        }
    }
    
    public List<NewsItem> fetchNewsFor(DateTime d) {
        synchronized(LOADING_LOCK) {
            if(archive.containsKey(d)) {
                return archive.get(d);
            }
            return new ArrayList<>();
        }
    }
    
    public void loadNewsFor(int year, long seed) {
        final String METHOD_NAME = "loadNewsFor(int,long)"; //$NON-NLS-1$
        synchronized(LOADING_LOCK) {
            archive = new HashMap<>();
            news = new HashMap<>();
            int id = 0;
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                    "Starting load of news data for " + year + " from XML..."); //$NON-NLS-1$
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;
        
            
            try(FileInputStream fis = new FileInputStream("data/universe/news.xml")) {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();
        
                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex);
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
                        NewsItem newsItem = null;
                        try {
                            newsItem = (NewsItem) unmarshaller.unmarshal(wn);
                        } catch(JAXBException e) {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, e);
                            continue;
                        }
                        if(null == newsItem.getDate()) {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                    "The date is null for news Item " + newsItem.getHeadline()); //$NON-NLS-1$
                        }
                        if(!newsItem.isInYear(year)) {
                            continue;
                        }
                        List<NewsItem> items;
                        newsItem.finalizeDate(seed);
                        if(null == archive.get(newsItem.getDate())) {
                            items = new ArrayList<NewsItem>();
                            items.add(newsItem);
                            archive.put(newsItem.getDate(), items);
                        } else {
                            items = archive.get(newsItem.getDate());
                            items.add(newsItem);
                            archive.put(newsItem.getDate(), items);
                        }
                        newsItem.setId(id);
                        news.put(id, newsItem);
                        ++ id;
                    }
                }
            }   
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                    "loaded " + archive.size() + " days of news items for " + year); //$NON-NLS-1$
        }
    }
}