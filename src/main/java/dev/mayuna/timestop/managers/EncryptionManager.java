package dev.mayuna.timestop.managers;

import dev.mayuna.timestop.config.EncryptionConfig;
import lombok.Getter;
import lombok.var;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * This class manages the encryption keys
 */
@Getter
public class EncryptionManager {

    private final EncryptionConfig encryptionConfig;

    private KeyPair asymetricKeyPair;
    private Key symmetricKey;

    /**
     * Creates a new encryption manager
     *
     * @param encryptionConfig The encryption config
     */
    public EncryptionManager(EncryptionConfig encryptionConfig) {
        this.encryptionConfig = encryptionConfig;
    }

    /**
     * Encrypts the given data using the given key
     *
     * @param data The data
     * @param key  The key
     *
     * @return The decrypted data
     *
     * @throws NoSuchAlgorithmException  If the algorithm is not supported
     * @throws NoSuchPaddingException    If the padding is not supported
     * @throws InvalidKeyException       If the key is invalid
     * @throws BadPaddingException       If the padding is bad
     * @throws IllegalBlockSizeException If the block size is illegal
     */
    public static byte[] encryptDataUsingKey(byte[] data, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts the given data using the given key
     *
     * @param data The data
     * @param key  The key
     *
     * @return The decrypted data
     *
     * @throws NoSuchPaddingException    If the padding is not supported
     * @throws NoSuchAlgorithmException  If the algorithm is not supported
     * @throws IllegalBlockSizeException If the block size is illegal
     * @throws BadPaddingException       If the padding is bad
     * @throws InvalidKeyException       If the key is invalid
     */
    public static byte[] decryptDataUsingKey(byte[] data, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Loads the asymmetric public key from the given bytes
     *
     * @param publicKeyBytes The public key bytes
     *
     * @return The public key
     *
     * @throws NoSuchAlgorithmException If the algorithm is not supported
     * @throws InvalidKeySpecException  If the key spec is invalid
     */
    public PublicKey loadAsymmetricPublicKeyFromBytes(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(encryptionConfig.getAsymmetricKeyType());
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * Generates a new asymmetric key pair
     *
     * @throws NoSuchAlgorithmException If the algorithm is not supported
     */
    public void generateAsymmetricKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(encryptionConfig.getAsymmetricKeyType());
        kpg.initialize(encryptionConfig.getAsymmetricKeySize());
        asymetricKeyPair = kpg.generateKeyPair();
    }

    /**
     * Generates a new symmetric key
     *
     * @throws NoSuchAlgorithmException If the algorithm is not supported
     */
    public void generateSymmetricKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(encryptionConfig.getSymmetricKeyType());
        keyGen.init(encryptionConfig.getSymmetricKeySize());
        symmetricKey = keyGen.generateKey();
    }

    /**
     * Gets the public key of the asymmetric key pair
     *
     * @return The public key
     */
    public byte[] getAsymmetricPublicKeyBytes() {
        if (asymetricKeyPair == null) {
            throw new IllegalStateException("Asymmetric key pair has not been generated/loaded yet");
        }

        return asymetricKeyPair.getPublic().getEncoded();
    }

    /**
     * Gets the private key of the asymmetric key pair
     *
     * @return The private key
     */
    public byte[] getAsymmetricPrivateKeyBytes() {
        if (asymetricKeyPair == null) {
            throw new IllegalStateException("Asymmetric key pair has not been generated/loaded yet");
        }

        return asymetricKeyPair.getPrivate().getEncoded();
    }

    /**
     * Gets the symmetric key
     *
     * @return The symmetric key
     */
    public byte[] getSymmetricKeyBytes() {
        if (symmetricKey == null) {
            throw new IllegalStateException("Symmetric key has not been generated/loaded yet");
        }

        return symmetricKey.getEncoded();
    }

    /**
     * Checks if the asymmetric key pair has been generated/loaded
     *
     * @return If the asymmetric key pair has been generated/loaded
     */
    public boolean hasAsymmetricKeyPair() {
        return asymetricKeyPair != null;
    }

    /**
     * Checks if the symmetric key has been generated/loaded
     *
     * @return If the symmetric key has been generated/loaded
     */
    public boolean hasSymmetricKey() {
        return symmetricKey != null;
    }

    /**
     * Saves the asymmetric key pair to the given file name
     *
     * @param fileNameWithoutExtension The file name without extension (will be .pub and .key)
     *
     * @throws IOException If the files could not be created
     */
    public void saveAsymmetricKeyPair(String fileNameWithoutExtension) throws IOException {
        if (asymetricKeyPair == null) {
            throw new IllegalStateException("Asymmetric key pair has not been generated/loaded yet");
        }

        File publicKeyFile = new File(fileNameWithoutExtension + ".pub");
        File privateKeyFile = new File(fileNameWithoutExtension + ".key");

        if (!publicKeyFile.exists()) {
            if (!publicKeyFile.createNewFile()) {
                throw new IOException("Could not create public key file at " + publicKeyFile.getAbsolutePath());
            }
        }

        if (!privateKeyFile.exists()) {
            if (!privateKeyFile.createNewFile()) {
                throw new IOException("Could not create private key file at " + privateKeyFile.getAbsolutePath());
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(publicKeyFile)) {
            outputStream.write(asymetricKeyPair.getPublic().getEncoded());
        }

        try (FileOutputStream outputStream = new FileOutputStream(privateKeyFile)) {
            outputStream.write(asymetricKeyPair.getPrivate().getEncoded());
        }
    }

    /**
     * Loads the asymmetric key pair from the given file name
     *
     * @param fileNameWithoutExtension The file name without extension (will be .pub and .key)
     *
     * @throws IOException              If the files could not be read
     * @throws NoSuchAlgorithmException If the algorithm is not supported
     * @throws InvalidKeySpecException  If the key spec is invalid
     */
    public void loadAsymmetricKeyPair(String fileNameWithoutExtension) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File publicKeyFile = new File(fileNameWithoutExtension + ".pub");
        File privateKeyFile = new File(fileNameWithoutExtension + ".key");

        if (!publicKeyFile.exists()) {
            throw new IOException("Public key file at " + publicKeyFile.getAbsolutePath() + " does not exist");
        }

        if (!privateKeyFile.exists()) {
            throw new IOException("Private key file at " + privateKeyFile.getAbsolutePath() + " does not exist");
        }

        KeyFactory keyFactory = KeyFactory.getInstance(encryptionConfig.getAsymmetricKeyType());

        // Load private key
        byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        // Load public key
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
        PublicKey publicKey = loadAsymmetricPublicKeyFromBytes(publicKeyBytes);

        asymetricKeyPair = new KeyPair(publicKey, privateKey);
    }

    /**
     * Saves the symmetric key to the given file name
     *
     * @param fileNameWithoutExtension The file name without extension (will be .key)
     *
     * @throws IOException If the file could not be created
     */
    public void saveSymmetricKey(String fileNameWithoutExtension) throws IOException {
        if (symmetricKey == null) {
            throw new IllegalStateException("Symmetric key has not been generated/loaded yet");
        }

        File keyFile = new File(fileNameWithoutExtension + ".key");

        if (!keyFile.exists()) {
            if (!keyFile.createNewFile()) {
                throw new IOException("Could not create key file at " + keyFile.getAbsolutePath());
            }
        }

        try (var outputStream = new FileOutputStream(keyFile)) {
            outputStream.write(symmetricKey.getEncoded());
        }
    }

    /**
     * Loads the symmetric key from the given file name
     *
     * @param fileNameWithoutExtension The file name without extension (will be .key)
     *
     * @throws IOException If the file could not be read
     */
    public void loadSymmetricKey(String fileNameWithoutExtension) throws IOException {
        File keyFile = new File(fileNameWithoutExtension + ".key");

        if (!keyFile.exists()) {
            throw new IOException("Key file at " + keyFile.getAbsolutePath() + " does not exist");
        }

        // Load key
        byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
        symmetricKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts the given data using the asymmetric key pair
     *
     * @param data Data
     *
     * @return Encrypted data
     *
     * @throws NoSuchAlgorithmException  If the algorithm is not supported
     * @throws NoSuchPaddingException    If the padding is not supported
     * @throws InvalidKeyException       If the key is invalid
     * @throws BadPaddingException       If the padding is bad
     * @throws IllegalBlockSizeException If the block size is illegal
     */
    public byte[] encryptUsingAsymmetricKey(byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (asymetricKeyPair == null) {
            throw new IllegalStateException("Asymmetric key pair has not been generated/loaded yet");
        }

        return encryptDataUsingKey(data, asymetricKeyPair.getPublic());
    }

    /**
     * Decrypts the given data using the asymmetric key pair
     *
     * @param data Data
     *
     * @return Decrypted data
     *
     * @throws NoSuchAlgorithmException  If the algorithm is not supported
     * @throws NoSuchPaddingException    If the padding is not supported
     * @throws InvalidKeyException       If the key is invalid
     * @throws BadPaddingException       If the padding is bad
     * @throws IllegalBlockSizeException If the block size is illegal
     */
    public byte[] decryptUsingAsymmetricKey(byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (asymetricKeyPair == null) {
            throw new IllegalStateException("Asymmetric key pair has not been generated/loaded yet");
        }

        return decryptDataUsingKey(data, asymetricKeyPair.getPrivate());
    }

    /**
     * Encrypts the given data using the symmetric key
     *
     * @param data Data
     *
     * @return Encrypted data
     *
     * @throws NoSuchAlgorithmException  If the algorithm is not supported
     * @throws NoSuchPaddingException    If the padding is not supported
     * @throws InvalidKeyException       If the key is invalid
     * @throws BadPaddingException       If the padding is bad
     * @throws IllegalBlockSizeException If the block size is illegal
     */
    public byte[] encryptUsingSymmetricKey(byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (symmetricKey == null) {
            throw new IllegalStateException("Symmetric key has not been generated/loaded yet");
        }

        return encryptDataUsingKey(data, symmetricKey);
    }

    /**
     * Decrypts the given data using the symmetric key
     *
     * @param data Data
     *
     * @return Decrypted data
     *
     * @throws NoSuchAlgorithmException  If the algorithm is not supported
     * @throws NoSuchPaddingException    If the padding is not supported
     * @throws InvalidKeyException       If the key is invalid
     * @throws BadPaddingException       If the padding is bad
     * @throws IllegalBlockSizeException If the block size is illegal
     */
    public byte[] decryptUsingSymmetricKey(byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (symmetricKey == null) {
            throw new IllegalStateException("Symmetric key has not been generated/loaded yet");
        }

        return decryptDataUsingKey(data, symmetricKey);
    }

    /**
     * Sets the asymmetric key pair from the given bytes
     *
     * @param keyBytes The key bytes
     */
    public void setSymmetricKeyFromBytes(byte[] keyBytes) {
        symmetricKey = new SecretKeySpec(keyBytes, "AES");
    }
}
