package de.agra.sat.koselleck.decompiling;

/** TODO List:
 * - handle dconst and fconst
 * ----- ----- ----- ----- ----- */

/** imports */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;

import de.agra.sat.koselleck.annotations.Variable;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractBooleanConstraint;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractConstraint;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractConstraintFormula;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractConstraintLiteral;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractConstraintValue;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractPrematureConstraintValue;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractSingleConstraint;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractSubConstraint;
import de.agra.sat.koselleck.decompiling.datatypes.ArithmeticOperator;
import de.agra.sat.koselleck.decompiling.datatypes.BooleanConnector;
import de.agra.sat.koselleck.decompiling.datatypes.ConstraintOperator;
import de.agra.sat.koselleck.decompiling.datatypes.ConstraintValueType;
import de.agra.sat.koselleck.decompiling.datatypes.PrefixedClass;
import de.agra.sat.koselleck.decompiling.datatypes.PrefixedField;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLine;
import de.agra.sat.koselleck.disassembling.datatypes.DisassembledMethod;
import de.agra.sat.koselleck.disassembling.datatypes.Opcode;
import de.agra.sat.koselleck.exceptions.MissformattedBytecodeLineException;
import de.agra.sat.koselleck.exceptions.UnknownArithmeticOperatorException;
import de.agra.sat.koselleck.exceptions.UnknownConstraintOperatorException;
import de.agra.sat.koselleck.exceptions.UnknownOpcodeException;
import de.agra.sat.koselleck.exceptions.UnknownSwitchCaseException;
import de.agra.sat.koselleck.utils.KoselleckUtils;

/**
 * Decompiler implements a decompiler for java byte code.
 * 
 * @author Max Nitze
 */
public class Decompiler {
	/** stack to process on */
	private final Stack<AbstractConstraintValue> stack;
	/** store to store values */
	private final Map<Integer, AbstractConstraintValue> store;
	
	/**
	 * Private constructor for a new Decompiler.
	 */
	private Decompiler() {
		this.stack = new Stack<AbstractConstraintValue>();
		this.store = new HashMap<Integer, AbstractConstraintValue>();
	}
	
	/**
	 * decompile clears the stack and store and then returns the abstract
	 *  constraint starting at index zero of the byte code lines.
	 * 
	 * @param method the method to decompile
	 * @param bytecodeLines the byte code lines of the method to decompile
	 * @param argumentValues the method parameters
	 * 
	 * @return the abstract constraint starting at index 0 of the byte code
	 *  lines
	 */
	private AbstractConstraint decompile(Method method, Map<Integer, BytecodeLine> bytecodeLines, AbstractConstraintValue... argumentValues) {
		this.stack.clear();
		this.store.clear();
		
		this.store.put(0, new  AbstractConstraintLiteral(
				new PrefixedClass(method.getDeclaringClass(), Opcode.aload_0), ConstraintValueType.PREFIXED_CLASS, false));
		
		for(int i=0; i<argumentValues.length; i++)
			this.store.put(i+1, argumentValues[i]);
		
		return parseMethodBytecode(bytecodeLines, 0);
	}
	
	/** static methods
	 * ----- ----- ----- ----- ----- */
	
	/**
	 * decompile instantiates a new decompiler with the given disassembled
	 *  method and returns the decompiled abstract constraint.
	 * 
	 * @param disassembledMethod the disassembled method to decompile
	 * @param argumentValues the initial elements on the stack
	 * 
	 * @return the decompiled abstract constraint of the disassembled method
	 */
	public static AbstractConstraint decompile(DisassembledMethod disassembledMethod, AbstractConstraintValue... argumentValues) {
		AbstractConstraintValue[] arguments;
		if(argumentValues.length == 0 && disassembledMethod.method.getParameterTypes().length > 0) {
			arguments = new AbstractConstraintValue[disassembledMethod.method.getParameterTypes().length];
			for(int i=0; i<arguments.length; i++)
				arguments[i] = new AbstractConstraintLiteral(
						new PrefixedClass(disassembledMethod.method.getParameterTypes()[i], Opcode.aload),
						ConstraintValueType.PREFIXED_CLASS, false);
		} else
			arguments = argumentValues;
		
		return new Decompiler().decompile(disassembledMethod.method, disassembledMethod.bytecodeLines, arguments);
	}
	
	/** constraint value methods
	 * ----- ----- ----- ----- ----- */
	
	/**
	 * parseBytecode returns the constraint starting at the given index of the
	 *  map of byte code lines. Recursively every single constraint is added to
	 *  the abstract constraint.
	 * 
	 * @param method the method to decompile
	 * @param bytecodeLines the byte code lines to process
	 * @param offset the offset of the byte code line to start from
	 * 
	 * @return the abstract constraint starting at the given index
	 */
	private AbstractConstraint parseMethodBytecode(Map<Integer, BytecodeLine> bytecodeLines, int offset) {
		BytecodeLine bytecodeLine = bytecodeLines.get(offset);
		if(bytecodeLine == null)
			return new AbstractBooleanConstraint(false, null);
		
		int nextOffset;
		
		List<PrefixedField> prefixedFields = new ArrayList<PrefixedField>();
		
		PrefixedField prefixedField = null;
		PrefixedClass prefixedClass = null;
		PrefixedField newPrefixedField = null;
		
		AbstractConstraintValue constraintValue = null;
		AbstractConstraintLiteral constraintLiteral = null;
		AbstractConstraintFormula constraintFormula = null;
		AbstractConstraintValue constraintValue1 = null;
		AbstractConstraintValue constraintValue2 = null;
		AbstractConstraintLiteral constraintLiteral1 = null;
		AbstractConstraintLiteral constraintLiteral2 = null;
		
		ConstraintOperator constraintOperator = null;
		
		Method lineMethod = null;
		List<AbstractConstraintValue> argumentValues = null;
		
		List<PrefixedField> preFields = null;
		
		do {
			nextOffset = bytecodeLine.followingLineNumber;
			
			switch(bytecodeLine.opcode) {
			case aload_0:
			case aload:
				this.stack.push(this.store.get(bytecodeLine.value));
				break;
				
			case iconst:
			case bipush:
				this.stack.push(new AbstractConstraintLiteral(bytecodeLine.value, ConstraintValueType.Integer, false));
				break;
				
			case getfield:
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral)) {
					String message = "could not get field";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new MissformattedBytecodeLineException(message);
				}
				constraintLiteral = (AbstractConstraintLiteral)constraintValue;
				
				// TODO aload_0 + aload ... what to do?
				if(constraintLiteral.valueType == ConstraintValueType.PREFIXED_FIELD) {
					prefixedField = (PrefixedField)constraintLiteral.value;
					preFields.add(prefixedField);
					
					newPrefixedField = new PrefixedField(
							(Field)bytecodeLine.type.accessibleObject,
							bytecodeLine.type.accessibleObjectType,
							prefixedField.fieldCode,
							bytecodeLine.value, preFields,
							prefixedField.prefix + bytecodeLine.constantTableIndex + "_");
					
				} else if(constraintLiteral.valueType == ConstraintValueType.PREFIXED_CLASS) {
					prefixedClass = (PrefixedClass)constraintLiteral.value;
					
					preFields = new ArrayList<PrefixedField>();
					
					newPrefixedField = new PrefixedField(
							(Field)bytecodeLine.type.accessibleObject,
							bytecodeLine.type.accessibleObjectType,
							prefixedClass.fieldCode,
							bytecodeLine.value, preFields,
							"v" + bytecodeLine.constantTableIndex + "_");
					
				} else {
					String message = "could not get field";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new MissformattedBytecodeLineException(message);
				}
				
				prefixedFields.add(newPrefixedField);
				this.stack.push(new AbstractConstraintLiteral(
						newPrefixedField, ConstraintValueType.PREFIXED_FIELD, newPrefixedField.isVariable));
				break;
			case getstatic:
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral) ||
						((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.PREFIXED_FIELD) {
					String message = "could not get field";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new MissformattedBytecodeLineException(message);
				}
				
				prefixedField = (PrefixedField)(((AbstractConstraintLiteral)constraintValue).value);
				if(prefixedField.field != null)
					preFields.add(prefixedField);
				
				Field field = (Field)bytecodeLine.type.accessibleObject;
				if(field.getAnnotation(Variable.class) != null) {
					newPrefixedField = new PrefixedField(
							field,
							bytecodeLine.type.accessibleObjectType,
							prefixedField.fieldCode,
							bytecodeLine.value, preFields,
							prefixedField.prefix + bytecodeLine.constantTableIndex + "_");
					
					prefixedFields.add(newPrefixedField);
					this.stack.push(new AbstractConstraintLiteral(
							newPrefixedField, ConstraintValueType.PREFIXED_FIELD, newPrefixedField.isVariable));
				} else {
					field.setAccessible(true);
					try {
						this.stack.push(new AbstractConstraintLiteral(
								field.get(null), ConstraintValueType.fromClass(bytecodeLine.type.accessibleObjectType), false));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						String message = "could not access static field \"" + field.getName() +"\"";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new IllegalArgumentException(message);
					}
				}
				break;
				
			case checkcast:
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral) ||
						((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.PREFIXED_FIELD) {
					String message = "could not cast given value \"" + constraintValue + "\" to AbstractConstraintLiteral";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new ClassCastException(message);
				}
				
				Field cField = ((PrefixedField)((AbstractConstraintLiteral)constraintValue).value).field;
				if(!cField.getType().isAssignableFrom(bytecodeLine.type.clazz)) {
					String message = "could not cast from \"" + cField.getType().getSimpleName() + "\" to \"" + bytecodeLine.type.clazz.getSimpleName() + "\"";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new ClassCastException(message);
				}
				break;
				
			case add:
			case sub:
			case mul:
			case div:
				constraintValue2 = this.stack.pop();
				constraintValue1 = this.stack.pop();
				this.stack.push(getCalculatedValue(
						constraintValue1, ArithmeticOperator.fromOpcode(bytecodeLine.opcode), constraintValue2));
				break;
				
			case tableswitch:
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral) ||
						((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.Integer) {
					String message = "could not cast given value \"" + constraintValue + "\" to AbstractConstraintLiteral.";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new ClassCastException(message);
				}
				
				Integer caseOffset = bytecodeLine.switchOffsets.get(((Integer)((AbstractConstraintLiteral)constraintValue).value).toString());
				if(caseOffset == null) {
					caseOffset = bytecodeLine.switchOffsets.get("default");
					if(caseOffset == null) {
						String message = "no case for value \"" + ((Integer)constraintLiteral.value).toString() + "\" and no default case";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new UnknownSwitchCaseException(message);
					} else
						nextOffset = caseOffset;
				} else
					nextOffset = caseOffset;
				break;
				
			case invokestatic:
				lineMethod = (Method)bytecodeLine.type.accessibleObject;
				
				argumentValues = new ArrayList<AbstractConstraintValue>();
				boolean isStaticPremature = false;
				for(int i=0; i<lineMethod.getParameterTypes().length; i++) {
					AbstractConstraintValue argument = this.stack.pop();
					if(!isStaticPremature &&
							(!(argument instanceof AbstractConstraintLiteral) ||
									!((AbstractConstraintLiteral)argument).valueType.isFinishedType))
						isStaticPremature = true;
					argumentValues.add(argument);
				}
				Collections.reverse(argumentValues);
				
				if(isStaticPremature)
					this.stack.push(new AbstractPrematureConstraintValue(
							new AbstractConstraintLiteral(null, ConstraintValueType.NULL, false), lineMethod, argumentValues));
				else {
					Object[] args = new Object[argumentValues.size()];
					for(int i=0; i<argumentValues.size(); i++)
						args[i] = ((AbstractConstraintLiteral)argumentValues.get(i)).value;
					
					try {
						this.stack.push(new AbstractConstraintLiteral(
								lineMethod.invoke(null, args), ConstraintValueType.fromClass(lineMethod.getReturnType()), false));
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						StringBuilder arguments = new StringBuilder();
						for(Object argument : args) {
							if(arguments.length() != 0)
								arguments.append(", ");
							arguments.append(argument);
						}
						
						String message = "could not access static method \"" + lineMethod.getDeclaringClass().getCanonicalName() + "." + lineMethod.getName() +"(" + arguments + ")\"";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new IllegalArgumentException(message);
					}
				}
				
				break;
				
			case invokevirtual:
				lineMethod = (Method)bytecodeLine.type.accessibleObject;
				
				argumentValues = new ArrayList<AbstractConstraintValue>();
				boolean isVirtualPremature = false;
				for(int i=0; i<lineMethod.getParameterTypes().length; i++) {
					AbstractConstraintValue argument = this.stack.pop();
					if(!isVirtualPremature &&
							(!(argument instanceof AbstractConstraintLiteral) ||
									!((AbstractConstraintLiteral)argument).valueType.isFinishedType))
						isVirtualPremature = true;
					argumentValues.add(argument);
				}
				Collections.reverse(argumentValues);
				
				constraintValue = this.stack.pop();
				
				if(!isVirtualPremature && constraintValue instanceof AbstractConstraintLiteral &&
						((AbstractConstraintLiteral)constraintValue).valueType.hasClass(lineMethod.getDeclaringClass())) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					try {
						System.out.println("Literal = " + constraintLiteral.value.getClass() + " : " + argumentValues.size());
						
						this.stack.push(new AbstractConstraintLiteral(
								lineMethod.invoke(constraintLiteral.value, argumentValues.toArray(new Object[0])),
								ConstraintValueType.fromClass(lineMethod.getReturnType()), false));
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						
						System.out.println(e);
						
						StringBuilder arguments = new StringBuilder();
						for(Object argument : argumentValues) {
							if(arguments.length() != 0)
								arguments.append(", ");
							arguments.append(argument);
						}
						
						String message = "could not invoke method \"" + lineMethod.getName() +"(" + arguments.toString() + ")\"";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new IllegalArgumentException(message);
					}
				} else if(lineMethod.getDeclaringClass().getClassLoader() != null) {
					DisassembledMethod disassembledSubMethod = KoselleckUtils.getDisassembledMethod(lineMethod);
					AbstractConstraint abstractConstraint = new Decompiler().decompile(
							disassembledSubMethod.method, disassembledSubMethod.bytecodeLines,
							argumentValues.toArray(new AbstractConstraintValue[0]));
					
					if(abstractConstraint instanceof AbstractBooleanConstraint &&
							((AbstractBooleanConstraint)abstractConstraint).value == true)
						this.stack.push(((AbstractBooleanConstraint)abstractConstraint).returnValue);
				} else
					this.stack.push(new AbstractPrematureConstraintValue(
							constraintValue, lineMethod, argumentValues));
				
				break;
				
			case _goto:
				nextOffset = bytecodeLine.offset;
				break;
				
			case iload:
				this.stack.push(this.store.get(bytecodeLine.value));
				break;
				
			case istore:
				this.store.put(bytecodeLine.value, this.stack.pop());
				break;
			
			case ifeq:
			case ifne:
				if(bytecodeLine.opcode == Opcode.ifeq)
					constraintOperator = ConstraintOperator.EQUAL;
				else if(bytecodeLine.opcode == Opcode.ifne)
					constraintOperator = ConstraintOperator.NOT_EQUAL;
				
				constraintValue = this.stack.pop();
				
				if(constraintValue instanceof AbstractConstraintLiteral) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					
					if(constraintLiteral.valueType.isComparableNumberType) {
						if(constraintOperator.compare((Integer)constraintLiteral.value, 0))
							nextOffset = bytecodeLine.offset;
						else
							nextOffset = bytecodeLine.followingLineNumber;
					} else
						return 	new AbstractSubConstraint(
								new AbstractSubConstraint(
										new AbstractSingleConstraint(
												constraintLiteral,
												constraintOperator,
												new AbstractConstraintLiteral(0, ConstraintValueType.Integer, false),
												prefixedFields),
										BooleanConnector.AND,
										this.parseMethodBytecode(bytecodeLines, bytecodeLine.offset)),
								BooleanConnector.OR,
								new AbstractSubConstraint(
										new AbstractSingleConstraint(
												constraintLiteral,
												ConstraintOperator.fromOppositeAsciiName(constraintOperator.asciiName),
												new AbstractConstraintLiteral(0, ConstraintValueType.Integer, false),
												prefixedFields),
										BooleanConnector.AND,
										this.parseMethodBytecode(bytecodeLines, bytecodeLine.followingLineNumber)));
				} else if(constraintValue instanceof AbstractConstraintFormula) {
					constraintFormula = (AbstractConstraintFormula)constraintValue;
					
					return new AbstractSubConstraint(
							new AbstractSubConstraint(
									new AbstractSingleConstraint(
											constraintFormula,
											constraintOperator,
											new AbstractConstraintLiteral(0, ConstraintValueType.Integer, false),
											prefixedFields),
									BooleanConnector.AND,
									this.parseMethodBytecode(bytecodeLines, bytecodeLine.offset)),
							BooleanConnector.OR,
							new AbstractSubConstraint(
									new AbstractSingleConstraint(
											constraintFormula,
											ConstraintOperator.fromOppositeAsciiName(constraintOperator.asciiName),
											new AbstractConstraintLiteral(0, ConstraintValueType.Integer, false),
											prefixedFields),
									BooleanConnector.AND,
									this.parseMethodBytecode(bytecodeLines, bytecodeLine.followingLineNumber)));
				} else {
					String message = "could not cast given value \"" + constraintLiteral + "\" to AbstractConstraintLiteral.";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new ClassCastException(message);
				}
				
				break;
				
			case if_icmpne:
			case if_icmpge:
			case if_icmpgt:
			case if_icmple:
			case if_icmplt:
			case if_icmpeq:
				constraintValue2 = this.stack.pop();
				constraintValue1 = this.stack.pop();
				
				return new AbstractSubConstraint(
						new AbstractSubConstraint(
								getSingleConstraint(
										constraintValue1,
										ConstraintOperator.fromOpcode(bytecodeLine.opcode.name),
										constraintValue2,
										prefixedFields),
								BooleanConnector.AND,
								this.parseMethodBytecode(bytecodeLines, bytecodeLine.offset)),
						BooleanConnector.OR,
						new AbstractSubConstraint(
								getSingleConstraint(
										constraintValue1,
										ConstraintOperator.fromOppositeOpcode(bytecodeLine.opcode.name),
										constraintValue2,
										prefixedFields),
								BooleanConnector.AND,
								this.parseMethodBytecode(bytecodeLines, bytecodeLine.followingLineNumber)));
			
			case fcmpg:
			case fcmpl:
			case dcmpg:
			case dcmpl:
				constraintValue2 = this.stack.pop();
				constraintValue1 = this.stack.pop();
				
				if((constraintValue1 instanceof AbstractConstraintLiteral) &&
						(constraintValue1 instanceof AbstractConstraintLiteral)) {
					constraintLiteral1 = (AbstractConstraintLiteral)constraintValue1;
					constraintLiteral2 = (AbstractConstraintLiteral)constraintValue2;
					
					if((bytecodeLine.opcode == Opcode.dcmpg || bytecodeLine.opcode == Opcode.dcmpl) &&
							constraintLiteral1.valueType == ConstraintValueType.Double &&
							constraintLiteral2.valueType == ConstraintValueType.Double) {
						this.stack.push(new AbstractConstraintLiteral(
								((Double)constraintLiteral.value).compareTo((Double)constraintLiteral2.value),
								ConstraintValueType.Integer, false));
					} else if((bytecodeLine.opcode == Opcode.fcmpg || bytecodeLine.opcode == Opcode.fcmpl) &&
							constraintLiteral1.valueType == ConstraintValueType.Float &&
							constraintLiteral2.valueType == ConstraintValueType.Float) {
						this.stack.push(new AbstractConstraintLiteral(
								((Float)constraintLiteral.value).compareTo((Float)constraintLiteral2.value),
								ConstraintValueType.Integer, false));
					} else
						this.stack.push(
								new AbstractConstraintFormula(
										constraintValue1, ArithmeticOperator.SUB, constraintValue2));
				} else
					this.stack.push(
							new AbstractConstraintFormula(
									constraintValue1, ArithmeticOperator.SUB, constraintValue2));
				
				break;
			
			case ireturn:
				AbstractConstraintValue returnValue = this.stack.pop();
				
				if(returnValue instanceof AbstractConstraintLiteral) {
					AbstractConstraintLiteral returnLiteral = (AbstractConstraintLiteral)returnValue;
					switch(returnLiteral.valueType) {
					case Double:
					case Float:
					case Integer:
						return new AbstractBooleanConstraint((returnLiteral.value.equals(0) ? false : true), returnValue);
					default:
						return new AbstractBooleanConstraint(true, returnValue);
					}
				} else
					return new AbstractBooleanConstraint(true, returnValue);
				
			default:
				UnknownOpcodeException exception = new UnknownOpcodeException(bytecodeLine.opcode);
				Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
				throw exception;
			}
			
			bytecodeLine = bytecodeLines.get(nextOffset);
		} while(nextOffset > 0);
		
		/** should never happen */
		return new AbstractBooleanConstraint(true, null);
	}
	
	/**
	 * getCalculatedValue returns an abstract constraint value for the given
	 *  constraint values and the arithmetic operator. If the constraint values
	 *  are both numbers the new value is calculated, otherwise a new abstract
	 *  constraint formula is returned.
	 * 
	 * @param constraintValue1 the first abstract constraint value
	 * @param operator the arithmetic operator to calculate the values
	 * @param constraintValue2 the second abstract constraint value
	 * 
	 * @return the calculated value as an abstract constraint literal if both
	 *  values are numbers, a new abstract constraint formula with the abstract
	 *  constraint values and the arithmetic operator otherwise
	 */
	private AbstractConstraintValue getCalculatedValue(AbstractConstraintValue constraintValue1, ArithmeticOperator operator, AbstractConstraintValue constraintValue2) {
		if(
				constraintValue1 instanceof AbstractConstraintLiteral &&
				constraintValue2 instanceof AbstractConstraintLiteral) {
			AbstractConstraintLiteral constraintLiteral1 = (AbstractConstraintLiteral)constraintValue1;
			AbstractConstraintLiteral constraintLiteral2 = (AbstractConstraintLiteral)constraintValue2;
			
			switch(constraintLiteral1.valueType) {
			case Double:
				switch(constraintLiteral2.valueType) {
				case Double:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() + ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case SUB:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() - ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case MUL:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() * ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case DIV:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() / ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				case Float:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() + ((Float)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case SUB:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() - ((Float)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case MUL:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() * ((Float)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case DIV:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() / ((Float)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				case Integer:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() + ((Integer)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case SUB:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() - ((Integer)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case MUL:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() * ((Integer)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case DIV:
						return new AbstractConstraintLiteral(((Double)constraintLiteral1.value).doubleValue() / ((Integer)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				default:
					return new AbstractConstraintFormula(
							constraintValue1, operator, constraintValue2);
				}

			case Float:
				switch(constraintLiteral2.valueType) {
				case Double:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).doubleValue() + ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case SUB:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).doubleValue() - ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case MUL:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).doubleValue() * ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case DIV:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).doubleValue() / ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				case Float:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() + ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case SUB:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() - ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case MUL:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() * ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case DIV:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() / ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				case Integer:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() + ((Integer)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case SUB:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() - ((Integer)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case MUL:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() * ((Integer)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case DIV:
						return new AbstractConstraintLiteral(((Float)constraintLiteral1.value).floatValue() / ((Integer)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				default:
					return new AbstractConstraintFormula(
							constraintValue1, operator, constraintValue2);
				}
			case Integer:
				switch(constraintLiteral2.valueType) {
				case Double:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).doubleValue() + ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case SUB:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).doubleValue() - ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case MUL:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).doubleValue() * ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					case DIV:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).doubleValue() / ((Double)constraintLiteral2.value).doubleValue(), ConstraintValueType.Double, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				case Float:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).floatValue() + ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case SUB:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).floatValue() - ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case MUL:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).floatValue() * ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					case DIV:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).floatValue() / ((Float)constraintLiteral2.value).floatValue(), ConstraintValueType.Float, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				case Integer:
					switch(operator) {
					case ADD:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).intValue() + ((Integer)constraintLiteral2.value).intValue(), ConstraintValueType.Integer, false);
					case SUB:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).intValue() - ((Integer)constraintLiteral2.value).intValue(), ConstraintValueType.Integer, false);
					case MUL:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).intValue() * ((Integer)constraintLiteral2.value).intValue(), ConstraintValueType.Integer, false);
					case DIV:
						return new AbstractConstraintLiteral(((Integer)constraintLiteral1.value).intValue() / ((Integer)constraintLiteral2.value).intValue(), ConstraintValueType.Integer, false);
					default:
						UnknownArithmeticOperatorException exception = new UnknownArithmeticOperatorException(operator);
						Logger.getLogger(Decompiler.class).fatal(exception.getMessage());
						throw exception;
					}
				default:
					return new AbstractConstraintFormula(
							constraintValue1, operator, constraintValue2);
				}
				
			default:
				return new AbstractConstraintFormula(
						constraintValue1, operator, constraintValue2);
			}
		} else
			return new AbstractConstraintFormula(
					constraintValue1, operator, constraintValue2);
	}
	
	/**
	 * getSingleConstraint returns an abstract constraint for the given
	 *  abstract constraint values and the constraint operator. If the
	 *  constraint values are both numbers the boolean value is calculated,
	 *  otherwise a new abstract single constraint is returned.
	 * 
	 * @param constraintValue1 the first abstract constraint value
	 * @param constraintOperator the constraint operator
	 * @param constraintValue2 the second abstract constraint value
	 * @param prefixedFields the list of prefixed fields of the abstract
	 *  constraint values
	 * 
	 * @return the calculated boolean value as an abstract constraint if both
	 *  values are numbers, a new abstract single constraint with the abstract
	 *  constraint values and the constraint operator otherwise
	 */
	private AbstractConstraint getSingleConstraint(AbstractConstraintValue constraintValue1, ConstraintOperator constraintOperator, AbstractConstraintValue constraintValue2, List<PrefixedField> prefixedFields) {
		if(constraintValue1 instanceof AbstractConstraintLiteral &&
				constraintValue2 instanceof AbstractConstraintLiteral) {
			AbstractConstraintLiteral constraintLiteral1 = (AbstractConstraintLiteral)constraintValue1;
			AbstractConstraintLiteral constraintLiteral2 = (AbstractConstraintLiteral)constraintValue2;
			
			if(constraintLiteral1.valueType.isComparableNumberType && constraintLiteral2.valueType.isComparableNumberType) {
				switch(constraintOperator) {
				case EQUAL:
					return new AbstractBooleanConstraint(constraintLiteral1.compareTo(constraintLiteral2) == 0);
				case GREATER:
					return new AbstractBooleanConstraint(constraintLiteral1.compareTo(constraintLiteral2) > 0);
				case GREATER_EQUAL:
					return new AbstractBooleanConstraint(constraintLiteral1.compareTo(constraintLiteral2) >= 0);
				case LESS:
					return new AbstractBooleanConstraint(constraintLiteral1.compareTo(constraintLiteral2) < 0);
				case LESS_EQUAL:
					return new AbstractBooleanConstraint(constraintLiteral1.compareTo(constraintLiteral2) <= 0);
				case NOT_EQUAL:
					return new AbstractBooleanConstraint(constraintLiteral1.compareTo(constraintLiteral2) != 0);
				default:
					Logger.getLogger(Decompiler.class).fatal("constraint operator " + (constraintOperator == null ? "null" : "\"" + constraintOperator.asciiName + "\"") + " is not known");
					throw new UnknownConstraintOperatorException(constraintOperator);
				}
			} else
				return new AbstractSingleConstraint(
						constraintValue1,
						constraintOperator,
						constraintValue2,
						prefixedFields);
		} else
			return new AbstractSingleConstraint(
					constraintValue1,
					constraintOperator,
					constraintValue2,
					prefixedFields);
	}
}
