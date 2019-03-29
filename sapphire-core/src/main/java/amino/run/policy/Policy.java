package amino.run.policy;

/**
 * Class that describes how MicroService Policies look like. Each policy should extend this class.
 * Each MicroService Policy contains a Server Policy, a Client Policy and a Group Policy. The
 * Policies contain a set of internal functions (used by the Amino.Run runtime system), a set of
 * upcall functions that are called when an event happened and a set of functions that implement the
 * Amino.Run API for policies.
 */
public abstract class Policy extends DefaultUpcallImpl {
    public abstract static class ClientPolicy extends DefaultUpcallImpl.ClientPolicy {}

    public abstract static class ServerPolicy extends DefaultUpcallImpl.ServerPolicy {}

    public abstract static class GroupPolicy extends DefaultUpcallImpl.GroupPolicy {}
}
