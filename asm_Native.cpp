#include "asm_Native.h"
#include <Windows.h>

JNIEXPORT jint JNICALL Java_asm_Native_GetConsoleMode
(JNIEnv *, jclass, jint handle) {
	DWORD mode;
	GetConsoleMode(GetStdHandle(handle), &mode);

	return mode;
}

/*
 * Class:     asm_Native
 * Method:    SetConsoleMode
 * Signature: (II)I
 */
JNIEXPORT void JNICALL Java_asm_Native_SetConsoleMode
(JNIEnv *, jclass, jint handle, jint mode) {
	SetConsoleMode(GetStdHandle(handle), mode);
}