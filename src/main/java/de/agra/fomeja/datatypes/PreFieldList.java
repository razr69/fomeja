package de.agra.fomeja.datatypes;

/* imports */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class PreFieldList extends ArrayList<PreField> {
	/** generated serialisation uid */
	private static final long serialVersionUID = -4686167580108058185L;

	/** COMMENT */
	private Object object;

	/** COMMENT */
	private int variablePreFields;

	/**
	 * COMMENT
	 * 
	 * @param object
	 */
	public PreFieldList(Object object) {
		this.object = object;
		this.variablePreFields = 0;
	}

	/**
	 * COMMENT
	 * 
	 * @param object
	 * @param preFields
	 */
	public PreFieldList(Object object, List<PreField> preFields) {
		super(preFields);
		this.object = object;
		this.variablePreFields = 0;
		for (PreField preField : preFields)
			if (preField.isVariable())
				++this.variablePreFields;
	}

	/**
	 * COMMENT
	 * 
	 * @param object
	 * @param preFields
	 */
	public PreFieldList(Object object, PreField... preFields) {
		super(Arrays.asList(preFields));
		this.object = object;
		this.variablePreFields = 0;
		for (PreField preField : preFields)
			if (preField.isVariable())
				++this.variablePreFields;
	}

	/* getter/setter methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public Object getObject() {
		return this.object;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public String getName() {
		StringBuilder name = new StringBuilder();

		if (this.object instanceof Class<?>)
			name.append(((Class<?>) this.object).getSimpleName());
		else {
			name.append(this.object.getClass().getSimpleName());
			name.append("@");
			name.append(Integer.toHexString(this.object.hashCode()));
		}

		for (PreField preField : this)
			name.append("_")
				.append(preField.toString());

		return name.toString();
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public boolean isVariable() {
		return this.variablePreFields > 0;
	}

	/* class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param index
	 * 
	 * @return
	 */
	public PreFieldList head(int index) {
		return new PreFieldList(this.object, this.subList(0, index >= 0 ? index : this.size()+index));
	}

	/**
	 * COMMENT
	 * 
	 * @param index
	 * 
	 * @return
	 */
	public PreFieldList tail(int index) {
		return new PreFieldList(this.object, this.subList(index >= 0 ? index : this.size()-1+index, this.size()));
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public PreFieldList variableHead() {
		int i;
		for (i=this.size(); i>0; i--)
			if (this.get(i-1).isVariable())
				break;
		return new PreFieldList(this.object, this.subList(0, i));
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public PreField first() {
		if (this.size() > 0)
			return this.get(0);
		else
			return null;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public PreField last() {
		if (this.size() > 0)
			return this.get(this.size()-1);
		else
			return null;
	}

	/**
	 * COMMENT
	 * 
	 * @param preFieldList
	 * 
	 * @return
	 */
	public boolean isListPrefix(PreFieldList preFieldList) {
		if (this.size() < preFieldList.size())
			return false;
		for (int i=0; i<preFieldList.size() ; i++)
			if (!this.get(i).equals(preFieldList.get(i)))
				return false;
		return true;
	}

	/**
	 * COMMENT
	 * 
	 * @param index
	 * @param startingObject
	 * 
	 * @return
	 */
	public Object getFieldValue(int index, Object startingObject) {
		Object object = startingObject;
		for (int i=index; i<this.size(); i++) {
			try {
				object = this.get(i).getField().get(object);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				String message = "cannot get value of field \"" + this.get(i).getField() + "\" on object \"" + object + "\"";
				Logger.getLogger(PreFieldList.class).fatal(message);
				throw new IllegalArgumentException(message);
			}
		}

		return object;
	}

	/* overridden methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public boolean add(PreField preField) {
		boolean result = super.add(preField);
		if (result && preField.isVariable())
			++this.variablePreFields;
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends PreField> preFields) {
		boolean result = false;
		for (PreField preField : preFields)
			result |= this.add(preField);
		return result;
	}

	@Override
	public boolean remove(Object object) {
		boolean result = super.remove(object);
		if (result && object instanceof PreField && ((PreField) object).isVariable())
			--this.variablePreFields;
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof PreFieldList))
			return false;

		PreFieldList preFieldList = (PreFieldList) object;

		if (this.size() != preFieldList.size())
			return false;
		
		for (int i=0; i<this.size(); i++)
			if (!this.get(i).equals(preFieldList.get(i)))
				return false;

		return true;
	}

	@Override
	public PreFieldList clone() {
		return new PreFieldList(this.object, this);
	}
}