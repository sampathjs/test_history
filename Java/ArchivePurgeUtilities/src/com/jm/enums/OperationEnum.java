/**
 * 
 */
package com.jm.enums;

/**
 * This enum is used to define operation being performed.
 * @author SharmV03
 *
 */
public enum OperationEnum
{
	ARCHIVING("Archiving"),
	PURGING("Purging");

	private final String value;

	private OperationEnum(String argValue)
	{
		this.value = argValue;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
