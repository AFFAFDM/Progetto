package absyn;

import java.io.FileWriter;
import java.io.IOException;

import semantical.TypeChecker;
import types.ClassType;
import types.FixtureSignature;
import types.VoidType;

public class FixtureDeclaration extends CodeDeclaration {

	public FixtureDeclaration(int pos, Command body, ClassMemberDeclaration next) {
		super(pos, null, body, next);
	
	}

	@Override
	protected void toDotAux(FileWriter where) throws IOException {
		linkToNode("body", getBody().toDot(where), where);

	}

	@Override
	protected void addTo(ClassType clazz) {
		FixtureSignature fSig = new FixtureSignature(clazz, this);

		clazz.addFixture(fSig);

		// we record the signature of this constructor inside this abstract syntax
		setSignature(fSig);
	}

	@Override
	protected void typeCheckAux(ClassType currentClass) {
		TypeChecker checker;
		
		checker = new TypeChecker(VoidType.INSTANCE , currentClass.getErrorMsg());
		checker = checker.putVar("this", currentClass);

		// we type-check the body of the method in the resulting type-checker
		getBody().typeCheck(checker);

	}

}
