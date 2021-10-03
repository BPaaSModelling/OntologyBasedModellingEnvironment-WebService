package ch.fhnw.modeller.webservice.ontology;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import ch.fhnw.modeller.webservice.config.ConfigReader;


public final class OntologyManager {

	private static OntologyManager INSTANCE;
	private boolean localOntology = true;
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
		addNamespacesToQuery(queryStr);
		System.out.println("***Performed query***\n" + queryStr.toString() + "***Performed query***\n");
		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution qexec;
		
		qexec = QueryExecutionFactory.sparqlService(QUERYENDPOINT, query);
	
		return qexec;
	}
	

	public void insertQuery(ParameterizedSparqlString queryStr) {
		try{
		addNamespacesToQuery(queryStr);
		System.out.println("***Trying to insert***\n" + queryStr.toString() + "***End query***\n");
		UpdateRequest update = UpdateFactory.create(queryStr.toString());
		UpdateProcessor up;
		up = UpdateExecutionFactory.createRemote(update, UPDATEENDPOINT);
		up.execute();
		}catch(Exception e){
		}finally{
	
		Date date = new Date();
		System.out.println(new Timestamp(date.getTime()));
		
		}
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

	public static String getREADENDPOINT() {
		return READENDPOINT;
	}

	
}
