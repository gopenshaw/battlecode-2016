package jeremy.util;

public class BoundedQueue<T> {

    private int capacity;
    private int head;
    private int tail;
    private int size;

    private T[] memory;

    public BoundedQueue(int capacity) {
        memory = (T[]) new Object[capacity];
        this.capacity = capacity;
    }

    public void add(T object) {
        memory[tail] = object;
        tail = (tail + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    public T remove() {
        size--;
        T object = memory[head];
        head = (head + 1) % capacity;
        return object;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    public boolean contains(T element) {
        for (int i = 0; i < size; i++) {
            if (memory[(head + i) % capacity].equals(element)) {
                return true;
            }
        }

        return false;
    }
}


