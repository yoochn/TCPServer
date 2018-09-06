package coronet.tcp;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TCPResponse<T>
{
    public enum status {SUCCESS,WARNING,ERROR};
    private status responseCode;

    public status getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(status responseCode) {
        this.responseCode = responseCode;
    }

    private String message;
    private T result;


    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
