package rioncompass;

public enum RIONMode {
	
	IDLE, ANSREPLY, AUTO;

	@Override
	public String toString() {
		switch (this) {
		case ANSREPLY:
			return("Poll reply");
		case AUTO:
			return("Auto send");
		case IDLE:
			return("Idle");
		default:
			break;
		
		}
		return super.toString();
	}
	
	
}
