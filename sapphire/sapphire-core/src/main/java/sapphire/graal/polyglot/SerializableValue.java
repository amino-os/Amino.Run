package sapphire.graal.polyglot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import sapphire.graal.io.Deserializer;
import sapphire.graal.io.Serializer;

public class SerializableValue implements java.io.Serializable {
    private byte[] data;

    private SerializableValue() {}

    private void setData(byte[] data) {
        this.data = data;
    }

    private byte[] getData() {
        return this.data;
    }

    public static SerializableValue getSerializeValue(Value valObj, String lang) throws Exception {
        SerializableValue serializeValue = new SerializableValue();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer ser = new Serializer(out, lang);
        ser.serialize(valObj);
        serializeValue.setData(out.toByteArray());
        return serializeValue;
    }

    public static Value getDeserializedValue(SerializableValue serializedVal, Context c)
            throws Exception {
        Deserializer de = new Deserializer(new ByteArrayInputStream(serializedVal.getData()), c);
        return de.deserialize();
    }
}
