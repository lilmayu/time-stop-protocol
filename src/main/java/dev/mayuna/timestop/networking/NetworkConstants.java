package dev.mayuna.timestop.networking;

/**
 * Holds constants for networking. Used when not specifying your own values.
 */
public class NetworkConstants {

    /**
     * The size of the write buffer for the network<br> Should be bigger than {@link NetworkConstants#OBJECT_BUFFER_SIZE}
     */
    public static int WRITE_BUFFER_SIZE = 4194304; // 4MB

    /**
     * The size of the object buffer for the network<br> Should be bigger than the largest object size
     */
    public static int OBJECT_BUFFER_SIZE = 1048576; // 1MB

}
