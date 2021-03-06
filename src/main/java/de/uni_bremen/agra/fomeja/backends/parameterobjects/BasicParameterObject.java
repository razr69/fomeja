package de.uni_bremen.agra.fomeja.backends.parameterobjects;

import de.uni_bremen.agra.fomeja.datatypes.PreFieldList;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class BasicParameterObject extends ParameterObject {
	/**
	 * COMMENT
	 * 
	 * @param preFields COMMENT
	 */
	public BasicParameterObject(PreFieldList preFields) {
		super(preFields);
	}

	/**
	 * COMMENT
	 * 
	 * @param preFields COMMENT
	 * @param dependentParameterObject COMMENT
	 */
	public BasicParameterObject(PreFieldList preFields, ObjectParameterObject dependentParameterObject) {
		super(preFields, dependentParameterObject);
	}

	/* overridden methods
	 * ----- ----- ----- ----- ----- */

	@Override
	protected Object getFieldObject(Object proverResult) {
		return proverResult;
	}
}
