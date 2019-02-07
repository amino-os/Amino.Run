package amino.run.policy.servercache;

import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;

/*
 * This class must be applied to an object that extends the List class.
 *
 * It interposes on each method call, stores everything to disk and caches sublists. (see Facebook's TAO paper)
 */

public class DurableOnDemandCachedList extends DefaultPolicy {

    public static class DurableOnDemandCachedListClientPolicy extends DefaultClientPolicy {
        @Override
        public Object onRPC(
                String appMethod,
                ArrayList<Object> appParams,
                String prevDMMethod,
                ArrayList<Object> prevDMParams)
                throws Exception {
            /* Switch on the method we need to execute */
            return null;
        }
    }

    // TODO: think about concurrency
    public static class DurableOnDemandCachedListServerPolicy extends DefaultServerPolicy {
        int listSize; // cache the size of the list
        int numMisses; // to automatically grow the cache if possible

        @Override
        public Object onRPC(
                String appMethod,
                ArrayList<Object> appParams,
                String nextDMMethod,
                ArrayList<Object> nextDMParams)
                throws Exception {
            return null;
        }
    }

    public static class DurableOnDemandCachedListGroupPolicy extends DefaultGroupPolicy {}
}
