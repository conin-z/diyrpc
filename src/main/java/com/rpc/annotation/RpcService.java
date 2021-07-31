package com.rpc.annotation;

import java.lang.annotation.*;

/**
 * like {@code Dubbo}'s @Service : the way of annotation;
 * for registry
 *
 * @user KyZhang
 * @date
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RpcService {

}
