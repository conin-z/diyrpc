package com.rpc.message;


import java.io.Serializable;

/**
 * wrapper; like the app layer protocol for RPC
 *
 * @user KyZhang
 * @date
 */
public class RequestImpl implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String requestId;  // by currentMilli
    protected final MessageType messageType;

    protected String itfName;
    protected Class<?> itf;
    protected String methodName;
    protected String responseType;
    protected Object[] args;
    protected Class<?>[] paraClassTypes;


    public RequestImpl(String id, MessageType messageType) {
        this.requestId = id;
        this.messageType = messageType;
    }

    public Class<?>[] getParaClassTypes() {
        return paraClassTypes;
    }

    public void setParaClassTypes(Class<?>[] paraClassTypes) {
        this.paraClassTypes = paraClassTypes;
    }

    public String getItfName() {
        return itfName;
    }

    public String getRequestId() {
        return requestId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setItfName(String itfName) {
        this.itfName = itfName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?> getItf() {
        return itf;
    }

    public void setItf(Class<?> itf) {
        this.itf = itf;
    }

}
