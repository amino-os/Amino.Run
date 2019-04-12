package amino.run.policy.cache.explicitcaching;

public interface ExplicitCacher {
    void pull() throws Exception;

    void push() throws Exception;
}
