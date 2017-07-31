package com.jboxers.flashscore.util;

import java.nio.ByteBuffer;

/**
 * Created by nikolayrusev on 7/28/17.
 */
public abstract class ByteBufferUtils {
    private ByteBufferUtils(){}

    public static ByteBuffer toByteBuffer(byte[] data){
        return ByteBuffer.wrap(data);

    }

    public static ByteBuffer toByteBuffer(String data){
        return ByteBuffer.wrap(data.getBytes());
    }

    public static String toString(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes);
    }
}
