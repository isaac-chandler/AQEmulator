package asm.parsing;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

public class Tokenizer implements Closeable {

	public static final int KEYWORD = 1;
	public static final int SYMBOL = 2;
	public static final int IDENTIFIER = 3;
	public static final int INT_CONST = 4;

	private static final HashSet<String> keywords = new HashSet<>();
	private static final HashSet<Character> symbols = new HashSet<>();

	private String name;
	private Scanner in;
	private int tokenType = 0;
	private String token;
	private String currentLine = "\n";
	private int lineNumber = 0;
	private boolean blockComment = false;

	static {
		keywords.add("a");
		keywords.add("b");
		keywords.add("c");
		keywords.add("d");
		keywords.add("e");
		keywords.add("fp");
		keywords.add("sp");
		keywords.add("ip");
		keywords.add("imm");

		symbols.add('(');
		symbols.add(')');
		symbols.add('&');
		symbols.add(',');
		symbols.add(';');
		symbols.add(':');
		symbols.add('+');
		symbols.add('-');
		symbols.add('/');
		symbols.add('*');
	}

	public Tokenizer(File file) throws IOException {
		name = file.getName();
		in = new Scanner(file);
	}

	private boolean hasMoreTokens() {
		try {
			if (in.hasNextLine()) {
				return true;
			} else {
				close();
				return !currentLine.matches("\\s*");
			}
		} catch (IllegalStateException e) {
			return !currentLine.matches("\\s*");
		}
	}

	public Tokenizer advance() throws EOFException, CodeException {
		while (hasMoreTokens()) {
			if (!blockComment) {
				tokenType = 0;
				char c = readChar();
				token = "";

				if (Character.isDigit(c)) {
					token += c;
					c = readChar();
					while (Character.isDigit(c) || (Character.toLowerCase(c) >= 'a' && Character.toLowerCase(c) <= 'f')) {
						token += c;
						c = readChar();
					}
					tokenType = INT_CONST;
					if (Character.toLowerCase(c) == 'h') {
						intVal = Integer.valueOf(token, 16);
					} else if (Character.toLowerCase(c) == 'o') {
						intVal = Integer.valueOf(token, 8);
					} else if (Character.toLowerCase(c) == 'b') {
						intVal = Integer.valueOf(token, 2);
					} else {
						intVal = Integer.valueOf(token, 10);
						undoRead(c);
					}
				} else if (c == '\'') {
					boolean escape = false;
					tokenType = INT_CONST;

					while (true) {
						c = readChar();

						if (escape) {
							escape = false;
							switch (c) {
								case 'b':
									token += '\b';
									break;
								case 'n':
									token += '\n';
									break;
								case 't':
									token += '\t';
									break;
								case 'r':
									token += '\r';
									break;
								case 'f':
									token += '\f';
									break;
								case '\'':
									token += '\'';
									break;
								case '\\':
									token += '\\';
									break;
								case '0':
									token += '\0';
									break;
								default:
									throw new CodeException("Invalid escape character: \\" + c, lineNumber(), name());
							}
							continue;
						}

						if (c == '\\') {
							escape = true;
							continue;
						}

						if (c == '\'') {
							break;
						}

						if ((c & 0xFF00) != 0) {
							throw new CodeException("Character not supported: " + c, lineNumber(), name());
						}

						token += c;
					}

					if (token.length() == 0) {
						intVal = 0;
					} else if (token.length() == 1) {
						intVal = token.charAt(0) & 0xFF;
					} else if (token.length() == 2) {
						intVal = (token.charAt(0) & 0xFF) | (token.charAt(1) << 8);
					} else {
						throw new CodeException("String immediate cannot be greater than 2 characters: \'" + token + '\'', lineNumber(), name());
					}
				} else if (symbols.contains(c)) {
					try {
						char c2 = readChar();
						if (c == '/' && c2 == '/') {
							currentLine = "";
							continue;
						} else if (c == '/' && c2 == '*') {
							blockComment = true;
							continue;
						} else {
							undoRead(c2);
							token += c;
							tokenType = SYMBOL;
						}
					} catch (StringIndexOutOfBoundsException e) {

					}
				} else if (Character.isLetter(c) || c == '_') {
					token += c;
					while (Character.isLetterOrDigit(c = readChar()) || c == '_' || c == '$' || c == '.') {
						token += c;
					}
					undoRead(c);
					if (keywords.contains(token.toLowerCase())) {
						token = token.toLowerCase();
						tokenType = KEYWORD;
					} else
						tokenType = IDENTIFIER;
				} else if (Character.isWhitespace(c)) {
					continue;
				} else {
					throw new CodeException("Unexpected symbol: " + c, lineNumber, name);
				}
			} else {
				boolean previousAsterisk = false;
				while (true) {
					char c = readChar();
					if (previousAsterisk && c == '/') {
						blockComment = false;
						break;
					} else if (c == '*')
						previousAsterisk = true;
					else
						previousAsterisk = false;
				}
				continue;
			}

			if (intVal > 65535)
				throw new CodeException("Integer to large", lineNumber(), name());

			if (intVal < -32768)
				throw new CodeException("Integer to large", lineNumber(), name());
			return this;
		}

		token = "<eof>";
		throw new EOFException("Unexpected end of code");

	}

	private char readChar() {
		try {
			if (currentLine.matches("\\s*")) {
				currentLine = in.nextLine();
				lineNumber++;
				return '\n';
			}
		} catch (IllegalStateException e) {
			return '\n';
		}

		char ret = currentLine.charAt(0);
		currentLine = currentLine.substring(1);
		return ret;
	}

	private void undoRead(char c) {
		if (Character.isWhitespace(c)) {
			return;
		}
		currentLine = c + currentLine;
	}

	public int tokenType() {
		return tokenType;
	}

	public String keyword() {
		return token.toLowerCase();
	}

	public char symbol() {
		return token.charAt(0);
	}

	public String identifier() {
		return token;
	}

	private int intVal;

	public short shortVal() {
		return (short) intVal;
	}

	public int intVal() {
		return intVal;
	}

	public int lineNumber() {
		return lineNumber;
	}

	private boolean closed = false;

	@Override
	public void close() {
		if (!closed) {
			in.close();
			closed = true;
		}
	}

	public String name() {
		return name;
	}
}
