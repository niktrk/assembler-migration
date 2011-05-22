package main;

/**
 * Main application class.
 * 
 * @author Nikola Trkulja
 * 
 */
public class App {

	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner("src/asm/main.asm");
		Parser p = new Parser(sc);
		System.out.println(p.parse());
	}

}
