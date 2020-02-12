package asm;

import asm.instructions.Instructions;
import asm.parsing.CodeException;
import asm.parsing.Parser;
import asm.parsing.Tokenizer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class AlphaQAsm {

	private HashMap<String, Integer> symbolTable;
	private short[] program;

	public AlphaQAsm(File file) throws IOException, CodeException {
		CodeException e1 = null;
		try (Tokenizer tokenizer = new Tokenizer(file)) {
			Parser parser = new Parser(tokenizer, Instructions.AQ2);
			parser.parse();

			symbolTable = parser.getSymbolTable();
			program = parser.getProgram();
		} catch (CodeException e) {
			e1 = e;
			e.show();
		}

		if (e1 != null)
			throw e1;
	}

	public HashMap<String, Integer> getSymbolTable() {
		return symbolTable;
	}

	public short[] getProgram() {
		return program;
	}
}
