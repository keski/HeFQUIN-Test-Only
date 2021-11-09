package se.liu.ida.hefquin.engine.federation.access;

import se.liu.ida.hefquin.engine.federation.access.utils.CypherUtils;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.TableRecord;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class Neo4jConnectionFactory {

    static public Neo4jConnection connect(final String URL){
        return new Neo4jConnection(URL);
    }

    public static class Neo4jConnection
    {
        protected final String URL;

        public Neo4jConnection( final String URL ) {
            assert URL != null;
            this.URL = URL;
        }

        public String getURL() {
            return URL;
        }

        public List<TableRecord> execute(final Neo4jRequest req ) throws Neo4JConnectionException {
            return executeQuery( req.getCypherQuery() );
        }

        protected List<TableRecord> executeQuery(final String cypher ) throws Neo4JConnectionException {
            final String data = "{\n" +
                    "  \"statements\" : [ {\n" +
                    "    \"statement\" : \""+cypher+"\",\n" +
                    "    \"parameters\" : {}\n" +
                    "  } ]\n" +
                    "}";

            final var request = HttpRequest.newBuilder(
                    URI.create(this.URL))
                    .header("Accept", "application/json;charset=UTF-8")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();

            final HttpClient client = HttpClient.newHttpClient();
            try {
                final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return CypherUtils.parse(response.body());
            } catch ( final IOException e ) {
                throw new Neo4JConnectionException("Data could not be sent to the server", e, this);
            } catch ( final InterruptedException e ) {
                throw new Neo4JConnectionException("Neo4j server could not be reached.", e, this);
            }
        }
    }

}
