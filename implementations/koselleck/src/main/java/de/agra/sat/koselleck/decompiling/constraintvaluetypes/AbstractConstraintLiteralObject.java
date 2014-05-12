package de.agra.sat.koselleck.decompiling.constraintvaluetypes;

import java.util.Map;

import org.apache.log4j.Logger;

import de.agra.sat.koselleck.exceptions.NoCalculatableNumberTypeException;
import de.agra.sat.koselleck.exceptions.NoComparableNumberTypeException;
import de.agra.sat.koselleck.types.ArithmeticOperator;

/**
 * 
 * @author Max Nitze
 */
public class AbstractConstraintLiteralObject extends AbstractConstraintLiteral<Object> {
	/**
	 * 
	 * @param value
	 */
	public AbstractConstraintLiteralObject(Object value) {
		super(value, false, false, true);
	}

	@Override
	public void replaceAll(String regex, String replacement) {}

	@Override
	public AbstractConstraintValue evaluate() {
		return this;
	}

	@Override
	public AbstractConstraintValue substitute(Map<Integer, Object> constraintArguments) {
		return this;
	}

	@Override
	public boolean matches(String regex) {
		return false;
	}

	@Override
	public boolean equals(Object object) {
		if(!(object instanceof AbstractConstraintLiteralObject))
			return false;

		AbstractConstraintLiteralObject abstractConstraintLiteralObject = (AbstractConstraintLiteralObject)object;

		return this.value != null && this.value.equals(abstractConstraintLiteralObject.value)
				&& this.isVariable == abstractConstraintLiteralObject.isVariable;
	}

	@Override
	public AbstractConstraintLiteralObject clone() {
		return new AbstractConstraintLiteralObject(this.value);
	}

	@Override
	public String toString() {
		return this.value + "[" + (this.isVariable ? " variable;" : " not variable;") + " no number type]";
	}

	@Override
	public int compareTo(AbstractConstraintLiteral<?> constraintLiteral) {
		NoComparableNumberTypeException exception = new NoComparableNumberTypeException(this);
		Logger.getLogger(AbstractConstraintLiteralObject.class).fatal(exception.getMessage());
		throw exception;
	}

	@Override
	public AbstractConstraintLiteral<?> calc(AbstractConstraintLiteral<?> constraintLiteral, ArithmeticOperator operator) {
		NoCalculatableNumberTypeException exception = new NoCalculatableNumberTypeException(this);
		Logger.getLogger(AbstractConstraintLiteralObject.class).fatal(exception.getMessage());
		throw exception;
	}
}