package amino.run.compiler;

import amino.run.samplepolicy.TestPolicy;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;

public class PolicyStubTest {

    @Test
    public void testServerPolicyNonStaticPublicMethod() {
        PolicyStub appStub = new PolicyStub(TestPolicy.ServerPolicy.class);

        TreeSet<Stub.MethodStub> stubMethods = appStub.getMethods();
        for (Stub.MethodStub methodStub : stubMethods) {
            // Assert to check for static method generation in Server policy stub methods set.
            Assert.assertNotEquals(methodStub.getName(), "getPolicyName");
        }
    }

    @Test
    public void testGroupPolicyNonStaticPublicMethod() {
        PolicyStub appStub = new PolicyStub(TestPolicy.GroupPolicy.class);

        TreeSet<Stub.MethodStub> stubMethods = appStub.getMethods();
        for (Stub.MethodStub methodStub : stubMethods) {
            // Assert to check for static method generation in Group policy stub methods set.
            Assert.assertNotEquals(methodStub.getName(), "getPolicyName");
        }
    }
}
