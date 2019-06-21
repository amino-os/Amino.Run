package amino.run.appdemo;

import amino.run.appdemo.stubs.KeyValueStore_Stub;
import java.io.Serializable;
import java.util.Date;

/** Client class for testing {@link KeyValueStore_Stub} */
public class KeyValueStoreClient {
    public static class TestClass implements Serializable {
        public int intValue = 0;
        public String stringValue = "defaultStringValue";

        public TestClass() {}

        public TestClass(int i) {
            intValue = i;
            stringValue = "Value" + i;
        }

        @Override
        public String toString() {
            return String.format("intValue=%s, stringValue=%s", intValue, stringValue);
        }
    }

    public static void main(String[] args) throws Exception {

        String hostIP = System.getenv("HOST_IP");
        String hostPort = System.getenv("HOST_PORT");
        String omsIP = System.getenv("OMS_IP");
        String omsPort = System.getenv("OMS_PORT");
        String yamlFile = System.getenv("YAML_FILE");

        if (null == hostIP || hostIP.isEmpty()) hostIP = "127.0.0.2";
        if (null == hostPort || hostPort.isEmpty()) hostPort = "22345";
        if (null == omsIP || omsIP.isEmpty()) omsIP = "127.0.0.1";
        if (null == omsPort || omsPort.isEmpty()) omsPort = "22222";
        if (null == yamlFile || yamlFile.isEmpty()) yamlFile = "KeyValueStore.yaml";

        System.out.println(
                String.format(
                        "hostIP=%s, hostPort=%s, omsIP=%s, omsPort=%s, yamlFile=%s",
                        hostIP, hostPort, omsIP, omsPort, yamlFile));
        KeyValueStore_Stub store =
                KeyValueStore_Stub.getStub(yamlFile, omsIP, omsPort, hostIP, hostPort);

        for (int i = 0; i < 5; ++i) {
            String key = "key_" + i;
            Object val = "val_" + i;
            if (i % 2 == 0) val = new Date();
            else val = new TestClass(i);

            System.out.println(String.format("<client>: setting %s = %s", key, val));
            store.set(key, val);
            val = store.get(key);
            System.out.println(String.format("<client>: got value %s with key %s", val, key));
        }
    }
}
