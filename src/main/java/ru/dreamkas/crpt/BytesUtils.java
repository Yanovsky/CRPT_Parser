package ru.dreamkas.crpt;

import java.util.zip.CRC32;

import org.apache.commons.lang3.StringUtils;

public class BytesUtils {
    public static boolean hasBit(int number, int bitNum) {
        return (number & (1 << bitNum)) > 0;
    }

    public static int getBit(int number, int bitNum) {
        return number & (1 << bitNum);
    }

    public static byte setBit(byte number, int bitNum, boolean bitValue) {
        if (bitValue) {
            return (byte) (number | (1 << bitNum));
        }
        return (byte) (number & (~(1 << bitNum)));
    }

    public static int setBit(int number, int bitNum, boolean bitValue) {
        if (bitValue) {
            return number | (1 << bitNum);
        }
        return number & (~(1 << bitNum));
    }

    public static long setBit(long number, int bitNum, boolean bitValue) {
        if (bitValue) {
            return number | (1L << bitNum);
        }
        return number & (~(1L << bitNum));
    }

    public static String toString(byte[] bytes) {
        return toString(bytes, " ", '[', ']');
    }

    @SuppressWarnings("DuplicatedCode")
    public static String toString(byte[] bytes, String delimiter, Character open, Character close) {
        if (bytes == null) {
            return (open != null ? "[" : "") + "null" + (close != null ? "]" : "");
        }
        if (bytes.length <= 0) {
            return (open != null ? "[" : "") + (close != null ? "]" : "");
        }

        StringBuilder str = new StringBuilder();
        if (open != null) {
            str.append(open);
        }
        for (byte b : bytes) {
            str.append(String.format("%02X", b));
            if (delimiter != null) {
                str.append(delimiter);
            }
        }
        String result = str.toString().trim();
        if (close != null) {
            result += close;
        }

        return result;
    }

    @SuppressWarnings("DuplicatedCode")
    public static String toString(int[] values, String delimiter, Character open, Character close) {
        if (values == null || values.length <= 0) {
            return (open != null ? "[" : "") + "null" + (close != null ? "]" : "");
        }
        StringBuilder sb = new StringBuilder();
        if (open != null) {
            sb.append(open);
        }
        for (int i = 0; i < values.length - 1; i++) {
            sb.append(toUnsignedByte(values[i]));
            if (delimiter != null) {
                sb.append(delimiter);
            }
        }
        sb.append(toUnsignedByte(values[values.length - 1]));
        if (close != null) {
            sb.append(close);
        }
        return sb.toString();
    }

    public static String toUnsignedByte(byte b) {
        return String.format("0x%02X", b & 0xFF);
    }

    public static String toUnsignedByte(int b) {
        return String.format("0x%02X", b & 0xFF);
    }

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    public static byte[] hexStringToByteArray(String hexString) {
        if (StringUtils.isBlank(hexString)) {
            return new byte[0];
        }
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static int calculateCRC16(byte[] bytes) {
        int crc = 0xFFFF;
        int i = 0;
        while (i < bytes.length) {
            byte aByte = bytes[i];
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (aByte & 0xff);
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
            i++;
        }
        crc &= 0xffff;
        return crc;
    }

    public static long calculateCRC32(byte[] bytes) {
        CRC32 crc32 = new CRC32();
        crc32.reset();
        crc32.update(bytes);
        return crc32.getValue();
    }
}
