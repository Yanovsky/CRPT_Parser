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

public enum Parser {
    INSTANCE;

    private final Charset cp1251 = Charset.forName("cp1251");
    private final Map<String, HeaderFormat> header;
    private final int headerSize;

    Parser() {
        header = new LinkedHashMap<>();
        header.put("Наименование файла выгрузки: «%s»", new HeaderFormat(getLastLength(), 66));
        header.put("Программа выгрузки: «%s»", new HeaderFormat(getLastLength(), 256));
        header.put("Регистрационный номер ККТ: «%s»", new HeaderFormat(getLastLength(), 20));
        header.put("Номер ФН: «%s»", new HeaderFormat(getLastLength(), 16));
        header.put("Номер версии ФФД: %d", new HeaderFormat(getLastLength(), 1));
        header.put("Номер первого документа: %d", new HeaderFormat(getLastLength(), 4));
        header.put("Номер последнего документа: %d", new HeaderFormat(getLastLength(), 4));
        header.put("Количество уведомлений о реализации маркированного товара: %d", new HeaderFormat(getLastLength(), 4));
        header.put("Контрольная сумма файла выгрузки: %d, расчитанная %d. Контрольные суммы %s", new HeaderFormat(getLastLength(), 4));
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
                .map(headerFormat -> {
                    int offset = headerFormat.getValue().getOffset();
                    int length = headerFormat.getValue().getSize();
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
                            values = new Object[]{ value, crc32, BooleanUtils.toString(crc32 == value, "совпадают", "НЕ СОВПАДАЮТ!") };
                            break;
                        default:
                            values = new Object[]{ new String(Arrays.copyOfRange(bytes, offset, offset + length), cp1251) };
                            break;
                    }
                    return String.format(headerFormat.getKey(), values);
                })
                .collect(Collectors.toList())
        );
        int offset = headerSize;
        writeString(outputFile, "Список уведомлений:\r\n");
        while (offset < bytes.length) {
            int length = Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            byte[] dataBytes = Arrays.copyOfRange(bytes, offset, offset + length + 2);
            long crc16 = Short.toUnsignedInt(ByteBuffer.wrap(dataBytes, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            byte[] bytesForCRC = ByteBuffer.allocate(length - 2).put(dataBytes, 2, 2).put(dataBytes, 6, length - 4).array();
            long crc16Calculated = Integer.toUnsignedLong(BytesUtils.calculateCRC16(bytesForCRC));

            int number = Short.toUnsignedInt(ByteBuffer.wrap(dataBytes, 20, 2).getShort());
            writeString(outputFile,
                String.format("\tУведомление №%d (%d bytes), CRC16 в файле: %d, рассчитанная: %d. CRC16 %s. Данные: %s%n",
                    number, length, crc16, crc16Calculated, BooleanUtils.toString(crc16 == crc16Calculated, "совпадают", "НЕ СОВПАДАЮТ!"),
                    BytesUtils.toString(dataBytes)
                )
            );
            offset = offset + length + 2;
        }
    }

    private void writeStrings(File outputFile, Collection<String> value) {
        try {
            FileUtils.writeLines(outputFile, StandardCharsets.UTF_8.name(), value, true);
            value.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeString(File outputFile, String value) {
        try {
            FileUtils.writeStringToFile(outputFile, value, StandardCharsets.UTF_8, true);
            System.out.print(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
