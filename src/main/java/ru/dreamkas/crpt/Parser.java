package ru.dreamkas.crpt;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;

public class Parser {
    private final Charset cp1251 = Charset.forName("cp1251");
    private final Map<String, Map.Entry<Integer, Integer>> header;
    private final int headerSize;

    public Parser() {
        header = new LinkedHashMap<>();
        header.put("Наименование файла выгрузки: «%s»", new AbstractMap.SimpleEntry<>(0, 66));
        header.put("Программа выгрузки: «%s»", new AbstractMap.SimpleEntry<>(66, 256));
        header.put("Регистрационный номер ККТ: «%s»", new AbstractMap.SimpleEntry<>(66 + 256, 20));
        header.put("Номер ФН: «%s»", new AbstractMap.SimpleEntry<>(66 + 256 + 20, 16));
        header.put("Номер версии ФФД: %d", new AbstractMap.SimpleEntry<>(66 + 256 + 20 + 16, 1));
        header.put("Номер первого документа: %d", new AbstractMap.SimpleEntry<>(66 + 256 + 20 + 16 + 1, 4));
        header.put("Номер последнего документа: %d", new AbstractMap.SimpleEntry<>(66 + 256 + 20 + 16 + 1 + 4, 4));
        header.put("Количество уведомлений о реализации маркированного товара: %d", new AbstractMap.SimpleEntry<>(66 + 256 + 20 + 16 + 1 + 4 + 4, 4));
        header.put("Контрольная сумма файла выгрузки: %d, расчитанная %d. Контрольные суммы %s", new AbstractMap.SimpleEntry<>(66 + 256 + 20 + 16 + 1 + 4 + 4 + 4, 4));
        headerSize = header.values().stream().mapToInt(Map.Entry::getValue).sum();
    }

    public void parse(File file) throws IOException {
        File outputFile = Paths.get(".").resolve(FilenameUtils.removeExtension(file.getName()) + ".res").toFile();
        FileUtils.deleteQuietly(outputFile);
        byte[] bytes = FileUtils.readFileToByteArray(file);

        ByteBuffer dataBuffer = ByteBuffer.allocate(bytes.length - 4);
        dataBuffer.put(bytes, 0, headerSize - 4).put(bytes, headerSize, bytes.length - headerSize);
        final long calcCrc = BytesUtils.calculateCRC32(dataBuffer.array());
        FileUtils.writeLines(outputFile,
            StandardCharsets.UTF_8.name(),
            header.entrySet().stream()
                .map(e -> {
                    int offset = e.getValue().getKey();
                    int length = e.getValue().getValue();
                    ByteBuffer valueBytes = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN);
                    Object[] values;
                    switch (length) {
                        case 1:
                            values = new Object[]{ Byte.toUnsignedInt(valueBytes.get()) };
                            break;
                        case 2:
                            values = new Object[]{ Short.toUnsignedInt(valueBytes.getShort()) };
                            break;
                        case 4:
                            long value = Integer.toUnsignedLong(valueBytes.getInt());
                            values = new Object[]{ value, calcCrc, BooleanUtils.toString(calcCrc == value, "совпадают", "НЕ СОВПАДАЮТ!") };
                            break;
                        default:
                            values = new Object[]{ new String(Arrays.copyOfRange(bytes, offset, offset + length), cp1251) };
                            break;
                    }
                    return String.format(e.getKey(), values);
                })
                .collect(Collectors.toList()),
            true
        );
        int offset = headerSize;
        FileUtils.writeStringToFile(outputFile, "Список уведомлений:\r\n", StandardCharsets.UTF_8, true);
        while (offset < bytes.length) {
            int length = Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            byte[] dataBytes = Arrays.copyOfRange(bytes, offset, offset + length);
            long crc16 = Short.toUnsignedInt(ByteBuffer.wrap(dataBytes, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            byte[] bytesForCRC = ByteBuffer.allocate(length - 2).put(dataBytes, 0, 2).put(dataBytes, 4, length - 4).array();
            long crc16Calculated = Integer.toUnsignedLong(BytesUtils.calculateCRC16(bytesForCRC));

            int number = Short.toUnsignedInt(ByteBuffer.wrap(dataBytes, 18, 2).getShort());
            FileUtils.writeStringToFile(outputFile,
                String.format("\tУведомление №%d (%d bytes), CRC16 в файле: %d, рассчитанная: %d. CRC16 %s. Данные: %s%n",
                    number, length, crc16, crc16Calculated, BooleanUtils.toString(crc16 == crc16Calculated, "совпадают", "НЕ СОВПАДАЮТ!"),
                    BytesUtils.toString(dataBytes)
                ),
                StandardCharsets.UTF_8,
                true
            );
            offset = offset + length;
        }
    }
}
