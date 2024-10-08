package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import lombok.Getter;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;

import java.util.logging.Logger;

import com.auth0.json.mgmt.users.User;

/**
 * THIS CLASS IS NOT USED IN THE CURRENT IMPLEMENTATION
 * UserService is responsible for managing user-related operations.
 * It provides methods to initialize user graph, check if the graph already exists
 * and duplicate the default graph for each new user.
 * UserService is updated for each JAX-RS request by the JWTAuthFilter.java filter
 * It is used by OntologyManager to access user data and set the user graph.
 */
public class UserService {
    private final OntologyManager ontologyManager;
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
//    private static final ExecutorService executorService = Executors.newFixedThreadPool(10); // Adjust the pool size as needed

    @Getter
    private final User user;
    @Getter
    private final String userGraphUri;

    public UserService(User user) {
        // Set User, current OntologyManager and respective UserService upon initialization
        this.user = user;
        this.ontologyManager = OntologyManager.getInstance();
        //this.ontologyManager.setUserService(this);
        this.userGraphUri = OntologyManager.getTRIPLESTOREENDPOINT() + '/' + user.getEmail();
    }

    /**
     * Initializes the user graph.
     * Checks if the dataset is empty, if the graph exists and duplicates the default graph for the user.
     * @param userGraphUri The URI of the user graph.
     * @return A string message indicating the result of the operation.
     * @throws Exception If an error occurs during the operation.
     */
    public String initializeUserGraph(String userGraphUri) throws Exception {
        if (userGraphUri == null || userGraphUri.isEmpty()) {
            throw new IllegalArgumentException("UserGraph cannot be null or empty");
        }

        if (!datasetIsEmpty()) {
            if (!checkIfGraphExists(userGraphUri)) {
                duplicateDefaultGraphForUser(userGraphUri);
                LOGGER.info("Graph created for user: " + userGraphUri);
                return "Graph created for user: " + userGraphUri;
            } else {
                LOGGER.info("Successfully connected to graph: " + userGraphUri);
                return "Successfully connected to graph: " + userGraphUri;
            }
        } else
            throw new NoResultsException("Dataset is empty. Add triples to the default graph first on Jena Fuseki.");
    }

    private boolean checkIfGraphExists(String userGraphUri) throws Exception {
        String queryString = String.format("ASK WHERE { GRAPH <" + userGraphUri + "> { ?s ?p ?o } }");
        ParameterizedSparqlString query = new ParameterizedSparqlString(queryString);
        try (QueryExecution qexec = ontologyManager.query(query)) {
            return qexec.execAsk();
        } catch (Exception e) {
            LOGGER.severe("Error checking if graph exists: " + e.getMessage());
            throw new Exception("Error checking if graph exists", e);
        }
    }

    private void duplicateDefaultGraphForUser(String graphUri) {
        try {
            String updateString = "ADD DEFAULT TO GRAPH <" + graphUri + "> ";
            ParameterizedSparqlString updateQuery = new ParameterizedSparqlString(updateString);
            ontologyManager.insertQuery(updateQuery);
        } catch (Exception e) {
            LOGGER.severe("Error duplicating default graph for user: " + e.getMessage());
        }
    }

    public boolean datasetIsEmpty() {
        // Query to check if the dataset is empty
        String queryString = "ASK { ?s ?p ?o }";
        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(OntologyManager.getTRIPLESTOREENDPOINT() + "/query", queryString)) {
            return !qExec.execAsk(); // execAsk returns true if the dataset contains any triples
        } catch (Exception e) {
            LOGGER.severe("Error checking if dataset is empty: " + e.getMessage());
            // Handle error (e.g., log, throw a custom exception, or return a default value)
            return false; // or true, depending on how you want to handle errors
        }
    }
}
