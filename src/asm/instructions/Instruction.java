package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public abstract class Instruction {

	protected byte opcode;

	public Instruction(byte opcode) {
		this.opcode = opcode;
	}

	public static class Immediate {
		public short value;
		public boolean forcedImmediate;
		public Parser.Variable variable;
	}

	public static Immediate getImmediate(Parser parser, Tokenizer in, Immediate immediate) throws CodeException, EOFException {

		boolean negate = false;
		if (in.tokenType() == Tokenizer.KEYWORD && in.keyword().equals("imm")) {
			in.advance();
			if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == ':') {
				in.advance();
				immediate.forcedImmediate = true;
			} else {
				parser.error("Expected :");
			}
		}

		if (in.tokenType() == Tokenizer.SYMBOL) {
			if (in.symbol() == '+') {
				in.advance();
			} else if (in.symbol() == '-') {
				in.advance();
				negate = true;
			} else {
				parser.error("Expected immediate value");
			}
		} else if (in.tokenType() == Tokenizer.IDENTIFIER) {
			immediate.variable = new Parser.Variable();

			immediate.variable.name = in.identifier();

			in.advance();

			if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '(') {
				in.advance();
				boolean negateIndex = false;
				if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '-') {
					in.advance();
					negateIndex = true;
				}
				if (in.tokenType() != Tokenizer.INT_CONST) {
					parser.error("Expected constant offset");
				}

				immediate.variable.offset = negateIndex ? -in.shortVal() : in.shortVal();
				in.advance();
				checkSymbol(parser, in, ')');
				in.advance();
			}

			return immediate;
		}

		if (in.tokenType() == Tokenizer.INT_CONST) {
			short val = in.shortVal();
			immediate.value = negate ? (short) -val : val;
			in.advance();
		} else {
			parser.error("Expected immediate value");
		}


		return immediate;
	}

	public static void checkSymbol(Parser parser, Tokenizer in, char symbol) throws EOFException, CodeException {
		if (in.tokenType() != Tokenizer.SYMBOL || in.symbol() != symbol)
			parser.error("Expected " + symbol);
	}

	public static byte parseArg1(Parser parser, Tokenizer in) throws CodeException {
		if (in.tokenType() == Tokenizer.KEYWORD) {
			switch (in.keyword()) {
				case "a":
					return 0;
				case "b":
					return 1;
				case "c":
					return 2;
				case "d":
					return 3;
				case "e":
					return 4;
				case "fp":
					return 5;
				case "sp":
					return 6;
				case "ip":
					return 7;
				default:
					parser.error("Expected register");
			}
		} else {
			parser.error("Expected register");
		}

		return 0;
	}

	public static byte parseArg2(Parser parser, Tokenizer in, Immediate immediate) throws CodeException, EOFException {
		if (in.tokenType() == Tokenizer.KEYWORD) {
			switch (in.keyword()) {
				case "a":
					in.advance();
					return 0;
				case "b":
					in.advance();
					return 1;
				case "c":
					in.advance();
					return 2;
				case "d":
					in.advance();
					return 3;
				case "imm":
					getImmediate(parser, in, immediate);
					return 7;
				default:
					parser.error("Expected register or immediate");
					break;
			}
		} else {
			getImmediate(parser, in, immediate);
		}

		if (immediate.variable != null) {
			return 7;
		} else if (immediate.value == 0) {
			return 4;
		} else if (immediate.value == 1) {
			return 5;
		} else if (immediate.value == -1) {
			return 6;
		} else {
			return 7;
		}
	}

	public static void write(Parser parser, byte opcode, byte arg1, byte arg2, byte condition, Immediate immediate) throws CodeException {
//		if (arg2 == 7) {
//			System.out.printf("Instruction: %X:%d %d, %d(%d);\n", opcode, condition, arg1, arg2, immediate.value);
//		} else {
//			System.out.printf("Instruction: %X:%d %d, %d;\n", opcPrinterode, condition, arg1, arg2);
//		}

		parser.addWord((short) ((opcode << 10) | (arg1 << 7) | (arg2 << 4) | condition));
		if (arg2 == 7)
			parser.addImmediate(immediate);
	}


	public abstract void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException;
}
