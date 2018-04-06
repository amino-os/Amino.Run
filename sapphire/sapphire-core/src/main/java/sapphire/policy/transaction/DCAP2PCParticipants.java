package sapphire.policy.transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static sapphire.policy.SapphirePolicy.SapphireClientPolicy;

/**
 * DCAP transaction context based on thread local storage
 */
public class DCAP2PCParticipants implements I2PCParticipants {
    Set participants = new ConcurrentHashMap<SapphireClientPolicy, Object>().keySet();

    @Override
    public void register(SapphireClientPolicy cohort) {
        this.participants.add(cohort);
    }

    @Override
    public Collection<SapphireClientPolicy> getParticipants() {
        return new ArrayList<SapphireClientPolicy>(this.participants);
    }
}
