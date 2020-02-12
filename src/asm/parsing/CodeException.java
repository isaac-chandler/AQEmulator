package asm.parsing;

public class CodeException extends Exception {
	private int line;
	private String file;

	public CodeException(String message, int line, String file) {
		super(message);
		this.line = line;
		this.file = file;
	}

	public CodeException(String message, int line, String file, Throwable cause) {
		super(message, cause);
		this.line = line;
		this.file = file;
	}

	public int line() {
		return line;
	}

	public void show() {
		System.err.printf("Error in %s at line %d: %s\n", file, line, getMessage());
	}
}
