package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class AQ2SwapInstruction extends Instruction {
	public AQ2SwapInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		byte opcode = 0x15, arg2 = 0;
		Immediate immediate = new Immediate();

		byte arg1 = parseArg1(parser, in);

		in.advance();
		checkSymbol(parser, in, ',');
		in.advance();

		if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '&') {
			in.advance();

			arg2 = parseArg2(parser, in, immediate);
			opcode = 0x22;
		} else {
			arg2 = parseArg1(parser, in);
			in.advance();
		}

		checkSymbol(parser, in, ';');

		write(parser, opcode, arg1, arg2, condition, immediate);
	}
}
