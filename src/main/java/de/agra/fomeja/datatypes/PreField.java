package de.agra.fomeja.datatypes;

/* imports */
import java.lang.reflect.Field;

import de.agra.fomeja.annotations.Variable;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class PreField {
	/** COMMENT */
	private Field field;
	/** COMMENT */
	private boolean isVariable;

	/**
	 * COMMENT
	 * 
	 * @param field
	 */
	public PreField(Field field) {
		this.field = field;
		this.isVariable = field != null && field.getAnnotation(Variable.class) != null;
	}

	/* getter/setter methods
	 * ----- ----- ----- ----- ----- */

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
	public boolean isVariable() {
		return this.isVariable;
	}

	/* class methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof PreField))
			return false;

		PreField preField = (PreField) object;

		return this.field != null && preField.field != null && this.field.equals(preField.field);
	}

	@Override
	public String toString() {
		return this.field != null ? this.field.getName() : "NULL";
	}
}
