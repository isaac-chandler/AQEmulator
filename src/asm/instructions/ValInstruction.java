package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class ValInstruction extends Instruction {

	public ValInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		Immediate immediate = getImmediate(parser, in, new Immediate());

		if (condition != 0) {
			parser.error("Val instruction cannot have a condition");
		}

		checkSymbol(parser, in, ';');
		parser.addImmediate(immediate);
	}
}
