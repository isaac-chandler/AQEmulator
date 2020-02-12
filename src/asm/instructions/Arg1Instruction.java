package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class Arg1Instruction extends Instruction {

	private byte arg2;

	public Arg1Instruction(int opcode, int arg2) {
		super((byte) opcode);
		this.arg2 = (byte) arg2;
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		byte arg1 = parseArg1(parser, in);

		in.advance();
		checkSymbol(parser, in, ';');
		write(parser, opcode, arg1, arg2, condition, null);
	}
}
