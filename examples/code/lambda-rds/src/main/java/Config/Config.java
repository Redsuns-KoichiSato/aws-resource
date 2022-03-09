package Config;


public class Config {
    public Credentials credentials;
    public DatabaseConf databaseConf;

    public Config(Credentials credentials, DatabaseConf databaseConf){
        this.credentials = credentials;
        this.databaseConf = databaseConf;
    }

    public Config() {
        this.databaseConf = new DatabaseConf();
        this.credentials = new Credentials();
    }

    @Override
    public String toString() {
        return "{Credentials: " + this.credentials.toString() + " databaseConf: " + this.databaseConf.toString() + "}";
    }
}
