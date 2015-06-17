package javaBytecodeGenerator;

import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import types.ClassMemberSignature;
import types.ClassType;
import types.ConstructorSignature;
import types.FixtureSignature;
import types.TestSignature;
import types.TypeList;
import bytecode.NEWSTRING;

@SuppressWarnings("serial")
public class TestClassGenerator extends JavaClassGenerator{

	private final int indexOf10000;
	private final int indexOf100;

	public TestClassGenerator(ClassType clazz, Set<ClassMemberSignature> sigs) {
		super(clazz.getName()+"Test", // name of the class
			// the superclass of the Kitten Object class is set to be the Java java.lang.Object class
			clazz.getSuperclass() != null ? clazz.getSuperclass().getName() : "java.lang.Object",
			clazz.getName() + ".kit", // source file
			Constants.ACC_PUBLIC, // Java attributes: public!
			noInterfaces, // no interfaces
			new ConstantPoolGen()); // empty constant pool, at the beginning
		
	
		// we add fixtures
		for (FixtureSignature f : clazz.getFixtures())
			if (sigs == null || sigs.contains(f))
				f.createFixture(this);
		
		// we add tests
		for (TestSignature t : clazz.getTests().values())
			if (sigs == null || sigs.contains(t))
				t.createTest(this);
		
		indexOf10000 = this.getConstantPool().addInteger(10000);
		indexOf100 = this.getConstantPool().addInteger(100);
		
		InstructionList mainInstructions = new InstructionList();

		MethodGen methodGen = new MethodGen
				(Constants.ACC_PUBLIC | Constants.ACC_STATIC, // public and static
				org.apache.bcel.generic.Type.VOID, // return type
				new org.apache.bcel.generic.Type[] // parameters
					{ new org.apache.bcel.generic.ArrayType("java.lang.String", 1) },
				null, // parameters names: we do not care
				"main", // method's name
				this.getClassName(), // defining class
				mainInstructions, // bytecode of the method
				this.getConstantPool()); // constant pool
	
		mainInstructions.append(this.generateMainJavaByteCode(clazz, methodGen));
		// we must always call these methods before the getMethod()
		// method below. They set the number of local variables and stack
		// elements used by the code of the method
		methodGen.setMaxStack();
		methodGen.setMaxLocals();

		// we add a method to the class that we are generating
		this.addMethod(methodGen.getMethod());
}
	
	private InstructionList generateMainJavaByteCode(ClassType clazz, MethodGen methodGen) {
		InstructionList instructions = new InstructionList();
		
		LocalVariableGen o = methodGen.addLocalVariable("O", clazz.toBCEL(), null, null); 
		int oIndex = o.getIndex();
		
		LocalVariableGen startTime = methodGen.addLocalVariable("startTime", 
																org.apache.bcel.generic.Type.LONG, 
																null, null);
		int startIndex = startTime.getIndex();
		
		LocalVariableGen startTestTime = methodGen.addLocalVariable("startTestTime", 
																org.apache.bcel.generic.Type.LONG, 
																null, null);
		int startTestIndex = startTestTime.getIndex();
		
		LocalVariableGen testFailed = methodGen.addLocalVariable("testFailed", 
																org.apache.bcel.generic.Type.INT, 
																null, null);
		int testFailedIndex = testFailed.getIndex();
		
		LocalVariableGen testPassed = methodGen.addLocalVariable("testPassed", 
																org.apache.bcel.generic.Type.INT, 
																null, null);
		int testPassedIndex = testPassed.getIndex();
		
		LocalVariableGen outputText = methodGen.addLocalVariable("outputText", 
																STRING_TYPE.toBCEL(), 
																null, null);
		int outputIndex = outputText.getIndex();
				
		instructions.append(createGetTimeInstructions(startIndex));
		
		instructions.append(factory.createPrintln("Test execution for class " + clazz + ":"));
		
		instructions.append(createInvokeTests(clazz, oIndex, startTestIndex, testPassedIndex, testFailedIndex, outputIndex));
		
		instructions.append(createTestsReportPrint(startIndex, testPassedIndex, testFailedIndex, outputIndex));
		
		instructions.append(InstructionFactory.createReturn(Type.VOID));
		
		return instructions;
	}
	
	
	private InstructionList createObjectOfClass(ClassType clazz, int oIndex) {
		InstructionList instructions = new InstructionList();
		
		instructions.append(factory.createNew(clazz.toBCEL().toString()));
		
		instructions.append(new ASTORE(oIndex));
		
		instructions.append(new ALOAD(oIndex));
		
		ConstructorSignature constructor = clazz.constructorLookup(TypeList.EMPTY);
		
		instructions.append(factory.createInvoke
	   					(clazz.toBCEL().toString(), // name of the class
	   					constructor.getName().toString(), // name of the method or constructor
	   					constructor.getReturnType().toBCEL(), // return type
	   					constructor.getParameters().toBCEL(), // parameters types
	   					Constants.INVOKESPECIAL)); // the type of invocation (static, special, ecc.))
		
		return instructions;
	}
	
	private InstructionList createNanoTimeInvoke() {
		
		InstructionList instructions = new InstructionList();
		
		instructions.append(factory.createInvoke
							("java/lang/System", // name of the class
							"nanoTime", // name of the method or constructor
							org.apache.bcel.generic.Type.LONG, // return type
							org.apache.bcel.generic.Type.NO_ARGS, // parameters types
							Constants.INVOKESTATIC)); // the type of invocation (static, special, ecc.))
		
		return instructions;
	}
	
	private InstructionList createGetTimeInstructions(int varIndex) {
		
		InstructionList instructions = new InstructionList();
		
		instructions.append(createNanoTimeInvoke());
		
		instructions.append(InstructionFactory.createStore(org.apache.bcel.generic.Type.LONG, varIndex));
		
		return instructions;
	}
	
	private InstructionList createInvokeFixtures(ClassType clazz, int oIndex) {
		InstructionList instructions = new InstructionList();
		
		for (FixtureSignature f : clazz.getFixtures()) {
			instructions.append(new ALOAD(oIndex));
			
			instructions.append(f.createINVOKESTATIC(this));
		}
		
		return instructions;
	}
	
	private InstructionList createInvokeTests(ClassType clazz, int oIndex, int startTestIndex, int testPassedIndex, 
												int testFailedIndex, int outputIndex) {
		InstructionList instructions = new InstructionList();
		
		instructions.append(createInitializeIntVar(testPassedIndex));
		instructions.append(createInitializeIntVar(testFailedIndex));

		for (TestSignature t : clazz.getTests().values()) {
			instructions.append(createObjectOfClass(clazz, oIndex));

			instructions.append(createInvokeFixtures(clazz, oIndex));
			
			instructions.append(createGetTimeInstructions(startTestIndex));
			
			instructions.append(new ALOAD(oIndex));
			
			instructions.append(t.createINVOKESTATIC(this));
			
			instructions.append(createCheckString(t.getName(), startTestIndex, testPassedIndex, testFailedIndex, outputIndex));
			
			instructions.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
								"output", // name of the method or constructor
								org.apache.bcel.generic.Type.VOID, // return type
								org.apache.bcel.generic.Type.NO_ARGS, // parameters types
								Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)
		}
		
		return instructions;
	}
	
	private InstructionList createInitializeIntVar(int varIndex) {
		InstructionList il = new InstructionList();
		
		il.append(InstructionConstants.ICONST_0);
		il.append(InstructionFactory.createStore(org.apache.bcel.generic.Type.INT, varIndex));
		
		return il;
	}
	
	private InstructionList incrementCounter(int counterIndex) {
		InstructionList il = new InstructionList();
		
		il.append(new IINC(counterIndex, 1));
		
		return il;
	}

	private static types.Type STRING_TYPE = ClassType.mk(runTime.String.class.getSimpleName());
		
	private InstructionList createCheckString(String testName, int startTestIndex, int testPassedIndex, 
												int testFailedIndex, int outputIndex) {
		InstructionList il = new InstructionList();
		
		InstructionHandle end = il.insert((InstructionFactory.NOP));
		InstructionHandle success = il.insert(createTestResInstructions(testName, startTestIndex, 
																	testPassedIndex, true, outputIndex));
		il.insert(new org.apache.bcel.generic.GOTO(end));
		il.insert(createTestResInstructions(testName, startTestIndex, testFailedIndex, false, outputIndex));
		il.insert(new org.apache.bcel.generic.IFEQ(success));
		
		il.insert(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
									"length", // name of the method or constructor
									org.apache.bcel.generic.Type.INT, // return type
									org.apache.bcel.generic.Type.NO_ARGS, // parameters types
									Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)
		
		il.insert(InstructionConstants.DUP);
		
		return il;
	}

	private InstructionList createTestResInstructions(String testName, int startTimeIndex, int testIndex,
														boolean passed, int outputIndex) {
		InstructionList il = new InstructionList();
		
		il.append(incrementCounter(testIndex));
		
		il.append(new NEWSTRING("  - " + testName + ": " + (passed? "passed":"failed") + " ").generateJavaBytecode(this));
		
		il.append(createFormattedTimeDiffInstructions(startTimeIndex, outputIndex));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.))
		
		il.append(InstructionConstants.SWAP);
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
								"concat", // name of the method or constructor
								STRING_TYPE.toBCEL(), // return type
								new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
								Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.))
		
		il.append(new NEWSTRING("\n").generateJavaBytecode(this));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.))
		
		return il;
	}
	
	private InstructionList createFormattedTimeDiffInstructions(int firstTimeIndex, int outputIndex) {
		InstructionList il = new InstructionList();
		
		il.append(new NEWSTRING("[").generateJavaBytecode(this));
		
		il.append(createTimeDifferenceInstructions(firstTimeIndex, outputIndex));
	
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.))
		
		il.append(new NEWSTRING("ms] ").generateJavaBytecode(this));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.))
		
		return il;
	}

	private InstructionList createTimeDifferenceInstructions(int firstTimeIndex, int outputIndex) {
		InstructionList il = new InstructionList();
		
		il.append(createNanoTimeInvoke());
		
		il.append(InstructionFactory.createLoad(org.apache.bcel.generic.Type.LONG, firstTimeIndex));
		
		il.append(InstructionConstants.LSUB);
		
		il.append(InstructionConstants.L2F);
		
		il.append(new LDC(indexOf10000));
		
		il.append(InstructionConstants.I2F);
		
		il.append(InstructionConstants.FDIV);
		
		il.append(InstructionConstants.F2I); // tronca il numero affinchè abbia
		il.append(InstructionConstants.I2F); // solo due cifre dopo la virgola
		
		il.append(new LDC(indexOf100));
		
		il.append(InstructionConstants.I2F);
		
		il.append(InstructionConstants.FDIV);
		
		il.append(new NEWSTRING("").generateJavaBytecode(this));
		
		il.append(InstructionFactory.createStore(STRING_TYPE.toBCEL(), outputIndex));
		
		il.append(InstructionFactory.createLoad(STRING_TYPE.toBCEL(), outputIndex));
		
		il.append(InstructionConstants.SWAP);

		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), 
										"concat",//Constants.CONSTRUCTOR_NAME, //dovrebbe essere concat
										STRING_TYPE.toBCEL(),
										new org.apache.bcel.generic.Type[] { org.apache.bcel.generic.Type.FLOAT }, // parameters types, 
										Constants.INVOKEVIRTUAL));//Constants.INVOKESPECIAL));
				
		return il;
	}
	
	private InstructionList createTestsReportPrint(int startTimeIndex, int testPassedIndex, 
													int testFailedIndex, int outputIndex) {
		InstructionList il = new InstructionList();
		
		il.append(new NEWSTRING(" \n").generateJavaBytecode(this));
		
		il.append(new ILOAD(testPassedIndex));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
										"concat", // name of the method or constructor
										STRING_TYPE.toBCEL(), // return type
										new org.apache.bcel.generic.Type[] { org.apache.bcel.generic.Type.INT }, // parameters types
										Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)))
		
		il.append(new NEWSTRING(" test passed, ").generateJavaBytecode(this));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
										"concat", // name of the method or constructor
										STRING_TYPE.toBCEL(), // return type
										new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
										Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.))
		
		il.append(new ILOAD(testFailedIndex));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
										"concat", // name of the method or constructor
										STRING_TYPE.toBCEL(), // return type
										new org.apache.bcel.generic.Type[] { org.apache.bcel.generic.Type.INT }, // parameters types
										Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)))
		
		il.append(new NEWSTRING(" failed ").generateJavaBytecode(this));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)))
		
		il.append(createFormattedTimeDiffInstructions(startTimeIndex, outputIndex));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)))
		
		il.append(new NEWSTRING("\n").generateJavaBytecode(this));
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"concat", // name of the method or constructor
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { STRING_TYPE.toBCEL() }, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)))
		
		il.append(factory.createInvoke(STRING_TYPE.toBCEL().toString(), // name of the class
				"output", // name of the method or constructor
				org.apache.bcel.generic.Type.VOID, // return type
				org.apache.bcel.generic.Type.NO_ARGS, // parameters types
				Constants.INVOKEVIRTUAL)); // the type of invocation (static, special, ecc.)
		
		return il;
	}
}
