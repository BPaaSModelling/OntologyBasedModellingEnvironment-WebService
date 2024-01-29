package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.model.graphEnvironment.Answer;
import ch.fhnw.modeller.model.palette.PaletteCategory;
import ch.fhnw.modeller.model.palette.PaletteElement;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;
import ch.fhnw.modeller.webservice.ontology.NAMESPACE;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import ch.fhnw.modeller.persistence.GlobalVariables;

import lombok.Getter;
import org.apache.http.HttpException;
import org.apache.jena.Jena;
import org.apache.jena.ontology.OntologyException;
import org.apache.jena.query.*;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import static ch.fhnw.modeller.webservice.ontology.OntologyManager.getTRIPLESTOREENDPOINT;

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

        this.userGraphUri = OntologyManager.getTRIPLESTOREENDPOINT()+"/graphs/"+user.getEmail();
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

        if (!checkIfGraphExists(userGraphUri)) {
            duplicateDefaultGraphForUser(userGraphUri);
            Logger.getLogger("UserService").info("Graph for user " + userGraphUri + " created");
        }
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
}
