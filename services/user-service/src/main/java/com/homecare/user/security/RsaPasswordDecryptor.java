package com.homecare.user.security;

import com.homecare.user.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Decrypts RSA-OAEP encrypted password payloads sent by the frontend.
 *
 * Wire format: "rsa_oaep_sha256:" + base64(ciphertext)
 *
 * Note: This only obfuscates password in transit at the application layer.
 * Use HTTPS/TLS for real transport security.
 */
@Slf4j
@Component
public class RsaPasswordDecryptor {

    public static final String PREFIX = "rsa_oaep_sha256:";

    private final PrivateKey privateKey;

    public RsaPasswordDecryptor(@Value("${app.security.login-rsa.private-key:}") String privateKeyPem) {
        this.privateKey = parsePrivateKeyOrNull(privateKeyPem);
    }

    public String decryptRequired(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(400, "密码不能为空");
        }

        if (!password.startsWith(PREFIX)) {
            throw new BusinessException(400, "密码必须为加密密文");
        }

        if (privateKey == null) {
            throw new BusinessException(500, "服务端未配置登录解密密钥");
        }

        String b64 = password.substring(PREFIX.length()).trim();
        if (!StringUtils.hasText(b64)) {
            throw new BusinessException(400, "密码加密格式错误");
        }

        try {
            byte[] ciphertext = Base64.getDecoder().decode(b64);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            // Keep OAEP params aligned with WebCrypto:
            // RSA-OAEP + SHA-256 digest + MGF1(SHA-256).
            OAEPParameterSpec oaepSha256 = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSha256);
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "密码加密格式错误");
        } catch (Exception e) {
            log.warn("RSA-OAEP 解密失败（多为前端公钥与后端私钥不是同一对，或密文损坏）: {}", e.getMessage());
            throw new BusinessException(400,
                    "密码解密失败，请确认前端构建时的 VITE_LOGIN_RSA_PUBLIC_KEY 与服务端 app.security.login-rsa.private-key 为同一密钥对");
        }
    }

    private static PrivateKey parsePrivateKeyOrNull(String privateKeyPem) {
        if (!StringUtils.hasText(privateKeyPem)) {
            return null;
        }
        try {
            String normalized = privateKeyPem.replace("\\n", "\n");
            String pem = normalized
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new BusinessException(500, "服务端登录解密密钥配置错误");
        }
    }
}

