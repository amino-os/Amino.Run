package sapphire.policy.interfaces.dht;

import java.util.Map;


/**
 * Any Sapphire Object that wants to use the DHTPolicy must implement this interface.
 * @author aaasz
 *
 */

public interface DHTInterface {
	/**
	 * Returns the Map like structure used to index the data
	 * 
	 * @return
	 */
	public Map<DHTKey, ?> dhtGetData();
}