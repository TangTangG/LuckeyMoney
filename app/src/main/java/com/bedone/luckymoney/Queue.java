package com.bedone.luckymoney;


/**
 * @author Caigao.Tang
 * @date 2018/1/9
 * description:
 * An array queue,which known as FIFO.
 *
 * This model does not support thread safety.If you need it,
 * use <ArrayBlockingQueue.java> which provided by JDK.
 *
 * The default size of this queue is 16(also you can specify
 * the size when initialization),each expansion size is double.
 *
 * If the head comes to half of this queue capacity
 * ,queue will be reorganize itself.
 *
 * The adjustment of the array occurs every time the remove function
 * is called, so it is not recommended to use the delete frequently.
 */
public class Queue<E>{

    private Object[] queue;
    private int head = 0;
    private int tail = 0;

    public Queue() {
        this(16);
    }

    public Queue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("");
        }
        queue = new Object[capacity];
    }

    public E offer(E e) {
        if (e == null) {
            return null;
        }
        queue[tail] = e;
        if (++tail == queue.length) {
            doubleCapacity();
        }
        return e;
    }

    private void doubleCapacity() {
        int old = queue.length;
        int h = head;
        int size = old - h;
        int newCapacity = old << 1;
        //overflow
        if (newCapacity < 0) {
            throw new IllegalStateException("queue is too big");
        }
        Object[] a = new Object[newCapacity];
        System.arraycopy(queue, h, a, 0, size);
        queue = a;
        head = 0;
        tail = size;
    }

    @SuppressWarnings("unchecked")
    public E poll() {
        if (tail <= 0) {
            return null;
        }
        Object result = queue[head];
        if (result == null) {
            return null;
        }
        queue[head] = null;
        if (++head == (queue.length >>> 1)) {
            reorganizeQueue();
        }
        return (E) result;
    }

    private void reorganizeQueue() {
        int t = tail;
        int h = head;
        int size = t - h;
        Object[] a = new Object[t];
        System.arraycopy(queue, h, a, 0, size);
        queue = a;
        head = 0;
        tail = size;
    }

    @SuppressWarnings("unchecked")
    public E peek() {
        Object o = queue[head];
        if (o == null) {
            return null;
        }
        return (E) o;
    }

    public boolean remove(E element) {
        for (int i = head; i < tail; i++) {
            Object o = queue[i];
            if (o.equals(element)) {
                return delete(i);
            }
        }
        return false;
    }

    private boolean delete(int i) {
        if (i == head) {
            poll();
            return true;
        }
        if (i == tail - 1) {
            queue[i] = null;
            tail = i;
            return true;
        }
        //head <= i < tail
        int h = head;
        int t = tail;
        int front = i - h;
        int back = t - i - 1;
        Object[] queue = this.queue;
        queue[i] = null;
        if (front < back) {
            System.arraycopy(queue, h, queue, h + 1, front);
            head = h + 1;
        } else {
            System.arraycopy(queue, i + 1, queue, i, back);
            tail = t - 1;
        }
        return true;
    }

    public int size() {
        return tail - head;
    }

    public void clear() {
        for (int i = head; i < tail; i++) {
            queue[i] = null;
        }
        head = tail = 0;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = head; i < tail; i++) {
            builder.append(queue[i]).append(" ");
        }
        return builder.toString();
    }
}
