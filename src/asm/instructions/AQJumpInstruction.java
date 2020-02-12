package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class AQJumpInstruction extends Instruction {
	public AQJumpInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		byte opcode = 0x01, arg2 = 0;
		Immediate immediate = new Immediate();

		if (in.tokenType() == in.symbol() && in.symbol() == '&') {
			in.advance();
			if (in.tokenType() == in.symbol() && in.symbol() == '(') {
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
					opcode = 0x05;
				}
			} else {
				arg2 = parseArg2(parser, in, immediate);
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

		checkSymbol(parser, in, ';');

		write(parser, opcode, (byte) 7, arg2, condition, immediate);
	}
}
