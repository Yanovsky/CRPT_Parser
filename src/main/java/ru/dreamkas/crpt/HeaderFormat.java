package ru.dreamkas.crpt;

public class HeaderFormat {
    private final int offset;
    private final int size;

    public HeaderFormat(int offset, int size) {
        this.offset = offset;
        this.size = size;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    public int getLength() {
        return offset + size;
    }
}
