package geniusweb.pompfan.particles;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

public class AbstractPolicyDeserializer extends KeyDeserializer{

    @Override
    public AbstractPolicy deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return null;
    }
    
}
