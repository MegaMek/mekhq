package mekhq.campaign.io;

/** 
 * Raised when a {@link Campaign} cannot be parsed from XML.
 */
public class CampaignXmlParseException extends Exception {

    private static final long serialVersionUID = -1862554265022111338L;
    
    public CampaignXmlParseException() {
    }

    public CampaignXmlParseException(Throwable e) {
        super(e);
    }

    public CampaignXmlParseException(String message, Throwable e) {
        super(message, e);
    }
}
