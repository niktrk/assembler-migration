package main;

/**
 * Main application class.
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 */
public class App {

	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner("src/asm/nestedmacro.asm");
		Parser p = new Parser(sc);
		System.out.println(p.parse());
	}

}
