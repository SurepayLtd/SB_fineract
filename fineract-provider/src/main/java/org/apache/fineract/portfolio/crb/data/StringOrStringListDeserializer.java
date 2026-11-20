package org.apache.fineract.portfolio.crb.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class StringOrStringListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {

        JsonToken token = parser.currentToken();

        if (token == JsonToken.VALUE_STRING) {
            String value = parser.getValueAsString();

            return value == null || value.isBlank()
                    ? Collections.emptyList()
                    : Collections.singletonList(value);
        }

        if (token == JsonToken.START_ARRAY) {
            return parser.getCodec().readValue(parser,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, String.class)
            );
        }

        return (List<String>) context.handleUnexpectedToken(List.class, parser);
    }
}