package absyn;

import java.io.FileWriter;
import java.io.IOException;

import semantical.TypeChecker;
import types.ClassType;
import types.TestSignature;
import types.VoidType;

public class TestDeclaration extends CodeDeclaration {

	private String name;
	
	public TestDeclaration(int pos, String name, Command body, ClassMemberDeclaration next) {
		super(pos, null, body, next);
		this.name=name;
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void toDotAux(FileWriter where) throws IOException {
		linkToNode("name", toDot(name, where), where);
		linkToNode("body", getBody().toDot(where), where);
	}

	@Override
	protected void addTo(ClassType clazz) {
		TestSignature tSig = new TestSignature(clazz, name, this);
		
		clazz.addTest(name, tSig, this.getPos());
		// we record the signature of this constructor inside this abstract syntax
		setSignature(tSig);
	}

	@Override
	protected void typeCheckAux(ClassType currentClass) {
		TypeChecker checker;
		
		checker = new TypeChecker(VoidType.INSTANCE , currentClass.getErrorMsg(), true);
		checker = checker.putVar("this", currentClass);

		// we type-check the body of the method in the resulting type-checker
		getBody().typeCheck(checker);
		
	}

}
