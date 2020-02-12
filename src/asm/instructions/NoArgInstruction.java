package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class NoArgInstruction extends Instruction {
	private byte arg1;
	private byte arg2;

	public NoArgInstruction(int opcode, int arg1, int arg2) {
		super((byte) opcode);
		this.arg1 = (byte) arg1;
		this.arg2 = (byte) arg2;
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		checkSymbol(parser, in, ';');

		write(parser, opcode, arg1, arg2, condition, null);
	}
}
