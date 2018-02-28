package ca.mcgill.ecse428.a2;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.*;

/**
 * Contains information and helper methods for connection to the API.
 * @author Kevin Laframboise - 260687529
 *
 */
public class APIConnection {

    private Client client;
    private static final String USER = "edf0a61ede8cb0b4";
    private static final String PASS = "0e92ad8d90776584a84f6e";
    private static final String LINK = "https://ct.soa-gw.canadapost.ca/rs/ship/price";
    
    public APIConnection() {
        ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
        client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(USER, PASS));
    }

    public ClientResponse createMailingScenario(Object xml) throws UniformInterfaceException {
        WebResource aWebResource = client.resource(LINK);
        return aWebResource.accept("application/vnd.cpc.ship.rate-v3+xml").header("Content-Type", "application/vnd.cpc.ship.rate-v3+xml").acceptLanguage("en-CA").post(ClientResponse.class, xml);
    }

    public void close() {
        client.destroy();
    }
    
}
