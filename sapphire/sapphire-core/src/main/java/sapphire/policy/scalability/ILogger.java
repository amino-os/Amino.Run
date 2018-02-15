package sapphire.policy.scalability;

/**
 * @author terryz
 */
public interface ILogger<T> {
    long append(T entry);
    void setIndexOfCommittedEntry(long indexOfCommittedEntry);
}
