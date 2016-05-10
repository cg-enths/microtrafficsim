package microtrafficsim.core.logic;

public class IndicesCalculator {

	public static final byte NO_MATCH = -42;

	/**
     * TODO
	 * <p>
	 * This method assumes to get two strings containing sorted indices like
	 * they appear in traffic logic ({@link DirectedEdge}) to calculate two
	 * vehicles' priorities at a crossroad.
	 * </p>
	 * <p>
	 * Because the indices are sorted in an ascending order, there is only one
	 * unique or no matching. Therefore, this algorithm just go through the
	 * strings from left to right repeatedly and returns the first found
	 * matching char. IT DOES NOT DETECT ANY DIFFERENCE FROM THIS, so you have
	 * to know whether the indices are sorted.
	 * </p>
	 * <p>
	 * Runtime (circa):<br>
	 * The runtime of worst case is O(n^2), but n is the count of all indices in
	 * the given strings. The indices are limited by the number of streets per
	 * node, so it's quite small.<br>
	 * Let len1 := s1.length(); len2 := s2.length(); n := len1 + len2;<br>
	 * INIT_TIME := n;<br>
	 * WORST_CASE (= no match) := INIT_TIME + n(n-1)/2;<br>
	 * BEST_CASE := INIT_TIME + 1 <br>
	 * <br>
	 * Example:<br>
	 * len1 = 4 = len2; n = 8; INIT_TIME = 8; WORST_CASE = 8 + 28 = 36;
	 * BEST_CASE := 9
	 * </p>
	 * 
	 * @param s1
	 *            String indices of vehicle 1, sorted in an ascending order
	 * @param s2
	 *            String indices of vehicle 2, sorted in an ascending order
	 * @return The first index of this unique matching or
	 *         StringMatcher.NO_MATCH, if there is no matching.
	 */
	public static byte leftmostIndexInMatching(byte origin1, byte destination1, byte origin2, byte destination2,
			byte indicesPerNode) {

		byte[] s1 = getIndices(origin1, destination1, indicesPerNode);
		byte[] s2 = getIndices(origin2, destination2, indicesPerNode);
		int len1 = s1.length;
		int len2 = s2.length;
		int n = len1 + len2;
		// set two arrays for comparison
		byte[] c1 = new byte[n];
		byte[] c2 = new byte[n];
		for (int i = 0; i < n; i++)
			if (i < len1) {
				c1[i] = s1[i];
				c2[i] = NO_MATCH;
			} else { // i = len2 + len1; i < n
				c1[i] = NO_MATCH;
				c2[i] = s2[i - len1];
			}
		// compare
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n - i; j++)
				if (c1[j] == c2[j + i])
					if (c1[j] != NO_MATCH)
						return c1[j];
		}

		return NO_MATCH;
	}

	private static byte[] getIndices(byte origin, byte destination, byte indicesPerNode) {
		int delta = destination - origin;
		if (delta < 0) {
			delta = indicesPerNode + delta;
		}
		byte[] indices = new byte[delta + 1];

		byte index = origin;
		int arrayIndex = 0;
		while (index != destination) {
			indices[arrayIndex++] = index;
			index = (byte) ((index + 1) % indicesPerNode);
		}
		indices[arrayIndex] = destination;

		return indices;
	}

	public static boolean areIndicesCrossing(byte origin1, byte destination1, byte origin2, byte destination2,
			byte indicesPerNode) {
		int i = origin1;

		// DFA: A := start1,end1; B := start2,end2
		// state 0: A -A-> false
		// state 1: A -B-> AB
		// state 2: AB -B-> false
		// state 3: AB -A-> true
		// => 2 states => boolean
		// if common destination: true should be returned => order of the if-statements is relevant
		boolean stateA = true;
		int irrelevantCounter = 0;
		while (irrelevantCounter++ < 2 * indicesPerNode) {
			i = (i + 1) % indicesPerNode;
			if (stateA) { // state A
				if (i == origin2 || i == destination2) // -B->
                    stateA = false; // state AB
				else if (i == destination1) // -A->
					return false; // AA
			} else { // state AB
				if (i == destination1 || destination1 == destination2) // -A->
					return true; // ABA
				else if (i == origin2 || i == destination2) // -B->
					return false; // ABB
			}
		}
		return false; // should never be reached if method parameters are
						// correct
	}
}