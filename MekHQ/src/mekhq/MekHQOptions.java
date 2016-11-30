package mekhq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MekHQOptions {
	private static final MekHQOptions instance = new MekHQOptions();

	public static final String MEKHQ_OPTIONS_FILE_NAME = "mekhqOptions.xml";

	public static final String DATE_TIME_PATTERN_ISO = "yyyy-MM-dd HH:mm:ss";

	public static final String DATE_PATTERN_ISO_LONG = "yyyy-MM-dd";
	public static final String DATE_PATTERN_ISO_SHORT = "yyyy-MM-dd";
	
	public static final String DATE_PATTERN_BIG_ENDIAN_LONG = "yyyy MMMM dd, EEEE";
	public static final String DATE_PATTERN_BIG_ENDIAN_SHORT = "yyyy-MM-dd";
	
	public static final String DATE_PATTERN_MIDDLE_ENDIAN_LONG = "EEEE, MMMM dd yyyy";
	public static final String DATE_PATTERN_MIDDLE_ENDIAN_SHORT = "MM/dd/yyyy";

	public static final String DATE_PATTERN_LITTLE_ENDIAN_LONG = "EEEE, dd MMMM yyyy";
	public static final String DATE_PATTERN_LITTLE_ENDIAN_SHORT = "dd-MM-yyyy";

	private SimpleDateFormat dateFormatDataStorage;
	private SimpleDateFormat dateFormatLong;
	private SimpleDateFormat dateFormatShort;

	public static MekHQOptions getInstance() {
		return instance;
	}

	private MekHQOptions() {
		initWithDefaults();

		// attempt to autoload saved options
		try {
			load();
		} catch (FileNotFoundException e) {
			// if options file does not exist, create it using current defaults
			MekHQ.logMessage("MekHQ options file is missing. Creating a new one.");
			try {
				save();
			} catch (FileNotFoundException e1) {
				// we cannot write here. nothing to be done really.
				MekHQ.logError(e1);
			}
		} catch (Exception e) {
			// the options file is invalid, recreate it using current defauts
			MekHQ.logMessage("MekHQ options file is corrupt. Creating a new one.");
			try {
				save();
			} catch (FileNotFoundException e1) {
				// we cannot write here. nothing to be done really.
				MekHQ.logError(e1);
			}
		}

	}

	private void initWithDefaults() {
		dateFormatDataStorage = new SimpleDateFormat(DATE_TIME_PATTERN_ISO);
		dateFormatLong = new SimpleDateFormat(DATE_PATTERN_BIG_ENDIAN_LONG);
		dateFormatShort = new SimpleDateFormat(DATE_PATTERN_BIG_ENDIAN_SHORT);
	}

	public SimpleDateFormat getDateFormatDataStorage() {
		return dateFormatDataStorage;
	}

	public void setDateFormatDataStorage(SimpleDateFormat dateFormatDataStorage) {
		this.dateFormatDataStorage = dateFormatDataStorage;
	}

	public SimpleDateFormat getDateFormatLong() {
		return dateFormatLong;
	}

	public void setDateFormatLong(SimpleDateFormat dateFormatLong) {
		this.dateFormatLong = dateFormatLong;
	}

	public SimpleDateFormat getDateFormatShort() {
		return dateFormatShort;
	}

	public void setDateFormatShort(SimpleDateFormat dateFormatShort) {
		this.dateFormatShort = dateFormatShort;
	}

	/*
	 * Shortcut method that will load xml using default filename
	 */
	public void load() throws FileNotFoundException, IllegalArgumentException {
		FileInputStream optionsInput = new FileInputStream(new File(MEKHQ_OPTIONS_FILE_NAME));
		this.loadFromXml(optionsInput);

		try {
			optionsInput.close();
		} catch (IOException e) {
			// this should not happen. ever. how can closing a read-only stream fail?
			MekHQ.logError(e);
		}
	}

	public void save() throws FileNotFoundException {
		PrintWriter optionsOutput = new PrintWriter(new File(MEKHQ_OPTIONS_FILE_NAME));
		this.writeToXml(optionsOutput, 0);
		optionsOutput.close();
	}

	public void writeToXml(PrintWriter pw, int indent) {
		pw.println(MekHqXmlUtil.indentStr(indent) + "<mekhqOptions>");

		MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, "dateFormatDataStorage", dateFormatDataStorage.toPattern());
		MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, "dateFormatLong", dateFormatLong.toPattern());
		MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, "dateFormatShort", dateFormatShort.toPattern());

		pw.println(MekHqXmlUtil.indentStr(indent) + "</mekhqOptions>");
	}

	public void loadFromXml(FileInputStream fis) throws IllegalArgumentException {
		MekHQ.logMessage("Loading MekHQ Options from XML...", 4);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;

		try {
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// Parse using builder to get DOM representation of the XML file
			xmlDoc = db.parse(fis);
		} catch (Exception ex) {
			MekHQ.logError(ex);
		}

		Element optionsEle = xmlDoc.getDocumentElement();
		optionsEle.normalize();
		if (optionsEle.getTagName() != "mekhqOptions") {
			throw new IllegalArgumentException("Invalid MekHQ Options File!");
		}

		NodeList wList = optionsEle.getChildNodes();

		for (int x = 0; x < wList.getLength(); x++) {
			Node wn = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			MekHQ.logMessage("---", 5);
			MekHQ.logMessage(wn.getNodeName(), 5);
			MekHQ.logMessage("\t" + wn.getTextContent(), 5);

			if (wn.getNodeName().equalsIgnoreCase("dateFormatDataStorage")) {
				this.dateFormatDataStorage = new SimpleDateFormat(wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("dateFormatLong")) {
				this.dateFormatLong = new SimpleDateFormat(wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("dateFormatShort")) {
				this.dateFormatShort = new SimpleDateFormat(wn.getTextContent().trim());
			}
		}

		MekHQ.logMessage("Load MekHQ Options Complete!", 4);
	}

}
