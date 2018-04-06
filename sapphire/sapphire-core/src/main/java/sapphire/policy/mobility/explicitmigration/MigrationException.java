package sapphire.policy.mobility.explicitmigration;

/**
 * Created by Malepati Bala Siva Sai Akhil on 4/2/18.
 */

public class MigrationException extends Exception {
    public MigrationException() {}

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(Throwable cause) {
        super(cause);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
