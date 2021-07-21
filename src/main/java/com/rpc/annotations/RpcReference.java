package com.rpc.annotations;

import java.lang.annotation.*;

/**
 * like [Dubbo]'s @Reference;
 * for subscription
 *
 * @user KyZhang
 * @date
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RpcReference {

}
