package cloud.metaapi.sdk.meta_api;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import cloud.metaapi.sdk.clients.meta_api.ExpertAdvisorClient;
import cloud.metaapi.sdk.clients.meta_api.ExpertAdvisorClient.ExpertAdvisorDto;
import cloud.metaapi.sdk.clients.meta_api.ExpertAdvisorClient.NewExpertAdvisorDto;
import cloud.metaapi.sdk.util.Async;

/**
 * Implements an expert advisor entity
 */
public class ExpertAdvisor {
  
  private ExpertAdvisorDto data;
  private String accountId;
  private ExpertAdvisorClient expertAdvisorClient;
  
  /**
   * Constructs an expert advisor entity
   * @param data Expert advisor dto
   * @param accountId Account id
   * @param client Expert advisor client
   */
  public ExpertAdvisor(ExpertAdvisorDto data, String accountId, ExpertAdvisorClient client) {
    this.data = data;
    this.accountId = accountId;
    this.expertAdvisorClient = client;
  }
  
  /**
   * Returns expert id
   * @return expert id
   */
  public String getExpertId() {
    return data.expertId;
  }
  
  /**
   * Returns expert period
   * @return expert period
   */
  public String getPeriod() {
    return data.period;
  }
  
  /**
   * Returns expert symbol
   * @return expert symbol
   */
  public String getSymbol() {
    return data.symbol;
  }
  
  /**
   * Returns true if expert file was uploaded
   * @return whether expert file was uploaded
   */
  public boolean isFileUploaded() {
    return data.fileUploaded;
  }
  
  /**
   * Reloads expert advisor from API
   * (see https://metaapi.cloud/docs/provisioning/api/expertAdvisor/readExpertAdvisor/)
   * @return completable future resolving when expert advisor is updated
   */
  public CompletableFuture<Void> reload() {
    return expertAdvisorClient.getExpertAdvisor(accountId, getExpertId()).thenAccept(dto -> {
      data = dto;
    });
  }
  
  /**
   * Updates expert advisor data
   * (see https://metaapi.cloud/docs/provisioning/api/expertAdvisor/updateExpertAdvisor/)
   * @param expert new expert advisor data
   * @return completable future resolving when expert advisor is updated
   */
  public CompletableFuture<Void> update(NewExpertAdvisorDto expert) {
    return Async.run(() -> {
      expertAdvisorClient.updateExpertAdvisor(accountId, getExpertId(), expert).join();
      reload().join();
    });
  }
  
  /**
   * Uploads an expert advisor file. EAs which use DLLs are not supported
   * (see https://metaapi.cloud/docs/provisioning/api/expertAdvisor/uploadEAFile/)
   * @param filePath path to a file to upload
   * @return completable future which resolves when the file was uploaded
   */
  public CompletableFuture<Void> uploadFile(String filePath) {
    return Async.run(() -> {
      expertAdvisorClient.uploadExpertAdvisorFile(accountId, getExpertId(), filePath).join();
      reload().join();
    });
  }
  
  /**
   * Uploads an expert advisor file. EAs which use DLLs are not supported
   * (see https://metaapi.cloud/docs/provisioning/api/expertAdvisor/uploadEAFile/)
   * @param fileContents input stream containing file contents
   * @return completable future which resolves when the file was uploaded
   */
  public CompletableFuture<Void> uploadFile(InputStream fileContents) {
    return Async.run(() -> {
      expertAdvisorClient.uploadExpertAdvisorFile(accountId, getExpertId(), fileContents).join();
      reload().join();
    });
  }
  
  /**
   * Removes expert advisor
   * (see https://metaapi.cloud/docs/provisioning/api/expertAdvisor/deleteExpertAdvisor/)
   * @return completable future resolving when expert advisor removed
   */
  public CompletableFuture<Void> remove() {
    return expertAdvisorClient.deleteExpertAdvisor(accountId, getExpertId());
  }
}