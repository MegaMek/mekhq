package mekhq;

import java.io.PrintWriter;

public interface MekHqXmlSerializable {
	void writeToXml(PrintWriter pw1, int indent);
}
