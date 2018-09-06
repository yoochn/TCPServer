package coronet.tcp;

import java.util.List;
import java.util.Optional;

public interface ITCP
{
	<T>TCPResponse get(String key);
	<T>TCPResponse getAllKeys(String key);
	<T>TCPResponse rightAdd(String key,String value);
	<T>TCPResponse leftAdd(String key,String value);
	<T>TCPResponse set(String key, List<String> values);
}
