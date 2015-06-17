package absyn;

import java.io.FileWriter;

import semantical.TypeChecker;
import translation.Block;
import types.ClassType;
import bytecode.NEWSTRING;
import bytecode.RETURN;

public class Assert extends Command {

	private Expression asserted;
	
	private String failureMsg;
	
	private final static ClassType STRING_TYPE = ClassType.mk(runTime.String.class.getSimpleName());

	public Assert(int pos, Expression asserted) {
		super(pos);
		
		this.asserted = asserted;
	}

	public Expression getAsserted() {
		return asserted;
	}
	
	@Override
	protected TypeChecker typeCheckAux(TypeChecker checker) {
		asserted.mustBeBoolean(checker);

		
		// assert allowed only in tests
		if(!checker.isAssertAllowed())
			error("assert allowed only in tests");
		
		failureMsg = "at " + checker.getErrorPosition(getPos());
		
		// we return the original type-checker. Hence local declarations
		// inside the then or _else are not visible after the conditional
		return checker;
	}

	@Override
	public boolean checkForDeadcode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Block translate(Block continuation) {
		
		// by making the continuation unmergeable with whatever we
		// prefix to it, we avoid duplicating it in the then and
		// else branch. This is just an optimisation!
		// Try removing this line: everything will work, but the code will be larger
		continuation.doNotMerge();
		
		Block success = new NEWSTRING("").followedBy(
							new Block(new RETURN(STRING_TYPE)));
		
		success.linkTo(continuation);
		
		Block failure = new NEWSTRING(failureMsg).followedBy(
								new Block(new RETURN(STRING_TYPE)));
		
		failure.linkTo(continuation);
		
		return asserted.translateAsTest(success, failure);
	}
	
	@Override
	protected void toDotAux(FileWriter where) throws java.io.IOException {
		if (asserted != null)
			linkToNode("asserted", asserted.toDot(where), where);
	}

}