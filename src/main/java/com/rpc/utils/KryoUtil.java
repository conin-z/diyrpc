package com.rpc.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.rpc.message.RequestImpl;
import com.rpc.message.ResponseImpl;
import io.netty.buffer.ByteBuf;
import org.apache.commons.codec.binary.Base64;
import org.objenesis.strategy.StdInstantiatorStrategy;

import io.netty.buffer.ByteBufInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
 * see: https://www.cnblogs.com/hntyzgn/p/7122709.html
 *
 * @user KyZhang
 * @date
 */
public class KryoUtil {

    /**
     * using ThreadLocal to ensure thread safety
     * Also: KryoPool：https://github.com/EsotericSoftware/kryo#threading
     */
    private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {

        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();

            /**
             * 不要轻易改变这里的配置！更改之后，序列化的格式就会发生变化，
             * 上线的同时就必须清除 Redis 里的所有缓存，
             * 否则那些缓存再回来反序列化时就会报错
             *
             * 除了常见的 JDK 类型、以及这些类型组合而来的普通 POJO，Kryo 还支持以下特殊情况：
             * 枚举；
             * 任意 Collection、数组；
             * 子类/多态；
             * 循环引用；
             * 内部类；
             * 泛型对象； //Gson不支持 会擦除
             * Builder 模式；
             * 其中部分特性的支持，需要使用者手动设定 Kryo 的某些配置 see /~/
             */

            // circular references (otherwise stack overflow)
            kryo.setReferences(true); //default : true; not to change this
            kryo.setRegistrationRequired(false); //default : false; not to change this

            kryo.register(ResponseImpl.class); // register some classes to be serialized ahead of time
            kryo.register(RequestImpl.class);


            // fix the NPE bug when deserializing Collections.   /~/
            ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy())
                    .setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());

            return kryo;
        }
    };

    /**
     * gets an instance of Kryo for the current thread
     *
     * @return  Kryo instance
     */
    public static Kryo getInstance() {
        return kryoLocal.get();
    }



    //--------------------------------------------------------------------
    //     Serialize / Deserialize objects, with type information
    //     The result of serialization contains the info about [type]
    //     The [type] is no longer required when deserializing
    //--------------------------------------------------------------------

    /**
     * serializes objects [with types] into byte arrays
     * usage : directly  --> KryoEncoder
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> byte[] writeToByteArray(T obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        //
        Kryo kryo = getInstance();
        try {
            kryo.writeClassAndObject(output, obj);  //~
            output.flush();
        } finally {
            kryoLocal.remove();
            output.close();
        }
        byte[] bytes = byteArrayOutputStream.toByteArray();
        try {
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * serializes the object [with its type] to String
     * Base64 encoding is utilized
     * usage : be combined with --> pipeline.addLast(new StringEncoder());
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String writeToString(T obj) {
        try {
            return new String(Base64.encodeBase64(writeToByteArray(obj)),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * deserializes the byte array to original object
     * usage : directly  --> KryoDecoder
     *
     * @param byteArray
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T readFromByteArray(ByteBuf byteArray) {
        ByteBufInputStream byteArrayInputStream = new ByteBufInputStream(byteArray);
        Input input = new Input(byteArrayInputStream);
        //
        Kryo kryo = getInstance();
        T t = (T) kryo.readClassAndObject(input);
        kryoLocal.remove();
        return t;
    }

}
