package com.rpc.exception;

/**
 * @user KyZhang
 * @date
 */
public class AppException extends Exception{

    static final long serialVersionUID = 3L;

    public AppException(){ }
    public AppException(String message) {
        super(message);
    }

}
