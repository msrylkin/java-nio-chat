package nio.headers;

/**
 * Created by user on 20.12.2015.
 */
public interface Header {
    /**
     *
     * @return bytes count in header
     */
    int byteLength();

    /**
     *
     * @return max len of the message
     */
    long maxLength();

    /**
     * Convert header part to int number of bytes
     * @param bytes - header
     * @return - bytes count
     */
    long bytesToLength(byte[] bytes);

    /**
     * Convert number of count bytes to usable header
     * @param len - bytes count
     * @return - header array
     */
    byte[] lengthToBytes(long len);
}
