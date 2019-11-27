package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

/*
 * History:
 * 2018-11-22	V1.0	jwaechter	- Initial Version
 */

/**
 * The generix "accept all" (*) predicate.
 * @author jwaechter
 * @version 1.0
 * @param <T>
 */
public class KleeneStar<T> extends AbstractPredicate<T> {
	public KleeneStar(Class<T> typeClass, String unparsedPredicate,
			int weight) {
		super(typeClass, unparsedPredicate, false, weight);
	}

	@Override
	public boolean evaluate(String input) {
		return true;
	}
}
