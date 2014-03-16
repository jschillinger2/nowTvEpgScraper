import java.util.Date;

/**
 * A program information.
 * 
 * @author Julian Schillinger
 *
 */
public class Program {

	public String channel;
	public String name;
	public String description;
	public Date startTime;
	public Date endTime;
	
	@Override
	public String toString() {
		return "Program ["
				+ (channel != null ? "channel=" + channel + ", " : "")+ (name != null ? "name=" + name + ", " : "")
				+ (description != null ? "description=" + description + ", "
						: "")
				+ (startTime != null ? "startTime=" + startTime + ", " : "")
				+ (endTime != null ? "endTime=" + endTime : "") + "]";
	}

	
	
}
