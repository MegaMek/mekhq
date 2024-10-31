package mekhq.utilities;

public class MoreObjects {

    private MoreObjects() {
    }

    public static <O> O firstNonNull(O first, O second) {
        return first != null ? first : second;
    }
}

