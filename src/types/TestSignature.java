package types;

import javaBytecodeGenerator.JavaClassGenerator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.MethodGen;

import bytecode.RETURN;
import absyn.CodeDeclaration;
import translation.Block;

public class TestSignature extends CodeSignature {
	
	private static Type STRING_TYPE = ClassType.mk(runTime.String.class.getSimpleName());

	public TestSignature(ClassType clazz, String name, CodeDeclaration abstractSyntax) {
		super(clazz, VoidType.INSTANCE, TypeList.EMPTY, name, abstractSyntax);
	}

	@Override
	protected Block addPrefixToCode(Block code) {
		// TODO Auto-generated method stub
		return code;
	}
	
	public void createTest(JavaClassGenerator classGen) {
		
		Block testCode = getCode();
		testCode.linkTo(new Block(new RETURN(STRING_TYPE)));
		
		MethodGen methodGen = new MethodGen
				(Constants.ACC_PUBLIC | Constants.ACC_STATIC, // public and static
				STRING_TYPE.toBCEL(), // return type
				new org.apache.bcel.generic.Type[] // parameters
					{ this.getDefiningClass().toBCEL() },
				null, // parameters names: we do not care
				getName().toString(), // method's name
				classGen.getClassName(), // defining class
				classGen.generateJavaBytecode(testCode), // bytecode of the method
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
		 // we use the instruction factory in order to put automatically inside
    	// the constant pool a reference to the Java signature of this method or constructor
		
    	return (INVOKESTATIC) classGen.getFactory().createInvoke
   			(getDefiningClass().toBCEL().toString()+"Test", // name of the class
			getName().toString(), // name of the method or constructor
			STRING_TYPE.toBCEL(), // return type
			new org.apache.bcel.generic.Type[] { getDefiningClass().toBCEL() }, // parameters types
			Constants.INVOKESTATIC); // the type of invocation (static, special, ecc.)
	}
}
