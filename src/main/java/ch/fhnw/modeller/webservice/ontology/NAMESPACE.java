package ch.fhnw.modeller.webservice.ontology;

public enum NAMESPACE {
	RDFS(		"rdfs",							"http://www.w3.org/2000/01/rdf-schema#"),
	RDF(		"rdf", 							"http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
	OWL(		"owl", 							"http://www.w3.org/2002/07/owl#"),
	ARCHI(		"archi", 						"http://ikm-group.ch/archiMEO/archimate#"),
	APQC(		ONTOLOGY.APQC.getPrefix(),		"http://ikm-group.ch/archimeo/apqc#"),
	MEDIATYPES(	"media-types", 					"http://www.iana.org/assignments/media-types#"),
	BPMN(		ONTOLOGY.BPMN.getPrefix(),		"http://ikm-group.ch/archiMEO/BPMN#"),
	CMMN(		ONTOLOGY.CMMN.getPrefix(),		"http://ikm-group.ch/archimeo/cmmn#"),
	BPAAS(		ONTOLOGY.BPAAS.getPrefix(),		"http://ikm-group.ch/archimeo/bpaas#"),
	EO(			ONTOLOGY.EO.getPrefix(), 		"http://ikm-group.ch/archiMEO/eo#"),
	FBPDO(		ONTOLOGY.FBPDO.getPrefix(),		"http://ikm-group.ch/archimeo/fbpdo#"),
	TOP(		ONTOLOGY.TOP.getPrefix(),		"http://ikm-group.ch/archiMEO/top#"),
	NCO(		ONTOLOGY.NCO.getPrefix(),		"http://ikm-group.ch/archiMEO/nco#"),
	BMM(		ONTOLOGY.BMM.getPrefix(),		"http://ikm-group.ch/archiMEO/BMM#"),
	XSD(		"xsd",							"http://www.w3.org/2001/XMLSchema#"),
	LO(		"lo",								"http://fhnw.ch/modelingEnvironment/LanguageOntology#"),
	DO(		"do",								"http://fhnw.ch/modelingEnvironment/DomainOntology#"),
	questionnaire (ONTOLOGY.questionnaire.getPrefix(), "http://ikm-group.ch/archiMEO/questionnaire#"),
	questiondata (ONTOLOGY.questiondata.getPrefix(), "http://ikm-group.ch/archiMEO/questiondata#"),
	EMO( "emo",								"http://ikm-group.ch/archiMEO/emo#"),
	ISO42010( "iso42010",						"http://ikm-group.ch/archimeo/iso42010#"),
	DSML4PTM( "dsml4ptm",						"http://fhnw.ch/modelingEnvironment/DSML4PTM#"),
	SH( "sh",									"http://www.w3.org/ns/shacl#"),
	RDFSPLUS( "rdfsplus",						"http://topbraid.org/spin/rdfsplus#"),
	ARCHIMEO( "archimeo",						"http://ikm-group.ch/archiMEO#"),
	DCTERMS( "dcterms",						"http://purl.org/dcterms#"),
	FOAF( "foaf",								"http://xmlns.com/foaf/spec#"),
	CC( "cc",									"http://creativecommons.org/ns#"),
	ONTOGOV( "ontogov",						"http://ch.fhnw.ontogov#"),
	UTIL( "util",								"http://ikm-group.ch/archiMEO/util#"),
	ELEMENTS( "elements",						"http://purl.org/dc/elements/1.1#"),
	DCMI( "dcmi-type-vocabulary",				"http://dublincore.org/documents/2000/07/11/dcmi-type-vocabulary#"),
	PO( "po",									"http://fhnw.ch/modelingEnvironment/PaletteOntology#"),
	DC( "dc",									"http://purl.org/dc/elements/1.1/"),
	//APQC( "apqc",								"http://ikm-group.ch/archimeo/apqc#"),
	DKMM( "dkmm",								"http://fhnw.ch/modelingEnvironment/dkmm#"),
	ICF( "icf",								"http://who.int/icf#"),
	SAPSCENES( "sapscenes",						"http://fhnw.ch/SAPScenesOntology#");
	
	private String prefix;
	private String url;
	
	NAMESPACE(String prefix, String url) {
		this.prefix = prefix;
		this.url = url;
	}

	public String getPrefix() {
		return prefix;
	}
	
	public String getURI() {
		return url;
	}
}