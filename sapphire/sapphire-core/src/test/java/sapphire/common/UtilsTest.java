package sapphire.common;

import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

/**
 * Created by quinton on 1/23/18.
 */
public class UtilsTest {

    static class TestOuter implements Serializable {
        int o;
        TestInner innerObj;
        TestOuter() {
            innerObj=new TestInner();
        }
    }
    static class TestInner implements Serializable {
        int i;
    }
    @Test
    public void clonesAreDisjoint() throws Exception {
        UtilsTest.TestOuter testObj = new TestOuter();
        testObj.o = 1;
        testObj.innerObj.i=1;
        TestOuter cloneObj = (TestOuter)Utils.ObjectCloner.deepCopy(testObj);
        cloneObj.o = 2;
        cloneObj.innerObj.i = 2;
        assertNotEquals(testObj.o, cloneObj.o);
        assertNotEquals(testObj.innerObj.i, cloneObj.innerObj.i);
    }
}