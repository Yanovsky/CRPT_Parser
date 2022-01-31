package ru.dreamkas.crpt;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Parser {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(Parser.class);
    private final Charset cp1251 = Charset.forName("cp1251");
    private final Map<String, HeaderFormat> header;
    private final int headerSize;

    Parser() {
        header = new LinkedHashMap<>();
        header.put("Наименование файла выгрузки (%s): «%s»", new HeaderFormat(getLastLength(), 66));
        header.put("Программа выгрузки (%s): «%s»", new HeaderFormat(getLastLength(), 256));
        header.put("Регистрационный номер ККТ (%s): «%s»", new HeaderFormat(getLastLength(), 20));
        header.put("Номер ФН (%s): «%s»", new HeaderFormat(getLastLength(), 16));
        header.put("Номер версии ФФД (%s): %d", new HeaderFormat(getLastLength(), 1));
        header.put("Номер первого документа (%s): %d", new HeaderFormat(getLastLength(), 4));
        header.put("Номер последнего документа (%s): %d", new HeaderFormat(getLastLength(), 4));
        header.put("Количество уведомлений о реализации маркированного товара (%s): %d", new HeaderFormat(getLastLength(), 4));
        header.put("Контрольная сумма файла выгрузки (%s): %d, расчитанная %d. Контрольные суммы %s", new HeaderFormat(getLastLength(), 4));
        headerSize = header.values().stream().mapToInt(HeaderFormat::getSize).sum();
    }

    private int getLastLength() {
        return header.isEmpty() ? 0 : header.values().toArray(new HeaderFormat[0])[header.size() -1].getLength();
    }

    public void parse(File file) throws IOException {
        File outputFile = Paths.get(file.getParent()).resolve(FilenameUtils.removeExtension(file.getName()) + ".res").toFile();
        FileUtils.deleteQuietly(outputFile);
        byte[] bytes = FileUtils.readFileToByteArray(file);

        ByteBuffer dataBuffer = ByteBuffer.allocate(bytes.length - 4);
        dataBuffer.put(bytes, 0, headerSize - 4).put(bytes, headerSize, bytes.length - headerSize);
        final long crc32 = BytesUtils.calculateCRC32(dataBuffer.array());
        writeStrings(outputFile,
            header.entrySet().stream()
                .map(entry -> {
                    HeaderFormat headerFormat = entry.getValue();
                    int offset = headerFormat.getOffset();
                    int length = headerFormat.getSize();
                    ByteBuffer valueBytes = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN);
                    Object[] values;
                    String partOfBytes = BytesUtils.toString(Arrays.copyOfRange(valueBytes.array(), offset, headerFormat.getLength()));
                    switch (length) {
                        case 1:
                            values = new Object[]{ partOfBytes, Byte.toUnsignedInt(valueBytes.get()) };
                            break;
                        case 2:
                            values = new Object[]{ partOfBytes, Short.toUnsignedInt(valueBytes.getShort()) };
                            break;
                        case 4:
                            long value = Integer.toUnsignedLong(valueBytes.getInt());
                            values = new Object[]{ partOfBytes, value, crc32, BooleanUtils.toString(crc32 == value, "совпадают", "НЕ СОВПАДАЮТ!") };
                            break;
                        default:
                            values = new Object[]{ partOfBytes, new String(Arrays.copyOfRange(bytes, offset, offset + length), cp1251) };
                            break;
                    }
                    return String.format(entry.getKey(), values);
                })
                .collect(Collectors.toList())
        );
        int offset = headerSize;
        writeString(outputFile, "Список уведомлений:");
        while (offset < bytes.length) {
            byte[] lengthBytes = Arrays.copyOfRange(bytes, offset, offset + 2);
            int length = Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            byte[] dataBytes = Arrays.copyOfRange(bytes, offset, offset + length + 2);
            byte[] crc16Bytes = Arrays.copyOfRange(bytes, offset+4, offset + 6);
            long crc16 = Short.toUnsignedInt(ByteBuffer.wrap(dataBytes, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            byte[] bytesForCRC = ByteBuffer.allocate(length - 2).put(dataBytes, 2, 2).put(dataBytes, 6, length - 4).array();
            long crc16Calculated = Integer.toUnsignedLong(BytesUtils.calculateCRC16(bytesForCRC));

            int number = Short.toUnsignedInt(ByteBuffer.wrap(dataBytes, 20, 2).getShort());
            writeString(outputFile,
                String.format("\tУведомление №%d (%d bytes %s), CRC16 в файле (%s): %d, рассчитанная: %d. CRC16 %s. Данные: %s",
                    number, length, BytesUtils.toString(lengthBytes), BytesUtils.toString(crc16Bytes), crc16, crc16Calculated, BooleanUtils.toString(crc16 == crc16Calculated, "совпадают", "НЕ СОВПАДАЮТ!"),
                    BytesUtils.toString(dataBytes)
                )
            );
            offset = offset + length + 2;
        }
    }

    private void writeStrings(File outputFile, Collection<String> value) {
        try {
            FileUtils.writeLines(outputFile, StandardCharsets.UTF_8.name(), value, true);
            value.forEach(log::debug);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeString(File outputFile, String value) {
        try {
            FileUtils.writeStringToFile(outputFile, value+"\r\n", StandardCharsets.UTF_8, true);
            log.debug(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
