package com.olf.jm.pricewebservice.model;

/*
 * History:
 * 2015-14-15	V1.0	jwaechter - initial version
 */

/**
 * Represents a generic pair of values
 * @author jwaechter
 * @version 1.0
 *
 * @param <Left> type of the left side of the pair
 * @param <Right> type of the right side of the pair
 */
public class Pair <Left, Right> {
	private final Left left;
	private final Right right;
	
	public Pair (Left left, Right right) {
		this.left = left;
		this.right = right;
	}

	public Left getLeft() {
		return left;
	}

	public Right getRight() {
		return right;
	}

	@Override
	public String toString() {
		return "(" + left + ", " + right + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (getClass() != obj.getClass()){
			return false;
		}
		Pair other = (Pair) obj;
		if (left == null) {
			if (other.left != null){
				return false;
			}
		} else if (!left.equals(other.left)){
			return false;
		}
		if (right == null) {
			if (other.right != null){
				return false;
			}
		} else if (!right.equals(other.right)){
			return false;
		}
		return true;
	}
}
