package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;
import java.util.ArrayList;

public class VarInstruction extends Instruction {

	public VarInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		if (condition != 0) {
			parser.error("var cannot have a condition");
		}

		if (in.tokenType() != Tokenizer.IDENTIFIER) {
			parser.error("Expected variable name");
		}

		String name = in.identifier();
		short[] value = null;

		in.advance();
		if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '(') {
			in.advance();
			if (in.tokenType() != Tokenizer.SYMBOL || in.symbol() != ')') {
				if (in.tokenType() != Tokenizer.INT_CONST)
					parser.error("Expected array length");

				int length = in.intVal();

				if (length == 1)
					parser.warn("Array syntax used for single word varaiable");

				if (length < 0)
					parser.errorShort("Cannot have negative length array");

				if (length > 32767)
					parser.warn("Large array >32767, size must be unsigned");

				value = new short[length];
				in.advance();
				checkSymbol(parser, in, ')');
			}
			in.advance();
		} else {
			value = new short[1];
		}

		if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == ',') {
			in.advance();

			if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '(') {
				in.advance();
				if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == ')') {
					in.advance();
					if (value != null) {
						parser.warn("Empty array initializer");
					} else {
						parser.warn("Empty array initializer with unspecified array length, assuming zero length array");
					}
				} else {

					if (value == null) {
						ArrayList<Short> parsed = new ArrayList<>();
						while (in.tokenType() == Tokenizer.INT_CONST) {
							parsed.add(in.shortVal());
							in.advance();
							if (in.tokenType() == Tokenizer.SYMBOL) {
								if (in.symbol() == ',') {
									in.advance();
								} else if (in.symbol() == ')') {
									break;
								}
							}
						}

						value = new short[parsed.size()];
						if (value.length > 32767)
							parser.warn("Large array >32767, size must be unsigned");

						for (int i = 0; i < value.length; i++) {
							value[i] = parsed.get(i);
						}
					} else {
						int index = 0;

						while (in.tokenType() == Tokenizer.INT_CONST) {
							if (index >= value.length)
								parser.errorShort("Array not large enough for initializer");
							value[index++] = in.shortVal();
							in.advance();
							if (in.tokenType() == Tokenizer.SYMBOL) {
								if (in.symbol() == ',') {
									in.advance();
								} else if (in.symbol() == ')') {
									break;
								}
							}
						}

						if (index < value.length)
							parser.warn("Only " + index + "/" + value.length + " values of array initialized");
					}

					checkSymbol(parser, in, ')');
					in.advance();
				}
			} else {
				if (in.tokenType() != Tokenizer.INT_CONST)
					parser.error("Expected constant or array initializer");

				if (value == null || value.length > 1)
					parser.warn("Integer initializer used for array");

				if (value == null)
					value = new short[1];

				value[0] = in.shortVal();

				in.advance();
			}




		}

		if (value == null) {
			parser.errorShort("No initializer, can't calculate array length");
		}

		checkSymbol(parser, in, ';');
		parser.addVariable(name, value);
		parser.addDefinition(name + ".len", (short) value.length);
	}
}
