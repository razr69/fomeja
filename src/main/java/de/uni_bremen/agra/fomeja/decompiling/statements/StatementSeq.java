package de.uni_bremen.agra.fomeja.decompiling.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

import de.uni_bremen.agra.fomeja.decompiling.expressions.Expression;
import de.uni_bremen.agra.fomeja.decompiling.expressions.atomar.AtomExpr;
import de.uni_bremen.agra.fomeja.decompiling.expressions.atomar.AtomStringExpr;
import de.uni_bremen.agra.fomeja.decompiling.expressions.atomar.AtomVoidExpr;
import de.uni_bremen.agra.fomeja.decompiling.misc.ComponentVariables;
import de.uni_bremen.agra.fomeja.decompiling.statements.misc.State;
import de.uni_bremen.agra.fomeja.exceptions.EvaluationException;
import de.uni_bremen.agra.fomeja.utils.ClassUtils;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class StatementSeq implements Cloneable {
	/** COMMENT */
	private List<AssignmentStmt> paramStmts;
	/** COMMENT */
	private List<Statement> stmts;
	/** COMMENT */
	private ReturnStmt returnStmt;
	/** COMMENT */
	private int storeIndex;

	/**
	 * COMMENT
	 * 
	 * @param storeIndex COMMENT
	 */
	public StatementSeq(int storeIndex) {
		this.paramStmts = new ArrayList<AssignmentStmt>();
		this.stmts = new ArrayList<Statement>();
		this.returnStmt = new ReturnStmt(new AtomVoidExpr());
		this.storeIndex = storeIndex;
	}

	/**
	 * COMMENT
	 * 
	 * @param storeIndex COMMENT
	 * @param stmts COMMENT
	 */
	public StatementSeq(int storeIndex, Statement... stmts) {
		this.paramStmts = new ArrayList<AssignmentStmt>();
		this.stmts = new ArrayList<Statement>();
		for (Statement stmt : stmts)
			this.add(stmt);
		this.returnStmt = new ReturnStmt(new AtomVoidExpr());
		this.storeIndex = storeIndex;
	}

	/* getter methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public ReturnStmt getReturnStmt() {
		return this.returnStmt;
	}

	/**
	 * COMMENT
	 * 
	 * @param returnStmt COMMENT
	 */
	public void setReturnStmt(ReturnStmt returnStmt) {
		this.returnStmt = returnStmt;
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public int getStoreIndex() {
		return this.storeIndex;
	}

	/* class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param stmt COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean add(Statement stmt) {
		return this.stmts.add(stmt);
	}

	/**
	 * COMMENT
	 * 
	 * @param stmt COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean addParam(AssignmentStmt stmt) {
		return this.paramStmts.add(stmt);
	}

	/**
	 * COMMENT
	 * 
	 * @param stmtSeq COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean add(StatementSeq stmtSeq) {
		this.returnStmt = stmtSeq.returnStmt;
		this.stmts.addAll(stmtSeq.paramStmts);
		this.stmts.addAll(stmtSeq.stmts);
		return true;
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public int length() {
		return this.stmts.size() + (this.returnStmt != null ? 1 : 0);
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean isEmpty() {
		return this.length() == 0;
	}

	/**
	 * COMMENT
	 */
	public void clear() {
		this.stmts.clear();
	}

	/* class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public Class<?> getResultType() {
		Class<?> resultType = this.returnStmt.getReturnExpr().getResultType();
		for (Statement stmt : this.stmts)
			if (stmt instanceof LoopStmt
					|| stmt instanceof ReturnStmt
					|| stmt instanceof IfThenElseStmt)
				resultType = ClassUtils.getMostCommonTypeVoidExcluded(resultType, stmt.getResultType());

		return resultType;
	}

	/**
	 * COMMENT
	 * 
	 * @param regex COMMENT
	 * @param replacement COMMENT
	 */
	public void replaceAll(String regex, Object replacement) {
		for (AssignmentStmt stmt : this.paramStmts)
			stmt.replaceAll(regex, replacement);
		for (Statement stmt : this.stmts)
			stmt.replaceAll(regex, replacement);
		this.returnStmt.replaceAll(regex, replacement);
	}

	/**
	 * COMMENT
	 * 
	 * @param regex COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean matches(String regex) {
		for (AssignmentStmt stmt : this.paramStmts)
			if (stmt.matches(regex))
				return true;
		for (Statement stmt : this.stmts)
			if (stmt.matches(regex))
				return true;
		return this.returnStmt.matches(regex);
	}

	/**
	 * COMMENT
	 * 
	 * @param exprs COMMENT
	 */
	public void substitude(Map<String, Expression> exprs) {
		this.substitudeParams(exprs);
		for (Statement stmt : this.stmts)
			stmt.substitude(exprs);
		this.returnStmt.substitude(exprs);
	}

	/**
	 * COMMENT
	 * 
	 * @param exprs COMMENT
	 */
	public void substitudeParams(Map<String, Expression> exprs) {
		for (AssignmentStmt stmt : this.paramStmts)
			stmt.substitude(exprs);
	}

	/**
	 * COMMENT
	 * 
	 * @param outerState COMMENT
	 * @param compVars COMMENT
	 * 
	 * @return COMMENT
	 */
	public FlowControlStmt evaluate(State outerState, ComponentVariables compVars) {
		State state = outerState.clone();

		for (AssignmentStmt stmt : this.paramStmts)
			stmt.evaluate(state, compVars);

		FlowControlStmt flowControlStmt = this.evaluateSeq(state, compVars, 0);
		if (flowControlStmt == null || (flowControlStmt instanceof ReturnStmt && ((ReturnStmt) flowControlStmt).getReturnExpr() instanceof AtomVoidExpr))
			flowControlStmt = this.returnStmt.evaluate(state, compVars);
		outerState.merge(state);

		return flowControlStmt;
	}

	/**
	 * COMMENT
	 * 
	 * @param state COMMENT
	 * @param compVars COMMENT
	 * @param index COMMENT
	 * 
	 * @return COMMENT
	 */
	private FlowControlStmt evaluateSeq(State state, ComponentVariables compVars, int index) {
		for (int i=index; i<this.stmts.size(); i++) {
			Statement stmt = this.stmts.get(i);
			if (stmt instanceof AssignmentStmt) {
				stmt.evaluate(state, compVars);
			} else if (stmt instanceof FlowControlStmt) {
				FlowControlStmt evalStmt = ((FlowControlStmt) stmt).evaluate(state, compVars);
				if (evalStmt instanceof ReturnStmt)
					this.evaluateVoidExprs((ReturnStmt) evalStmt, compVars, i);
				return evalStmt;
			} else if (stmt instanceof IfThenElseStmt) {
				FlowControlStmt evalStmt = ((IfThenElseStmt) stmt).evaluate(state, compVars);
				if (!(evalStmt instanceof ReturnStmt && ((ReturnStmt) evalStmt).getResultType().equals(Void.class))) {
					if (evalStmt instanceof ReturnStmt)
						this.evaluateVoidExprs((ReturnStmt) evalStmt, compVars, i);
					return evalStmt;
				}
			} else if (stmt instanceof LoopStmt) {
				ReturnStmt evalStmt = ((LoopStmt) stmt).evaluate(state, compVars);
				if (!evalStmt.getResultType().equals(Void.class)) {
					this.evaluateVoidExprs((ReturnStmt) evalStmt, compVars, i);
					return evalStmt;
				}
			} else {
				String message = "statement of type \"" + (stmt == null ? "null" : stmt.getClass().getSimpleName()) + "\" is not supported";
				Logger.getLogger(StatementSeq.class).fatal(message);
				throw new EvaluationException(message);
			}
		}

		return this.returnStmt.evaluate(state, compVars);
	}

	/**
	 * COMMENT
	 * 
	 * @param returnStmt COMMENT
	 * @param compVars COMMENT
	 * @param index COMMENT
	 */
	private void evaluateVoidExprs(ReturnStmt returnStmt, ComponentVariables compVars, int index) {
		Set<AtomVoidExpr> lastAtomVoidExprs, curAtomVoidExprs = returnStmt.getReturnExpr().getAtomVoidExprs();
		do {
			Map<String, Expression> substExprs = new HashMap<String, Expression>();

			for (AtomVoidExpr atomVoidExpr : curAtomVoidExprs) {
				FlowControlStmt innerEvalStmt = this.evaluateSeq(atomVoidExpr.getState(), compVars, index+1);
				if (innerEvalStmt instanceof ReturnStmt)
					substExprs.put(atomVoidExpr.getName(), ((ReturnStmt) innerEvalStmt).getReturnExpr());
			}

			returnStmt.substitude(substExprs);

			lastAtomVoidExprs = curAtomVoidExprs;
			curAtomVoidExprs = returnStmt.getReturnExpr().getAtomVoidExprs();
		} while (!curAtomVoidExprs.isEmpty() && !curAtomVoidExprs.containsAll(lastAtomVoidExprs));
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean evalsToBoolExpr() {
		return ClassUtils.isBooleanType(this.getResultType()) || ClassUtils.isIntegerType(this.getResultType());
	}

	/**
	 * COMMENT
	 */
	public void simplify() {
		for (AssignmentStmt stmt : this.paramStmts)
			stmt.simplify();
		for (Statement stmt : this.stmts)
			stmt.simplify();
		this.returnStmt.simplify();
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean isUnfinished() {
		for (AssignmentStmt stmt : this.paramStmts)
			if (stmt.isUnfinished())
				return true;

		for (Statement stmt : this.stmts)
			if (stmt.isUnfinished())
				return true;

		return this.returnStmt.isUnfinished();
	}

	/* class atomar expr methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param isRequired COMMENT
	 * @param compVars COMMENT
	 * @param state COMMENT
	 * 
	 * @return COMMENT
	 */
	public Set<AtomExpr<?>> getRequiredAtomExprs(boolean isRequired, ComponentVariables compVars, State state) {
		Set<AtomExpr<?>> requiredAtomExprs = new HashSet<AtomExpr<?>>();
		for (AssignmentStmt stmt : this.paramStmts)
			requiredAtomExprs.addAll(stmt.getRequiredAtomExprs(isRequired, compVars, state));
		for (Statement stmt : this.stmts)
			requiredAtomExprs.addAll(stmt.getRequiredAtomExprs(isRequired, compVars, state));
		requiredAtomExprs.addAll(this.returnStmt.getRequiredAtomExprs(isRequired, compVars, state));
		return requiredAtomExprs;
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public boolean hasAtomStringExprs() {
		for (AssignmentStmt stmt : this.paramStmts)
			if (stmt.hasAtomStringExprs())
				return true;

		for (Statement stmt : this.stmts)
			if (stmt.hasAtomStringExprs())
				return true;

		return this.returnStmt.hasAtomStringExprs();
	}

	/**
	 * COMMENT
	 * 
	 * @return COMMENT
	 */
	public Set<AtomStringExpr> getAtomStringExprs() {
		Set<AtomStringExpr> atomStringExprs = new HashSet<AtomStringExpr>();
		for (AssignmentStmt stmt : this.paramStmts)
			atomStringExprs.addAll(stmt.getAtomStringExprs());
		for (Statement stmt : this.stmts)
			atomStringExprs.addAll(stmt.getAtomStringExprs());
		atomStringExprs.addAll(this.returnStmt.getAtomStringExprs());
		return atomStringExprs;
	}

	/* overridden object methods
	 * ----- ----- ----- ----- ----- */

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof StatementSeq))
			return false;

		StatementSeq stmtSeq = (StatementSeq) object;

		if (this.paramStmts.size() != stmtSeq.paramStmts.size())
			return false;

		for (int i=0; i<this.paramStmts.size(); i++)
			if (!this.paramStmts.get(i).equals(stmtSeq.paramStmts.get(i)))
				return false;

		if (this.stmts.size() != stmtSeq.stmts.size())
			return false;

		for (int i=0; i<this.stmts.size(); i++)
			if (!this.stmts.get(i).equals(stmtSeq.stmts.get(i)))
				return false;

		return this.returnStmt.equals(stmtSeq.returnStmt);
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(167, 131)
				.append(this.returnStmt);
		for (AssignmentStmt assignmentStmt : this.paramStmts)
			hashCodeBuilder.append(assignmentStmt);
		for (Statement stmt : this.stmts)
			hashCodeBuilder.append(stmt);
		return hashCodeBuilder.toHashCode();
	}

	@Override
	public StatementSeq clone() {
		StatementSeq stmtSeq = new StatementSeq(this.storeIndex);
		for (AssignmentStmt stmt : this.paramStmts)
			stmtSeq.addParam(stmt.clone());
		for (Statement stmt : this.stmts)
			stmtSeq.add(stmt.clone());
		stmtSeq.returnStmt = this.returnStmt.clone();
		return stmtSeq;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();

		for (AssignmentStmt stmt : this.paramStmts) {
			if (stringBuilder.length() > 0)
				stringBuilder.append("\n");
			stringBuilder.append(stmt.toString());
		}

		if (stringBuilder.length() > 0)
			stringBuilder.append("\n----- ----- ----- ----- -----");

		for (Statement stmt : this.stmts) {
			if (stringBuilder.length() > 0)
				stringBuilder.append("\n");
			stringBuilder.append(stmt.toString());
		}

		if (this.returnStmt != null && !this.returnStmt.getResultType().equals(Void.class)) {
			if (stringBuilder.length() > 0)
				stringBuilder.append("\n");
			stringBuilder.append(this.returnStmt.toString());
		}

		return stringBuilder.toString();
	}
}
