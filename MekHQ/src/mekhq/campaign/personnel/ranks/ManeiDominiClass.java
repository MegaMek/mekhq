package mekhq.campaign.personnel.ranks;

import java.util.Optional;

/**
 * Models a WoB Manei Domini class.
 * 
 * @see "http://www.sarna.net/wiki/Manei_Domini"
 */
public enum ManeiDominiClass {

    @SuppressWarnings({"javadoc","nls"}) GHOST       (1,"Ghost"),
    @SuppressWarnings({"javadoc","nls"}) WRAITH      (2,"Wraith"),
    @SuppressWarnings({"javadoc","nls"}) BANSHEE     (3,"Banshee"),
    @SuppressWarnings({"javadoc","nls"}) ZOMBIE      (4,"Zombie"),
    @SuppressWarnings({"javadoc","nls"}) PHANTOM     (5,"Phantom"),
    @SuppressWarnings({"javadoc","nls"}) SPECTER     (6,"Specter"),
    @SuppressWarnings({"javadoc","nls"}) POLTERGEIST (7,"Poltergeist");

    public static Optional<ManeiDominiClass> ofId(int id) {
        for (ManeiDominiClass r : values()) {
            if (r.getId() == id) return Optional.of(r);
        }
        return Optional.empty();
    }
    
    private ManeiDominiClass(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    private final int id;
    private final String displayName;

    /**
     * Retrieves the identifier corresponding to this Manei Domini class.
     * <p>
     * Values are the same as the "old" constants in
     * {@link mekhq.campaign.personnel.Person} (except for
     * {@code MD_NONE = 0}, which was a magic value rather than a class):
     * 
     * <pre>
     * public static final int MD_GHOST        = 1;
     * public static final int MD_WRAITH       = 2;
     * public static final int MD_BANSHEE      = 3;
     * public static final int MD_ZOMBIE       = 4;
     * public static final int MD_PHANTOM      = 5;
     * public static final int MD_SPECTER      = 6;
     * public static final int MD_POLTERGEIST  = 7;
     * </pre>
     * 
     * @see #ofId(int)
     */
    public int getId() {
        return id;
    }

    @SuppressWarnings("javadoc")
    public String getDisplayName() {
        return displayName;
    }

}
