package de.agra.sat.koselleck.decompiling;

/** TODO List:
 * - handle dconst and fconst
 * ----- ----- ----- ----- ----- */

/** imports */
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import de.agra.sat.koselleck.decompiling.datatypes.AbstractIfThenElseConstraint;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractPrematureConstraintValue;
import de.agra.sat.koselleck.decompiling.datatypes.AbstractSingleConstraint;
import de.agra.sat.koselleck.decompiling.datatypes.ArithmeticOperator;
import de.agra.sat.koselleck.decompiling.datatypes.ConstraintOperator;
import de.agra.sat.koselleck.decompiling.datatypes.ConstraintValueType;
import de.agra.sat.koselleck.decompiling.datatypes.PrefixedClass;
import de.agra.sat.koselleck.decompiling.datatypes.PrefixedField;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLine;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLineConstantTableAccessibleObject;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLineConstantTableClass;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLineOffset;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLineTableswitch;
import de.agra.sat.koselleck.disassembling.datatypes.BytecodeLineValue;
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
		
		this.store.put(0, new AbstractConstraintLiteral(
				new PrefixedClass(method.getDeclaringClass(), Opcode.load, 0), ConstraintValueType.PrefixedClass, false));
		
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
						new PrefixedClass(disassembledMethod.method.getParameterTypes()[i], Opcode.load, i+1),
						ConstraintValueType.PrefixedClass, false);
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
		
		BytecodeLineValue bytecodeLineValue = null;
		BytecodeLineOffset bytecodeLineOffset = null;
		BytecodeLineConstantTableClass bytecodeLineConstantTableClass = null;
		BytecodeLineConstantTableAccessibleObject bytecodeLineConstantTableAccessibleObject = null;
		BytecodeLineTableswitch bytecodeLineTableswitch = null;
		
		PrefixedField prefixedField = null;
		PrefixedField innerPrefixedField = null;
		PrefixedField newPrefixedField = null;
		PrefixedClass prefixedClass = null;
		
		List<PrefixedField> prefixedFields = new ArrayList<PrefixedField>();
		
		AbstractConstraintValue constraintValue = null;
		AbstractConstraintValue innerConstraintValue = null;
		AbstractConstraintLiteral constraintLiteral = null;
		AbstractConstraintLiteral innerConstraintLiteral = null;
		AbstractConstraintValue constraintValue1 = null;
		AbstractConstraintValue constraintValue2 = null;
		AbstractConstraintLiteral constraintLiteral1 = null;
		AbstractConstraintLiteral constraintLiteral2 = null;
		
		AbstractConstraintValue returnValue = null;
		
		ConstraintOperator constraintOperator = null;
		
		Method lineMethod = null;
		Constructor<?> lineConstructor = null;
		ArgumentList argumentValues = null;
		
		do {
			nextOffset = bytecodeLine.followingLineNumber;
			
			switch(bytecodeLine.opcode) {
			case load_:
			case load:
				this.stack.push(this.store.get(((BytecodeLineValue)bytecodeLine).value));
				break;
				
			case store_:
			case store:
				this.store.put((Integer)((BytecodeLineValue)bytecodeLine).value, this.stack.pop());
				break;
				
			case _const:
			case _const_:
			case bipush:
				this.stack.push(
						new AbstractConstraintLiteral(((BytecodeLineValue)bytecodeLine).value, ConstraintValueType.Integer, false));
				break;
				
			case getfield:
				bytecodeLineConstantTableAccessibleObject = (BytecodeLineConstantTableAccessibleObject)bytecodeLine;
				
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral)) {
					String message = "could not get field";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new MissformattedBytecodeLineException(message);
				}
				
				this.stack.push(
						this.getField(bytecodeLineConstantTableAccessibleObject, (AbstractConstraintLiteral)constraintValue, prefixedFields));
				
				break;
			case getstatic:
				bytecodeLineConstantTableAccessibleObject = (BytecodeLineConstantTableAccessibleObject)bytecodeLine;
				
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral) ||
						((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.PrefixedField) {
					String message = "could not get field";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new MissformattedBytecodeLineException(message);
				}
				
				this.stack.push(
						this.getStaticField(bytecodeLineConstantTableAccessibleObject, (AbstractConstraintLiteral)constraintValue, prefixedFields));
				
				break;
				
			case checkcast:
				constraintValue = this.stack.pop();
				if(!(constraintValue instanceof AbstractConstraintLiteral) ||
						((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.PrefixedField) {
					String message = "could not cast given value \"" + constraintValue + "\" to AbstractConstraintLiteral";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new ClassCastException(message);
				}
				
				Field cField = ((PrefixedField)((AbstractConstraintLiteral)constraintValue).value).field;
				if(!cField.getType().isAssignableFrom(((BytecodeLineConstantTableClass)bytecodeLine).clazz)) {
					String message = "could not cast from \"" + cField.getType().getSimpleName() + "\" to \"" + ((BytecodeLineConstantTableClass)bytecodeLine).clazz.getSimpleName() + "\"";
					Logger.getLogger(Decompiler.class).fatal(message);
					throw new ClassCastException(message);
				}
				break;
				
			case i2d:
				constraintValue = this.stack.pop();
				
				/** check for abstract constraint literal */
				if(constraintValue instanceof AbstractConstraintLiteral) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					
					/** check for integer */
					if(constraintLiteral.valueType == ConstraintValueType.Integer) {
						/** push corresponding double to stack */
						this.stack.push(
								new AbstractConstraintLiteral(new Double((Integer)constraintLiteral.value), ConstraintValueType.Double, false));
					} else if(constraintLiteral.valueType.isComparableNumberType) {
						String message = "could not cast constraint value " + constraintLiteral.value + " to integer";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new MissformattedBytecodeLineException(message);
					} else
						this.stack.push(constraintLiteral);
				} else
					this.stack.push(constraintValue);
				
				break;
			case i2f:
				constraintValue = this.stack.pop();
				
				/** check for abstract constraint literal */
				if(constraintValue instanceof AbstractConstraintLiteral) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					
					/** check for integer */
					if(constraintLiteral.valueType == ConstraintValueType.Integer) {
						/** push corresponding double to stack */
						this.stack.push(
								new AbstractConstraintLiteral(new Float((Integer)constraintLiteral.value), ConstraintValueType.Float, false));
					} else if(constraintLiteral.valueType.isComparableNumberType) {
						String message = "could not cast constraint value " + constraintLiteral.value + " to float";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new MissformattedBytecodeLineException(message);
					} else
						this.stack.push(constraintLiteral);
				} else
					this.stack.push(constraintValue);
				
				break;
				
			case f2d:
				constraintValue = this.stack.pop();
				
				/** check for abstract constraint literal */
				if(constraintValue instanceof AbstractConstraintLiteral) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					
					/** check for integer */
					if(constraintLiteral.valueType == ConstraintValueType.Float) {
						/** push corresponding double to stack */
						this.stack.push(
								new AbstractConstraintLiteral(new Double((Float)constraintLiteral.value), ConstraintValueType.Double, false));
					} else if(constraintLiteral.valueType.isComparableNumberType) {
						String message = "could not cast constraint value " + constraintLiteral.value + " to double";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new MissformattedBytecodeLineException(message);
					} else
						this.stack.push(constraintLiteral);
				} else
					this.stack.push(constraintValue);
				
				break;
				
			case ldc:
			case ldc2_w:
				bytecodeLineValue = (BytecodeLineValue)bytecodeLine;
				this.stack.push(
						new AbstractConstraintLiteral(bytecodeLineValue.value, ConstraintValueType.fromClass(bytecodeLineValue.value.getClass()), false));
				break;
				
			case add:
			case sub:
			case mul:
			case div:
				constraintValue2 = this.stack.pop();
				constraintValue1 = this.stack.pop();
				this.stack.push(this.getCalculatedValue(
						constraintValue1, ArithmeticOperator.fromOpcode(bytecodeLine.opcode), constraintValue2));
				break;
				
			case _new:
				bytecodeLineConstantTableClass = (BytecodeLineConstantTableClass)bytecodeLine;
				
				this.stack.push(new AbstractConstraintLiteral(
						new PrefixedClass(bytecodeLineConstantTableClass.clazz, null, -1), ConstraintValueType.PrefixedClass, false));
				
				break;
				
			case invokestatic:
			case invokevirtual:
			case invokespecial:
				bytecodeLineConstantTableAccessibleObject = (BytecodeLineConstantTableAccessibleObject)bytecodeLine;
				
				/** get arguments for method or constructor */
				argumentValues = this.getArgumentList(bytecodeLineConstantTableAccessibleObject.accessibleObject);
				
				/** pop value from stack */
				constraintValue = this.stack.pop();
				
				/** no premature value and accessible object is a method that can get invoked */
				if(!argumentValues.hasPrematureArgument && constraintValue instanceof AbstractConstraintLiteral &&
						((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.PrefixedField &&
						(bytecodeLine.opcode == Opcode.invokestatic || ((AbstractConstraintLiteral)constraintValue).valueType != ConstraintValueType.PrefixedClass)) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					
					/** get argument values from abstract constraint values in argument list */
					Object[] arguments = new Object[argumentValues.size()];
					for(int i=0; i<argumentValues.size(); i++)
						arguments[i] = ((AbstractConstraintLiteral)argumentValues.get(i)).value;
					
					/**  */
					if(bytecodeLineConstantTableAccessibleObject.accessibleObject instanceof Method && bytecodeLine.opcode != Opcode.invokestatic &&
							constraintLiteral.valueType.hasClass(((Method)bytecodeLineConstantTableAccessibleObject.accessibleObject).getDeclaringClass())) {
						lineMethod = (Method)bytecodeLineConstantTableAccessibleObject.accessibleObject;
						/** try to invoke method */
						try {
							/** push result of invoked method to stack */
							this.stack.push(new AbstractConstraintLiteral(
									lineMethod.invoke(constraintLiteral.value, arguments),
									ConstraintValueType.fromClass(lineMethod.getReturnType()), false));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							String message = "could not invoke method \"" + lineMethod.toGenericString().replaceAll(".*\\s(\\S+)$", "$1") + "\"";
							Logger.getLogger(Decompiler.class).fatal(message);
							throw new IllegalArgumentException(message);
						}
					}
					/**  */
					else if(bytecodeLineConstantTableAccessibleObject.accessibleObject instanceof Method && bytecodeLine.opcode == Opcode.invokestatic &&
							constraintLiteral.valueType == ConstraintValueType.PrefixedClass) {
						lineMethod = (Method)bytecodeLineConstantTableAccessibleObject.accessibleObject;
						/** try to invoke method */
						try {
							/** push result of invoked method to stack */
							this.stack.push(new AbstractConstraintLiteral(
									lineMethod.invoke(null, arguments),
									ConstraintValueType.fromClass(lineMethod.getReturnType()), false));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							String message = "could not invoke static method \"" + lineMethod.toGenericString().replaceAll(".*\\s(\\S+)$", "$1") + "\"";
							Logger.getLogger(Decompiler.class).fatal(message);
							throw new IllegalArgumentException(message);
						}
					}
					/**  */
					else if(bytecodeLineConstantTableAccessibleObject.accessibleObject instanceof Constructor<?> && bytecodeLine.opcode != Opcode.invokestatic &&
							constraintLiteral.valueType == ConstraintValueType.PrefixedClass &&
							((PrefixedClass)(constraintLiteral.value)).clazz.equals(((Constructor<?>)bytecodeLineConstantTableAccessibleObject.accessibleObject).getDeclaringClass())) {
						lineConstructor = (Constructor<?>)bytecodeLineConstantTableAccessibleObject.accessibleObject;
						/** try to instantiate class */
						try {
							/** push new instantiation of class to stack */
							this.stack.push(new AbstractConstraintLiteral(
									lineConstructor.newInstance(arguments),
									ConstraintValueType.fromClass(lineConstructor.getDeclaringClass()), false));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e) {
							String message = "could not instantiate new \"" + lineConstructor.getDeclaringClass().getName() + "\" \"" + lineConstructor.getName() + "\"";
							Logger.getLogger(Decompiler.class).fatal(message);
							throw new IllegalArgumentException(message);
						}
					}
					/**  */
					else {
						String message = "no valid access to accessible object \"" + bytecodeLineConstantTableAccessibleObject.accessibleObject + "\"";
						Logger.getLogger(Decompiler.class).fatal(message);
						throw new IllegalArgumentException(message);
					}
				}
				/** accessible object is a method and class file can get loaded from the classloader */
				else if(bytecodeLineConstantTableAccessibleObject.accessibleObject instanceof Method &&
						((Method)bytecodeLineConstantTableAccessibleObject.accessibleObject).getDeclaringClass().getClassLoader() != null) {
					DisassembledMethod disassembledSubMethod = KoselleckUtils.getDisassembledMethod((Method)bytecodeLineConstantTableAccessibleObject.accessibleObject);
					AbstractConstraint abstractConstraint = new Decompiler().decompile(
							disassembledSubMethod.method, disassembledSubMethod.bytecodeLines,
							argumentValues.toArray(new AbstractConstraintValue[0]));
					
					if(abstractConstraint instanceof AbstractBooleanConstraint &&
							((AbstractBooleanConstraint)abstractConstraint).value == true) {
						innerConstraintValue = ((AbstractBooleanConstraint)abstractConstraint).returnValue;
						
						if(constraintValue instanceof AbstractConstraintLiteral &&
								innerConstraintValue instanceof AbstractConstraintLiteral) {
							constraintLiteral = (AbstractConstraintLiteral)constraintValue;
							innerConstraintLiteral = (AbstractConstraintLiteral)innerConstraintValue; 
							
							if(innerConstraintLiteral.valueType == ConstraintValueType.PrefixedField) {
								innerPrefixedField = (PrefixedField)innerConstraintLiteral.value;
								
								if(constraintLiteral.valueType == ConstraintValueType.PrefixedField) {
									prefixedField = (PrefixedField)constraintLiteral.value;
									
									List<PrefixedField> preFields = new ArrayList<PrefixedField>();
									preFields.addAll(prefixedField.preFields);
									preFields.add(prefixedField);
									preFields.addAll(innerPrefixedField.preFields);
									
									newPrefixedField = new PrefixedField(
										innerPrefixedField.field,
										innerPrefixedField.fieldType,
										prefixedField.fieldCode,
										prefixedField.value,
										preFields,
										prefixedField.prefix + innerPrefixedField.prefix.substring(1));
									
									prefixedFields.remove(prefixedField);
									prefixedFields.add(newPrefixedField);
								} else if(constraintLiteral.valueType == ConstraintValueType.PrefixedClass) {
									prefixedClass = (PrefixedClass)constraintLiteral.value;
									
									List<PrefixedField> preFields = new ArrayList<PrefixedField>(innerPrefixedField.preFields);
									
									newPrefixedField = new PrefixedField(
										innerPrefixedField.field,
										innerPrefixedField.fieldType,
										prefixedClass.fieldCode,
										prefixedClass.value,
										preFields,
										"v" + bytecodeLineConstantTableAccessibleObject.constantTableIndex + "_" + innerPrefixedField.prefix.substring(1));
								} else
									throw new RuntimeException("TODO outer literal is no prefixed field"); // TODO implement
								
								prefixedFields.remove(prefixedField);
								prefixedFields.add(newPrefixedField);
								this.stack.push(new AbstractConstraintLiteral(newPrefixedField));
							} else
								throw new RuntimeException("TODO invokevirtual --> inner literal is no prefixed field"); // TODO implement
						} else
							throw new RuntimeException("TODO invokevirtual --> no abstract constraint literal"); // TODO implement
					} else
						throw new RuntimeException("TODO invokevirtual --> no abstract boolean constraint"); // TODO implement
				}
				/** class file can not get loaded from the classloader (e.g. java.lang classes) */
				else
					this.stack.push(new AbstractPrematureConstraintValue(
							constraintValue, bytecodeLineConstantTableAccessibleObject.accessibleObject, argumentValues));
				
				break;
				
			case tableswitch:
				bytecodeLineTableswitch = (BytecodeLineTableswitch)bytecodeLine;
				
				constraintValue = this.stack.pop();
				
				if(constraintValue instanceof AbstractConstraintLiteral &&
						((AbstractConstraintLiteral)constraintValue).valueType == ConstraintValueType.Integer) {
					Integer caseOffset = bytecodeLineTableswitch.offsetsMap.get(((Integer)((AbstractConstraintLiteral)constraintValue).value).toString());
					if(caseOffset == null) {
						caseOffset = bytecodeLineTableswitch.offsetsMap.get("default");
						if(caseOffset == null) {
							String message = "neither a case for value \"" + ((Integer)constraintLiteral.value).toString() + "\" nor a default case";
							Logger.getLogger(Decompiler.class).fatal(message);
							throw new UnknownSwitchCaseException(message);
						} else
							nextOffset = caseOffset;
					} else
						nextOffset = caseOffset;
				} else
					return this.getTableswitchConstraint(
							constraintValue, bytecodeLineTableswitch.offsetsMap, bytecodeLineTableswitch.offsetsMap.keySet().iterator(), bytecodeLineTableswitch.offsetsMap.get("default"), bytecodeLines, prefixedFields);
				
				break;
				
			case dup:
				this.stack.push(this.stack.peek());
				break;
				
			case _goto:
				bytecodeLineOffset = (BytecodeLineOffset)bytecodeLine;
				
				nextOffset = bytecodeLineOffset.offset;
				break;
			
			case ifeq:
			case ifne:
				bytecodeLineOffset = (BytecodeLineOffset)bytecodeLine;
				
				if(bytecodeLineOffset.opcode == Opcode.ifeq)
					constraintOperator = ConstraintOperator.EQUAL;
				else if(bytecodeLineOffset.opcode == Opcode.ifne)
					constraintOperator = ConstraintOperator.NOT_EQUAL;
				
				constraintValue = this.stack.pop();
				
				if(constraintValue instanceof AbstractConstraintLiteral &&
						((AbstractConstraintLiteral)constraintLiteral).valueType.isComparableNumberType) {
					constraintLiteral = (AbstractConstraintLiteral)constraintValue;
					
					if(constraintOperator.compare((Integer)constraintLiteral.value, 0))
						nextOffset = bytecodeLineOffset.offset;
					else
						nextOffset = bytecodeLineOffset.followingLineNumber;
				} else if(constraintValue instanceof AbstractConstraintLiteral ||
						constraintValue instanceof AbstractConstraintFormula) {
					return new AbstractIfThenElseConstraint(
							new AbstractSingleConstraint(
									constraintValue, constraintOperator, 
									new AbstractConstraintLiteral(0, ConstraintValueType.Integer, false), prefixedFields),
							this.parseMethodBytecode(bytecodeLines, bytecodeLineOffset.offset),
							this.parseMethodBytecode(bytecodeLines, bytecodeLineOffset.followingLineNumber));
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
				
				bytecodeLineOffset = (BytecodeLineOffset)bytecodeLine;
				
				return new AbstractIfThenElseConstraint(
						this.getSingleConstraint(
								constraintValue1,
								ConstraintOperator.fromOpcode(bytecodeLine.opcode.name),
								constraintValue2,
								prefixedFields),
						this.parseMethodBytecode(bytecodeLines, bytecodeLineOffset.offset),
						this.parseMethodBytecode(bytecodeLines, bytecodeLineOffset.followingLineNumber));
			
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
							constraintLiteral2.valueType == ConstraintValueType.Double)
						this.stack.push(new AbstractConstraintLiteral(
								((Double)constraintLiteral.value).compareTo((Double)constraintLiteral2.value),
								ConstraintValueType.Integer, false));
					else if((bytecodeLine.opcode == Opcode.fcmpg || bytecodeLine.opcode == Opcode.fcmpl) &&
							constraintLiteral1.valueType == ConstraintValueType.Float &&
							constraintLiteral2.valueType == ConstraintValueType.Float)
						this.stack.push(new AbstractConstraintLiteral(
								((Float)constraintLiteral.value).compareTo((Float)constraintLiteral2.value),
								ConstraintValueType.Integer, false));
					else
						this.stack.push(
								new AbstractConstraintFormula(
										constraintValue1, ArithmeticOperator.SUB, constraintValue2));
				} else
					this.stack.push(
							new AbstractConstraintFormula(
									constraintValue1, ArithmeticOperator.SUB, constraintValue2));
				
				break;
			
			case _return:
				returnValue = this.stack.pop();
				
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
	
	/**
	 * TODO comment
	 * 
	 * @param constraintValue
	 * @param offsetsMap
	 * @param offsetsMapKeyIterator
	 * @param defaultOffset
	 * @param bytecodeLines
	 * @param prefixedFields
	 * 
	 * @return
	 */
	private AbstractConstraint getTableswitchConstraint(AbstractConstraintValue constraintValue, Map<String, Integer> offsetsMap, Iterator<String> offsetsMapKeyIterator, Integer defaultOffset, Map<Integer, BytecodeLine> bytecodeLines, List<PrefixedField> prefixedFields) {
		if(offsetsMapKeyIterator.hasNext()) {
			String offsetsKey = offsetsMapKeyIterator.next();
			if(offsetsKey.matches("\\d+"))
				return new AbstractIfThenElseConstraint(
						new AbstractSingleConstraint(constraintValue, ConstraintOperator.EQUAL, 
								new AbstractConstraintLiteral(Integer.parseInt(offsetsKey), ConstraintValueType.Integer, false), prefixedFields),
						this.parseMethodBytecode(bytecodeLines, offsetsMap.get(offsetsKey)),
						this.getTableswitchConstraint(constraintValue, offsetsMap, offsetsMapKeyIterator, defaultOffset, bytecodeLines, prefixedFields));
			else if(offsetsKey.equals("default"))
				return this.getTableswitchConstraint(constraintValue, offsetsMap, offsetsMapKeyIterator, defaultOffset, bytecodeLines, prefixedFields);
			else {
				String message = "case of a tableswitch needs to be integer or default case but is \"" + offsetsKey + "\"";
				Logger.getLogger(Decompiler.class).fatal(message);
				throw new MissformattedBytecodeLineException(message);
			}
		} else {
			if(defaultOffset != null)
				return this.parseMethodBytecode(bytecodeLines, defaultOffset);
			else
				return new AbstractBooleanConstraint(false);
		}
	}
	
	/**
	 * TODO comment
	 * 
	 * @param bytecodeLineConstantTableAccessibleObject
	 * @param constraintLiteral
	 * @param preFields
	 * 
	 * @return
	 */
	private AbstractConstraintValue getField(BytecodeLineConstantTableAccessibleObject bytecodeLineConstantTableAccessibleObject, AbstractConstraintLiteral constraintLiteral, List<PrefixedField> prefixedFields) {
		if(constraintLiteral.valueType == ConstraintValueType.PrefixedField) {
			PrefixedField prefixedField = (PrefixedField)constraintLiteral.value;
			
			List<PrefixedField> preFields = new ArrayList<PrefixedField>(prefixedField.preFields);
			preFields.add(prefixedField);
			
			PrefixedField newPrefixedField = new PrefixedField(
					(Field)bytecodeLineConstantTableAccessibleObject.accessibleObject,
					((Field)bytecodeLineConstantTableAccessibleObject.accessibleObject).getType(),
					prefixedField.fieldCode,
					prefixedField.value, preFields,
					prefixedField.prefieldsPrefixedName + "_" + bytecodeLineConstantTableAccessibleObject.constantTableIndex + "_");
			prefixedFields.add(newPrefixedField);
			
			return new AbstractConstraintLiteral(newPrefixedField);
			
		} else if(constraintLiteral.valueType == ConstraintValueType.PrefixedClass) {
			PrefixedClass prefixedClass = (PrefixedClass)constraintLiteral.value;
			
			PrefixedField newPrefixedField = new PrefixedField(
					(Field)bytecodeLineConstantTableAccessibleObject.accessibleObject,
					((Field)bytecodeLineConstantTableAccessibleObject.accessibleObject).getType(),
					prefixedClass.fieldCode,
					prefixedClass.value, new ArrayList<PrefixedField>(),
					"v" + bytecodeLineConstantTableAccessibleObject.constantTableIndex + "_");
			prefixedFields.add(newPrefixedField);
			
			return new AbstractConstraintLiteral(newPrefixedField);
			
		} else {
			String message = "could not get field";
			Logger.getLogger(Decompiler.class).fatal(message);
			throw new MissformattedBytecodeLineException(message);
		}
	}
	
	/**
	 * TODO comment
	 * 
	 * @param bytecodeLineConstantTableAccessibleObject
	 * @param constraintLiteral
	 * @param prefixedFields
	 * 
	 * @return
	 */
	private AbstractConstraintValue getStaticField(BytecodeLineConstantTableAccessibleObject bytecodeLineConstantTableAccessibleObject, AbstractConstraintLiteral constraintLiteral, List<PrefixedField> prefixedFields) {
		Field field = (Field)bytecodeLineConstantTableAccessibleObject.accessibleObject;
		
		/** non-variable static field */
		if(field.getAnnotation(Variable.class) == null) {
			field.setAccessible(true);
			try {
				return new AbstractConstraintLiteral(
						field.get(null), ConstraintValueType.fromClass(field.getType()), false);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				String message = "could not access static field \"" + field.getName() +"\"";
				Logger.getLogger(Decompiler.class).fatal(message);
				throw new IllegalArgumentException(message);
			}
		}
		/** variable static field */
		else
			return this.getField(bytecodeLineConstantTableAccessibleObject, constraintLiteral, prefixedFields);
	}
	
	/**
	 * 
	 * @param accessibleObject
	 * 
	 * @return
	 */
	private ArgumentList getArgumentList(AccessibleObject accessibleObject) {
		/** get count of parameters */
		int parameterCount = 0;
		if(accessibleObject instanceof Method)
			parameterCount = ((Method)accessibleObject).getParameterTypes().length;
		else if(accessibleObject instanceof Constructor<?>)
			parameterCount = ((Constructor<?>)accessibleObject).getParameterTypes().length;
		else {
			String message = "accessible object needs to be method or constructor but is \"" + accessibleObject.getClass().getName() + "\"";
			Logger.getLogger(Decompiler.class).fatal(message);
			throw new IllegalArgumentException(message);
		}
		
		/** pop argument values from stack */
		ArgumentList argumentValues = new ArgumentList();
		for(int i=0; i<parameterCount; i++) {
			AbstractConstraintValue argument = this.stack.pop();
			if(!argumentValues.hasPrematureArgument &&
					(!(argument instanceof AbstractConstraintLiteral) ||
							!((AbstractConstraintLiteral)argument).valueType.isFinishedType))
				argumentValues.hasPrematureArgument = true;
			argumentValues.add(argument);
		}
		Collections.reverse(argumentValues);
		
		return argumentValues;
	}
	
	/**
	 * 
	 * @author Max Nitze
	 */
	private class ArgumentList extends ArrayList<AbstractConstraintValue> {
		/**  */
		private static final long serialVersionUID = 4116003574027287498L;
		
		/**  */
		public boolean hasPrematureArgument;
		
		public ArgumentList() {
			super();
			this.hasPrematureArgument = false;
		}
	}
}
