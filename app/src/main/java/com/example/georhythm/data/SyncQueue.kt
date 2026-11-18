package com.example.georhythm.data

import java.util.LinkedList

class SyncQueue {
    private val queue = LinkedList<SyncEvent>()

    @Synchronized
    fun enqueue(event: SyncEvent) {
        queue.addLast(event)
    }

    @Synchronized
    fun peek(): SyncEvent? = queue.peekFirst()

    @Synchronized
    fun dequeue(): SyncEvent? = queue.pollFirst()

    @Synchronized
    fun size(): Int = queue.size

    @Synchronized
    fun clear() = queue.clear()
}
