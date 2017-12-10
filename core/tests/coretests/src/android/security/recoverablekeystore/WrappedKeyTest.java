/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security.recoverablekeystore;

import static org.junit.Assert.assertEquals;

import android.security.keystore.AndroidKeyStoreSecretKey;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WrappedKeyTest {
    private static final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String WRAPPING_KEY_ALIAS = "WrappedKeyTestWrappingKeyAlias";
    private static final int GCM_TAG_LENGTH_BYTES = 16;
    private static final int BITS_PER_BYTE = 8;
    private static final int GCM_TAG_LENGTH_BITS = GCM_TAG_LENGTH_BYTES * BITS_PER_BYTE;

    @After
    public void tearDown() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
        keyStore.load(/*param=*/ null);
        keyStore.deleteEntry(WRAPPING_KEY_ALIAS);
    }

    @Test
    public void fromSecretKey_createsWrappedKeyThatCanBeUnwrapped() throws Exception {
        SecretKey wrappingKey = generateAndroidKeyStoreKey();
        SecretKey rawKey = generateKey();

        WrappedKey wrappedKey = WrappedKey.fromSecretKey(wrappingKey, rawKey);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(
                Cipher.UNWRAP_MODE,
                wrappingKey,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, wrappedKey.getNonce()));
        SecretKey unwrappedKey = (SecretKey) cipher.unwrap(
                wrappedKey.getKeyMaterial(), KEY_ALGORITHM, Cipher.SECRET_KEY);
        assertEquals(rawKey, unwrappedKey);
    }

    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(/*keySize=*/ 256);
        return keyGenerator.generateKey();
    }

    private AndroidKeyStoreSecretKey generateAndroidKeyStoreKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KEY_ALGORITHM,
                ANDROID_KEY_STORE_PROVIDER);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                WRAPPING_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return (AndroidKeyStoreSecretKey) keyGenerator.generateKey();
    }
}
