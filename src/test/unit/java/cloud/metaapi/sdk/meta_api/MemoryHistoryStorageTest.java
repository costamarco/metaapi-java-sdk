package cloud.metaapi.sdk.meta_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import cloud.metaapi.sdk.clients.meta_api.models.MetatraderDeal;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderOrder;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderDeal.DealType;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderOrder.OrderType;
import cloud.metaapi.sdk.clients.models.*;
import cloud.metaapi.sdk.meta_api.HistoryFileManager.History;
import cloud.metaapi.sdk.util.ServiceProvider;

/**
 * Tests {@link MemoryHistoryStorage}
 */
class MemoryHistoryStorageTest {

  private MemoryHistoryStorage storage;
  private HistoryFileManager storageFileManagerMock;
  private MetatraderDeal testDeal;
  private MetatraderOrder testOrder;
  
  @BeforeEach
  void setUp() throws Exception {
    storageFileManagerMock = Mockito.mock(HistoryFileManager.class);
    Mockito.when(storageFileManagerMock.getHistoryFromDisk()).thenReturn(CompletableFuture.completedFuture(
      new History() {{ deals = Lists.list(); historyOrders = Lists.list(); }}));
    Mockito.when(storageFileManagerMock.deleteStorageFromDisk()).thenReturn(CompletableFuture.completedFuture(null));
    ServiceProvider.setHistoryFileManagerMock(storageFileManagerMock);
    storage = new MemoryHistoryStorage("accountId");
    testDeal = new MetatraderDeal() {{ id = "37863643"; type = DealType.DEAL_TYPE_BALANCE; magic = 0;
      time = new IsoTime(new Date(100)); commission = 0.0; swap = 0.0; profit = 10000;
      platform = "mt5"; comment = "Demo deposit 1"; }};
    testOrder = new MetatraderOrder() {{ id = "61210463"; type = OrderType.ORDER_TYPE_SELL;
      state = OrderState.ORDER_STATE_FILLED; symbol = "AUDNZD"; magic = 0; time = new IsoTime(new Date(50));
      doneTime = new IsoTime(new Date(100)); currentPrice = 1; volume = 0.01; currentVolume = 0;
      positionId = "61206630"; platform = "mt5"; comment = "AS_AUDNZD_5YyM6KS7Fv:"; }};
    storage.onConnected("1:ps-mpa-1", 0);
  }

  @AfterEach
  void tearDown() {
    ServiceProvider.reset();
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#loadDataFromDisk()}
   */
  @Test
  void testLoadsDataFromTheFileManager() throws Exception {
    Mockito.when(storageFileManagerMock.getHistoryFromDisk()).thenReturn(CompletableFuture.completedFuture(
      new History() {{ deals = Lists.list(testDeal); historyOrders = Lists.list(testOrder); }}));
    storage.loadDataFromDisk().get();
    assertThat(storage.getDeals()).usingRecursiveComparison().isEqualTo(Lists.list(testDeal));
    assertThat(storage.getHistoryOrders()).usingRecursiveComparison().isEqualTo(Lists.list(testOrder));
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#updateDiskStorage()}
   */
  @Test
  void testUpdatesDiskStorage() throws InterruptedException, ExecutionException {
    Mockito.when(storageFileManagerMock.updateDiskStorage()).thenReturn(CompletableFuture.completedFuture(null));
    storage.updateDiskStorage().get();
    Mockito.verify(storageFileManagerMock).updateDiskStorage();
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#getLastHistoryOrderTime()}
   */
  @Test
  void testReturnsLastHistoryOrderTime() throws Exception {
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder(null));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("2020-01-01T00:00:00.000Z"));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("2020-01-02T00:00:00.000Z"));
    assertThat(storage.getLastHistoryOrderTime().get()).usingRecursiveComparison()
      .isEqualTo(new IsoTime("2020-01-02T00:00:00.000Z"));
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#getLastDealTime()}
   */
  @Test
  void testReturnsLastHistoryDealTime() throws Exception {
    storage.onDealAdded("1:ps-mpa-1", new MetatraderDeal() {{ time = new IsoTime(Date.from(Instant.ofEpochSecond(0))); }});
    storage.onDealAdded("1:ps-mpa-1", new MetatraderDeal() {{ time = new IsoTime("2020-01-01T00:00:00.000Z"); }});
    storage.onDealAdded("1:ps-mpa-1", new MetatraderDeal() {{ time = new IsoTime("2020-01-02T00:00:00.000Z"); }});
    assertThat(storage.getLastDealTime().get()).usingRecursiveComparison()
      .isEqualTo(new IsoTime("2020-01-02T00:00:00.000Z"));
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#getDeals()}
   */
  @Test
  void testReturnsSavedDeals() {
    storage.onDealAdded("1:ps-mpa-1", createDeal("1", "2020-01-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL));
    storage.onDealAdded("1:ps-mpa-1", createDeal("7", "2020-05-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY));
    storage.onDealAdded("1:ps-mpa-1", createDeal("8", "2020-02-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL));
    storage.onDealAdded("1:ps-mpa-1", createDeal("6", "2020-10-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY));
    storage.onDealAdded("1:ps-mpa-1", createDeal("4", "2020-02-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL));
    storage.onDealAdded("1:ps-mpa-1", createDeal("5", "2020-06-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY));
    storage.onDealAdded("1:ps-mpa-1", createDeal("11", null, DealType.DEAL_TYPE_SELL));
    storage.onDealAdded("1:ps-mpa-1", createDeal("3", "2020-09-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY));
    storage.onDealAdded("1:ps-mpa-1", createDeal("5", "2020-06-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY));
    storage.onDealAdded("1:ps-mpa-1", createDeal("2", "2020-08-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL));
    storage.onDealAdded("1:ps-mpa-1", createDeal("10", null,  DealType.DEAL_TYPE_SELL));
    storage.onDealAdded("1:ps-mpa-1", createDeal("12", null,  DealType.DEAL_TYPE_BUY));
    assertThat(storage.getDeals()).usingRecursiveComparison().isEqualTo(Lists.list(
      createDeal("10", null,  DealType.DEAL_TYPE_SELL),
      createDeal("11", null, DealType.DEAL_TYPE_SELL),
      createDeal("12", null,  DealType.DEAL_TYPE_BUY),
      createDeal("1", "2020-01-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL),
      createDeal("4", "2020-02-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL),
      createDeal("8", "2020-02-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL),
      createDeal("7", "2020-05-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY),
      createDeal("5", "2020-06-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY),
      createDeal("2", "2020-08-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL),
      createDeal("3", "2020-09-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY),
      createDeal("6", "2020-10-01T00:00:00.000Z", DealType.DEAL_TYPE_BUY)
    ));
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#getHistoryOrders()}
   */
  @Test
  void testReturnsSavedHistoryOrders() {
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("1", "2020-01-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("7", "2020-05-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("8", "2020-02-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("6", "2020-10-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("4", "2020-02-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("5", "2020-06-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("11", null, OrderType.ORDER_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("3", "2020-09-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("5", "2020-06-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("2", "2020-08-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("10", null,  OrderType.ORDER_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("12", null,  OrderType.ORDER_TYPE_BUY));
    assertThat(storage.getHistoryOrders()).usingRecursiveComparison().isEqualTo(Lists.list(
      createOrder("10", null,  OrderType.ORDER_TYPE_SELL),
      createOrder("11", null, OrderType.ORDER_TYPE_SELL),
      createOrder("12", null,  OrderType.ORDER_TYPE_BUY),
      createOrder("1", "2020-01-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL),
      createOrder("4", "2020-02-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL),
      createOrder("8", "2020-02-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL),
      createOrder("7", "2020-05-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY),
      createOrder("5", "2020-06-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY),
      createOrder("2", "2020-08-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL),
      createOrder("3", "2020-09-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY),
      createOrder("6", "2020-10-01T00:00:00.000Z", OrderType.ORDER_TYPE_BUY)
    ));
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#isOrderSynchronizationFinished()}
   */
  @Test
  void testReturnsSavedOrderSynchronizationStatus() {
    assertFalse(storage.isOrderSynchronizationFinished());
    storage.onOrderSynchronizationFinished("1:ps-mpa-1", "synchronizationId");
    assertTrue(storage.isOrderSynchronizationFinished());
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#isDealSynchronizationFinished()}
   */
  @Test
  void testReturnsSavedDealSynchronizationStatus() throws IllegalAccessException {
    Mockito.when(storageFileManagerMock.updateDiskStorage()).thenReturn(CompletableFuture.completedFuture(null));
    assertFalse(storage.isDealSynchronizationFinished());
    storage.onDealSynchronizationFinished("1:ps-mpa-1", "synchronizationId");
    Mockito.verify(storageFileManagerMock, Mockito.times(1)).updateDiskStorage();
    assertTrue(storage.isDealSynchronizationFinished());
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#reset()}
   */
  @Test
  void testResetsStorage() {
    storage.onDealAdded("1:ps-mpa-1", createDeal("1", "2020-01-01T00:00:00.000Z", DealType.DEAL_TYPE_SELL));
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("1", "2020-01-01T00:00:00.000Z", OrderType.ORDER_TYPE_SELL));
    storage.clear().join();
    assertTrue(storage.getDeals().isEmpty());
    assertTrue(storage.getHistoryOrders().isEmpty());
    Mockito.verify(storageFileManagerMock).deleteStorageFromDisk();
  }
  
  /**
   * Tests {@link MemoryHistoryStorage#reset}
   */
  @Test
  void testRecordsInstanceDataFromMultipleStreams() {
    storage.onHistoryOrderAdded("1:ps-mpa-1", createOrder("2", "2020-01-01T00:00:00.000Z"));
    assertEquals(storage.getLastHistoryOrderTime(1).join(), new IsoTime("2020-01-01T00:00:00.000Z"));
    storage.onHistoryOrderAdded("1:ps-mpa-2", createOrder("3", "2020-01-02T00:00:00.000Z"));
    assertEquals(storage.getLastHistoryOrderTime(1).join(), new IsoTime("2020-01-02T00:00:00.000Z"));
    storage.onDealAdded("1:ps-mpa-1", createDeal("2", "2020-01-01T00:00:00.000Z"));
    assertEquals(storage.getLastDealTime(1).join(), new IsoTime("2020-01-01T00:00:00.000Z"));
    storage.onDealAdded("1:ps-mpa-2", createDeal("3", "2020-01-02T00:00:00.000Z"));
    assertEquals(storage.getLastDealTime(1).join(), new IsoTime("2020-01-02T00:00:00.000Z"));
  };
  
  private MetatraderOrder createOrder(String doneIsoTime) {
    return new MetatraderOrder() {{
      this.doneTime = doneIsoTime != null ? new IsoTime(doneIsoTime) : null;
    }};
  }
  
  private MetatraderOrder createOrder(String id, String doneIsoTime) {
    MetatraderOrder result = new MetatraderOrder();
    result.id = id;
    result.doneTime = doneIsoTime != null ? new IsoTime(doneIsoTime) : null; 
    return result;
  }
  
  private MetatraderOrder createOrder(String id, String doneIsoTime, OrderType type) {
    MetatraderOrder result = new MetatraderOrder();
    result.id = id;
    result.doneTime = doneIsoTime != null ? new IsoTime(doneIsoTime) : null; 
    result.type = type;
    return result;
  }
  
  private MetatraderDeal createDeal(String id, String isoTime) {
    MetatraderDeal result = new MetatraderDeal();
    result.id = id;
    result.time = (isoTime != null ? new IsoTime(isoTime) : new IsoTime(Date.from(Instant.ofEpochSecond(0)))); 
    return result;
  }

  private MetatraderDeal createDeal(String id, String isoTime, DealType type) {
    MetatraderDeal result = new MetatraderDeal();
    result.id = id;
    result.time = (isoTime != null ? new IsoTime(isoTime) : new IsoTime(Date.from(Instant.ofEpochSecond(0)))); 
    result.type = type;
    return result;
  }
}