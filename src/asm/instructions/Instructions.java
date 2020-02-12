package asm.instructions;

import java.util.HashMap;

public class Instructions {

	private HashMap<String, Instruction> instructions = new HashMap<>();

	public static final Instructions AQ = new Instructions();
	public static final Instructions AQ2 = new Instructions();

	static {
		AQ.instructions.put("_def", new DefInstruction());
		AQ.instructions.put("_val", new ValInstruction());
		AQ.instructions.put("_var", new VarInstruction());
		AQ.instructions.put("set", new AQSetInstruction());
		AQ.instructions.put("jump", new AQJumpInstruction());
		AQ.instructions.put("noop", new NoArgInstruction(0x01, 0, 0));
		AQ.instructions.put("push", new Arg1Instruction(0x02,0));
		AQ.instructions.put("pop", new Arg1Instruction(0x06,0));
		AQ.instructions.put("ret", new NoArgInstruction(0x06,7, 0));
		AQ.instructions.put("pshs", new Arg1Arg2Instruction(0x07));
		AQ.instructions.put("call", new Arg2Instruction(0x07, 7));
		AQ.instructions.put("getf", new Arg1Instruction(0x10, 0));
		AQ.instructions.put("setf", new Arg2Instruction(0x11, 0));
		AQ.instructions.put("clrf", new NoArgInstruction(0x11, 0, 4));
		AQ.instructions.put("qery", new Arg1Arg2Instruction(0x12));
		AQ.instructions.put("in", new Arg1Instruction(0x13, 0));
		AQ.instructions.put("out", new Arg2Instruction(0x14, 0));
		AQ.instructions.put("flip", new Arg1Instruction(0x18, 0));
		AQ.instructions.put("and", new Arg1Arg2Instruction(0x19));
		AQ.instructions.put("test", new Arg1Arg2Instruction(0x1A));
		AQ.instructions.put("nand", new Arg1Arg2Instruction(0x1B));
		AQ.instructions.put("or", new Arg1Arg2Instruction(0x1C));
		AQ.instructions.put("nor", new Arg1Arg2Instruction(0x1D));
		AQ.instructions.put("xor", new Arg1Arg2Instruction(0x1E));
		AQ.instructions.put("not", new Arg1Instruction(0x1E, 6));
		AQ.instructions.put("xnor", new Arg1Arg2Instruction(0x1F));
		AQ.instructions.put("rotl", new Arg1Arg2Instruction(0x20));
		AQ.instructions.put("rotr", new Arg1Arg2Instruction(0x21));
		AQ.instructions.put("shl", new Arg1Arg2Instruction(0x28));
		AQ.instructions.put("shlc", new Arg1Arg2Instruction(0x29));
		AQ.instructions.put("shr", new Arg1Arg2Instruction(0x2A));
		AQ.instructions.put("shrc", new Arg1Arg2Instruction(0x2B));
		AQ.instructions.put("shra", new Arg1Arg2Instruction(0x2C));
		AQ.instructions.put("add", new Arg1Arg2Instruction(0x38));
		AQ.instructions.put("inc", new Arg1Instruction(0x38, 5));
		AQ.instructions.put("addc", new Arg1Arg2Instruction(0x39));
		AQ.instructions.put("sub", new Arg1Arg2Instruction(0x3A));
		AQ.instructions.put("dec", new Arg1Instruction(0x3A, 5));
		AQ.instructions.put("subb", new Arg1Arg2Instruction(0x3B));
		AQ.instructions.put("comp", new Arg1Arg2Instruction(0x3C));
		AQ.instructions.put("chck", new Arg1Instruction(0x3C, 4));
		AQ.instructions.put("sbrc", new Arg1Arg2Instruction(0x3D));
		AQ.instructions.put("subr", new Arg1Arg2Instruction(0x3E));
		AQ.instructions.put("neg", new Arg1Instruction(0x3E, 4));

		AQ2.instructions.put("_def", new DefInstruction());
		AQ2.instructions.put("_val", new ValInstruction());
		AQ2.instructions.put("_var", new VarInstruction());
		AQ2.instructions.put("set", new AQ2SetInstruction());
		AQ2.instructions.put("jump", new AQ2JumpInstruction());
		AQ2.instructions.put("noop", new NoArgInstruction(0x01, 0, 0));
		AQ2.instructions.put("push", new AQ2PushInstruction());
		AQ2.instructions.put("pop", new Arg1Instruction(0x06, 0));
		AQ2.instructions.put("ret", new NoArgInstruction(0x06, 7, 0));
		AQ2.instructions.put("pshs", new Arg1Arg2Instruction(0x07));
		AQ2.instructions.put("call", new Arg2Instruction(0x07, 7));
		AQ2.instructions.put("getf", new Arg1Instruction(0x0C, 0));
		AQ2.instructions.put("setf", new Arg2Instruction(0x0E, 0));
		AQ2.instructions.put("out", new Arg2Instruction(0x10, 0));
		AQ2.instructions.put("bpat", new Arg1Arg2Instruction(0x14));
		AQ2.instructions.put("tpat", new Arg1Instruction(0x14, 5));
		AQ2.instructions.put("fpat", new Arg1Instruction(0x14, 4));
		AQ2.instructions.put("swap", new AQ2SwapInstruction());
		AQ2.instructions.put("qery", new Arg1Arg2Instruction(0x16));
		AQ2.instructions.put("steq", new Arg1Arg2Instruction(0x18));
		AQ2.instructions.put("flip", new Arg1Instruction(0x18, 4));
		AQ2.instructions.put("and", new Arg1Arg2Instruction(0x19));
		AQ2.instructions.put("test", new Arg1Arg2Instruction(0x1A));
		AQ2.instructions.put("nand", new Arg1Arg2Instruction(0x1B));
		AQ2.instructions.put("or", new Arg1Arg2Instruction(0x1C));
		AQ2.instructions.put("nor", new Arg1Arg2Instruction(0x1D));
		AQ2.instructions.put("xor", new Arg1Arg2Instruction(0x1E));
		AQ2.instructions.put("not", new Arg1Instruction(0x1E, 6));
		AQ2.instructions.put("xnor", new Arg1Arg2Instruction(0x1F));
		AQ2.instructions.put("rotl", new Arg1Arg2Instruction(0x20));
		AQ2.instructions.put("rotr", new Arg1Arg2Instruction(0x21));
		AQ2.instructions.put("in", new Arg1Instruction(0x23, 0));
		AQ2.instructions.put("stnq", new Arg1Instruction(0x24, 0));
		AQ2.instructions.put("shl", new Arg1Arg2Instruction(0x28));
		AQ2.instructions.put("shlc", new Arg1Arg2Instruction(0x29));
		AQ2.instructions.put("shr", new Arg1Arg2Instruction(0x2A));
		AQ2.instructions.put("shrc", new Arg1Arg2Instruction(0x2B));
		AQ2.instructions.put("shra", new Arg1Arg2Instruction(0x2C));
		AQ2.instructions.put("add", new Arg1Arg2Instruction(0x38));
		AQ2.instructions.put("inc", new Arg1Instruction(0x38, 5));
		AQ2.instructions.put("addc", new Arg1Arg2Instruction(0x39));
		AQ2.instructions.put("sub", new Arg1Arg2Instruction(0x3A));
		AQ2.instructions.put("dec", new Arg1Instruction(0x3A, 5));
		AQ2.instructions.put("subb", new Arg1Arg2Instruction(0x3B));
		AQ2.instructions.put("comp", new Arg1Arg2Instruction(0x3C));
		AQ2.instructions.put("chck", new Arg1Instruction(0x3C, 4));
		AQ2.instructions.put("sbbr", new Arg1Arg2Instruction(0x3D));
		AQ2.instructions.put("subr", new Arg1Arg2Instruction(0x3E));
		AQ2.instructions.put("neg", new Arg1Instruction(0x3E, 4));
	}

	public Instruction get(String name) {
		return instructions.get(name);
	}

}
