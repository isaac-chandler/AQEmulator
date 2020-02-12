package asm.terminal;

import java.awt.*;

public class Colors {
	public static final Color BLACK = new Color(0x000000);
	public static final Color DARK_GRAY = new Color(0x545454);
	public static final Color LIGHT_GRAY = new Color(0xA8A8A8);
	public static final Color RED = new Color(0xFF0000);
	public static final Color DARK_RED = new Color(0xB00000);
	public static final Color PINK = new Color(0xFF5E88);
	public static final Color YELLOW = new Color(0xFFFF00);
	public static final Color ORANGE = new Color(0xFF9100);
	public static final Color GOLD = new Color(0xD7B300);
	public static final Color LIME = new Color(0x00FF00);
	public static final Color DARK_GREEN = new Color(0x006900);
	public static final Color LIGHT_GREEN = new Color(0x58BE00);
	public static final Color CYAN = new Color(0x00FFFF);
	public static final Color LIGHT_BLUE = new Color(0x58BEFF);
	public static final Color BLUE = new Color(0x0000FF);
	public static final Color DARK_BLUE = new Color(0x000077);
	public static final Color BROWN = new Color(0x803e00);
	public static final Color MAGENTA = new Color(0xFF00FF);
	public static final Color LIGHT_PURPLE = new Color(0x7755FF);
	public static final Color DARK_PURPLE = new Color(0x372091);

	public static final Color[] COLORS = {
			BLACK,
			DARK_GRAY,
			LIGHT_GRAY,
			RED,
			DARK_RED,
			PINK,
			YELLOW,
			ORANGE,
			GOLD,
			LIME,
			DARK_GREEN,
			LIGHT_GREEN,
			CYAN,
			LIGHT_BLUE,
			BLUE,
			DARK_BLUE,
			BROWN,
			MAGENTA,
			LIGHT_PURPLE,
			DARK_PURPLE
	};

	public static final int BLACK_ID = findColor(BLACK);
	public static final int DARK_GRAY_ID = findColor(DARK_GRAY);
	public static final int LIGHT_GRAY_ID = findColor(LIGHT_GRAY);
	public static final int RED_ID = findColor(RED);
	public static final int DARK_RED_ID = findColor(DARK_RED);
	public static final int PINK_ID = findColor(PINK);
	public static final int YELLOW_ID = findColor(YELLOW);
	public static final int ORANGE_ID = findColor(ORANGE);
	public static final int GOLD_ID = findColor(GOLD);
	public static final int LIME_ID = findColor(LIME);
	public static final int DARK_GREEN_ID = findColor(DARK_GREEN);
	public static final int LIGHT_GREEN_ID = findColor(LIGHT_GREEN);
	public static final int CYAN_ID = findColor(CYAN);
	public static final int LIGHT_BLUE_ID = findColor(LIGHT_BLUE);
	public static final int BLUE_ID = findColor(BLUE);
	public static final int DARK_BLUE_ID = findColor(DARK_BLUE);
	public static final int BROWN_ID = findColor(BROWN);
	public static final int MAGENTA_ID = findColor(MAGENTA);
	public static final int LIGHT_PURPLE_ID = findColor(LIGHT_PURPLE);
	public static final int DARK_PURPLE_ID = findColor(DARK_PURPLE);

	private static int findColor(Color color) {
		for (int i = 0; i < COLORS.length; i++)
			if (COLORS[i] == color)
				return i;

		return -1;
	}
}
