package com.bbvideoeditor.app.playback

import org.junit.Assert.*
import org.junit.Test

class PreviewRequestsTest {
    @Test fun thousandsOfOffersRetainOnlyNewestAndCannotOverwriteAnInflightGeneration() {
        val requests=PreviewRequests()
        requests.offer(100,1);val first=requests.take(1)!!
        repeat(10_000){requests.offer(it.toLong(),1)}
        assertNull(requests.take(1))
        assertEquals(9999L,requests.pending!!.positionUs)
        assertNull(requests.submitted(2,100,0))
        assertNull(requests.submitted(1,101,0))
        assertEquals(first,requests.submitted(1,100,0))
        assertEquals(9999L,requests.take(1)!!.positionUs)
        assertEquals(9999L,requests.coalesced)
    }
    @Test fun revisionReplacementAndLifecycleClearDiscardOldWork() {
        val requests=PreviewRequests()
        requests.offer(10,1);requests.take(1);requests.clear()
        requests.offer(20,2)
        assertNull(requests.submitted(1,10,0))
        assertEquals(20L,requests.take(2)!!.positionUs)
        requests.clear();assertFalse(requests.busy)
    }
}
