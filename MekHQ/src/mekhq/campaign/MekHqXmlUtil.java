package mekhq.campaign;

import java.io.PrintWriter;

public class MekHqXmlUtil {
	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, String val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, int val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, boolean val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, long val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, double val) {
		for (int x=0; x<indent; x++)
			pw1.print("\t");
		
		pw1.print("<"+name+">");
		pw1.print(val);
		pw1.println("</"+name+">");
	}

	public static String indentStr(int level) {
		String retVal = "";
	
		for (int x=0; x<level; x++)
			retVal += "\t";
		
		return retVal;
	}
}
