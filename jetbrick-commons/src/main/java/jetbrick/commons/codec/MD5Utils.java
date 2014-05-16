/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 * Email: subchen@gmail.com
 * URL: http://subchen.github.io/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.commons.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jetbrick.commons.lang.CharsetUtils;

public class MD5Utils {
    private static final MessageDigest md5 = getDigest("MD5");

    public static byte[] md5(byte[] data) {
        return md5.digest(data);
    }

    public static byte[] md5(String data) {
        return md5(data.getBytes(CharsetUtils.UTF_8));
    }

    public static String md5Hex(byte[] data) {
        return HexUtils.encodeHexString(md5(data));
    }

    public static String md5Hex(String data) {
        return HexUtils.encodeHexString(md5(data));
    }

    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}