package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;

import lombok.Getter;
import org.apache.jena.query.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.logging.Logger;

/**
 * UserService class is responsible for managing user-related operations.
 * It provides methods to initialize user graph, check if the graph already exists
 * and duplicate the default graph for each new user.
 * UserService is updated for each JAX-RS request by the CookieResponseFilter.
 * It is used by OntologyManager to access user data and set the user graph.
 */
public class UserService {
    private final OntologyManager ontologyManager;
    //public static UserService INSTANCE;
    @Getter
    private final User user;
    @Getter
    private final String userGraphUri;
    public UserService(User user) {
        // Set User, current OntologyManager and respective UserService upon initialization
        this.user = user;
        this.ontologyManager = OntologyManager.getInstance();
        //this.ontologyManager.setUserService(this);

        this.userGraphUri = OntologyManager.getTRIPLESTOREENDPOINT()+'/'+user.getEmail();
    }

    public static UserService getUserService(HttpServletRequest request){
        HttpSession session = request.getSession();
        if(session.getAttribute("userService") == null){
            throw new IllegalArgumentException("No user service found in session");
        }
        return (UserService) session.getAttribute("userService");
    }

    public void initializeUserGraph(String userGraphUri) throws NoResultsException {
        if(userGraphUri == null || userGraphUri.isEmpty()){
            throw new IllegalArgumentException("UserGraph cannot be null or empty");
        }
        //URL url = new URL(OntologyManager.getTRIPLESTOREENDPOINT());
        if (!datasetIsEmpty()) {
            if (!checkIfGraphExists(userGraphUri)) {

                duplicateDefaultGraphForUser(userGraphUri);
                Logger.getLogger("UserService").info("Graph for user " + userGraphUri + " created");
            }
        } else
            throw new NoResultsException("Dataset is empty. Add triples to the default graph first on Jena Fuseki.");
    }

    private boolean checkIfGraphExists(String userGraphUri) throws NoResultsException {
        String queryString = String.format("ASK WHERE { GRAPH <" + userGraphUri + "> { ?s ?p ?o } }");
        ParameterizedSparqlString query = new ParameterizedSparqlString(queryString);
        try (QueryExecution qexec = ontologyManager.query(query)) {
            return qexec.execAsk();
        }
    }

    private void duplicateDefaultGraphForUser(String graphUri) {
        String updateString = "ADD DEFAULT TO GRAPH <"+ graphUri +"> ";
        ParameterizedSparqlString updateQuery = new ParameterizedSparqlString(updateString);
        ontologyManager.insertQuery(updateQuery);
    }

    public boolean datasetIsEmpty() {
        // Query to check if the dataset is empty
        String queryString = "ASK { ?s ?p ?o }";
        // Set up the query execution
        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(OntologyManager.getTRIPLESTOREENDPOINT() + "/query", queryString)) {
            return !qExec.execAsk(); // execAsk returns true if the dataset contains any triples
        } catch (Exception e) {
            e.printStackTrace();
            // Handle error (e.g., log, throw a custom exception, or return a default value)
            return false; // or true, depending on how you want to handle errors
        }
    }
}
