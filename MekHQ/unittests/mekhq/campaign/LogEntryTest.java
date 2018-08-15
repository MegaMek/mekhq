package mekhq.campaign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class LogEntryTest {

	@Test
	public void testNullDescriptionBecomesEmpty() {
		Assert.assertEquals("", new LogEntry(null, null).getDesc());
	}

	@Test
	public void testXmlMarshalling() throws Exception {
		checkMarshalling(new LogEntry(null, null, null));
		checkMarshalling(new LogEntry(new Date(0l), "", ""));
		checkMarshalling(new LogEntry(new Date(0l), "<desc>Some description</desc>", "<type>Some type</type>"));
		checkMarshalling(new LogEntry(new Date(0l), "Some <em>xml-fragment</em> description", "Some <em>xml-fragment</em> type"));
	}

	private static void checkMarshalling(LogEntry le) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		PrintWriter pw = new PrintWriter(baos);
		le.writeToXml(pw, 0);
		pw.close();
		
		Node node = DocumentBuilderFactory.newInstance()
		                                  .newDocumentBuilder()
		                                  .parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())))
		                                  .getDocumentElement();
		
		Assert.assertEquals(le, LogEntry.generateInstanceFromXML(node));
	}

}
