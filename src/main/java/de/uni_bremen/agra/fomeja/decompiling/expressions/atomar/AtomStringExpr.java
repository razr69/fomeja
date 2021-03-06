package de.uni_bremen.agra.fomeja.decompiling.expressions.atomar;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.uni_bremen.agra.fomeja.datatypes.PreFieldList;
import de.uni_bremen.agra.fomeja.decompiling.expressions.Expression;
import de.uni_bremen.agra.fomeja.decompiling.misc.ComponentVariables;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class AtomStringExpr extends AtomExpr<String> {
	/** COMMENT */
	private Expression lengthExpr;
	/** COMMENT */
	private AtomCharacterExpr[] charExprs;

	/**
	 * COMMENT
	 * 
	 * @param value COMMENT
	 */
	public AtomStringExpr(String value) {
		super(value);

		this.lengthExpr = new AtomIntegerExpr(value.length());

		this.charExprs = new AtomCharacterExpr[value.length()];
		for (int i=0; i<value.length(); i++)
			this.charExprs[i] = new AtomCharacterExpr(value.charAt(i));
			
	}

	/**
	 * COMMENT
	 * 
	 * @param field COMMENT
	 * @param preFields COMMENT
	 */
	public AtomStringExpr(Field field, PreFieldList preFields) {
		super(field, preFields);

		this.lengthExpr = new AtomIntegerExpr(this.getName() + "_length");
		this.charExprs = new AtomCharacterExpr[0];
	}

	/**
	 * COMMENT
	 * 
	 * @param value COMMENT
	 * @param nullObject COMMENT
	 */
	public AtomStringExpr(String value, Object nullObject) {
		super(value, false);

		this.lengthExpr = new AtomIntegerExpr(this.getName() + "_length");
		this.charExprs = new AtomCharacterExpr[0];
	}

	/* class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public Expression getLengthExpr() {
		return this.lengthExpr;
	}

	/**
	 * COMMENT
	 * 
	 * @param index COMMENT
	 * 
	 * @return COMMENT
	 */
	public AtomCharacterExpr getCharacterExpr(int index) {
		if (this.isVariable())
			return new AtomCharacterExpr("string-" + this.getName() + "-c" + index);
		else {
			return this.charExprs[index];
		}
	}

	/* overridden methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public Class<String> getResultType() {
		return String.class;
	}

	/* overridden atomar expr methods
	 * ----- ----- ----- ----- ----- */

	@Override // TODO probably remove this here and add if (!(expr instanceof AtomStringExpr)) to LoopStmt#eval
	public Set<AtomExpr<?>> getRequiredAtomExprs(boolean isRequired, ComponentVariables compVars) {
		return new HashSet<AtomExpr<?>>();
	}

	@Override
	public boolean hasAtomStringExprs() {
		return true;
	}

	@Override
	public boolean hasStraightPreparableAtomStringExprs() {
		return true;
	}

	@Override
	public Set<AtomStringExpr> getAtomStringExprs() {
		Set<AtomStringExpr> atomStringExprs = new HashSet<AtomStringExpr>();
		atomStringExprs.add(this);
		return atomStringExprs;
	}

	/* overridden object methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof AtomStringExpr))
			return false;

		AtomStringExpr atomStringExpr = (AtomStringExpr) object;

		return super.equals(atomStringExpr)
				&& this.lengthExpr.equals(atomStringExpr.lengthExpr)
				&& Arrays.deepEquals(this.charExprs, atomStringExpr.charExprs);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(71, 89)
			.appendSuper(super.hashCode())
			.append(this.lengthExpr)
			.append(this.charExprs)
			.toHashCode();
	}

	@Override
	public AtomStringExpr clone() {
		if (this.isFinishedType())
			return new AtomStringExpr(this.getValue());
		else if (this.getField() != null) {
			AtomStringExpr expr =  new AtomStringExpr(this.getField(), this.getPreFieldList());
			expr.setName(this.getName());
			expr.setReplacedValue(this.getReplacedValue());
			return expr;
		} else {
			AtomStringExpr expr =  new AtomStringExpr(this.getName(), null);
			expr.setReplacedValue(this.getReplacedValue());
			return expr;
		}
	}

	@Override
	public String toString() {
		if (this.isVariable())
			return super.toString();
		else
			return "\"" + super.toString() + "\"";
	}
}
