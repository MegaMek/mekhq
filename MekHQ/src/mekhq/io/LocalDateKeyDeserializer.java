package mekhq.io;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateKeyDeserializer extends KeyDeserializer {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return LocalDate.parse(key, formatter);
    }
}
