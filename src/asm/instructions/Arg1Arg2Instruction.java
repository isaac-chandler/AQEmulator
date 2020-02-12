package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class Arg1Arg2Instruction extends Instruction {

	public Arg1Arg2Instruction(int opcode) {
		super((byte) opcode);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		Immediate immediate = new Immediate();

		byte arg1 = parseArg1(parser, in);


		in.advance();
		checkSymbol(parser, in, ',');
		in.advance();

		byte arg2 = parseArg2(parser, in, immediate);

		checkSymbol(parser, in, ';');
		write(parser, opcode, arg1, arg2, condition, immediate);
	}
}
