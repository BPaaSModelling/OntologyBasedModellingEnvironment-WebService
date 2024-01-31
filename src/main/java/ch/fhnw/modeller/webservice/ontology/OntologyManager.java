package ch.fhnw.modeller.webservice.ontology;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import ch.fhnw.modeller.auth.UserService;
import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.filter.CookieRequestFilter;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfs.RDFSFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOTFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import ch.fhnw.modeller.webservice.config.ConfigReader;
import org.apache.jena.util.FileManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;


public final class OntologyManager {

	private static OntologyManager INSTANCE;
	private boolean localOntology = true;

//	@Getter
//	@Setter
	private UserService userService;

	//private Model rdfModel;
	
	/**
	 * The URL of the triplestore server.<br>
	 * This variable gets set based on the environment variable TRIPLESTORE_ENDPOINT.<br>
	 * If this environment variable does not exist it is assumed that the software is running
	 * locally and <i>http://localhost:3030/ModEnv</i> is used.
	 */
	private static String TRIPLESTOREENDPOINT 	= ConfigReader.getInstance().getEntry("TRIPLESTORE_ENDPOINT", "http://localhost:3030/ModEnv"); 

	private static String UPDATEENDPOINT 		= TRIPLESTOREENDPOINT + "/update";
	private static String QUERYENDPOINT			= TRIPLESTOREENDPOINT + "/query";
	private static String READENDPOINT			= TRIPLESTOREENDPOINT + "/get";
	private static String DATAENDPOINT			= TRIPLESTOREENDPOINT + "/data";

	public static synchronized OntologyManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new OntologyManager();
		}
		return INSTANCE;
	}

	public OntologyManager() {
//		rdfModel = ModelFactory.createDefaultModel();
//		setNamespaces(rdfModel);
//		loadOntologyiesToModel();
		/*DatasetGraph ds = DatasetGraphFactory.createTxnMem() ;
		DatasetGraph modellingEnv = DatasetGraphFactory.createTxnMem();
		FusekiServer server = FusekiServer.create()
			    .add("/ds", ds, true)
			    .add("modellingEnv", modellingEnv, true)
			    .build() ;
			server.start() ;*/
	}

	private void applyReasoningRulesToMainModel(String ruleFile) {
		List<String> ruleSet = null;
		try {
			ruleSet = RuleParser.parseRules(this.getClass().getClassLoader().getResourceAsStream(ruleFile));
			for (String rule : ruleSet) {
				performConstructRule(new ParameterizedSparqlString(rule));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public Model applyReasoningRulesToTempModel(Model tempModel, ParameterizedSparqlString constructQuery) {
		// System.out.println("### applyReasoningRulesToTempModel: "
		// +constructQuery.toString());
		return performConstructRule(tempModel, constructQuery);
	}

	public void setNamespaces(Model model) {
		for (NAMESPACE ns : NAMESPACE.values()) {
			model.setNsPrefix(ns.getPrefix(), ns.getURI());
		}
	}

//	private void loadOntologyiesToModel() {
//			rdfModel.read(READENDPOINT);
//	}

	private void addNamespacesToQuery(ParameterizedSparqlString queryStr) {
		for (NAMESPACE ns : NAMESPACE.values()) {
			queryStr.setNsPrefix(ns.getPrefix(), ns.getURI());
		}
	}

	public Model performConstructRule(Model model, ParameterizedSparqlString query) {
	
		// System.out.println("### performConstructRule: " +query.toString());
		Model temp = ModelFactory.createOntologyModel();
		addNamespacesToQuery(query);
		System.out.println("### local performConstructRule: " + query.toString());
		QueryExecution qexec = QueryExecutionFactory.create(query.toString(), model);
		temp = qexec.execConstruct();
		model = model.union(temp);
		return model;
	}
	
	public void performConstructRule(ParameterizedSparqlString query) {
		
		// System.out.println("### performConstructRule: " +query.toString());
		addNamespacesToQuery(query);
		System.out.println("### online performConstructRule: " + query.toString());
		QueryExecution qexec = QueryExecutionFactory.sparqlService(QUERYENDPOINT, query.toString());
		qexec.execConstruct();
		qexec.close();
	}

	public void printModel(Model model, String fileName) {
		// RDFDataMgr.write(System.out, model, Lang.TURTLE);

		try {
			RDFDataMgr.write(new FileOutputStream(fileName), model, Lang.TURTLE);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("****************************************************");
	}

//	public void printCurrentModel(String filename) {
//		this.printModel(this.rdfModel, filename);
//	}

	public QueryExecution query(ParameterizedSparqlString queryStr) {
		String userGraphUri = getCurrentUserGraph();
		addNamespacesToQuery(queryStr);

		//System.out.println("***Performed query***\n" + queryStr.toString() + "***Performed query***\n");
		Query query = QueryFactory.create(queryStr.toString());
		if (userGraphUri != null && !query.toString().contains("GRAPH")) {
			query.addGraphURI(userGraphUri);
			System.out.println("Graph QUERY \n" + query.toString());
		}
		QueryExecution qexec;

		qexec = QueryExecutionFactory.sparqlService(QUERYENDPOINT, query);

		return qexec;
	}
	

	public void insertQuery(ParameterizedSparqlString queryStr) {
		try{
		 	String userGraphUri = getCurrentUserGraph();// gets your specific graph URI
			addNamespacesToQuery(queryStr);

			// Modify the query based on its type
			String modifiedQuery = modifyQueryForGraph(queryStr.toString(), userGraphUri);

			//System.out.println("***Trying to insert***\n" + modifiedQuery.toString() + "***End query***\n");
			UpdateRequest update = UpdateFactory.create(modifiedQuery);
			UpdateProcessor up;
			up = UpdateExecutionFactory.createRemote(update, UPDATEENDPOINT);
			up.execute();
		}catch(Exception e){
			System.out.println(e.getMessage());
		}finally{
	
			Date date = new Date();
			System.out.println(new Timestamp(date.getTime()));
		
		}
	}

	// Utility method to modify query to use a specific Graph
	private String modifyQueryForGraph(String query, String graphUri) {
		if (graphUri == null || graphUri.isEmpty()) {
			System.out.println("No graph URI specified, using default graph");
			return query;
		}
		String graphClause = "{ GRAPH <" + graphUri + "> {";
		// Handle INSERT DATA
		if (query.trim().toUpperCase().contains("INSERT DATA")) {
			return query.replaceFirst("\\{", graphClause) + " }";
		}
		// Handle INSERT {...} WHERE {...}
		else if (query.trim().toUpperCase().contains("INSERT")) {
			return query.replaceAll("(?i)INSERT\\s*\\{", "INSERT " + graphClause)
					.replaceAll("(?i)WHERE", "}\nUSING <"+graphUri+">\n WHERE");
		}
		// Handle DELETE {...} WHERE {...}
		else if (query.trim().toUpperCase().contains("DELETE")) {
			return query.replaceAll("(?i)DELETE\\s*\\{", "DELETE " + graphClause)
					.replaceAll("(?i)WHERE", "}\nUSING <"+graphUri+">\n WHERE");
		}
		// Return the modified query
		return query;
	}

	
	public boolean insertMultipleQueries(List<ParameterizedSparqlString> queryStrList) {
		Model tempModel = ModelFactory.createOntologyModel();
		tempModel.read(READENDPOINT);
		
		try {
			//Try to execute queries in a temporary/local model
			for (int i = 0; i < queryStrList.size(); i++){
				addNamespacesToQuery(queryStrList.get(i));
				System.out.println("***Trying to insert query on local repo***<query #"+(i+1)+" of "+queryStrList.size()+">\n" + queryStrList.get(i).toString() + "***End query*** <query #"+(i+1)+" of "+queryStrList.size()+">\n");
				UpdateAction.parseExecute(queryStrList.get(i).toString(), tempModel);
			}
			//If no errors occur, I execute the queries on the online ontology

			for (int i = 0; i < queryStrList.size(); i++){
				insertQuery(queryStrList.get(i));
			}
			
		}catch (Exception e){
			System.out.println("***Error while inserting multiple queries: aborted***");
			return false;
		}finally{
			Date date = new Date();
			System.out.println(new Timestamp(date.getTime()));
		}
		return true;
	}
	
	public boolean isLocalOntology() {
		return localOntology;
	}

	private String getCurrentUserGraph() {
		//userService = (UserService) crc.getProperty("userService");
		//userService = UserService.getUserService(crc);
		//UserService userService = UserService.getUserService();
		// Modify the SPARQL query to target the specific user graph
		if (userService == null) {
			return null;
			//throw new IllegalArgumentException("UserService is not set to any user");
		}
		//User user = userService.getUser();
		String userGraphUri = userService.getUserGraphUri();

		return userGraphUri;
	}

//	TODO: Proper ErrorHandling where cookies are not passed. Therefore, in certain cases an error should not be thrown.
//	TODO: Check if SessionValidationServlet.java can be done via JAX RS instead of Servlets, it would be easier and more consistent.
//	TODO: Currently, because of a bug, setUserService is overloaded. Check if possible to remove the overloaded version.
//	TODO: Check if CookieResponseFilter.java is needed. if not, remove.
	public void setUserService(ContainerRequestContext crc) {
		//this.userService = userService;
		userService = (UserService) crc.getProperty("userService");
		// Modify the SPARQL query to target the specific user graph
		if (this.userService == null) {
			//throw new IllegalArgumentException("UserService is not set to any user");
			System.out.println("Welcome! UserService is not set to any user, this happens upon your first login.");
		}
	}
//overloaded method, delete when not needed
//	public void setUserService(UserService userService) {
//		//this.userService = userService;
//		this.userService = userService;
//		// Modify the SPARQL query to target the specific user graph
//		if (this.userService == null) {
//			//throw new IllegalArgumentException("UserService is not set to any user");
//			System.out.println("UserService is not set to any user");
//		}
//	}

	public static String getREADENDPOINT() {
		return READENDPOINT;
	}
	public static String getTRIPLESTOREENDPOINT() {
		return TRIPLESTOREENDPOINT;
	}

	public static String getDATAENDPOINT() {
		return DATAENDPOINT;
	}
	
}
