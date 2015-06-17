package types;

import javaBytecodeGenerator.JavaClassGenerator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.MethodGen;

import absyn.CodeDeclaration;
import translation.Block;

public class FixtureSignature extends CodeSignature {
	
	private static int FIXTURE_NUMBER = 0;
	
	private final int fixtureNum;

	public FixtureSignature(ClassType clazz, CodeDeclaration abstractSyntax) {
		super(clazz, VoidType.INSTANCE, TypeList.EMPTY, "", abstractSyntax);
		
		fixtureNum = ++FIXTURE_NUMBER;
	}

	@Override
	protected Block addPrefixToCode(Block code) {
		return code;
	}
	
	public String toString(){
		return getDefiningClass() + ".fixture" + fixtureNum;
	}
	
	public void createFixture(JavaClassGenerator classGen) {
		MethodGen methodGen = new MethodGen
				(Constants.ACC_PUBLIC | Constants.ACC_STATIC, // public and static
				org.apache.bcel.generic.Type.VOID, // return type
				new org.apache.bcel.generic.Type[] // parameters
					{ this.getDefiningClass().toBCEL() },
				null, // parameters names: we do not care
				"fixture"+fixtureNum, // method's name
				classGen.getClassName(), // defining class
				classGen.generateJavaBytecode(getCode()), // bytecode of the method
				classGen.getConstantPool()); // constant pool
		
		// we must always call these methods before the getMethod()
		// method below. They set the number of local variables and stack
		// elements used by the code of the method
		methodGen.setMaxStack();
		methodGen.setMaxLocals();

		// we add a method to the class that we are generating
		classGen.addMethod(methodGen.getMethod());
	}
	
	public INVOKESTATIC createINVOKESTATIC(JavaClassGenerator classGen) {
		
		return (INVOKESTATIC) classGen.getFactory().createInvoke
	   			(getDefiningClass().toBCEL().toString()+"Test", // name of the class
				"fixture"+fixtureNum, // name of the method or constructor
				getReturnType().toBCEL(), // return type
				new org.apache.bcel.generic.Type[] { getDefiningClass().toBCEL() }, // parameters types
				Constants.INVOKESTATIC); // the type of invocation (static, special, ecc.)
	}

}
