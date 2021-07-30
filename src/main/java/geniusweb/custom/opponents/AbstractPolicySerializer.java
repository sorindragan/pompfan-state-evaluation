package geniusweb.custom.opponents;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class AbstractPolicySerializer extends JsonSerializer<AbstractPolicy> {

    @Override
    public void serialize(AbstractPolicy value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeFieldName(value.getId().toString());
    }

}
