package dev.mayuna.timestop.config;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EncryptionConfig {

    private int asymmetricKeySize = 2048;
    private int symmetricKeySize = 256;
}
