package sapphire.appexamples.minnietwitter.app;

import java.util.ArrayList;
import java.util.List;

public class Util {
	/** Returns a serializable List (ArrayList) */
	public static <T> List<T> checkedSubList(List<T> l, int from, int to) {
		ArrayList<T> sl = new ArrayList<T>();
		int n = l.size();
		
		if (from >= n)
			return sl;
		
		if (to >= n)
			to = n - 1;
		
		while (from <= to) {
			sl.add(l.get(from));
			from += 1;
		}

		return sl;
	}
}
