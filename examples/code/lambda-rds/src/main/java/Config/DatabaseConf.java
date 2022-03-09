package Config;

public class DatabaseConf {
    public String name;
    public String hostname;
    public int port;
    public String driver;

    public void DatabaseConf() {}

    public void DatabaseConf(String driver, String hostname, int port, String name){
        this.driver = driver;
        this.hostname = hostname;
        this.port = port;
        this.name = name;
    }

    public String getDriver(){
        return this.driver;
    }

    public String getHostname(){
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }

    public String getName() {
        return this.name;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setName(String name){
        this.name = name;
    }

    public String DbUrl() {
        return "jdbc:" + this.driver + "://" +
                this.hostname + ":" +
                this.port + "/" +
                this.name;
    }

    @Override
    public String toString(){
        return "{driver: \"" + this.driver + "\" hostname: \"" + this.hostname + "\" port: \"" + this.port + "\"name: \"" + this.name + "\"}";
    }
}
