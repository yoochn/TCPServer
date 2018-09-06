package coronet.tcp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TCPUtils
{

    /**
     *
     * @param tcpRequest
     * @return
     */
    public static List<Object> extractInputParameters(TCPRequest tcpRequest)
    {
        List<Object> params = new ArrayList<>();
        int sizeParams = tcpRequest.getParamNames().size();
        int sizeValues = tcpRequest.getValues().size();
        if (tcpRequest!= null)
        {
            if ((tcpRequest.getParamNames()!=null) && (tcpRequest.getValues()!=null)
                    && (sizeParams == sizeValues))
            {
                if ((sizeParams == 1 && sizeValues == 1))
                {
                    if (tcpRequest.getValues().containsKey(tcpRequest.getParamNames().get(0)))
                    {
                        params.add(tcpRequest.getValues().get(tcpRequest.getParamNames().get(0)).get(0));
                    }
                    else
                    {
                        TCPResponse response = buildResponse(null,"Action failed: input parameters are invalid, Map doesn't contain key name parameter");
                        params.add(response);
                    }
                }
                else if ((sizeParams == 2 && sizeValues == 2))
                {
                    if ((tcpRequest.getValues().containsKey(tcpRequest.getParamNames().get(0)))
                            && (tcpRequest.getValues().containsKey(tcpRequest.getParamNames().get(1))))
                    {
                        params.add(tcpRequest.getValues().get(tcpRequest.getParamNames().get(0)).get(0));
                        if (tcpRequest.getMethod().equals("set"))
                        {
                            params.add(tcpRequest.getValues().get(tcpRequest.getParamNames().get(1)));
                        }
                        else
                        {
                            params.add(tcpRequest.getValues().get(tcpRequest.getParamNames().get(1)).get(0));
                        }

                    }
                    else
                    {
                        TCPResponse response = buildResponse(null,"Action failed: input parameters are invalid, Map doesn't contain key name parameter");
                        params.add(response);
                    }
                }
            }
            else
            {
                TCPResponse response = buildResponse(null,"Action failed: input parameters are invalid, please check your request structure and input data types");
                params.add(response);
            }
        }
        return params;
    }


    public static  <T>TCPResponse buildResponse(T result, String message)
    {
        TCPResponse<T> response = new TCPResponse();
        response.setMessage(message);
        if (result!=null)
        {
            response.setResponseCode(TCPResponse.status.SUCCESS);
            response.setResult(result);
        }
        else
        {
            response.setResponseCode(TCPResponse.status.ERROR);
        }
        return response;
    }
}
