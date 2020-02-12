package asm.parsing;

import asm.instructions.Instruction;
import asm.instructions.Instructions;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class Parser {

	private static final int MAX_PROGRAM_SIZE = 65536;
	private static final int OUTPUT_BUFFER_SIZE = MAX_PROGRAM_SIZE;

	private Instructions instructions;

	private HashMap<String, Short> definitions = new HashMap<>();
	private HashMap<String, ArrayList<Substitution>> substitutions = new HashMap<>();
	private LinkedHashMap<String, short[]> variables = new LinkedHashMap<>();
	private HashSet<String> reservedDefinitions = new HashSet<>();

	private LinkedHashMap<String, Integer> symbolTable = new LinkedHashMap<>();

	private Tokenizer in;
	private short[] dest = new short[OUTPUT_BUFFER_SIZE];
	private int wordCount = 0;
	private int requiredMem = 0;

	public HashMap<String, Integer> getSymbolTable() {
		return symbolTable;
	}

	public short[] getProgram() {
		return dest;
	}

	public static class Variable {
		public String name;
		public int offset;
	}

	private static class Substitution {
		public Substitution(short address, short offset) {
			this.address = address;
			this.offset = offset;
		}

		public short address;
		public short offset;
	}

	public void addSubstitution(Variable variable) throws CodeException {
		Short value = definitions.get(variable.name);
		if (value != null) {
			addWord((short) (value + variable.offset));
		} else {
			addMem(1);
			substitutions.computeIfAbsent(variable.name, __ -> new ArrayList<>()).add(new Substitution((short) wordCount, (short) variable.offset));

			wordCount++;
		}
	}

	public void addDefinition(String name, short value) throws CodeException {
		if (definitions.get(name) != null || variables.get(name) != null || reservedDefinitions.contains(name))
			error(name + " is already defined");

		ArrayList<Substitution> substitution = substitutions.get(name);
		if (substitution != null) {
			for (int i = 0; i < substitution.size(); i++) {
				dest[substitution.get(i).address] = (short) (value + substitution.get(i).offset);
			}

			substitutions.remove(name);
		}

		definitions.put(name, value);
	}

	public void addVariable(String name, short[] values) throws CodeException {
		if (definitions.get(name) != null || variables.get(name) != null)
			errorShort(name + " is already defined");

		addMem(values.length);
		variables.put(name, values);
	}

	private void addMem(int length) throws CodeException {
		requiredMem += length;

		if (requiredMem > MAX_PROGRAM_SIZE)
			error("Not enough memory for program");
	}

	public void addWord(short b) throws CodeException {
		addMem(1);
		dest[wordCount++] = b;
	}

	public void addImmediate(Instruction.Immediate immediate) throws CodeException {
		if (immediate.variable != null) {
			addSubstitution(immediate.variable);
		} else {
			addWord(immediate.value);
		}
	}

	public Parser(Tokenizer in, Instructions instructions) {
		this.in = in;

		this.instructions = instructions;
	}

	public void advance() throws CodeException {
		try {
			in.advance();
		} catch (EOFException e) {
			error("Token");
		}
	}

	public void parse() throws CodeException, EOFException {
		addDefinition("__codeStart", (short) 0);
		reservedDefinitions.add("__codeEnd");
		reservedDefinitions.add("__dataStart");
		reservedDefinitions.add("__dataEnd");

		while (true) {
			try {
				in.advance();
			} catch (EOFException e) {
				break;
			}

			if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == '(') {
				parseLabel();
			} else if (in.tokenType() == Tokenizer.IDENTIFIER) {
				Instruction instruction = instructions.get(in.identifier().toLowerCase());
				if (instruction == null) {
					error("Expected instruction name");
					return;
				}
				in.advance();

				byte condition = 0;

				if (in.tokenType() == Tokenizer.SYMBOL && in.symbol() == ':') {
					in.advance();
					switch (in.identifier().toLowerCase()) {
						case "un":
							condition = 0;
							break;
						case "a":
						case "nbe":
							condition = 1;
							break;
						case "ae":
						case "nb":
						case "nc":
							condition = 2;
							break;
						case "b":
						case "c":
						case "nae":
							condition = 3;
							break;
						case "be":
						case "na":
							condition = 4;
							break;
						case "z":
						case "e":
							condition = 5;
							break;
						case "nz":
						case "ne":
							condition = 6;
							break;
						case "g":
						case "nle":
							condition = 7;
							break;
						case "ge":
						case "nl":
							condition = 8;
							break;
						case "l":
						case "nge":
							condition = 9;
							break;
						case "le":
						case "ng":
							condition = 10;
							break;
						case "o":
							condition = 11;
							break;
						case "no":
							condition = 12;
							break;
						case "s":
							condition = 13;
							break;
						case "ns":
							condition = 14;
							break;
						case "cnz":
						case "cne":
							condition = 15;
							break;
						default:
							error("Expected condition");
							break;
					}

					in.advance();
				}

				instruction.parse(this, in, condition);
			} else {
				error("Expected instruction or label");
				return;
			}
		}

		reservedDefinitions.remove("__codeEnd");
		reservedDefinitions.remove("__dataStart");

		addDefinition("__codeEnd", (short) wordCount);
		addDefinition("__dataStart", (short) wordCount);


		LinkedHashMap<String, short[]> variablesSave = variables;
		variables = new LinkedHashMap<>();
		for (Map.Entry<String, short[]> variable : variablesSave.entrySet()) {
			System.arraycopy(variable.getValue(), 0, dest, wordCount, variable.getValue().length);
			addDefinition(variable.getKey(), (short) wordCount);
			symbolTable.put(variable.getKey(), wordCount);
			wordCount += variable.getValue().length;
		}

		reservedDefinitions.remove("__dataEnd");

		addDefinition("__dataEnd", (short) wordCount);

		if (!substitutions.isEmpty()) {
			errorShort(String.join(",", substitutions.keySet()) + " were never defined");
		}
	}

	private void parseLabel() throws CodeException {
		advance();


		if (in.tokenType() == Tokenizer.IDENTIFIER) {
			addDefinition(in.identifier(), (short) wordCount);
			symbolTable.put(in.identifier(), wordCount);
		} else {
			error("Expected label name");
			return;
		}

		advance();

		if (in.tokenType() != Tokenizer.SYMBOL || in.symbol() != ')') {
			error("Expected )");
		}
	}

	public void error(String message) throws CodeException {
		throw new CodeException(message + " instead of " + in.identifier(), in.lineNumber(), in.name());
	}

	public void warn(String message) {
		System.err.println("Warning:" + in.name() + ":" + (in.lineNumber() - 1) + ": " + message);
	}

	public void errorShort(String message) throws CodeException {
		throw new CodeException(message, in.lineNumber(), in.name());
	}
}
