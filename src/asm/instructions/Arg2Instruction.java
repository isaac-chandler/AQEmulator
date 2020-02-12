package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class Arg2Instruction extends Instruction {
	private byte arg1;

	public Arg2Instruction(int opcode, int arg1) {
		super((byte) opcode);
		this.arg1 = (byte) arg1;
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		Immediate immediate = new Immediate();

		byte arg2 = parseArg2(parser, in, immediate);

		checkSymbol(parser, in, ';');
		write(parser, opcode, arg1, arg2, condition, immediate);
	}
}
