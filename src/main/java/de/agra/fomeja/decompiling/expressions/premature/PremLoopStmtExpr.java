package de.agra.fomeja.decompiling.expressions.premature;

/* imports */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.agra.fomeja.decompiling.expressions.Expression;
import de.agra.fomeja.decompiling.expressions.atomar.AtomExpr;
import de.agra.fomeja.decompiling.expressions.atomar.AtomStringExpr;
import de.agra.fomeja.decompiling.expressions.atomar.AtomVoidExpr;
import de.agra.fomeja.decompiling.expressions.bool.BoolExpression;
import de.agra.fomeja.decompiling.misc.ComponentVariables;
import de.agra.fomeja.decompiling.statements.LoopStmt;
import de.agra.fomeja.decompiling.statements.ReturnStmt;
import de.agra.fomeja.decompiling.statements.StatementSeq;
import de.agra.fomeja.decompiling.statements.misc.State;
import de.agra.fomeja.utils.ClassUtils;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class PremLoopStmtExpr extends PrematureExpr {
	/** COMMENT */
	private BoolExpression condition;
	/** COMMENT */
	private StatementSeq body;
	/** COMMENT */
	private Expression subsequentExpr;

	/** COMMENT */
	private Map<String, Expression> stateExprs;
	/** COMMENT */
	private ComponentVariables compVars;

	/**
	 * COMMENT
	 * 
	 * @param loopStmt
	 * @param stateExprs
	 * @param compVars
	 */
	public PremLoopStmtExpr(LoopStmt loopStmt, State state, ComponentVariables compVars) {
		this.condition = loopStmt.getCondition();
		this.body = loopStmt.getBody();
		this.subsequentExpr = new AtomVoidExpr();
		this.stateExprs = state.getExprs();
		this.compVars = compVars;
	}

	/**
	 * COMMENT
	 * 
	 * @param condition
	 * @param body
	 * @param subsequentExpr
	 * @param stateExprs
	 * @param compVars
	 */
	public PremLoopStmtExpr(BoolExpression condition, StatementSeq body, Expression subsequentExpr, Map<String, Expression> stateExprs, ComponentVariables compVars) {
		this.condition = condition;
		this.body = body;
		this.subsequentExpr = subsequentExpr;
		this.stateExprs = stateExprs;
		this.compVars = compVars;
	}

	/** getter/setter methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public BoolExpression getCondition() {
		return this.condition;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public StatementSeq getBody() {
		return this.body;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public Expression getSubsequentExpr() {
		return this.subsequentExpr;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public Map<String, Expression> getStateExprs() {
		return this.stateExprs;
	}

	/** class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public BoolExpression getSubstitutedCondition() {
		return this.condition.clone().substitude(this.stateExprs);
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	public Expression evaluate() {
		return this.evaluate(this.compVars);
	}

	/**
	 * COMMENT
	 * 
	 * @param key
	 * @param value
	 * 
	 * @return
	 */
	public Expression putStateExpr(String key, Expression value) {
		return this.stateExprs.put(key, value);
	}

	/** overridden methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public Class<?> getResultType() {
		return ClassUtils.getMostCommonTypeVoidExcluded(this.body.getResultType(), this.subsequentExpr.getResultType());
	}

	@Override
	public boolean matches(String regex) {
		return this.condition.matches(regex) || this.body.matches(regex) || this.subsequentExpr.matches(regex);
	}

	@Override
	public void replaceAll(String regex, Object replacement) {
		this.condition.replaceAll(regex, replacement);
		this.body.replaceAll(regex, replacement);
		this.subsequentExpr.replaceAll(regex, replacement);
	}

	@Override
	public PremLoopStmtExpr substitude(Map<String, Expression> exprs) {
		this.condition = this.condition.substitude(exprs);
		this.body.substitude(exprs);
		this.subsequentExpr = this.subsequentExpr.substitude(exprs);
		return this;
	}

	@Override
	public Expression evaluate(ComponentVariables compVars) {
		ReturnStmt returnStmt = new LoopStmt(this.condition, this.body).evaluate(this.getState(), compVars);
		this.evaluateVoidExprs(returnStmt, compVars);
		return returnStmt.getReturnExpr();
	}

	@Override
	public PremLoopStmtExpr simplify() {
		this.condition = this.condition.simplify();
		this.body.simplify();
		this.subsequentExpr = this.subsequentExpr.simplify();
		return this;
	}

	/** overridden atomar expr methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public Set<AtomExpr<?>> getRequiredAtomExprs(boolean isRequired, ComponentVariables compVars) {
		Set<AtomExpr<?>> requiredAtomExprs = new HashSet<AtomExpr<?>>();
		requiredAtomExprs.addAll(this.condition.getRequiredAtomExprs(isRequired, compVars));
		requiredAtomExprs.addAll(this.body.getRequiredAtomExprs(isRequired, compVars, this.getState()));
		requiredAtomExprs.addAll(this.subsequentExpr.getRequiredAtomExprs(isRequired, compVars));
		return requiredAtomExprs;
	}

	@Override
	public boolean hasAtomVoidExprs() {
		return this.condition.hasAtomVoidExprs() || this.subsequentExpr.hasAtomVoidExprs();
	}

	@Override
	public Set<AtomVoidExpr> getAtomVoidExprs() {
		Set<AtomVoidExpr> atomVoidExprs = new HashSet<AtomVoidExpr>();
		atomVoidExprs.addAll(this.condition.getAtomVoidExprs());
		atomVoidExprs.addAll(this.subsequentExpr.getAtomVoidExprs());
		return atomVoidExprs;
	}

	@Override
	public boolean hasAtomStringExprs() {
		return this.condition.hasAtomStringExprs() || this.body.hasAtomStringExprs() || this.subsequentExpr.hasAtomStringExprs();
	}

	@Override
	public Set<AtomStringExpr> getAtomStringExprs() {
		Set<AtomStringExpr> atomStringExprs = new HashSet<AtomStringExpr>();
		atomStringExprs.addAll(this.condition.getAtomStringExprs());
		atomStringExprs.addAll(this.body.getAtomStringExprs());
		atomStringExprs.addAll(this.subsequentExpr.getAtomStringExprs());
		return atomStringExprs;
	}

	/** overridden object methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof PremLoopStmtExpr))
			return false;

		PremLoopStmtExpr premLoopStmtExpr = (PremLoopStmtExpr) object;
		return this.condition.equals(premLoopStmtExpr.condition)
				&& this.body.equals(premLoopStmtExpr.body)
				&& this.subsequentExpr.equals(premLoopStmtExpr.subsequentExpr);
	}

	@Override
	public PremLoopStmtExpr clone() {
		return new PremLoopStmtExpr(this.condition.clone(), this.body.clone(), this.subsequentExpr.clone(), this.getCloneStateExprs(), this.compVars.clone());
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append("PREMATURE\n  ")
				.append(new LoopStmt(this.condition, this.body).toString().replaceAll("\n", "\n  "))
				.append("\nwith subsequent expression\n  ")
				.append(this.subsequentExpr.toString().replaceAll("\n", "\n  "))
				.append("\nwith state\n  ")
				.append(this.getState().toString().replaceAll("\n", "\n  "))
				.append("\nand component variables\n  ")
				.append(this.compVars.toString().replaceAll("\n", "\n  ")).toString();
	}

	/** private methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	private State getState() {
		State state = new State();
		state.putAll(this.stateExprs);
		return state;
	}

	/**
	 * COMMENT
	 * 
	 * @return
	 */
	private Map<String, Expression> getCloneStateExprs() {
		Map<String, Expression> clonedStateExprs = new HashMap<String, Expression>();
		for (Map.Entry<String, Expression> stateExprsEntry : this.stateExprs.entrySet())
			clonedStateExprs.put(stateExprsEntry.getKey(), stateExprsEntry.getValue().clone());
		return clonedStateExprs;
	}

	/**
	 * COMMENT
	 * 
	 * @param returnStmt
	 * @param compVars
	 */
	private void evaluateVoidExprs(ReturnStmt returnStmt, ComponentVariables compVars) {
		Expression evaluatedSubsequentExpr = this.subsequentExpr.evaluate(compVars);
		Set<AtomVoidExpr> lastAtomVoidExprs, curAtomVoidExprs = returnStmt.getReturnExpr().getAtomVoidExprs();
		do {
			Map<String, Expression> substExprs = new HashMap<String, Expression>();

			for (AtomVoidExpr atomVoidExpr : curAtomVoidExprs)
				substExprs.put(atomVoidExpr.getName(), evaluatedSubsequentExpr);

			returnStmt.substitude(substExprs);

			lastAtomVoidExprs = curAtomVoidExprs;
			curAtomVoidExprs = returnStmt.getReturnExpr().getAtomVoidExprs();
		} while (!curAtomVoidExprs.isEmpty() && !curAtomVoidExprs.containsAll(lastAtomVoidExprs));
	}
}
