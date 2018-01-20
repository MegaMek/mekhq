package mekhq;

import java.io.PrintWriter;

public interface MekHqXmlSerializable {
	public abstract void writeToXml(PrintWriter pw1, int indent);
}
