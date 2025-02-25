package mekhq.gui;

import mekhq.campaign.storyarc.StoryArc;
import mekhq.gui.StoryArcEditorGUI;
import org.apache.logging.log4j.LogManager;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.util.UUID;

public class StoryPointHyperLinkListener implements HyperlinkListener {

    private StoryArc storyArc;
    private StoryArcEditorGUI editorGUI;

    public static final String STORYPOINT = "STORYPOINT";

    public StoryPointHyperLinkListener(final StoryArc arc, final StoryArcEditorGUI gui) {
        this.storyArc = arc;
        this.editorGUI = gui;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (evt.getDescription().startsWith(STORYPOINT)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split(":")[1]);
                    editorGUI.focusOnStoryPoint(id);
                } catch (Exception e) {
                    LogManager.getLogger().error("", e);
                }
            }
        }
    }
}
