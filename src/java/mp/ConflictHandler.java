package mp;

import org.codehaus.jackson.*;

public interface ConflictHandler {

	public void Resolve(ShMemObject mine, JsonNode theirs, String key);
}
