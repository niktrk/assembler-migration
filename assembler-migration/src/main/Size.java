package main;

/**
 * Enumeration class representing two supported sizes of assembler variables (and registers).
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 */
public enum Size {

	BYTE(8),

	DOUBLE_BYTE(16);

	private final int size;

	private Size(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

}
