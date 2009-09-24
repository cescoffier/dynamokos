package remote.protocols.eacometdbridge;


public interface EABridge {

    public void importTopic(String cometdTopic, String osgiTopic);

    public void exportTopic(String osgiTopic, String cometdTopic);

}
