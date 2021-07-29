package com.rpc.message;

import com.rpc.utils.JsonUtil;

import java.io.Serializable;

/**
 * wrapper; like the app layer protocol for RPC
 *
 * @user KyZhang
 * @date
 */
public class ResponseImpl implements Serializable {

    private static final long serialVersionUID = 2L;

    private String requestId;
    private Class<?> requestClz;

    private MessageType messageType;
    private ResponseStatus responseStatus;
    private String errorMessage;
    private String content;
    private Class<?> resultType;
    private boolean isArray;
    private Class<?> arrayType;
    private boolean isCollection;
    private Class<?> collectionType;

    private String serverName;

    public ResponseImpl() { }

    public ResponseImpl(String requestId, ResponseStatus status) {
        this.requestId = requestId;
        this.responseStatus = status;
    }

    public ResponseImpl(MessageType messageType) {
        this.messageType = messageType;
    }

    public Object returnObj() {
        Object o = null;
        if(resultType == Void.class)
            return o;
        return JsonUtil.jsonToPojo(content, collectionType);
    }

    public <T> T[] returnObj(Class<T> clz) {
        return (T[])JsonUtil.jsonToPojo(content, arrayType);
    }

    public Class<?> getArrayType() {
        return arrayType;
    }

    public void setArrayType(Class<?> arrayType) {
        this.arrayType = arrayType;
    }

    public Class<?> getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(Class<?> collectionType) {
        this.collectionType = collectionType;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean collection) {
        isCollection = collection;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setResultType(Class<?> resultType) {
        this.resultType = resultType;
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
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

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Class<?> getRequestClz() {
        return requestClz;
    }

    public void setRequestClz(Class<?> requestClz) {
        this.requestClz = requestClz;
    }
}
