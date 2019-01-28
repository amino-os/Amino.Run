package amino.run.policy.metric;

@FunctionalInterface
public interface ExecutionTimeInterface {
    Object execute() throws Exception;
}
