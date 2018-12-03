package sapphire.common;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/** Created by Srinivas on 12/03/18. */
public class LabelUtilsTest {

    @Test
    public void testvalidateNodeSelectRequirement_nullKey() {
        String key = null;
        String[] values = {"abc"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.In, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_nullOp() {
        String key = "key";
        String[] values = {key};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, null, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_InvOp() {
        String key = "key";
        String[] values = {key};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, key, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_valZeroInOP() {
        String key = "key";
        String[] values = {};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.In, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_valZeroEqlOP() {
        String key = "key";
        String[] values = {};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.Equals, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_val2EqlOP() {
        String key = "key";
        String[] values = {"key", "key2"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.Equals, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_val2ExistsOP() {
        String key = "key";
        String[] values = {"key", "key2"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(
                LabelUtils.validateNodeSelectRequirement(key, LabelUtils.GreaterThan, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_Invval1ExistsOP() {
        String key = "key";
        String[] values = {"key"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(
                LabelUtils.validateNodeSelectRequirement(key, LabelUtils.GreaterThan, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_Succ() {
        String key = "key";
        String[] values = {key};
        List<String> vals = Arrays.asList(values);
        Assert.assertTrue(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.In, vals));
    }
}
