package sapphire.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by quinton on 1/23/18.
 */

public class Utils {
    private Utils() {} // // so that nobody can accidentally create a Utils object
    public static class ObjectCloner { // see https://www.javaworld.com/article/2077578/learn-java/java-tip-76--an-alternative-to-the-deep-copy-technique.html
        // returns a deep copy of an object
        private ObjectCloner() {} // // so that nobody can accidentally creates an ObjectCloner object
        static public Serializable deepCopy(Serializable oldObj) throws Exception
        {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try
            {
                ByteArrayOutputStream bos =
                        new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
                // serialize and pass the object
                oos.writeObject(oldObj);
                oos.flush();
                ByteArrayInputStream bin =
                        new ByteArrayInputStream(bos.toByteArray());
                ois = new ObjectInputStream(bin);
                // return the new object
                return (Serializable)ois.readObject();
            }
            catch(Exception e)
            {
                System.out.println("Exception in ObjectCloner = " + e);
                throw(e);
            }
            finally
            {
                oos.close();
                ois.close();
            }
        }
    }
}
