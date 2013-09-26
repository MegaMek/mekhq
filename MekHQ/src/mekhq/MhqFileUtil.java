package mekhq;

public class MhqFileUtil {
    public static String escapeReservedCharacters (String text) {
        final char[] WIN_RESERVED = new char[]{'<', '>', ':', '"', '/', '\\', '|', '?', '*'};
        final char[] LIN_RESERVED = new char[]{'<', '>', ':', '/', '|', '&'};
        final char[] MAC_RESERVED = new char[]{':', '/', '.'};

        char[] restricted;
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("win")) {
            restricted = WIN_RESERVED;
        } else if (osName.toLowerCase().contains("linux")) {
            restricted = LIN_RESERVED;
        } else {
            restricted = MAC_RESERVED;
        }

        char[] textArray = text.toCharArray();
        for (int i = 0; i < textArray.length; i++) {
            char c = textArray[i];
            textArray[i] = escapeChar(c, restricted);
       }

        return new String(textArray);
    }

    private static char escapeChar(char esc, char[] restricted) {
        for (char c : restricted) {
            if (c == esc) {
                return '_';
            }
        }
        return esc;
    }
}
