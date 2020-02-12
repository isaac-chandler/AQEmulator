package asm.instructions;

import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.EOFException;

public class AQ2PushInstruction extends Instruction {
	public AQ2PushInstruction() {
		super((byte) 0);
	}

	@Override
	public void parse(Parser parser, Tokenizer in, byte condition) throws CodeException, EOFException {
		byte opcode = 0x02, arg1 = 0, arg2 = 0;
		Immediate immediate = new Immediate();

		if (in.tokenType() == Tokenizer.KEYWORD) {
			switch (in.keyword()) {
				case "a":
					in.advance();
					break;
				case "b":
					arg1 = 1;
					in.advance();
					break;
				case "c":
					arg1 = 2;
					in.advance();
					break;
				case "d":
					arg1 = 3;
					in.advance();
					break;
				case "e":
					arg1 = 4;
					in.advance();
					break;
				case "fp":
					arg1 = 5;
					in.advance();
					break;
				case "sp":
					arg1 = 6;
					in.advance();
					break;
				case "ip":
					arg1 = 7;
					in.advance();
					break;
				case "imm":
					opcode = 0x03;
					arg2 = parseArg2(parser, in, immediate);
					break;
				default:
					parser.error("Expected register");
			}
		} else {
			opcode = 0x03;
			arg2 = parseArg2(parser, in, immediate);
		}

		checkSymbol(parser, in, ';');

		write(parser, opcode, arg1, arg2, condition, immediate);
	}
}
