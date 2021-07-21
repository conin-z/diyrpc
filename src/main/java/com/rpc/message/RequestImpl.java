package com.rpc.message;


import java.io.Serializable;

/**
 * wrapper; the app layer protocol for serialization;
 *
 * @user KyZhang
 * @date
 */
public class RequestImpl implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;  // currentMilli
    private String itfName;
    private Class<?> itf;
    private String methodName;
    private String responseType;
    private Object[] args;
    private MessageType messageType;
    private Class<?>[] paraClassTypes;


    public RequestImpl() {
    }

    public RequestImpl(MessageType messageType) {
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

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
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
