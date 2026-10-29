package com.bbvideoeditor.app.media

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test

class PriorityWorkQueueTest {
    @Test fun aCancelledOperationDoesNotTerminateItsSharedWorker()=runBlocking {
        val queue=PriorityWorkQueue<Int>(1)
        try {
            val cancelled=async {queue.submit("cancelled-request",AssetPriority.VISIBLE){throw CancellationException("Obsolete request")}}
            withTimeout(5_000){cancelled.join()}
            assertTrue(cancelled.isCancelled)
            val result=withTimeout(5_000){queue.submit("new-visible",AssetPriority.VISIBLE){7}}
            assertEquals(7,result)
        }finally{queue.close()}
    }
    @Test fun visibleWorkDisplacesOldPrefetchAndDuplicateRequestsShareOneOperation()=runBlocking {
        val queue=PriorityWorkQueue<Int>(1,2)
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        try {
            val first=async(start=CoroutineStart.UNDISPATCHED){queue.submit("running",AssetPriority.VISIBLE){entered.complete(Unit);release.await();1}}
            withTimeout(5_000){entered.await()}
            val old=async(start=CoroutineStart.UNDISPATCHED){queue.submit("old",AssetPriority.PREFETCH){2}}
            val next=async(start=CoroutineStart.UNDISPATCHED){queue.submit("next",AssetPriority.PREFETCH){3}}
            var executions=0
            val visible=async(start=CoroutineStart.UNDISPATCHED){queue.submit("visible",AssetPriority.VISIBLE){executions++;4}}
            val duplicate=async(start=CoroutineStart.UNDISPATCHED){queue.submit("visible",AssetPriority.VISIBLE){error("Duplicate work ran")}}
            assertNull(old.await())
            assertEquals(2,queue.metrics().queued);assertEquals(1,queue.metrics().running)
            release.complete(Unit)
            withTimeout(5_000){assertEquals(1,first.await());assertEquals(4,visible.await());assertEquals(4,duplicate.await());assertEquals(3,next.await())}
            assertEquals(1,executions);assertEquals(1L,queue.metrics().discarded)
        }finally{release.complete(Unit);queue.close()}
    }

    @Test fun cancellingWaitersDoesNotFreeAnOccupiedNativeWorker()=runBlocking {
        val queue=PriorityWorkQueue<Int>(1)
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val order=Channel<Int>(Channel.UNLIMITED)
        try {
            val running=async(start=CoroutineStart.UNDISPATCHED){queue.submit("native",AssetPriority.VISIBLE){entered.complete(Unit);release.await();order.send(1);1}}
            withTimeout(5_000){entered.await()};running.cancelAndJoin()
            val cancelled=async(start=CoroutineStart.UNDISPATCHED){queue.submit("cancelled",AssetPriority.VISIBLE){error("Cancelled queued operation ran")}}
            cancelled.cancelAndJoin()
            val fresh=async(start=CoroutineStart.UNDISPATCHED){queue.submit("fresh",AssetPriority.VISIBLE){order.send(2);2}}
            assertEquals(1,queue.metrics().running);assertEquals(1,queue.metrics().queued)
            assertTrue(order.tryReceive().isFailure)
            release.complete(Unit)
            withTimeout(5_000){assertEquals(2,fresh.await());assertEquals(1,order.receive());assertEquals(2,order.receive())}
        }finally{release.complete(Unit);queue.close()}
        assertThrows(IllegalStateException::class.java){runBlocking{queue.submit("closed",AssetPriority.VISIBLE){1}}}
        Unit
    }
}
