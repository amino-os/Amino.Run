package amino.run.graal.io;

import amino.run.app.Language;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class SerializeValue implements java.io.Serializable {
    private byte[] data;

    private SerializeValue() {}

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }

    public static SerializeValue getSerializeValue(Value valObj, Language lang) throws Exception {
        SerializeValue serializeValue = new SerializeValue();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer ser = new Serializer(out, lang);
        ser.serialize(valObj);
        serializeValue.setData(out.toByteArray());
        return serializeValue;
    }

    public static Value getDeserializedValue(SerializeValue serializedVal, Context c)
            throws Exception {
        Deserializer de = new Deserializer(new ByteArrayInputStream(serializedVal.getData()), c);
        return de.deserialize();
    }

    @Override
    public String toString() {
        return new String(this.data);
    }
}
