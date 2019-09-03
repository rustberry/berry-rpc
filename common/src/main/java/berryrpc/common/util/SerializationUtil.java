package berryrpc.common.util;

public interface SerializationUtil {

    static <T> byte[] serialize(T obj) {
        return null;
    }

    static <T> T deserialize(byte[] data, Class<T> cls) {
        return null;
    }
}
