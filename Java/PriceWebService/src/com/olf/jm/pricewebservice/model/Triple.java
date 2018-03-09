package com.olf.jm.pricewebservice.model;

/*
 * History: 
 * 2015-04-13	V1.0	jwaechter - initial version
 */

/**
 * Class representing a triple (X,Y,Z) of values of arbitrary type.
 * 
 * @author jwaechter
 * @version 1.0
 * @param <Left>    left value of the triple
 * @param <Center>  center value of the triple
 * @param <Right>   right value of the triple
 */
public class Triple <Left, Center, Right>  {
	private final Left left;
	private final Center center;
	private final Right right;
	
	public Triple (Left left, Center center, Right right) {
		this.left = left;
		this.center = center;
		this.right = right;
	}

	public Left getLeft() {
		return left;
	}

	public Center getCenter() {
		return center;
	}

	public Right getRight() {
		return right;
	}

	@Override
	public String toString() {
		return "(" + left + ", " + center + ", "
				+ right + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((center == null) ? 0 : center.hashCode());
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Triple other = (Triple) obj;
		if (center == null) {
			if (other.center != null)
				return false;
		} else if (!center.equals(other.center))
			return false;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}
}
