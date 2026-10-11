package api;

import java.util.UUID;

final class Payloads {

    private Payloads() {
    }

    static String todo(String title) {
        return "{\"title\":\"" + title + "\"}";
    }

    static String todo(String title, boolean doneStatus, String description) {
        return "{\"title\":\"" + title + "\","
                + "\"doneStatus\":" + doneStatus + ","
                + "\"description\":\"" + description + "\"}";
    }

    static String todoXml(String title, boolean doneStatus) {
        return "<todo><title>" + title + "</title><doneStatus>" + doneStatus + "</doneStatus></todo>";
    }

    static String idOnly(String id) {
        return "{\"id\":\"" + id + "\"}";
    }

    static String uniqueTitle(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
