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

    protected final String requestId;
    protected final MessageType messageType;
    protected final ResponseStatus responseStatus;

    protected Class<?> requestClz;
    protected String errorMessage;
    protected String content;
    protected Class<?> resultType;
    protected boolean isArray;
    protected Class<?> arrayType;
    protected boolean isCollection;
    protected Class<?> collectionType;

    protected String serverName;  // when say bye

    public ResponseImpl(String requestId, MessageType messageType, ResponseStatus status) {
        this.requestId = requestId;
        this.messageType = messageType;
        this.responseStatus = status;
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

    public String getRequestId() {
        return requestId;
    }

    public MessageType getMessageType() {
        return messageType;
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
