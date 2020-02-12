package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class AQSetInstruction extends Instruction {
	public AQSetInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		byte opcode = 0x01, arg1 = 0, arg2 = 0;
		Immediate immediate = new Immediate();

		if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '&') {
			in.advance();

			if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '(') {
				in.advance();
				if (in.tokenType() == Tokenizer.KEYWORD) {
					if (in.keyword().equals("fp")) {
						opcode = 0x0D;
					} else if (in.keyword().equals("sp")) {
						opcode = 0x0F;
					} else {
						parser.error("Expected fp or sp");
					}

					in.advance();
					checkSymbol(parser, in, '+');
					in.advance();
					arg2 = parseArg2(parser, in, immediate);
					checkSymbol(parser, in, ')');
					in.advance();
					checkSymbol(parser, in, ',');
					in.advance();
					arg1 = parseArg1(parser, in);
					in.advance();
				}
			} else {
				byte arg1First = -1;
				byte firstArg = 0, secondArg = 0;

				if (in.tokenType() == Tokenizer.KEYWORD) {
					switch (in.keyword()) {
						case "a":
							firstArg = 0;
							in.advance();
							break;
						case "b":
							firstArg = 1;
							in.advance();
							break;
						case "c":
							firstArg = 2;
							in.advance();
							break;
						case "d":
							firstArg = 3;
							in.advance();
							break;
						case "e":
							firstArg = 4;
							arg1First = 1;
							in.advance();
							break;
						case "fp":
							firstArg = 5;
							arg1First = 1;
							in.advance();
							break;
						case "sp":
							firstArg = 6;
							arg1First = 1;
							in.advance();
							break;
						case "ip":
							firstArg = 7;
							arg1First = 1;
							in.advance();
							break;
						case "imm":
							firstArg = parseArg2(parser, in, immediate);
							arg1First = 0;
							break;
						default:
							parser.error("Expected register");
					}
				} else {
					arg1First = 0;
					firstArg = parseArg2(parser, in, immediate);
				}

				checkSymbol(parser, in, ',');
				in.advance();

				if (in.tokenType() == Tokenizer.KEYWORD) {
					switch (in.keyword()) {
						case "a":
							secondArg = 0;
							in.advance();
							break;
						case "b":
							secondArg = 1;
							in.advance();
							break;
						case "c":
							secondArg = 2;
							in.advance();
							break;
						case "d":
							secondArg = 3;
							in.advance();
							break;
						case "e":
							if (arg1First == 1)
								parser.error("Cannot use arg1 for both arguments of memory set");
							secondArg = 4;
							arg1First = 0;
							in.advance();
							break;
						case "fp":
							if (arg1First == 1)
								parser.error("Cannot use arg1 for both arguments of memory set");
							secondArg = 5;
							arg1First = 0;
							in.advance();
							break;
						case "sp":
							if (arg1First == 1)
								parser.error("Cannot use arg1 for both arguments of memory set");
							secondArg = 6;
							arg1First = 0;
							in.advance();
							break;
						case "ip":
							if (arg1First == 1)
								parser.error("Cannot use arg1 for both arguments of memory set");
							secondArg = 7;
							arg1First = 0;
							in.advance();
							break;
						case "imm":
							if (arg1First == 0)
								parser.error("Cannot use arg2 for both arguments of memory set");
							secondArg = parseArg2(parser, in, immediate);
							arg1First = 1;
							break;
						default:
							parser.error("Expected register");
					}
				} else {
					if (arg1First == 0)
						parser.error("Cannot use arg2 for both arguments of memory set");
					arg1First = 1;
					secondArg = parseArg2(parser, in, immediate);
				}

				if (arg1First == 0) {
					opcode = 0x04;
					arg1 = secondArg;
					arg2 = firstArg;
				} else {
					opcode = 0x03;
					arg1 = firstArg;
					arg2 = secondArg;
				}
			}
		} else {
			arg1 = parseArg1(parser, in);
			in.advance();
			checkSymbol(parser, in, ',');
			in.advance();

			if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '&') {
				in.advance();

				if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '(') {
					in.advance();
					if (in.tokenType() == Tokenizer.KEYWORD) {
						if (in.keyword().equals("fp")) {
							opcode = 0x0C;
						} else if (in.keyword().equals("sp")) {
							opcode = 0x0E;
						} else {
							parser.error("Expected fp or sp");
						}

						in.advance();
						checkSymbol(parser, in, '+');
						in.advance();
						arg2 = parseArg2(parser, in, immediate);
						checkSymbol(parser, in, ')');
						in.advance();
					} else {
						parser.error("Expected fp or sp");
					}
				} else {
					arg2 = parseArg2(parser, in, immediate);
					opcode = 0x05;
				}
			} else {
				if (in.tokenType() == Tokenizer.KEYWORD) {
					switch (in.keyword()) {
						case "a":
							arg2 = 0;
							in.advance();
							break;
						case "b":
							arg2 = 1;
							in.advance();
							break;
						case "c":
							arg2 = 2;
							in.advance();
							break;
						case "d":
							arg2 = 3;
							in.advance();
							break;
						case "e":
							opcode = 0x08;
							in.advance();
							break;
						case "fp":
							opcode = 0x09;
							in.advance();
							break;
						case "sp":
							opcode = 0x0A;
							in.advance();
							break;
						case "ip":
							opcode = 0x0B;
							in.advance();
							break;
						case "imm":
							arg2 = parseArg2(parser, in, immediate);
							break;
						default:
							parser.error("Expected register");
					}
				} else {
					arg2 = parseArg2(parser, in, immediate);
				}
			}
		}

		checkSymbol(parser, in, ';');

		write(parser, opcode, arg1, arg2, condition, immediate);
	}
}
