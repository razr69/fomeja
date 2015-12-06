package de.agra.fomeja.preprocessing;

/* imports */
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.agra.fomeja.FomejaDefaults;
import de.agra.fomeja.decompiling.expressions.ArithmeticExpr;
import de.agra.fomeja.decompiling.expressions.Expression;
import de.agra.fomeja.decompiling.expressions.IfThenElseExpr;
import de.agra.fomeja.decompiling.expressions.IfThenElseExpr.CondExprPair;
import de.agra.fomeja.decompiling.expressions.atomar.AtomExpr;
import de.agra.fomeja.decompiling.expressions.atomar.AtomIntegerExpr;
import de.agra.fomeja.decompiling.expressions.atomar.AtomStringExpr;
import de.agra.fomeja.decompiling.expressions.bool.AtomBoolExpr;
import de.agra.fomeja.decompiling.expressions.bool.BoolExpression;
import de.agra.fomeja.decompiling.expressions.bool.BoolIfThenElseExpr;
import de.agra.fomeja.decompiling.expressions.bool.CompareExpr;
import de.agra.fomeja.decompiling.expressions.bool.ConnectedBoolExpr;
import de.agra.fomeja.decompiling.expressions.bool.NotExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremAccessibleObjectExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremArrayelementExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremArraylengthExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremClasscastExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremGetfieldExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremLoopStmtExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremMethodExpr;
import de.agra.fomeja.decompiling.expressions.premature.PremStmtSeqExpr;
import de.agra.fomeja.decompiling.expressions.premature.PrematureExpr;
import de.agra.fomeja.exceptions.PreparationException;
import de.agra.fomeja.preprocessing.misc.CharSeqMap;
import de.agra.fomeja.types.BooleanConnector;
import de.agra.fomeja.types.CompareOperator;
import de.agra.fomeja.utils.ExpressionUtils;
import de.agra.fomeja.utils.RefactoringUtils;

/**
 * COMMENT
 * 
 * @author Max Nitze
 */
public class CharSeqMapPreprocessor {
	/** COMMENT */
	private static final Method equalsString = RefactoringUtils.getMethodForClass(String.class, "equals", Object.class);
	/** COMMENT */
	private static final Method lengthString = RefactoringUtils.getMethodForClass(String.class, "length");
	/** COMMENT */
	private static final Method charAtString = RefactoringUtils.getMethodForClass(String.class, "charAt", int.class);
	/** COMMENT */
	private static final Method substring1String = RefactoringUtils.getMethodForClass(String.class, "substring", int.class);
	/** COMMENT */
	private static final Method substring2String = RefactoringUtils.getMethodForClass(String.class, "substring", int.class, int.class);

	/**
	 * COMMENT
	 */
	public CharSeqMapPreprocessor() { }

	/** class methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param boolExprs
	 */
	public CharSeqMap prepare(List<BoolExpression> boolExprs) {
		boolean isNegated = false;
		LengthPairMap[] lengthPairMaps = new LengthPairMap[boolExprs.size()]; int i=0;
		for (BoolExpression boolExpr : boolExprs)
			lengthPairMaps[i++] = this.getLengthPairMap(boolExpr, isNegated);
		return this.getCharSeqMapFromLengthPairMap(this.boolMergeLengthPairMaps(BooleanConnector.AND, isNegated, lengthPairMaps));
	}

	/** private prepare methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param boolExpr
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMap(BoolExpression boolExpr, boolean isNegated) {
		if (boolExpr.hasAtomStringExprs()) {
			if (boolExpr instanceof AtomBoolExpr)
				return new LengthPairMap();
			else if (boolExpr instanceof CompareExpr)
				return this.getLengthPairMap((CompareExpr) boolExpr, isNegated);
			else if (boolExpr instanceof ConnectedBoolExpr) {
				ConnectedBoolExpr chainedBoolExpr = (ConnectedBoolExpr) boolExpr;

				LengthPairMap[] lengthPairMaps = new LengthPairMap[chainedBoolExpr.getBoolExprs().size()]; int i=0;
				for (BoolExpression cBoolExpr : chainedBoolExpr.getBoolExprs())
					lengthPairMaps[i++] = this.getLengthPairMap(cBoolExpr, isNegated);
				return this.boolMergeLengthPairMaps(chainedBoolExpr.getConnector(), isNegated, lengthPairMaps);
			} else if (boolExpr instanceof BoolIfThenElseExpr) {
				BoolIfThenElseExpr boolIfThenElseExpr = (BoolIfThenElseExpr) boolExpr;

				LengthPairMap[] lengthPairMaps = new LengthPairMap[boolIfThenElseExpr.getCondBoolExprPairs().size()+1];
				for (int i=0; i<=boolIfThenElseExpr.getCondBoolExprPairs().size(); i++) {
					LengthPairMap[] innerLengthPairMaps = new LengthPairMap[i<boolIfThenElseExpr.getCondBoolExprPairs().size() ? i+2 : i+1];
					for (int j=0; j<i; j++)
						innerLengthPairMaps[j] = this.getLengthPairMap(boolIfThenElseExpr.getCondBoolExprPairs().get(j).getCondition(), !isNegated);

					if (i<boolIfThenElseExpr.getCondBoolExprPairs().size()) {
						innerLengthPairMaps[i] = this.getLengthPairMap(boolIfThenElseExpr.getCondBoolExprPairs().get(i).getCondition(), isNegated);
						innerLengthPairMaps[i+1] = this.getLengthPairMap(boolIfThenElseExpr.getCondBoolExprPairs().get(i).getBoolExpr(), isNegated);
					} else
						innerLengthPairMaps[i] = this.getLengthPairMap(boolIfThenElseExpr.getElseCaseExpr(), isNegated);

					lengthPairMaps[i] = this.boolMergeLengthPairMaps(BooleanConnector.AND, isNegated, innerLengthPairMaps);
				}

				return this.boolMergeLengthPairMaps(BooleanConnector.OR, isNegated, lengthPairMaps);
			} else if (boolExpr instanceof NotExpr)
				return this.getLengthPairMap(((NotExpr) boolExpr).getBoolExpr(), !isNegated);
			else {
				String message = "boolean expression \"" + boolExpr + "\" of type \"" + boolExpr.getClass().getSimpleName() + "\" has string expression but is not considered for length preparation";
				Logger.getLogger(CharSeqMapPreprocessor.class).fatal(message);
				throw new PreparationException(message);
			}
		} else
			return new LengthPairMap();
	}

	/**
	 * COMMENT
	 * 
	 * @param expr
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMap(Expression expr, boolean isNegated) {
		if (expr.hasAtomStringExprs()) {
			if (expr instanceof BoolExpression)
				return this.getLengthPairMap((BoolExpression) expr, isNegated);
			else if (String.class.isAssignableFrom(expr.getResultType())) {
				LengthPairMap lengthPairMap = new LengthPairMap();
				lengthPairMap.add(this.getNamedLengthPair(expr));
				return lengthPairMap;
			} else if (expr instanceof AtomExpr<?>)
				return new LengthPairMap();
			else if (expr instanceof ArithmeticExpr) {
				ArithmeticExpr arithmeticExpr = (ArithmeticExpr) expr;
				return this.mergeLengthPairMaps(BooleanConnector.AND, isNegated, this.getLengthPairMap(arithmeticExpr.getExpr1(), isNegated), this.getLengthPairMap(arithmeticExpr.getExpr2(), isNegated));
			} else if (expr instanceof IfThenElseExpr) {
				IfThenElseExpr ifThenElseExpr = (IfThenElseExpr) expr;
				LengthPairMap[] lengthPairMaps = new LengthPairMap[ifThenElseExpr.getCondExprPairs().size()+1]; int i=0;
				for (CondExprPair condExprPair : ifThenElseExpr.getCondExprPairs())
					lengthPairMaps[i++] = this.mergeLengthPairMaps(BooleanConnector.AND, isNegated, this.getLengthPairMap(condExprPair.getCondition(), isNegated), this.getLengthPairMap(condExprPair.getExpr(), isNegated));
				return this.mergeLengthPairMaps(BooleanConnector.OR, isNegated, lengthPairMaps);
			} else if (expr instanceof PrematureExpr)
				return this.getLengthPairMap((PrematureExpr) expr, isNegated);
			else {
				String message = "can not get length pair for expression \"" + expr + "\" of unknown type \"" + expr.getClass().getSimpleName() + "\"";
				Logger.getLogger(ConstraintPreprocessor.class).fatal(message);
				throw new PreparationException(message);
			}
		} else
			return new LengthPairMap();
	}

	/**
	 * COMMENT
	 * 
	 * @param premExpr
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMap(PrematureExpr premExpr, boolean isNegated) {
		if (premExpr instanceof PremAccessibleObjectExpr<?>) {
			PremAccessibleObjectExpr<?> premAccessibelObjectExpr = (PremAccessibleObjectExpr<?>) premExpr;
			LengthPairMap[] lengthPairMaps = new LengthPairMap[premAccessibelObjectExpr.getArgumentExpressions().size()+1]; int i=0;
			lengthPairMaps[i++] = this.getLengthPairMap(premAccessibelObjectExpr.getExpr(), isNegated);
			for (Expression argExpr : premAccessibelObjectExpr.getArgumentExpressions())
				lengthPairMaps[i++] = this.getLengthPairMap(argExpr, isNegated);
			return this.mergeLengthPairMaps(BooleanConnector.AND, isNegated, lengthPairMaps);
		} else if (premExpr instanceof PremArrayelementExpr)
			return this.mergeLengthPairMaps(BooleanConnector.AND, isNegated,
					this.getLengthPairMap(((PremArrayelementExpr) premExpr).getArrayExpr(), isNegated),
					this.getLengthPairMap(((PremArrayelementExpr) premExpr).getIndexExpr(), isNegated));
		else if (premExpr instanceof PremArraylengthExpr)
			return this.getLengthPairMap(((PremArraylengthExpr) premExpr).getArrayExpr(), isNegated);
		else if (premExpr instanceof PremClasscastExpr)
			return this.getLengthPairMap(((PremClasscastExpr) premExpr).getExpr(), isNegated);
		else if (premExpr instanceof PremGetfieldExpr)
			return this.getLengthPairMap(((PremGetfieldExpr) premExpr).getExpr(), isNegated);
		else if (premExpr instanceof PremLoopStmtExpr)
			return this.getLengthPairMap(((PremLoopStmtExpr) premExpr).getSubstitutedCondition(), isNegated);
		else if (premExpr instanceof PremStmtSeqExpr)
			return new LengthPairMap(); 
		else {
			String message = "can not get length pair for premature expression \"" + premExpr + "\" of unknown type \"" + premExpr.getClass().getSimpleName() + "\"";
			Logger.getLogger(ConstraintPreprocessor.class).fatal(message);
			throw new PreparationException(message);
		}
	}

	/** private inner prepare methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param compareExpr
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMap(CompareExpr compareExpr, boolean isNegated) {
		if (compareExpr.hasAtomStringExprs()) {
			if (compareExpr.getExpr1() instanceof PremMethodExpr
					&& ((PremMethodExpr) compareExpr.getExpr1()).getMethod().equals(equalsString)
					&& ExpressionUtils.isFinishedAtomExpr(compareExpr.getExpr2(), AtomIntegerExpr.class)) {
				PremMethodExpr premMethodExpr = (PremMethodExpr) compareExpr.getExpr1();
				return this.getLengthPairMapFromEquals(
						premMethodExpr.getExpr(), CompareOperator.EQUAL, premMethodExpr.getArgumentExpressions().get(0),
						this.adjustNegatedByExpression(isNegated, compareExpr.getOperator(), (AtomIntegerExpr) compareExpr.getExpr2()));
			} else if (compareExpr.getExpr2() instanceof PremMethodExpr
					&& ((PremMethodExpr) compareExpr.getExpr2()).getMethod().equals(equalsString)
					&& ExpressionUtils.isFinishedAtomExpr(compareExpr.getExpr1(), AtomIntegerExpr.class)) {
				PremMethodExpr premMethodExpr = (PremMethodExpr) compareExpr.getExpr2();
				return this.getLengthPairMapFromEquals(
						premMethodExpr.getExpr(), CompareOperator.EQUAL, premMethodExpr.getArgumentExpressions().get(0),
						this.adjustNegatedByExpression(isNegated, compareExpr.getOperator(), (AtomIntegerExpr) compareExpr.getExpr1()));
			}
			else if (compareExpr.getExpr1() instanceof PremMethodExpr
					&& ((PremMethodExpr) compareExpr.getExpr1()).getMethod().equals(lengthString))
				return this.getLengthPairMapFromLength(((PremMethodExpr) compareExpr.getExpr1()).getExpr(), compareExpr.getOperator(), compareExpr.getExpr2(), isNegated);
			else if (compareExpr.getExpr2() instanceof PremMethodExpr
					&& ((PremMethodExpr) compareExpr.getExpr2()).getMethod().equals(lengthString))
				return this.getLengthPairMapFromLength(((PremMethodExpr) compareExpr.getExpr2()).getExpr(), compareExpr.getOperator().getOpposite(), compareExpr.getExpr1(), isNegated);

			else if (compareExpr.getExpr1() instanceof PremMethodExpr
					&& ((PremMethodExpr) compareExpr.getExpr1()).getMethod().equals(charAtString))
				return this.getLengthPairMapFromCharAt(((PremMethodExpr) compareExpr.getExpr1()).getExpr(), ((PremMethodExpr) compareExpr.getExpr1()).getArgumentExpressions().get(0), isNegated);
			else if (compareExpr.getExpr2() instanceof PremMethodExpr
					&& ((PremMethodExpr) compareExpr.getExpr2()).getMethod().equals(charAtString))
				return this.getLengthPairMapFromCharAt(((PremMethodExpr) compareExpr.getExpr2()).getExpr(), ((PremMethodExpr) compareExpr.getExpr2()).getArgumentExpressions().get(0), isNegated);

			else if (compareExpr.getExpr1() instanceof BoolExpression
					&& ExpressionUtils.isFinishedAtomExpr(compareExpr.getExpr2(), AtomIntegerExpr.class))
				return this.getLengthPairMap((BoolExpression) compareExpr.getExpr1(), this.adjustNegatedByExpression(isNegated, compareExpr.getOperator(), (AtomIntegerExpr) compareExpr.getExpr2()));
			else if (compareExpr.getExpr2() instanceof BoolExpression
					&& ExpressionUtils.isFinishedAtomExpr(compareExpr.getExpr1(), AtomIntegerExpr.class))
				return this.getLengthPairMap((BoolExpression) compareExpr.getExpr2(), this.adjustNegatedByExpression(isNegated, compareExpr.getOperator().getSwapped(), (AtomIntegerExpr) compareExpr.getExpr1()));

			else
				return new LengthPairMap();
		} else
			return new LengthPairMap();
	}

	/**
	 * COMMENT
	 * 
	 * @param stringExpr1
	 * @param operator
	 * @param stringExpr2
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMapFromEquals(Expression stringExpr1, CompareOperator operator, Expression stringExpr2, boolean isNegated) {
		LengthPairMap lengthPairMap = new LengthPairMap();
		if (!isNegated && operator == CompareOperator.EQUAL
				|| isNegated && operator == CompareOperator.NOT_EQUAL) {
			NamedLengthPair namedLengthPair1 = this.getNamedLengthPair(stringExpr1);
			NamedLengthPair namedLengthPair2 = this.getNamedLengthPair(stringExpr2);

			AtomStringExpr varStringExpr;
			int minLength, maxLength;
			if (namedLengthPair1.stringExpr.isVariable() && !namedLengthPair2.stringExpr.isVariable()) {
				varStringExpr = namedLengthPair1.stringExpr;
				minLength = namedLengthPair1.isFinished() ? namedLengthPair1.getMin() : namedLengthPair1.getMin() + namedLengthPair2.getMin();
				maxLength = namedLengthPair1.isFinished() ? namedLengthPair1.getMax() : namedLengthPair2.getMax();

				lengthPairMap.add(varStringExpr, minLength, maxLength);
			} else if (!namedLengthPair1.stringExpr.isVariable() && namedLengthPair2.stringExpr.isVariable()) {
				varStringExpr = namedLengthPair2.stringExpr;
				minLength = namedLengthPair2.isFinished() ? namedLengthPair2.getMin() : namedLengthPair2.getMin() + namedLengthPair1.getMin();
				maxLength = namedLengthPair2.isFinished() ? namedLengthPair2.getMax() : namedLengthPair1.getMax();

				lengthPairMap.add(varStringExpr, minLength, maxLength);
			}
		}

		return lengthPairMap;
	}

	/**
	 * COMMENT
	 * 
	 * @param stringExpr
	 * @param operator
	 * @param lengthExpr
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMapFromLength(Expression stringExpr, CompareOperator operator, Expression lengthExpr, boolean isNegated) {
		NamedLengthPair namedLengthPair = this.getNamedLengthPair(stringExpr);

		/** swap expressions if parameter expressions are swapped */
		if (lengthExpr instanceof PremMethodExpr
				&& ((PremMethodExpr) lengthExpr).getMethod().equals(lengthString)
				&& !namedLengthPair.stringExpr.isVariable()) {
			AtomIntegerExpr tmpLengthExpr = new AtomIntegerExpr(namedLengthPair.stringExpr.getValue().length());
			namedLengthPair = this.getNamedLengthPair(((PremMethodExpr) lengthExpr).getExpr());
			lengthExpr = tmpLengthExpr;
			operator = operator.getSwapped();
		}

		LengthPairMap lengthPairMap = new LengthPairMap();
		if (namedLengthPair.stringExpr.isVariable()) {
			int minLength = namedLengthPair.min;
			int maxLength = namedLengthPair.max;

			if (ExpressionUtils.isFinishedAtomExpr(lengthExpr, AtomIntegerExpr.class)
					&& !namedLengthPair.isFinished()) {
				switch (isNegated ? operator.getOpposite() : operator) {
				case EQUAL:
					minLength = maxLength = minLength + ((AtomIntegerExpr) lengthExpr).getValue();
					break;
				case GREATER:
					minLength += ((AtomIntegerExpr) lengthExpr).getValue()+1;
					break;
				case GREATER_EQUAL:
					minLength += ((AtomIntegerExpr) lengthExpr).getValue();
					break;
				case LESS:
					maxLength = minLength + ((AtomIntegerExpr) lengthExpr).getValue()-1;
					break;
				case LESS_EQUAL:
					maxLength = minLength + ((AtomIntegerExpr) lengthExpr).getValue();
					break;
				default:
					break;
				}
			}

			lengthPairMap.add(namedLengthPair.stringExpr, minLength, maxLength);
		}

		return lengthPairMap;
	}

	/**
	 * COMMENT
	 * 
	 * @param stringExpr
	 * @param indexExpr
	 * @param isNegated
	 * 
	 * @return
	 */
	private LengthPairMap getLengthPairMapFromCharAt(Expression stringExpr, Expression indexExpr, boolean isNegated) {
		LengthPairMap lengthPairMap = new LengthPairMap();
		NamedLengthPair namedLengthPair = this.getNamedLengthPair(stringExpr);
		
		if (ExpressionUtils.isFinishedAtomExpr(indexExpr, AtomIntegerExpr.class)
				&& ((AtomIntegerExpr) indexExpr).getValue()+1 > namedLengthPair.getMin())
			namedLengthPair.min = ((AtomIntegerExpr) indexExpr).getValue()+1;
		lengthPairMap.add(namedLengthPair);

		return lengthPairMap;
	}

	/**
	 * COMMENT
	 * 
	 * @param stringExpr
	 * 
	 * @return
	 */
	private NamedLengthPair getNamedLengthPair(Expression stringExpr) {
		if (stringExpr instanceof AtomStringExpr) {
			AtomStringExpr atomStringExpr = (AtomStringExpr) stringExpr;
			int minLength = 0, maxLength = Integer.MAX_VALUE;
			boolean isFinished = false;
			if (!atomStringExpr.isVariable()) {
				minLength = maxLength = atomStringExpr.getValue().length();
				isFinished = true;
			}
			return new NamedLengthPair((AtomStringExpr) stringExpr, minLength, maxLength, isFinished);
		} else if (stringExpr instanceof PremMethodExpr) {
			PremMethodExpr premMethodExpr = (PremMethodExpr) stringExpr;
			if (premMethodExpr.getMethod().equals(substring1String)) {
				Expression argExpr = premMethodExpr.getArgumentExpressions().get(0);

				NamedLengthPair namedLengthPair = this.getNamedLengthPair(premMethodExpr.getExpr());
				if (!namedLengthPair.isFinished() && ExpressionUtils.isFinishedAtomExpr(argExpr, AtomIntegerExpr.class))
					namedLengthPair.addSubstValue(((AtomIntegerExpr) argExpr).getValue());
				return namedLengthPair;
			} else if (premMethodExpr.getMethod().equals(substring2String)) {
				Expression argExpr2 = premMethodExpr.getArgumentExpressions().get(1);

				NamedLengthPair namedLengthPair = this.getNamedLengthPair(premMethodExpr.getExpr());
				if (!namedLengthPair.isFinished() && ExpressionUtils.isFinishedAtomExpr(argExpr2, AtomIntegerExpr.class)) {
					namedLengthPair.addSubstValue(((AtomIntegerExpr) argExpr2).getValue());
					namedLengthPair.setFinished();
				}
				return namedLengthPair;
			} else {
				String message = "can not get string length pair for premature method expression \"" + stringExpr + "\"";
				Logger.getLogger(CharSeqMapPreprocessor.class).fatal(message);
				throw new PreparationException(message);
			}
		} else {
			String message = "can not get string length pair for expression \"" + stringExpr + "\" of type \"" + stringExpr.getClass().getSimpleName() + "\"";
			Logger.getLogger(CharSeqMapPreprocessor.class).fatal(message);
			throw new PreparationException(message);
		}
	}

	/** private methods
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @param connector
	 * @param isNegated
	 * @param lengthPairMaps
	 * 
	 * @return
	 */
	private LengthPairMap boolMergeLengthPairMaps(BooleanConnector connector, boolean isNegated, LengthPairMap... lengthPairMaps) {
		return this.mergeLengthPairMaps(connector, isNegated, true, lengthPairMaps);
	}

	/**
	 * COMMENT
	 * 
	 * @param connector
	 * @param isNegated
	 * @param lengthPairMaps
	 * 
	 * @return
	 */
	private LengthPairMap mergeLengthPairMaps(BooleanConnector connector, boolean isNegated, LengthPairMap... lengthPairMaps) {
		return this.mergeLengthPairMaps(connector, isNegated, false, lengthPairMaps);
	}

	/**
	 * COMMENT
	 * 
	 * @param connector
	 * @param isNegated
	 * @param isBoolMerge
	 * @param lengthPairMaps
	 * 
	 * @return
	 */
	private LengthPairMap mergeLengthPairMaps(BooleanConnector connector, boolean isNegated, boolean isBoolMerge, LengthPairMap... lengthPairMaps) {
		Set<String> stringExprNames = new HashSet<String>();
		for (LengthPairMap lengthPairMap : lengthPairMaps)
			stringExprNames.addAll(lengthPairMap.map.keySet());

		connector = connector != null && isNegated && isBoolMerge ? connector.getOppositeConnector() : connector;

		LengthPairMap mergedLengthPairMap = new LengthPairMap();
		for (String stringExprName : stringExprNames) {
			int minLength = 0, maxLength = Integer.MAX_VALUE;
			for (LengthPairMap lengthPairMap : lengthPairMaps) {
				int curMinLength = 0, curMaxLength = Integer.MAX_VALUE;
				LengthPairMap.LengthPair lengthPair = lengthPairMap.get(stringExprName);
				if (lengthPair != null || isBoolMerge) {
					if (lengthPair != null) {
						curMinLength = lengthPair.min;
						curMaxLength = lengthPair.max;
					}

					if (connector == BooleanConnector.AND && curMinLength > minLength
							|| connector == BooleanConnector.OR && curMinLength < minLength)
						minLength = curMinLength;

					if (connector == BooleanConnector.AND && curMaxLength < maxLength
							|| connector == BooleanConnector.OR && curMaxLength > maxLength)
						maxLength = curMaxLength;
				}
			}

			mergedLengthPairMap.add(stringExprName, minLength, maxLength);
		}

		return mergedLengthPairMap;
	}

	/**
	 * COMMENT
	 * 
	 * @param lengthPairMap
	 * 
	 * @return
	 */
	private CharSeqMap getCharSeqMapFromLengthPairMap(LengthPairMap lengthPairMap) {
		CharSeqMap charSeqMap = new CharSeqMap();
		for (LengthPairMap.LengthPair lengthPair : lengthPairMap.map.values()) {
			charSeqMap.getOrCreate(lengthPair.getStringExpr()).addLengthValue(CompareOperator.GREATER_EQUAL, lengthPair.getMin());
			charSeqMap.getOrCreate(lengthPair.getStringExpr()).addLengthValue(CompareOperator.LESS_EQUAL, lengthPair.getMax());
		}

		return charSeqMap;
	}

	/**
	 * COMMENT
	 * 
	 * @param isNegated
	 * @param operator
	 * @param integerExpr
	 * 
	 * @return
	 */
	private boolean adjustNegatedByExpression(boolean isNegated, CompareOperator operator, AtomIntegerExpr integerExpr) {
		if (operator == CompareOperator.NOT_EQUAL)
			isNegated = !isNegated;
		else if (operator != CompareOperator.EQUAL) {
			String message = "can not adjust value \"isNegated\" with compare operator \"" + operator + "\"";
			Logger.getLogger(CharSeqMapPreprocessor.class).fatal(message);
			throw new PreparationException(message);
		}

		if (integerExpr.isVariable()) {
			String message = "can not adjust value \"isNegated\" with variable compare expression \"" + integerExpr + "\"";
			Logger.getLogger(CharSeqMapPreprocessor.class).fatal(message);
			throw new PreparationException(message);
		}

		if (integerExpr.getValue() == 0)
			isNegated = !isNegated;

		return isNegated;
	}

	/** inner classes
	 * ----- ----- ----- ----- ----- */

	/**
	 * COMMENT
	 * 
	 * @author Max Nitze
	 */
	private class LengthPairMap {
		/** COMMENT */
		private Map<String, LengthPair> map;

		/**
		 * COMMENT
		 */
		public LengthPairMap() {
			this.map = new HashMap<String, LengthPair>();
		}

		/** class methods
		 * ----- ----- ----- ----- ----- */

		/**
		 * COMMENT
		 * 
		 * @param name
		 * 
		 * @return
		 */
		public LengthPair get(String name) {
			return this.map.get(name);
		}

		/**
		 * COMMENT
		 * 
		 * @param name
		 * @param minLength
		 * @param maxLength
		 */
		public void add(String name, int minLength, int maxLength) {
			LengthPair lengthPair = this.get(name);
			if (lengthPair != null) {
				lengthPair.min = minLength;
				lengthPair.max = maxLength;
			} else
				this.map.put(name, new LengthPair(new AtomStringExpr(name, null), minLength, maxLength));
		}

		/**
		 * COMMENT
		 * 
		 * @param namedLengthPair
		 */
		public void add(NamedLengthPair namedLengthPair) {
			this.add(namedLengthPair.stringExpr, namedLengthPair.getMin(), namedLengthPair.getMax());
		}

		/**
		 * COMMENT
		 * 
		 * @param stringExpr
		 * @param minLength
		 * @param maxLength
		 */
		public void add(AtomStringExpr stringExpr, int minLength, int maxLength) {
			LengthPair lengthPair = this.get(stringExpr.getName());
			if (lengthPair != null) {
				lengthPair.min = minLength;
				lengthPair.max = maxLength;
			} else
				this.map.put(stringExpr.getName(), new LengthPair(stringExpr, minLength, maxLength));
		}

		/** overridden methods
		 * ----- ----- ----- ----- ----- */

		@Override
		public String toString() {
			StringBuilder stringRepresentation = new StringBuilder();
			if (!this.map.isEmpty()) {
				for (LengthPair lengthPair : this.map.values()) {
					if (stringRepresentation.length() > 0)
						stringRepresentation.append("\n");
					stringRepresentation.append(lengthPair.toString());
				}
			} else
				stringRepresentation.append("EMPTY");

			return stringRepresentation.toString();
		}

		/** inner classes
		 * ----- ----- ----- ----- ----- */

		/**
		 * COMMENT
		 * 
		 * @author Max Nitze
		 */
		public class LengthPair {
			/** COMMENT */
			private AtomStringExpr stringExpr;
			/** COMMENT */
			private int min, max;

			/**
			 * COMMENT
			 * 
			 * @param stringExpr
			 * @param min
			 * @param max
			 */
			public LengthPair(AtomStringExpr stringExpr, int min, int max) {
				this.stringExpr = stringExpr;
				this.min = min;
				this.max = max;
			}

			/** getter/setter methods
			 * ----- ----- ----- ----- ----- */

			/**
			 * COMMENT
			 * 
			 * @return
			 */
			public AtomStringExpr getStringExpr() {
				return this.stringExpr;
			}

			/**
			 * COMMENT
			 * 
			 * @return
			 */
			public int getMin() {
				return this.min;
			}

			/**
			 * COMMENT
			 * 
			 * @return
			 */
			public int getMax() {
				return max < Integer.MAX_VALUE ? max : FomejaDefaults.getDefaultStringLength();
			}

			/** overridden methods
			 * ----- ----- ----- ----- ----- */

			@Override
			public String toString() {
				return this.stringExpr + " with length " + this.getMin() + " to " + this.getMax();
			}
		}
	}

	/**
	 * COMMENT
	 * 
	 * @author Max Nitze
	 */
	private class NamedLengthPair {
		/** COMMENT */
		private AtomStringExpr stringExpr;
		/** COMMENT */
		private int min, max;
		/** COMMENT */
		private int substMin;
		/** COMMENT */
		private boolean isFinished;

		/**
		 * COMMENT
		 * 
		 * @param stringExpr
		 * @param min
		 * @param max
		 * @param isFinished
		 */
		public NamedLengthPair(AtomStringExpr stringExpr, int min, int max, boolean isFinished) {
			this.stringExpr = stringExpr;
			this.min = min;
			this.max = max;
			this.substMin = 0;
			this.isFinished = isFinished;
		}

		/**
		 * COMMENT
		 * 
		 * @return
		 */
		public int getMin() {
			return this.substMin < this.min ? this.min : this.substMin;
		}

		/**
		 * COMMENT
		 * 
		 * @return
		 */
		public int getMax() {
			return this.max;
		}

		/**
		 * COMMENT
		 * 
		 * @param value
		 */
		public void addSubstValue(int value) {
			this.substMin += value;
		}

		/**
		 * COMMENT
		 * 
		 * @return
		 */
		public boolean isFinished() {
			return this.isFinished;
		}

		/**
		 * COMMENT
		 */
		public void setFinished() {
			this.isFinished = true;
		}
	}
}
