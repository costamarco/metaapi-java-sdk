package cloud.metaapi.sdk.clients.meta_api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cloud.metaapi.sdk.util.JsonMapper;
import cloud.metaapi.sdk.util.ServiceProvider;

/**
 * A class which records packets into log files
 */
public class PacketLogger {

    private static SimpleDateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static Logger logger = Logger.getLogger(PacketLogger.class);
    private int fileNumberLimit;
    private int logFileSizeInHours;
    private boolean compressSpecifications;
    private boolean compressPrices;
    private Map<String, PreviousPrice> previousPrices;
    private Map<String, WriteQueueItem> writeQueue;
    private Timer recordInterval;
    private Timer deleteOldLogsInterval;
    private String root;
    
    /**
     * Packet logger options
     */
    public static class LoggerOptions {
        Integer fileNumberLimit;
        Integer logFileSizeInHours;
        Boolean compressSpecifications;
        Boolean compressPrices;
    }
    
    /**
     * Log message
     */
    public static class LogMessage {
        public Date date;
        public String message;
    }
    
    private static class PreviousPrice {
        JsonNode first;
        JsonNode last;
    }
    
    private static class WriteQueueItem {
        public boolean isWriting;
        public List<String> queue;
    }
    
    /**
     * Constructs the class with default options
     * @throws IOException if log directory cannot be created
     */
    public PacketLogger() throws IOException {
        this(new LoggerOptions());
    }
    
    /**
     * Constructs the class
     * @param opts packet logger options
     * @throws IOException if log directory cannot be created
     */
    public PacketLogger(LoggerOptions opts) throws IOException {
        this.fileNumberLimit = opts.fileNumberLimit != null ? opts.fileNumberLimit : 12;
        this.logFileSizeInHours = opts.logFileSizeInHours != null ? opts.logFileSizeInHours : 4;
        this.compressSpecifications = opts.compressSpecifications != null ? opts.compressSpecifications : true;
        this.compressPrices = opts.compressPrices != null ? opts.compressPrices : true;
        this.previousPrices = new HashMap<>();
        this.writeQueue = new HashMap<>();
        this.root = "./.metaapi/logs";
        Files.createDirectories(FileSystems.getDefault().getPath(this.root));
    }
    
    /**
     * Processes packets and pushes them into save queue
     * @param packet packet to log
     */
    public void logPacket(JsonNode packet) {
        String packetAccountId = packet.get("accountId").asText();
        String packetType = packet.get("type").asText();
        Integer packetSequenceNumber = packet.has("sequenceNumber") ? packet.get("sequenceNumber").asInt() : null;
        if (!writeQueue.containsKey(packetAccountId)) {
            writeQueue.put(packetAccountId, new WriteQueueItem() {{
                isWriting = false;
                queue = new ArrayList<>();
            }});
        }
        if (packetType.equals("status")) {
            return;
        }
        List<String> queue = writeQueue.get(packetAccountId).queue;
        PreviousPrice prevPrice = previousPrices.get(packetAccountId);
        if (!packetType.equals("prices")) {
            if (prevPrice != null) {
                recordPrices(packetAccountId);
            }
            if (packetType.equals("specifications") && compressSpecifications) {
                ObjectNode queueItem = JsonMapper.getInstance().createObjectNode();
                queueItem.put("type", packetType);
                queueItem.put("sequenceNumber", packetSequenceNumber);
                queue.add(queueItem.toString());
            } else {
                queue.add(packet.toString());
            }
        } else {
            if (!compressPrices) {
                queue.add(packet.toString());
            } else {
                if (prevPrice != null) {
                    Integer prevPriceLastSequenceNumber = prevPrice.last.has("sequenceNumber")
                        ? prevPrice.last.get("sequenceNumber").asInt() : null;
                    if (packetSequenceNumber != prevPriceLastSequenceNumber
                        && (prevPriceLastSequenceNumber == null 
                        || packetSequenceNumber != prevPriceLastSequenceNumber + 1)) {
                        recordPrices(packetAccountId);
                        previousPrices.put(packetAccountId, new PreviousPrice() {{ first = packet; last = packet; }});
                        queue.add(packet.toString());
                    } else {
                        prevPrice.last = packet;
                    }
                } else {
                    previousPrices.put(packetAccountId, new PreviousPrice() {{ first = packet; last = packet; }});
                    queue.add(packet.toString());
                }
            }
        }
    }
    
    /**
     * Returns log messages
     * @param accountId account id 
     * @return log messages
     */
    public List<LogMessage> readLogs(String accountId) {
        return readLogs(accountId, null, null);
    }
    
    /**
     * Returns log messages within date bounds as an array of objects
     * @param accountId account id 
     * @param dateAfter date to get logs after, or {@code null}
     * @param dateBefore date to get logs before, or {@code null}
     * @return log messages
     */
    public List<LogMessage> readLogs(String accountId, Date dateAfter, Date dateBefore) {
        File rootFolder = new File(root);
        File[] folders = rootFolder.listFiles();
        List<LogMessage> packets = new ArrayList<>();
        for (File folder : folders) {
            Path filePath = FileSystems.getDefault().getPath(root, folder.getName(), accountId + ".log");
            if (Files.exists(filePath)) {
                try {
                    List<String> contents = Files.readAllLines(filePath);
                    List<LogMessage> messages = new ArrayList<>();
                    contents.removeIf(line -> line.length() == 0);
                    for (String line : contents) {
                        messages.add(new LogMessage() {{
                            date = longDateFormat.parse(line.substring(1, 24));
                            message = line.substring(26);
                        }});
                    }
                    if (dateAfter != null) {
                        messages.removeIf(message -> message.date.compareTo(dateAfter) != 1);
                    }
                    if (dateBefore != null) {
                        messages.removeIf(message -> message.date.compareTo(dateBefore) != -1);
                    }
                    packets.addAll(messages);
                } catch (Throwable e) {
                    throw new CompletionException(e);
                }
            }
        }
        return packets;
    }
    
    /**
     * Returns path for account log file
     * @param accountId account id
     * @return file path
     * @throws IOException if failed to create file directory
     */
    public String getFilePath(String accountId) throws IOException {
        Date now = Date.from(ServiceProvider.getNow());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        int fileIndex = calendar.get(Calendar.HOUR_OF_DAY) / logFileSizeInHours;
        String folderName = shortDateFormat.format(now) + "-" + (fileIndex > 9 ? fileIndex : "0" + fileIndex);
        Files.createDirectories(FileSystems.getDefault().getPath(root, folderName));
        return root + "/" + folderName + "/" + accountId + ".log";
    }
    
    /**
     * Initializes the packet logger
     */
    public void start() {
        previousPrices.clear();
        if (recordInterval == null) {
            PacketLogger self = this;
            recordInterval = new Timer();
            recordInterval.schedule(new TimerTask() {
                @Override
                public void run() {
                    self.appendLogs();
                }
            }, 1000, 1000);
            deleteOldLogsInterval = new Timer();
            deleteOldLogsInterval.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        self.deleteOldData();
                    } catch (IOException e) {
                        logger.error("Failed to delete old data", e);
                    }
                }
            }, 10000, 10000);
        }
    }
    
    /**
     * Deinitializes the packet logger
     */
    public void stop() {
        if (recordInterval != null) {
            recordInterval.cancel();
            recordInterval = null;
            deleteOldLogsInterval.cancel();
            deleteOldLogsInterval = null;
        }
    }
    
    /**
     * Records price packet messages to log files
     * @param accountId account id
     */
    private void recordPrices(String accountId) {
        PreviousPrice prevPrice = previousPrices.get(accountId);
        List<String> queue = writeQueue.get(accountId).queue;
        previousPrices.remove(accountId);
        int firstSequenceNumber = prevPrice.first.get("sequenceNumber").asInt();
        int lastSequenceNumber = prevPrice.last.get("sequenceNumber").asInt();
        if (firstSequenceNumber != lastSequenceNumber) {
            queue.add(prevPrice.last.toString());
            queue.add("Recorded price packets " + firstSequenceNumber + "-" + lastSequenceNumber);
        }
    }
    
    /**
     * Writes logs to files
     */
    private void appendLogs() {
        writeQueue.keySet().forEach((key) -> {
            WriteQueueItem queue = writeQueue.get(key);
            if (!queue.isWriting && queue.queue.size() != 0) {
                queue.isWriting = true;
                try {
                    String filePath = getFilePath(key);
                    String writeString = "";
                    for (String line : queue.queue) {
                        writeString += "[" + longDateFormat.format(Date.from(ServiceProvider.getNow())) + "] "
                            + line + "\r\n";
                    }
                    queue.queue.clear();
                    FileUtils.writeByteArrayToFile(new File(filePath),
                        writeString.getBytes(StandardCharsets.UTF_8), true);
                } catch (Throwable e) {
                    logger.info("Error writing log", e);
                }
                queue.isWriting = false;
            }
        });
    }
    
    /**
     * Deletes folders when the folder limit is exceeded
     * @throws IOException if failed to delete an old data directory
     */
    private void deleteOldData() throws IOException {
        File rootFolder = new File(root);
        List<String> contents = Arrays.asList(rootFolder.list());
        Collections.sort(contents, Collator.getInstance());
        if (contents.size() > fileNumberLimit) {
            for (String folder : contents.subList(0, contents.size() - fileNumberLimit)) {
                FileUtils.deleteDirectory(new File(rootFolder + "/" + folder));
            }
        }
    }
}