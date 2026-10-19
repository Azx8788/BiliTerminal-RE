package com.RobinNotBad.BiliClient.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class PasswordEncryptUtil {

    public static String getKeyPem(String rawKey) {
        // String#strip 是 API 33 才有的方法，本工程 minSdk 19 且未开 core library desugaring，
        // 在低版本设备上会抛 NoSuchMethodError（被线程池兜住后表现为「密码登录静默失败」）。
        // 这里只需要去首尾空白，直接用 trim() 行为一致且全版本可用。
        String content = rawKey.trim();
        content = content.replace("-----BEGIN PUBLIC KEY-----", "");
        content = content.replace("-----END PUBLIC KEY-----", "");
        content = content.replace("\n", "").replace("\r", "").trim();
        return content;
    }

    public static String encryptPassword(String password, String hash, String pubKeyPem) throws Exception {
        String keyContent = getKeyPem(pubKeyPem);
        byte[] keyBytes = Base64.decode(keyContent, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal((hash + password).getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
    }
}