package coronet.tcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class  TCPRequest
{
    private String method;
    private  HashMap<String,List<String>> values;

    public List<String> getParamNames()
    {
        return paramNames;
    }

    public void setParamNames(List<String> paramNames)
    {
        this.paramNames = paramNames;
    }

    private List<String> paramNames;

    public TCPRequest(String method, HashMap<String,List<String>> values)
    {
          this.method = method;
          this.values = values;
          this.paramNames = new ArrayList<>();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public  HashMap<String,List<String>> getValues() {
        return values;
    }

    public void setValues( HashMap<String,List<String>> values) {
        this.values = values;
    }


}
