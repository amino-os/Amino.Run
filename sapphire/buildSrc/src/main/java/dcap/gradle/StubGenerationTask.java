package dcap.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.policy.SapphirePolicy;

public class StubGenerationTask extends DefaultTask {
    @TaskAction
    public void run() {
        System.out.println("hello from StubGenerator!");
    }
}
