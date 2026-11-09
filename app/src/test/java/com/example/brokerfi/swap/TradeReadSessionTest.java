package com.example.brokerfi.swap;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class TradeReadSessionTest {
    @Test public void userReadRunsBeforeQueuedBackgroundReadsWithoutInterruptingTheActiveOne() throws Exception {
        List<String> events=Collections.synchronizedList(new ArrayList<>());
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),complete=new CountDownLatch(3);
        TradeReadQueue.execute(() -> {
            events.add("active");started.countDown();
            try { release.await(3,TimeUnit.SECONDS);events.add("active_end"); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { complete.countDown(); }
        });
        try {
            assertTrue(started.await(3,TimeUnit.SECONDS));
            TradeReadQueue.execute(() -> { events.add("background");complete.countDown(); });
            TradeReadQueue.executeUser(() -> { events.add("user");complete.countDown(); });
            assertEquals(Arrays.asList("active"),events);
        } finally { release.countDown(); }
        assertTrue(complete.await(3,TimeUnit.SECONDS));
        assertEquals(Arrays.asList("active","active_end","user","background"),events);
    }
    @Test public void obsoleteSessionStopsBeforeNextQuery() throws Exception {
        AtomicBoolean active=new AtomicBoolean(true);
        List<Integer> calls=new ArrayList<>();
        TradeReadSession session=new TradeReadSession(active::get,calls::add);
        session.starting(); active.set(false);
        try { session.starting(); fail("Hidden page must stop reading"); }
        catch(TradeReadSession.Cancelled expected) { }
        assertEquals(Arrays.asList(1),calls);
    }
    @Test public void finishedReadCannotPublishAfterAccountOrPageChanged() throws Exception {
        AtomicBoolean current=new AtomicBoolean(true);
        TradeReadSession session=new TradeReadSession(current::get,null);
        session.starting(); current.set(false);
        try { session.check(); fail("Obsolete result must be rejected"); }
        catch(TradeReadSession.Cancelled expected) { }
    }
    @Test public void cancelledClientReadDoesNotSignOrContactNetwork() throws Exception {
        TradeReadSession session=new TradeReadSession(() -> false,null);
        BrokerSwapClient client=new BrokerSwapClient(null,session);
        try { client.read("not-a-private-key","not-an-address","invalid-data");fail("Must cancel before signing"); }
        catch(TradeReadSession.Cancelled expected) { }
    }
    @Test public void sharedQueueDoesNotOverlapSwapAndLiquidityBatches() throws Exception {
        List<String> events=Collections.synchronizedList(new ArrayList<>());
        CountDownLatch firstStarted=new CountDownLatch(1), release=new CountDownLatch(1), complete=new CountDownLatch(2);
        TradeReadQueue.execute(() -> {
            try {
                events.add("swap_start");firstStarted.countDown();
                release.await(3,TimeUnit.SECONDS);events.add("swap_end");
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { complete.countDown(); }
        });
        try {
            assertTrue(firstStarted.await(3,TimeUnit.SECONDS));
            TradeReadQueue.execute(() -> { events.add("liquidity");complete.countDown(); });
            assertEquals(Arrays.asList("swap_start"),events);
        } finally { release.countDown(); }
        assertTrue(complete.await(3,TimeUnit.SECONDS));
        assertEquals(Arrays.asList("swap_start","swap_end","liquidity"),events);
    }
}
