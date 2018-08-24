package mekhq.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class FileTypeTest {

    /**
     * Ensures we didn't forget to specify the extensions in some file type.
     */
    @Test
    public void testExtensionsProvided() {
        for (FileType ft : FileType.values()) {
            assertTrue(!ft.getExtensions().isEmpty());
        }
    }

    @Test
    public void testFileNamefilter() {

        Arrays.asList(
            "file.cpnx", "file.CPNX",
            "file.xml",  "file.XML",
            "some/dir/file.xml"
        ).forEach(fn -> assertTrue(fn + " was not accepted",FileType.CPNX.getNameFilter().test(fn)));;

        Arrays.asList(
            "file.abc",
            "file.xml.abc",
            "file.xmlabc",
            "file.abcxml"
        ).forEach(fn -> assertFalse(fn + " was not refused",FileType.CPNX.getNameFilter().test(fn)));;

    }

}
