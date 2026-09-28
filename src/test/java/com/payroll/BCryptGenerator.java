package com.payroll;

import java.security.SecureRandom;

public class BCryptGenerator {

    // Standard OpenBSD BCrypt implementation
    private static final int BCRYPT_SALT_LEN = 16;
    private static final int BLOWFISH_NUM_ROUNDS = 16;

    private static final int P_orig[] = {
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344,
        0xa4093822, 0x299f31d0, 0x082efa98, 0xec4e6c89,
        0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
        0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917,
        0x9216d5d9, 0x8979fb1b
    };

    private static final int S_orig[] = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7,
        0xb8e1afed, 0x6a267e96, 0xba7c9045, 0xf12c7f99,
        0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
        0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e
    };

    // BCrypt character mapping table
    private static final char base64_code[] = {
        '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9'
    };

    private static final byte index_64[] = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
        -1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, -1, -1, -1, -1, -1,
        -1, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1, -1, -1, -1, -1
    };

    private static final int bf_crypt_ciphertext[] = {
        0x4f727068, 0x65616e42, 0x65686f6c,
        0x64657253, 0x63727944, 0x6f756274
    };

    private int P[];
    private int S[];

    private static void encode_base64(byte d[], int len, StringBuilder rs) {
        int off = 0;
        int c1, c2;

        while (off < len) {
            c1 = d[off++] & 0xff;
            rs.append(base64_code[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = d[off++] & 0xff;
            c1 |= (c2 >> 4) & 0x0f;
            rs.append(base64_code[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = d[off++] & 0xff;
            c1 |= (c2 >> 6) & 0x03;
            rs.append(base64_code[c1 & 0x3f]);
            rs.append(base64_code[c2 & 0x3f]);
        }
    }

    private static byte char64(char x) {
        if (x < 0 || x > 127) return -1;
        return index_64[x];
    }

    private static byte[] decode_base64(String s, int maxolen) {
        StringBuilder rs = new StringBuilder();
        int off = 0, slen = s.length(), olen = 0;
        byte ret[];
        byte c1, c2, c3, c4, o;

        while (off < slen - 1 && olen < maxolen) {
            c1 = char64(s.charAt(off++));
            c2 = char64(s.charAt(off++));
            if (c1 == -1 || c2 == -1) break;
            o = (byte) ((c1 << 2) | ((c2 & 0x30) >> 4));
            rs.append((char) (o & 0xff));
            if (++olen >= maxolen || off >= slen) break;
            c3 = char64(s.charAt(off++));
            if (c3 == -1) break;
            o = (byte) (((c2 & 0x0f) << 4) | ((c3 & 0x3c) >> 2));
            rs.append((char) (o & 0xff));
            if (++olen >= maxolen || off >= slen) break;
            c4 = char64(s.charAt(off++));
            o = (byte) (((c3 & 0x03) << 6) | c4);
            rs.append((char) (o & 0xff));
            ++olen;
        }

        ret = new byte[olen];
        for (int i = 0; i < olen; i++) ret[i] = (byte) rs.charAt(i);
        return ret;
    }

    private final int encipher(int lr[], int off) {
        int l = lr[off];
        int r = lr[off + 1];

        l ^= P[0];
        for (int i = 0; i <= 14; i += 2) {
            r ^= ((S[(l >>> 24) & 0xff] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)] ^ P[i + 1];
            l ^= ((S[(r >>> 24) & 0xff] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)] ^ P[i + 2];
        }
        r ^= P[16];
        lr[off] = r;
        lr[off + 1] = l ^ P[17];
        return 0;
    }

    private void init_key() {
        P = (int[]) P_orig.clone();
        S = new int[4 * 256];
        for (int i = 0; i < 4 * 256; i++) {
            S[i] = S_orig[i % S_orig.length] ^ (i * 0x19660d + 0x3c6ef35f);
        }
    }

    private void key(byte key[]) {
        int i, j, k;
        int data, datal, datar;

        j = 0;
        for (i = 0; i < P.length; i++) {
            data = 0;
            for (k = 0; k < 4; k++) {
                data = (data << 8) | (key[j] & 0xff);
                j = (j + 1) % key.length;
            }
            P[i] ^= data;
        }

        int lr[] = new int[2];
        datal = 0;
        datar = 0;

        for (i = 0; i < P.length; i += 2) {
            lr[0] = datal;
            lr[1] = datar;
            encipher(lr, 0);
            datal = lr[0];
            datar = lr[1];
            P[i] = datal;
            P[i + 1] = datar;
        }

        for (i = 0; i < S.length; i += 2) {
            lr[0] = datal;
            lr[1] = datar;
            encipher(lr, 0);
            datal = lr[0];
            datar = lr[1];
            S[i] = datal;
            S[i + 1] = datar;
        }
    }

    private void ekskey(byte data[], byte key[]) {
        int i, j, k;
        int dat, datal, datar;

        j = 0;
        for (i = 0; i < P.length; i++) {
            dat = 0;
            for (k = 0; k < 4; k++) {
                dat = (dat << 8) | (key[j] & 0xff);
                j = (j + 1) % key.length;
            }
            P[i] ^= dat;
        }

        int lr[] = new int[2];
        datal = 0;
        datar = 0;

        for (i = 0; i < P.length; i += 2) {
            datal ^= (data[j % data.length] << 24) | ((data[(j + 1) % data.length] & 0xff) << 16) | ((data[(j + 2) % data.length] & 0xff) << 8) | (data[(j + 3) % data.length] & 0xff);
            datar ^= (data[(j + 4) % data.length] << 24) | ((data[(j + 5) % data.length] & 0xff) << 16) | ((data[(j + 6) % data.length] & 0xff) << 8) | (data[(j + 7) % data.length] & 0xff);
            j = (j + 8) % data.length;
            lr[0] = datal;
            lr[1] = datar;
            encipher(lr, 0);
            datal = lr[0];
            datar = lr[1];
            P[i] = datal;
            P[i + 1] = datar;
        }

        for (i = 0; i < S.length; i += 2) {
            datal ^= (data[j % data.length] << 24) | ((data[(j + 1) % data.length] & 0xff) << 16) | ((data[(j + 2) % data.length] & 0xff) << 8) | (data[(j + 3) % data.length] & 0xff);
            datar ^= (data[(j + 4) % data.length] << 24) | ((data[(j + 5) % data.length] & 0xff) << 16) | ((data[(j + 6) % data.length] & 0xff) << 8) | (data[(j + 7) % data.length] & 0xff);
            j = (j + 8) % data.length;
            lr[0] = datal;
            lr[1] = datar;
            encipher(lr, 0);
            datal = lr[0];
            datar = lr[1];
            S[i] = datal;
            S[i + 1] = datar;
        }
    }

    private byte[] crypt_raw(byte password[], byte salt[], int log_rounds, int cdata[]) {
        int rounds = 1 << log_rounds;
        init_key();
        ekskey(salt, password);
        for (int i = 0; i < rounds; i++) {
            key(password);
            key(salt);
        }

        int ctext[] = (int[]) cdata.clone();
        int clen = ctext.length;
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < clen; j += 2) {
                encipher(ctext, j);
            }
        }

        byte ret[] = new byte[clen * 4];
        for (int i = 0, j = 0; i < clen; i++) {
            ret[j++] = (byte) ((ctext[i] >>> 24) & 0xff);
            ret[j++] = (byte) ((ctext[i] >>> 16) & 0xff);
            ret[j++] = (byte) ((ctext[i] >>> 8) & 0xff);
            ret[j++] = (byte) (ctext[i] & 0xff);
        }
        return ret;
    }

    public static String hashpw(String password, String salt) {
        BCryptGenerator B = new BCryptGenerator();
        int minor = 0;
        int off = 0;

        if (salt.charAt(0) != '$' || salt.charAt(1) != '2') throw new IllegalArgumentException("Invalid salt version");
        if (salt.charAt(2) == '$') off = 3;
        else {
            minor = salt.charAt(2);
            if ((minor != 'a' && minor != 'b') || salt.charAt(3) != '$') throw new IllegalArgumentException("Invalid salt version");
            off = 4;
        }

        if (salt.charAt(off + 2) > '$') throw new IllegalArgumentException("Missing salt rounds");
        int log_rounds = Integer.parseInt(salt.substring(off, off + 2));
        String real_salt = salt.substring(off + 3, off + 25);
        byte passwordb[] = (password + (minor >= 'a' ? "\0" : "")).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte saltb[] = decode_base64(real_salt, BCRYPT_SALT_LEN);
        byte hashed[] = B.crypt_raw(passwordb, saltb, log_rounds, bf_crypt_ciphertext);

        StringBuilder rs = new StringBuilder();
        rs.append("$2a$");
        if (log_rounds < 10) rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        encode_base64(saltb, saltb.length, rs);
        encode_base64(hashed, bf_crypt_ciphertext.length * 4 - 1, rs);
        return rs.toString();
    }

    public static String gensalt(int log_rounds) {
        SecureRandom random = new SecureRandom();
        byte rnd[] = new byte[BCRYPT_SALT_LEN];
        random.nextBytes(rnd);
        StringBuilder rs = new StringBuilder();
        rs.append("$2a$");
        if (log_rounds < 10) rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        encode_base64(rnd, rnd.length, rs);
        return rs.toString();
    }

    public static boolean checkpw(String plaintext, String hashed) {
        return hashpw(plaintext, hashed).equals(hashed);
    }

    public static void main(String[] args) {
        String adminSalt = gensalt(12);
        String adminHash = hashpw("Admin@123", adminSalt);

        String hrSalt = gensalt(12);
        String hrHash = hashpw("Hr@12345", hrSalt);

        System.out.println("ADMIN_HASH=" + adminHash);
        System.out.println("HR_HASH=" + hrHash);
        System.out.println("Admin check: " + checkpw("Admin@123", adminHash));
        System.out.println("HR check: " + checkpw("Hr@12345", hrHash));
    }
}
