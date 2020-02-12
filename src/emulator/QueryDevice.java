package emulator;

public class QueryDevice {
	private static final short[] rom = new short[] {
			3, 1,
			'E' | ('m') << 8, 'u' | ('l' << 8), 'A' | ('Q' << 8), 't' | ('o' << 8), 'r', 0x0000, 0x0000,
			0x2051, 0x7250, 0x746F, 0x746F, 0x7079, 0x7365, 0x0000
	};

	public static short get(int address) {
		if (address == 0x100)
			return 3;

		if (address < rom.length)
			return rom[address];

		return 0;
	}
}
