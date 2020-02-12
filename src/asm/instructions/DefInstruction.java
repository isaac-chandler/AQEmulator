package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class DefInstruction extends Instruction {

	public DefInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		if (in.tokenType() != Tokenizer.IDENTIFIER) {
			parser.error("Expected definition name");
		}

		String definition = in.identifier();
		in.advance();

		if (in.tokenType() != Tokenizer.INT_CONST) {
			parser.error("Expected constant");
		}

		int value = in.shortVal();

		if (condition != 0) {
			parser.error("Def instruction cannot have a condition");
		}

		in.advance();
		checkSymbol(parser, in, ';');
		parser.addDefinition(definition, (short) value);
	}
}
