package dev.mayuna.timestop.encryption;

import dev.mayuna.timestop.config.EncryptionConfig;
import dev.mayuna.timestop.managers.EncryptionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

public class TestEncryptionManagerBasics {

    private static final EncryptionConfig encryptionConfig = new EncryptionConfig();

    @Test
    public void testNotGeneratedKeys() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertNull(encryptionManager.getAsymetricKeyPair());
        assertNull(encryptionManager.getSymmetricKey());
        assertFalse(encryptionManager.hasAsymmetricKeyPair());
        assertFalse(encryptionManager.hasSymmetricKey());
        assertThrows(IllegalStateException.class, encryptionManager::getSymmetricKeyBytes);
        assertThrows(IllegalStateException.class, encryptionManager::getAsymmetricPublicKeyBytes);
        assertThrows(IllegalStateException.class, encryptionManager::getAsymmetricPrivateKeyBytes);
    }

    @Test
    public void testGenerateAsymmetricKeyPair() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);

        assertNotNull(encryptionManager.getAsymetricKeyPair());
        assertTrue(encryptionManager.hasAsymmetricKeyPair());
        assertDoesNotThrow(encryptionManager::getAsymmetricPublicKeyBytes);
        assertDoesNotThrow(encryptionManager::getAsymmetricPrivateKeyBytes);
    }

    @Test
    public void testGenerateSymmetricKey() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        assertNotNull(encryptionManager.getSymmetricKeyBytes());
        assertTrue(encryptionManager.hasSymmetricKey());
        assertDoesNotThrow(encryptionManager::getSymmetricKeyBytes);
    }

    @Test
    public void testEncryptDecryptAsymmetric() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);

        String testString = "Hi!";

        try {
            byte[] encrypted = encryptionManager.encryptUsingAsymmetricKey(testString.getBytes());
            byte[] decrypted = encryptionManager.decryptUsingAsymmetricKey(encrypted);
            assertEquals(testString, new String(decrypted));
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testEncryptDecryptSymmetric() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        String testString = "Hello, World!";

        try {
            byte[] encrypted = encryptionManager.encryptUsingSymmetricKey(testString.getBytes());
            byte[] decrypted = encryptionManager.decryptUsingSymmetricKey(encrypted);
            assertEquals(testString, new String(decrypted));
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testEncryptDecryptMethodsNulls() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);
        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        assertThrows(IllegalArgumentException.class, () -> encryptionManager.encryptUsingAsymmetricKey(null));
        assertThrows(IllegalArgumentException.class, () -> encryptionManager.decryptUsingAsymmetricKey(null));
        assertThrows(IllegalArgumentException.class, () -> encryptionManager.encryptUsingSymmetricKey(null));
        assertThrows(IllegalArgumentException.class, () -> encryptionManager.decryptUsingSymmetricKey(null));
    }

    @Test
    public void testEncryptDecryptNotGeneratedKeys() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertThrows(IllegalStateException.class, () -> encryptionManager.encryptUsingAsymmetricKey(null));
        assertThrows(IllegalStateException.class, () -> encryptionManager.decryptUsingAsymmetricKey(null));
        assertThrows(IllegalStateException.class, () -> encryptionManager.encryptUsingSymmetricKey(null));
        assertThrows(IllegalStateException.class, () -> encryptionManager.decryptUsingSymmetricKey(null));
    }

    @Test
    public void testSetSymmetricKeyFromBytes() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        byte[] keyBytes = encryptionManager.getSymmetricKeyBytes();

        assertDoesNotThrow(() -> encryptionManager.setSymmetricKeyFromBytes(keyBytes));

        assertArrayEquals(keyBytes, encryptionManager.getSymmetricKeyBytes());
    }

    @Test
    public void testSetSymmetricKeyFromBytesNull() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        assertThrows(IllegalArgumentException.class, () -> encryptionManager.setSymmetricKeyFromBytes(null));
    }

    @Test
    public void testSetSymmetricKeyFromBytesNotGenerated() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertThrows(IllegalArgumentException.class, () -> encryptionManager.setSymmetricKeyFromBytes(null));
    }

    @Test
    public void testSetAsymmetricKeyPairFromBytes() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);

        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);

        byte[] publicKeyBytes = encryptionManager.getAsymmetricPublicKeyBytes();

        PublicKey publicKey;

        try {
            publicKey = EncryptionManager.loadAsymmetricPublicKeyFromBytes(publicKeyBytes);
        } catch (Exception exception) {
            Assertions.fail(exception);
            return;
        }

        assertArrayEquals(publicKeyBytes, encryptionManager.getAsymmetricPublicKeyBytes());
    }
}
