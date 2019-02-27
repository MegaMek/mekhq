package mekhq;

import mekhq.campaign.Campaign;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

public final class TestUtilities {
    public static Campaign getTestCampaign() {
        return new Campaign();
    }

    public static InputStream ParseBase64XmlFile(String base64){
        return new ByteArrayInputStream(Decode(base64));
    }

    public static byte[] Decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
