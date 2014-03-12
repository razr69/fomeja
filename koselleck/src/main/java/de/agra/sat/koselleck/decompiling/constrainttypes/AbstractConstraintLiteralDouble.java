package de.agra.sat.koselleck.decompiling.constrainttypes;

import org.apache.log4j.Logger;

import de.agra.sat.koselleck.exceptions.NoComparableNumberTypeException;

/**
 * 
 * @author Max Nitze
 */
public class AbstractConstraintLiteralDouble extends AbstractConstraintLiteral<Double> {
	/**
	 * 
	 * @param value
	 */
	public AbstractConstraintLiteralDouble(Double value) {
		super(value, false, false, true);
	}

	@Override
	public void replaceAll(String regex, String replacement) {}

	@Override
	public AbstractConstraintValue evaluate() {
		return this;
	}

	@Override
	public boolean matches(String regex) {
		return false;
	}

	@Override
	public boolean equals(Object object) {
		if(!(object instanceof AbstractConstraintLiteralDouble))
			return false;
		
		AbstractConstraintLiteralDouble abstractConstraintLiteralDouble = (AbstractConstraintLiteralDouble)object;
		
		return this.value.equals(abstractConstraintLiteralDouble.value) &&
				this.isVariable == abstractConstraintLiteralDouble.isVariable;
	}

	@Override
	public AbstractConstraintLiteral<Double> clone() {
		return new AbstractConstraintLiteralDouble(this.value);
	}

	@Override
	public String toString() {
		return this.value + "[" + (this.isVariable ? " variable;" : " not variable;") + " no number type]";
	}

	@Override
	public int compareTo(AbstractConstraintLiteral<?> constraintLiteral) {
		if (constraintLiteral.value instanceof Double)
			return this.value.compareTo((Double) constraintLiteral.value);
		else if (constraintLiteral.value instanceof Float)
			return this.value.compareTo(((Float) constraintLiteral.value).doubleValue());
		else if (constraintLiteral.value instanceof Integer)
			return this.value.compareTo(((Integer) constraintLiteral.value).doubleValue());
		else {
			NoComparableNumberTypeException exception = new NoComparableNumberTypeException(this);
			Logger.getLogger(AbstractConstraintLiteralClass.class).fatal(exception.getMessage());
			throw exception;
		}
	}
}
