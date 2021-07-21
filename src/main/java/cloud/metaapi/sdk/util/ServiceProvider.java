package cloud.metaapi.sdk.util;

import java.time.Instant;

import cloud.metaapi.sdk.clients.meta_api.MetaApiWebsocketClient;
import cloud.metaapi.sdk.clients.models.IsoTime;
import cloud.metaapi.sdk.meta_api.ConnectionRegistry;
import cloud.metaapi.sdk.meta_api.HistoryFileManager;
import cloud.metaapi.sdk.meta_api.HistoryStorage;
import cloud.metaapi.sdk.meta_api.MetaApiConnection;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;

/**
 * Inner service provider that implements dependency injection (DI) of some SDK classes which are 
 * needed to be tested properly. This class creates objects as they are used in normal environment
 * giving opportunity to mock them for testing before they are actually created.
 */
public class ServiceProvider {
  
  private static HistoryFileManager historyFileManagerMock = null;
  private static MetaApiConnection metaApiConnectionMock = null;
  private static MetaApiConnectionProvider metaApiConnectionMockProvider = null;
  private static Instant nowInstantMock = null;
  private static Double randomMock = null;
  
  /**
   * Removes all mocks
   */
  public static void reset() {
    historyFileManagerMock = null;
    metaApiConnectionMock = null;
    metaApiConnectionMockProvider = null;
    nowInstantMock = null;
  }
  
  /**
   * Interface for creating {@link MetaApiConnection}. Using this interface it is possible to set MetaApiConnection
   * mock with function instead of passing already created object.
   */
  public interface MetaApiConnectionProvider {
    /**
     * Creates MetaApiConnection mock
     * @param websocketClient MetaApi websocket client
     * @param account MetaTrader account to connect to
     * @param historyStorage optional local terminal history storage. Use for accounts in user synchronization mode.
     * By default an instance of MemoryHistoryStorage will be used.
     * @param connectionRegistry metatrader account connection registry
     * @param historyStartTime history start sync time, or {@code null}
     * @return MetaApiConnection mock
     */
    MetaApiConnection create(
      MetaApiWebsocketClient websocketClient,
      MetatraderAccount account,
      HistoryStorage historyStorage,
      ConnectionRegistry connectionRegistry,
      IsoTime historyStartTime);
  }
  
  /**
   * Constructs history file manager with its corresponding constructor or returns a mock if it is set.
   * @param accountId account id
   * @param application MetaApi application id
   * @param storage storage
   * @return normal or mocked history file manager
   * @see #setHistoryFileManagerMock(HistoryFileManager)
   */
  public static HistoryFileManager createHistoryFileManager(String accountId, String application, HistoryStorage storage) {
    if (historyFileManagerMock != null) return historyFileManagerMock;
    return new HistoryFileManager(accountId, application, storage);
  }
  
  /**
   * Sets history file manager mock that will be created by this provider. 
   * If {@code null} is set, the mock is removed and the object will be created as normal.
   * @param mock mocked history file manager or {@code null}
   */
  public static void setHistoryFileManagerMock(HistoryFileManager mock) {
    historyFileManagerMock = mock;
  }
  
  /**
   * Constructs metaapi connection with its corresponding constructor or returns a mock if it is set.
   * @param websocketClient MetaApi websocket client
   * @param account MetaTrader account to connect to
   * @param historyStorage optional local terminal history storage. Use for accounts in user synchronization mode.
   * By default an instance of MemoryHistoryStorage will be used.
   * @param connectionRegistry metatrader account connection registry
   * @param historyStartTime history start sync time, or {@code null}
   * @return Metaapi connection
   * @see #setHistoryFileManagerMock(HistoryFileManager)
   */
  public static MetaApiConnection createMetaApiConnection(
    MetaApiWebsocketClient websocketClient,
    MetatraderAccount account,
    HistoryStorage historyStorage,
    ConnectionRegistry connectionRegistry,
    IsoTime historyStartTime
  ) {
    if (metaApiConnectionMock != null) return metaApiConnectionMock;
    else if (metaApiConnectionMockProvider != null) return metaApiConnectionMockProvider
      .create(websocketClient, account, historyStorage, connectionRegistry, historyStartTime);
    return new MetaApiConnection(websocketClient, account, historyStorage, connectionRegistry, historyStartTime);
  }
  
  /**
   * Sets metaapi connection mock that will be created by this provider. 
   * If {@code null} is set, the mock is removed and the object will be created as normal.
   * @param mock mocked metaapi connection or {@code null}
   */
  public static void setMetApiConnectionMock(MetaApiConnection mock) {
    metaApiConnectionMock = mock;
    metaApiConnectionMockProvider = null;
  }
  
  /**
   * Sets metaapi connection provider that will be used for creating the mock. 
   * If {@code null} is set, the mock provider is removed and the object will be created as normal.
   * @param mockProvider mocked metaapi connection or {@code null}
   */
  public static void setMetApiConnectionMock(MetaApiConnectionProvider mockProvider) {
    metaApiConnectionMockProvider = mockProvider;
    metaApiConnectionMock = null;
  }
  
  /**
   * Returns instant with current time or a mock if it is set
   * @return instant with current time or a mock if it is set
   * @see #setNowInstantMock(Instant)
   */
  public static Instant getNow() {
    if (nowInstantMock != null) return nowInstantMock;
    return Instant.now();
  }
  
  /**
   * Sets instant with current time that will be used for provided by this class instead of real instant.
   * If {@code null} is set, the mock provider is removed and the object will be created as normal.
   * @param instant mocked instant or {@code null}
   */
  public static void setNowInstantMock(Instant instant) {
    nowInstantMock = instant;
  }
  
  /**
   * Returns value of Math.random() or stubbed value
   * @return random value
   */
  public static double getRandom() {
    return randomMock != null ? randomMock : Math.random();
  }
  
  /**
   * Sets or removes mock for random value
   * @param value random value to return
   */
  public static void setRandom(Double value) {
    randomMock = value;
  }
}