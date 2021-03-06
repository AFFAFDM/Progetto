package javaBytecodeGenerator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.TargetLostException;

import translation.Block;
import bytecode.BranchingBytecode;

@SuppressWarnings("serial")
public abstract class JavaClassGenerator extends ClassGen {

	/**
	 * This is a utility class of the BCEL library that helps us generate complex Java bytecodes,
	 * or bytecodes that have different forms depending on the type of their operands,
	 * or require to store constants in the constant pool. We might
	 * generate them without this helper object, but it would be really complex!
	 */

	protected final InstructionFactory factory;

	/**
	 * An empty set of interfaces, used to generate the Java class.
	 */

	protected final static String[] noInterfaces = new String[] {};

	/**
	 * Builds a class generator for the given class type.
	 *
	 * @param clazz the class type
	 * @param sigs a set of class member signatures. These are those that must be translated
	 */

	public JavaClassGenerator(String class_name, String super_class_name,
			String file_name, int access_flags, String[] interfaces,
			ConstantPoolGen cp) {
		super(class_name, super_class_name, file_name, access_flags, interfaces, cp);

		// create a new instruction factory that places the constants
		// in the previous constant pool. This is useful for generating
		// complex bytecodes that access the constant pool
		this.factory = new InstructionFactory(getConstantPool());
	}
	
	

	/**
	 * Yields the instruction factory that can be used to create complex
	 * Java bytecodes for this class generator.
	 *
	 * @return the factory
	 */

	public final InstructionFactory getFactory() {
		return factory;
	}

	/**
	 * Generates the Java bytecode for the given block of code and for all
	 * blocks reachable from it. It calls {@link #generateJavaBytecodeFollows(Block, Map, InstructionList)}
	 * and then {@link #removeRedundancies(InstructionList)}.
	 *
	 * @param block the code from which the generation starts
	 * @return the Java bytecode for {@code block} and all blocks reachable from it
	 */

	public InstructionList generateJavaBytecode(Block block) {
		InstructionList instructions = new InstructionList();

		generateJavaBytecode(block, new HashMap<Block, InstructionHandle>(), instructions);

		return removeRedundancies(instructions);
	}

	/**
	 * Auxiliary method that generates the Java bytecode for the given block
	 * of code and for all blocks reachable from it. It uses a set of processed
	 * blocks in order to avoid looping.
	 *
	 * @param block the block from which the code generation starts
	 * @param done the set of blocks which have been already processed
	 * @param instructions the Java bytecode that has already been generated.
	 *                     It gets modified in order to include the Java bytecode generated
	 *                     for {@code block} and for those that are reachable from it
	 * @return a reference to the first instruction of the Java bytecode
	 *         generated for this block
	 */

	private InstructionHandle generateJavaBytecode(Block block, Map<Block, InstructionHandle> done, InstructionList instructions) {
		// we first check if we already processed the block
		InstructionHandle result = done.get(block);
		if (result != null)
			return result;

		// this is the first time that we process the block!

		// we generate the Java bytecode for the code inside the block, and
		// we put it at the end of the instructions already generated
		result = instructions.append(block.getBytecode().generateJavaBytecode(this));

		// we record the beginning of the Java bytecode generated for the block, for future lookup
		done.put(block, result);

		// we process the following blocks
		generateJavaBytecodeFollows(block, done, instructions);

		// we return the beginning of the Java bytecode generated for the block
		return result;
	}

	/**
	 * Auxiliary method that generates the Java bytecode for the blocks
	 * that follow a given block. That Java bytecode might include some
	 * <i>glue</i>, such as the conditional Java bytecode for the branching code blocks.
	 *
	 * @param block the block for whose followers the code is being generated
	 * @param done the set of blocks which have been already processed
	 * @param instructions the Java bytecode that has already been generated from the predecessors
	 *                     and from the code of {@code block}. It gets modified
	 *                     in order to include the Java bytecode generated for the followers
	 *                     of {@code block}, including some <i>glue</i>
	 */
 
	private void generateJavaBytecodeFollows(Block block, Map<Block, InstructionHandle> done, InstructionList instructions) {
		List<Block> follows = block.getFollows();

		// this is where the Java bytecode currently ends
		InstructionHandle ourLast = instructions.getEnd();

		if (!follows.isEmpty())
			if (follows.get(0).getBytecode().getHead() instanceof BranchingBytecode) {
				// we are facing a branch due to a comparison bytecode. That bytecode
				// and its negation are at the beginning of our two following blocks

				// we get the condition of the branching
				BranchingBytecode condition = (BranchingBytecode) follows.get(0).getBytecode().getHead();

				// we append the code for the two blocks that follow the block
				InstructionHandle noH = generateJavaBytecode(follows.get(1), done, instructions);
				InstructionHandle yesH = generateJavaBytecode(follows.get(0), done, instructions);

				// in between, we put some code that jumps to yesH if condition holds, and to noH otherwise
				instructions.append(ourLast, condition.generateJavaBytecode(this, yesH, noH));
			}
			else {
				// we append the code for the first follower
				InstructionHandle followJB = generateJavaBytecode(follows.get(0), done, instructions);

				// in between, we put a goto bytecode. Note that we need it since we have no guarantee
				// that the code for follows will be appended exactly after that for the block. The
				// follows blocks might indeed have been already translated into Java bytecode, and hence
				// followJB might be an internal program point in instructions
				instructions.append(ourLast, new GOTO(followJB));
			}
	}

	/**
	 * Simplifies a piece of Java bytecode, by removing:
	 * <ul>
	 * <li> {@code nop} bytecodes
	 * <li> {@code goto} bytecodes that jump to their subsequent program point
	 * </ul>
	 *
	 * @param il the Java bytecode which must be simplified
	 * @return the same Java bytecode, simplified as above
	 */

	private InstructionList removeRedundancies(InstructionList il) {
		@SuppressWarnings("unchecked")
		Iterator<InstructionHandle> it = il.iterator();

		while (it.hasNext()) {
			InstructionHandle handle = it.next();
			Instruction instruction = handle.getInstruction();

			if (instruction instanceof org.apache.bcel.generic.NOP ||
					(instruction instanceof GOTO && ((GOTO) instruction).getTarget() == handle.getNext()))
				try {
					il.redirectBranches(handle, handle.getNext());
					il.delete(handle);
				}
				catch (TargetLostException e) {
					// impossible
				}
		}

		return il;
	}
}
