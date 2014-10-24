package de.agra.sat.koselleck.decompiling.constraintvaluetypes;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.agra.sat.koselleck.datatypes.PreField;
import de.agra.sat.koselleck.exceptions.NoCalculatableNumberTypeException;
import de.agra.sat.koselleck.exceptions.NoComparableNumberTypeException;
import de.agra.sat.koselleck.exceptions.UnknownArithmeticOperatorException;
import de.agra.sat.koselleck.types.ArithmeticOperator;

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
		super(value, null, false, true, true);
	}

	/**
	 * 
	 * @param name
	 */
	public AbstractConstraintLiteralDouble(String name, List<PreField> preFields) {
		super(null, name, true, true, true, preFields);
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
		if (!(object instanceof AbstractConstraintLiteralDouble))
			return false;

		AbstractConstraintLiteralDouble abstractConstraintLiteralDouble = (AbstractConstraintLiteralDouble)object;

		return this.getValue().equals(abstractConstraintLiteralDouble.getValue()) &&
				this.isVariable() == abstractConstraintLiteralDouble.isVariable();
	}

	@Override
	public AbstractConstraintLiteralDouble clone() {
		return new AbstractConstraintLiteralDouble(this.getValue());
	}

	@Override
	public String toString() {
		return this.getValue().toString();
	}

	@Override
	public int compareTo(AbstractConstraintLiteral<?> constraintLiteral) {
		if (constraintLiteral.getValue() instanceof Double)
			return this.getValue().compareTo((Double) constraintLiteral.getValue());
		else if (constraintLiteral.getValue() instanceof Float)
			return this.getValue().compareTo(((Float) constraintLiteral.getValue()).doubleValue());
		else if (constraintLiteral.getValue() instanceof Integer)
			return this.getValue().compareTo(((Integer) constraintLiteral.getValue()).doubleValue());
		else {
			NoComparableNumberTypeException exception = new NoComparableNumberTypeException(this);
			Logger.getLogger(AbstractConstraintLiteralClass.class).fatal(exception.getMessage());
			throw exception;
		}
	}

	@Override
	public AbstractConstraintLiteral<?> calc(AbstractConstraintLiteral<?> constraintLiteral, ArithmeticOperator operator) {
		if (constraintLiteral.getValue() instanceof Double) {
			switch(operator) {
			case ADD:
				return new AbstractConstraintLiteralDouble(this.getValue() + ((Double) constraintLiteral.getValue()));
			case SUB:
				return new AbstractConstraintLiteralDouble(this.getValue() - ((Double) constraintLiteral.getValue()));
			case MUL:
				return new AbstractConstraintLiteralDouble(this.getValue() * ((Double) constraintLiteral.getValue()));
			case DIV:
				return new AbstractConstraintLiteralDouble(this.getValue() / ((Double) constraintLiteral.getValue()));
			default:
				Logger.getLogger(AbstractConstraintFormula.class).fatal("arithmetic operator " + (operator == null ? "null" : "\"" + operator.getAsciiName() + "\"") + " is not known");
				throw new UnknownArithmeticOperatorException(operator);
			}
		} else if (constraintLiteral.getValue() instanceof Float) {
			switch(operator) {
			case ADD:
				return new AbstractConstraintLiteralDouble(this.getValue() + ((Float) constraintLiteral.getValue()).doubleValue());
			case SUB:
				return new AbstractConstraintLiteralDouble(this.getValue() - ((Float) constraintLiteral.getValue()).doubleValue());
			case MUL:
				return new AbstractConstraintLiteralDouble(this.getValue() * ((Float) constraintLiteral.getValue()).doubleValue());
			case DIV:
				return new AbstractConstraintLiteralDouble(this.getValue() / ((Float) constraintLiteral.getValue()).doubleValue());
			default:
				Logger.getLogger(AbstractConstraintFormula.class).fatal("arithmetic operator " + (operator == null ? "null" : "\"" + operator.getAsciiName() + "\"") + " is not known");
				throw new UnknownArithmeticOperatorException(operator);
			}
		} else if (constraintLiteral.getValue() instanceof Integer) {
			switch(operator) {
			case ADD:
				return new AbstractConstraintLiteralDouble(this.getValue() + ((Integer) constraintLiteral.getValue()).doubleValue());
			case SUB:
				return new AbstractConstraintLiteralDouble(this.getValue() - ((Integer) constraintLiteral.getValue()).doubleValue());
			case MUL:
				return new AbstractConstraintLiteralDouble(this.getValue() * ((Integer) constraintLiteral.getValue()).doubleValue());
			case DIV:
				return new AbstractConstraintLiteralDouble(this.getValue() / ((Integer) constraintLiteral.getValue()).doubleValue());
			default:
				Logger.getLogger(AbstractConstraintFormula.class).fatal("arithmetic operator " + (operator == null ? "null" : "\"" + operator.getAsciiName() + "\"") + " is not known");
				throw new UnknownArithmeticOperatorException(operator);
			}
		} else {
			NoCalculatableNumberTypeException exception = new NoCalculatableNumberTypeException(constraintLiteral);
			Logger.getLogger(AbstractConstraintLiteralDouble.class).fatal(exception.getMessage());
			throw exception;
		}
	}
}
