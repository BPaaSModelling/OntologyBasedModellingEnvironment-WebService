package ch.fhnw.modeller.webservice.ontology;

public enum ONTOLOGY {
	
	APQC			("apqc",		"TTL",		"apqc.ttl"),
	BPAAS			("bpaas",		"TTL",		"bpaas.ttl"),
	FBPDO			("fbpdo",		"TTL",		"fbpdo.ttl"),
	ARCHIMATE		("archi",		"TTL",		"ARCHIMEO/ARCHIMATE/ArchiMate.ttl"),
	BMM				("bmm",			"TTL", 		"ARCHIMEO/BMM/BMM.ttl"),
	BPMN			("bpmn",		"TTL", 		"ARCHIMEO/BPMN/BPMN.ttl"),
	CMMN			("cmmn",		"TTL",		"ARCHIMEO/CMMN/CMMN.ttl"),		
	EMO				("emo",			"TTL",	 	"ARCHIMEO/EMO/EMO.ttl"),
	EO				("eo",			"TTL",	 	"ARCHIMEO/EO/EO.ttl"),
	NCO				("nco",			"TTL",	 	"ARCHIMEO/NCO/NCO.ttl"),
	TOP				("top",			"TTL",		"ARCHIMEO/TOP/TOP.ttl"),
	
	
	bdata			("bdata",			"TTL",	"bdata.ttl"),
	questionnaire	("questionnaire",	"TTL",	"questionnaire.ttl"),
	questiondata	("questiondata",	"TTL",	"questiondata.ttl");
	
	private String prefix;
	private String format;
	private String remoteURL;

	ONTOLOGY(String prefix, String format, String remoteURL) {
		this.prefix = prefix;
		this.format = format;
		this.remoteURL = remoteURL;
	}

	public String getLoadURL() {
		return remoteURL;
	}

	public String getFormat() {
		return format;
	}

	public String getPrefix() {
		return prefix;
	}
}
