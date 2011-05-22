package main;

public enum Size {
	 
	BYTE(8), 	
	DOUBLE_BYTE(16); 
	
	private int size;
	
	Size(int size){
		this.size = size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}
	
}
