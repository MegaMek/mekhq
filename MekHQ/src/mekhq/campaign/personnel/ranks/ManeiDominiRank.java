package mekhq.campaign.personnel.ranks;

import java.util.Optional;

/**
 * Models a WoB Manei Domini rank.
 * 
 * @see "http://www.sarna.net/wiki/Manei_Domini"
 */
public enum ManeiDominiRank {

    @SuppressWarnings({"javadoc","nls"}) ALPHA   (0,"Alpha"),
    @SuppressWarnings({"javadoc","nls"}) BETA    (1,"Beta"),
    @SuppressWarnings({"javadoc","nls"}) OMEGA   (2,"Omega"),
    @SuppressWarnings({"javadoc","nls"}) TAU     (3,"Tau"),
    @SuppressWarnings({"javadoc","nls"}) DELTA   (4,"Delta"),
    @SuppressWarnings({"javadoc","nls"}) SIGMA   (5,"Sigma"),
    @SuppressWarnings({"javadoc","nls"}) OMICRON (6,"Omicron");

    public static Optional<ManeiDominiRank> ofId(int id) {
        for (ManeiDominiRank r : values()) {
            if (r.getId() == id) return Optional.of(r);
        }
        return Optional.empty();
    }
    
    private ManeiDominiRank(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    private final int id;
    private final String displayName;

    /**
     * Retrieves the identifier corresponding to this Manei Domini rank.
     * <p>
     * Values are the same as the "old" constants in
     * {@link mekhq.campaign.personnel.Rank} (except for
     * {@code MD_RANK_NONE = -1}, which was a magic value rather than a rank):
     * 
     * <pre>
     * public static final int MD_RANK_ALPHA   = 0;
     * public static final int MD_RANK_BETA    = 1;
     * public static final int MD_RANK_OMEGA   = 2;
     * public static final int MD_RANK_TAU     = 3;
     * public static final int MD_RANK_DELTA   = 4;
     * public static final int MD_RANK_SIGMA   = 5;
     * public static final int MD_RANK_OMICRON = 6;
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
