package asm;

public class Native {
	public static native int GetConsoleMode(int handle);
	public static native void SetConsoleMode(int handle, int mode);
}
