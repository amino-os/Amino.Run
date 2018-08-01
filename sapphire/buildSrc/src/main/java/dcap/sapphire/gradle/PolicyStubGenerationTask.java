package dcap.sapphire.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.io.File;
import sapphire.compiler.StubGenerator;

/**
 * Gradle task that handles the generation of policy stubs.
 */
public class PolicyStubGenerationTask extends DefaultTask {
    /**
     * The directory where all sever policy class files live
     */
    private File srcDir;

    /**
     * The directory where the generated stub source files should go
     */
    private File dstDir;

    /**
     * The package name for generated stub classes.
     */
    private String pkgName;

    public void srcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public void dstDir(File dstDir) {
        this.dstDir = dstDir;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    @TaskAction
    public void run() {
        try {
            if (srcDir == null || ! srcDir.exists()) {
                throw new Exception(String.format("Source dir %s not exists. Please run build task first.", srcDir));
            }

            if (pkgName == null || pkgName.trim().length() == 0) {
                throw new Exception(String.format("Package name not specified"));
            }

            getLogger().info("Generating stub classes for srcDir {}, " +
                            "dstDir {}, and package name {}", srcDir, dstDir, pkgName);

            StubGenerator.generateStubs(srcDir.getCanonicalPath(), pkgName, dstDir.getCanonicalPath());
            getLogger().info("Successfully generated stub classes");
        } catch (Exception e) {
            getLogger().debug("Stub class generation failed.", e);
            throw new TaskExecutionException(this, e);
        }
    }
}
