package dev.mayuna.timestop.encryption;

import dev.mayuna.timestop.config.EncryptionConfig;
import dev.mayuna.timestop.managers.EncryptionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestEncryptionManagerFIleIO {

    private static final EncryptionConfig encryptionConfig = new EncryptionConfig();

    private static final String asymmetricKeyOnlyName = "./asymmetric-key";
    private static final String symmetricKeyOnlyName = "./symmetric-key";

    private static final String asymmetricPrivateKeyFileName = "./asymmetric-key.key";
    private static final String asymmetricPublicKeyFileName = "./asymmetric-key.pub";
    private static final String symmetricKeyFileName = "./symmetric-key.key";

    @AfterEach
    @BeforeEach
    public void deleteFiles() {
        File publicKeyFile = new File(asymmetricPublicKeyFileName);
        File privateKeyFile = new File(asymmetricPrivateKeyFileName);
        File symmetricKeyFile = new File("./symmetric-key.key");

        publicKeyFile.delete();
        privateKeyFile.delete();
        symmetricKeyFile.delete();
    }

    @Test
    public void testSaveAndLoad() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);
        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);
        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        assertDoesNotThrow(() -> encryptionManager.saveAsymmetricKeyPair(asymmetricKeyOnlyName));
        assertDoesNotThrow(() -> encryptionManager.saveSymmetricKey(symmetricKeyOnlyName));

        assertTrue(new File(asymmetricPrivateKeyFileName).exists());
        assertTrue(new File(asymmetricPublicKeyFileName).exists());
        assertTrue(new File(symmetricKeyFileName).exists());

        EncryptionManager encryptionManager2 = new EncryptionManager(encryptionConfig);
        assertDoesNotThrow(() -> encryptionManager2.loadAsymmetricKeyPair(asymmetricKeyOnlyName));
        assertDoesNotThrow(() -> encryptionManager2.loadSymmetricKey(symmetricKeyOnlyName));

        assertArrayEquals(encryptionManager.getAsymmetricPrivateKeyBytes(), encryptionManager2.getAsymmetricPrivateKeyBytes());
        assertArrayEquals(encryptionManager.getAsymmetricPublicKeyBytes(), encryptionManager2.getAsymmetricPublicKeyBytes());
        assertArrayEquals(encryptionManager.getSymmetricKeyBytes(), encryptionManager2.getSymmetricKeyBytes());
    }

    @Test
    public void testSaveWithoutGeneratedKeys() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);
        assertThrows(IllegalStateException.class, () -> encryptionManager.saveAsymmetricKeyPair(asymmetricKeyOnlyName));
        assertThrows(IllegalStateException.class, () -> encryptionManager.saveSymmetricKey(symmetricKeyOnlyName));
    }

    @Test
    public void testSaveToInvalidPath() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);
        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);
        assertDoesNotThrow(encryptionManager::generateSymmetricKey);

        assertThrows(IOException.class, () -> encryptionManager.saveAsymmetricKeyPair("./invalid/path/asymmetric-key"));
        assertThrows(IOException.class, () -> encryptionManager.saveSymmetricKey("./invalid/path/symmetric-key"));
    }

    @Test
    public void testLoadWhenFilesDoesNotExist() {
        EncryptionManager encryptionManager = new EncryptionManager(encryptionConfig);
        assertThrows(IOException.class, () -> encryptionManager.loadAsymmetricKeyPair(asymmetricKeyOnlyName));
        assertThrows(IOException.class, () -> encryptionManager.loadSymmetricKey(symmetricKeyOnlyName));

        assertNull(encryptionManager.getAsymetricKeyPair());
        assertNull(encryptionManager.getSymmetricKey());
        assertFalse(encryptionManager.hasAsymmetricKeyPair());
        assertFalse(encryptionManager.hasSymmetricKey());
        assertThrows(IllegalStateException.class, encryptionManager::getSymmetricKeyBytes);
        assertThrows(IllegalStateException.class, encryptionManager::getAsymmetricPublicKeyBytes);
        assertThrows(IllegalStateException.class, encryptionManager::getAsymmetricPrivateKeyBytes);

        // Test private key not exists
        assertDoesNotThrow(encryptionManager::generateAsymmetricKeyPair);
        assertDoesNotThrow(() -> encryptionManager.saveAsymmetricKeyPair(asymmetricKeyOnlyName));
        assertTrue(new File(asymmetricPrivateKeyFileName).exists());
        assertTrue(new File(asymmetricPublicKeyFileName).exists());
        assertTrue(new File(asymmetricPrivateKeyFileName).delete()); // Delete private key
        assertThrows(IOException.class, () -> encryptionManager.loadAsymmetricKeyPair(asymmetricKeyOnlyName));
    }
}
