package de.agra.sat.koselleck.decompiling.constraintvaluetypes;

/* imports */
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.agra.sat.koselleck.annotations.Variable;
import de.agra.sat.koselleck.datatypes.PreField;
import de.agra.sat.koselleck.datatypes.PreFieldList;
import de.agra.sat.koselleck.types.ArithmeticOperator;
import de.agra.sat.koselleck.types.Opcode;

/**
 * AbstractConstraintLiteral represents a literal in a constraint value.
 * 
 * @version 1.0.0
 * @author Max Nitze
 */
public abstract class AbstractConstraintLiteral<T> extends AbstractConstraintValue {
	/** the value of the literal */
	private T value;

	/** COMMENT */
	private String stringValue;

	/** the name of the value */
	private String name;

	/** COMMENT */
	private Field field;
	/** COMMENT */
	private int constantTableIndex;

	/** flag if the value is variable */
	private boolean isVariable;

	/** flag that indicates if the value is a number type */
	private boolean isNumberType;

	/** flag that indicates if the value is a finished type */
	private boolean isFinishedType;

	/**
	 * Constructor for a new AbstractConstraintLiteral.
	 * 
	 * @param value the new value for the literal
	 * @param isNumberType the new number type flag for the value
	 * @param isFinishedType the new finished type flag for the value
	 */
	public AbstractConstraintLiteral(T value, boolean isNumberType, boolean isFinishedType) {
		this.value = value;

		this.isVariable = false;
		this.isNumberType = isNumberType;
		this.isFinishedType = isFinishedType;

		this.field = null;
		this.constantTableIndex = -1;
		if (value != null)
			this.name = value.getClass().getSimpleName() + "_" + value.toString();
		else
			this.name = "";
	}

	/**
	 * Constructor for a new AbstractConstraintLiteral.
	 * 
	 * @param value the new value for the literal
	 * @param fieldCodeIndex COMMENT
	 * @param opcode COMMENT
	 * @param isNumberType the new number type flag for the value
	 * @param isFinishedType the new finished type flag for the value
	 */
	public AbstractConstraintLiteral(T value, int fieldCodeIndex, Opcode opcode, boolean isNumberType, boolean isFinishedType) {
		super(fieldCodeIndex, opcode);
		this.value = value;

		this.isVariable = false;
		this.isNumberType = isNumberType;
		this.isFinishedType = isFinishedType;

		this.field = null;
		this.constantTableIndex = -1;
		if (value != null)
			this.name = value.getClass().getSimpleName() + "_" + value.toString();
		else
			this.name = "";
	}

	/**
	 * Constructor for a new AbstractConstraintLiteral.
	 * 
	 * @param field
	 * @param fieldCodeIndex
	 * @param opcode
	 * @param constantTableIndex
	 * @param isNumberType the new number type flag for the value
	 * @param isFinishedType the new finished type flag for the value
	 */
	public AbstractConstraintLiteral(Field field, int fieldCodeIndex, Opcode opcode, int constantTableIndex, boolean isNumberType, boolean isFinishedType) {
		super(fieldCodeIndex, opcode);
		this.value = null;

		this.field = field;
		this.constantTableIndex = constantTableIndex;
		if (this.field != null && this.constantTableIndex >= 0)
			this.name = "v_" + this.constantTableIndex + "-" + this.field.getName();
		else
			this.name = "";

		this.isVariable = (this.field != null && this.field.getAnnotation(Variable.class) != null);
		this.isNumberType = isNumberType;
		this.isFinishedType = isFinishedType;
	}

	/**
	 * Constructor for a new AbstractConstraintLiteral.
	 * 
	 * @param field
	 * @param fieldCodeIndex
	 * @param opcode
	 * @param constantTableIndex
	 * @param isNumberType the new number type flag for the value
	 * @param isFinishedType the new finished type flag for the value
	 * @param preFields
	 */
	public AbstractConstraintLiteral(Field field, int fieldCodeIndex, Opcode opcode, int constantTableIndex, boolean isNumberType, boolean isFinishedType, List<PreField> preFields) {
		super(fieldCodeIndex, opcode, preFields);
		this.value = null;

		this.field = field;
		this.constantTableIndex = constantTableIndex;

		if (this.field != null && this.constantTableIndex >= 0)
			this.name = this.getPreFieldList().getName() +  "_" + this.constantTableIndex + "-" + this.field.getName();
		else
			this.name = "";

		this.isVariable = (this.field != null && this.field.getAnnotation(Variable.class) != null);
		this.isNumberType = isNumberType;
		this.isFinishedType = isFinishedType;
	}

	/**
	 * COMMENT
	 * 
	 * @param name
	 * @param isNumberType
	 * @param isFinishedType
	 */
	public AbstractConstraintLiteral(String name, boolean isNumberType, boolean isFinishedType) {
		super(-1, null);
		this.value = null;

		this.field = null;
		this.constantTableIndex = -1;

		this.name = name;

		this.isVariable = true;
		this.isNumberType = isNumberType;
		this.isFinishedType = isFinishedType;
	}

	/* getter/setter methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public T getValue() {
		return this.value;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * COMMENT
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public Field getField() {
		return this.field;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public int getConstantTableIndex() {
		return this.constantTableIndex;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public boolean isVariable() {
		return this.isVariable;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public boolean isNumberType() {
		return this.isNumberType;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public boolean isFinishedType() {
		return this.isFinishedType;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public boolean isFinishedNumberType() {
		return this.isNumberType && this.isFinishedType;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public boolean isUnfinishedFieldType() {
		return !this.isFinishedType && this.field != null;
	}

	/* overridden methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public void replaceAll(String regex, String replacement) {
		if (!this.isFinishedType && this.name.matches(regex)) {
			if (replacement.matches("^\\d+(\\.\\d+)?d$|^\\d+(\\.\\d+)?f$|^\\d+$"))
				this.stringValue = replacement;
			else
				this.name = replacement;
		}
	}

	@Override
	public AbstractConstraintValue evaluate() {
		if (!this.isFinishedType() && this.stringValue != null) {
			if (this.stringValue.matches("^\\d+(\\.\\d+)?d$"))
				return new AbstractConstraintLiteralDouble(Double.parseDouble(this.getName()));
			else if (this.stringValue.matches("^\\d+(\\.\\d+)?f$"))
				return new AbstractConstraintLiteralFloat(Float.parseFloat(this.getName()));
			else if (this.stringValue.matches("^\\d+$"))
				return new AbstractConstraintLiteralInteger(Integer.parseInt(this.getName()));
			else
				return this;
		} else
			return this;
	}

	@Override
	public AbstractConstraintValue substitute(Map<Integer, Object> constraintArguments) {
		if (!this.isFinishedNumberType()) {
			Object constraintArgument = constraintArguments.get(this.getFieldCodeIndex());
			if (this.isVariable || this.getPreFieldList().isVariable() || constraintArgument == null)
				return this;

			for (PreField preField : this.getPreFieldList()) {
				boolean accessibility = preField.getField().isAccessible();
				preField.getField().setAccessible(true);
				try {
					constraintArgument = preField.getField().get(constraintArgument);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					String message = "could not access field \"" + preField.getField() + "\" on object \"" + constraintArgument + "\"";
					Logger.getLogger(AbstractConstraintLiteralInteger.class).fatal(message);
					throw new IllegalArgumentException(message);
				} finally {
					preField.getField().setAccessible(accessibility);
				}
			}

			boolean accessibility = this.getField().isAccessible();
			this.getField().setAccessible(true);
			try {
				constraintArgument = this.getField().get(constraintArgument);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				String message = "could not access field \"" + this.getValue() + "\" on object \"" + constraintArgument + "\"";
				Logger.getLogger(AbstractConstraintLiteralInteger.class).fatal(message);
				throw new IllegalArgumentException(message);
			} finally {
				this.getField().setAccessible(accessibility);
			}

			if (constraintArgument instanceof Integer)
				return new AbstractConstraintLiteralInteger((Integer) constraintArgument);
			else if (constraintArgument instanceof Float)
				return new AbstractConstraintLiteralFloat((Float) constraintArgument);
			else if (constraintArgument instanceof Double)
				return new AbstractConstraintLiteralDouble((Double) constraintArgument);
			else if (constraintArgument instanceof Enum<?>)
				return new AbstractConstraintLiteralInteger(((Enum<?>) constraintArgument).ordinal());
			else
				return new AbstractConstraintLiteralObject(constraintArgument);
		} else
			return this;
	}

	@Override
	public boolean matches(String regex) {
		if (this.isFinishedType)
			return this.value.toString().matches(regex);
		else
			return this.name.matches(regex);
	}

	@Override
	public Set<AbstractConstraintLiteral<?>> getUnfinishedConstraintLiterals() {
		Set<AbstractConstraintLiteral<?>> unfinishedConstraintLiterals = new HashSet<AbstractConstraintLiteral<?>>();
		if (!this.isFinishedType)
			unfinishedConstraintLiterals.add(this);

		return unfinishedConstraintLiterals;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof AbstractConstraintLiteral<?>))
			return false;

		AbstractConstraintLiteral<?> abstractConstraintLiteral = (AbstractConstraintLiteral<?>) object;

		if (this.isFinishedType())
			return this.getValue().equals(abstractConstraintLiteral.getValue());
		else
			return this.field.equals(abstractConstraintLiteral.field)
					&& this.name.equals(abstractConstraintLiteral.name)
					&& this.getFieldCodeIndex() == abstractConstraintLiteral.getFieldCodeIndex()
					&& this.getOpcode().equals(abstractConstraintLiteral.getOpcode())
					&& this.constantTableIndex == abstractConstraintLiteral.constantTableIndex
					&& this.getPreFieldList().equals(abstractConstraintLiteral.getPreFieldList());
	}

	@Override
	public String toString() {
		return this.isFinishedType ? this.value != null ? this.value.toString() : "null" : this.name;
	}

	/* abstract methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param constraintLiteral
	 * 
	 * @return
	 */
	public abstract int compareTo(AbstractConstraintLiteral<?> constraintLiteral);

	/**
	 * COMMENT
	 * 
	 * @param constraintLiteral
	 * @param operator
	 * 
	 * @return
	 */
	public abstract AbstractConstraintValue calc(AbstractConstraintLiteral<?> constraintLiteral, ArithmeticOperator operator);

	/* class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public PreField toPreField() {
		return new PreField(this.field, this.constantTableIndex, this.getOpcode(), this.getFieldCodeIndex());
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public PreFieldList toPreFieldList() {
		PreFieldList preFieldList = new PreFieldList(this.constantTableIndex, this.getOpcode());
		preFieldList.addAll(this.getPreFieldList());
		preFieldList.add(this.toPreField());
		return preFieldList;
	}
}
