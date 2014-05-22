package de.agra.sat.koselleck.decompiling.constraintvaluetypes;

import java.util.Map;

/**
 * AbstractConstraint is an abstract class for all types of constraint values.
 * 
 * @version 1.0.0
 * @author Max Nitze
 */
public abstract class AbstractConstraintValue {
	public static AbstractConstraintValue NULLValue = new AbstractConstraintLiteralObject(null);

	/**
	 * replaceAll replaces all occurrences of the given regular expression
	 * 	{@code regex} with the given {@code replacement}.
	 * 
	 * @param regex the regular expression to look for
	 * @param replacement the replacement
	 */
	public abstract void replaceAll(String regex, String replacement);

	/**
	 * changeStringLiteralType changes the type of all string literals matching
	 *  the given regular expression with the given type.
	 * 
	 * @param regex the string literal to change
	 * @param type the new type of the string literals
	 */
	public abstract void changeStringLiteralType(String regex, Class<?> type);

	/**
	 * evaluate evaluates the abstract constraint value.
	 * 
	 * @return the new evaluated or this abstract constraint value 
	 */
	public abstract AbstractConstraintValue evaluate();

	/**
	 * substitute substitutes the abstract constraint value with the given
	 *  objects (method parameters).
	 * 
	 * @param constraintArguments the arguments the substitute the constraint
	 *  values with
	 */
	public abstract AbstractConstraintValue substitute(Map<Integer, Object> constraintArguments);

	/**
	 * matches checks if this abstract constraint value matches the given
	 *  regular expression {@code regex}.
	 * 
	 * @param regex the regular expression
	 * 
	 * @return {@code true} if this abstract constraint value matches the given
	 *  regular expression {@code regex}, {@code false} otherwise
	 */
	public abstract boolean matches(String regex);

	/**
	 * equals checks if this abstract constraint value and the given object are
	 *  equal.
	 * 
	 * @param object the object to check for equality
	 * 
	 * @return {@code true} if the given object matches this abstract
	 *  constraint value, {@code false} otherwise
	 */
	@Override
	public abstract boolean equals(Object object);

	/**
	 * clone returns a copy of this abstract constraint value.
	 * 
	 * @return a copy of this abstract constraint value
	 */
	@Override
	public abstract AbstractConstraintValue clone();

	/**
	 * toString returns the string representation of this abstract constraint
	 *  value.
	 * 
	 * @return the string representation of this abstract constraint value
	 */
	@Override
	public abstract String toString();
}
