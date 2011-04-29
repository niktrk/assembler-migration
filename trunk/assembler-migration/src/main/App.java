package main;

/**
 * Main application class.
 * 
 * @author Nikola Trkulja
 *
 */
public class App {

	public static void main(String[] args) throws Exception{
		Scanner sc = new Scanner("src/asm/main.asm");
		Token t = sc.next();
		while(t.code != 0){
			System.out.println("------------------");
			System.out.println(t.code);
			System.out.println(t.str);
			System.out.println(t.val);
			System.out.println(t.hex);
			t = sc.next();
		}
	}

}
