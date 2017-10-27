package ch.fhnw.modeller.webservice;

import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import com.google.gson.Gson;

import ch.fhnw.modeller.model.palette.PaletteCategory;
import ch.fhnw.modeller.model.palette.PaletteElement;
import ch.fhnw.modeller.persistence.GlobalVariables;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;

@Path("/ModEnv")
public class ModellingEnvironment {
	private Gson gson = new Gson();
	private OntologyManager ontology = OntologyManager.getInstance();
	private boolean debug_properties = false;


	@GET
	@Path("/getPaletteElements")
	public Response getPaletteElements() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested palette elements" );
		System.out.println("####################<end>####################");
		ArrayList<PaletteElement> all_palette_elements = new ArrayList<PaletteElement>();
		
		try {
				all_palette_elements = queryAllPaletteElements();

				if (debug_properties){
				for (int index = 0; index < all_palette_elements.size(); index++){
					System.out.println("Element "+index+": ");
					}
				}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}
		
		
		
		
		String json = gson.toJson(addChildElements(all_palette_elements));
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}
	
	private ArrayList<PaletteElement> queryAllPaletteElements() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<PaletteElement> result = new ArrayList<PaletteElement>();
		
		queryStr.append("SELECT ?element ?label ?representedClass ?imageURL ?thumbnailURL ?showedInPalette ?representedClassLabel ?parentElement ?textLabelSizeX2 ?textLabelSizeY2 ?paletteCategory WHERE {");
		queryStr.append("?element rdf:type* obme:PaletteElement .");
		queryStr.append("?element rdfs:label ?label .");
		queryStr.append("?element obme:paletteElementRepresentMetamodelElement ?representedClass .");
		queryStr.append("?representedClass rdfs:label ?representedClassLabel .");
		queryStr.append("?element obme:paletteElementImageURL ?imageURL .");
		queryStr.append("?element obme:paletteElementThumbnailURL ?thumbnailURL .");
		queryStr.append("?element obme:paletteElementX2ForTextLabelSize ?textLabelSizeX2 .");
		queryStr.append("?element obme:paletteElementY2ForTextLabelSize ?textLabelSizeY2 .");
		queryStr.append("OPTIONAL{");
		queryStr.append("?element obme:paletteElementIsShowedInRootPalette ?showedInPalette .");
		queryStr.append("}");
		queryStr.append("OPTIONAL{");
		queryStr.append("?element obme:paletteElementHasParentPaletteElement ?parentElement .");
		queryStr.append("}");
		queryStr.append("OPTIONAL{");
		queryStr.append("?element obme:paletteElementHasPaletteCategory ?paletteCategory .");
		queryStr.append("}");
		queryStr.append("}");
		//queryStr.append("ORDER BY ?domain ?field");

		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();
		
		if (results.hasNext()) {
			while (results.hasNext()) {
				PaletteElement tempPaletteElement = new PaletteElement();
				
				QuerySolution soln = results.next();
				tempPaletteElement.setId(soln.get("?element").toString());
				tempPaletteElement.setLabel(soln.get("?label").toString());
				tempPaletteElement.setRepresentedClass(soln.get("?representedClass").toString());
				tempPaletteElement.setRepresentedClassLabel(soln.get("?representedClassLabel").toString());
				tempPaletteElement.setImageURL(soln.get("?imageURL").toString());
				tempPaletteElement.setThumbnailURL(soln.get("?thumbnailURL").toString());
				tempPaletteElement.setTextLabelSizeX2(FormatConverter.ParseOntologyInteger(soln.get("?textLabelSizeX2").toString(), soln.get("?element").toString()));
				tempPaletteElement.setTextLabelSizeY2(FormatConverter.ParseOntologyInteger(soln.get("?textLabelSizeY2").toString(), soln.get("?element").toString()));
				
				if (soln.get("?showedInPalette") != null){
					tempPaletteElement.setShowedInPalette(FormatConverter.ParseOntologyBoolean(soln.get("?showedInPalette").toString(), soln.get("?element").toString()));
				}
				
				if (soln.get("?parentElement") != null){
					tempPaletteElement.setParentElement(soln.get("?parentElement").toString());
				}
				if (soln.get("?paletteCategory") != null){
					tempPaletteElement.setPaletteCategory(soln.get("?paletteCategory").toString());
				}
				
				
				result.add(tempPaletteElement);
			}
		}
		qexec.close();
		return result;
}
	@GET
	@Path("/getPaletteCategories")
	public Response getPaletteCategories() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested palette categories" );
		System.out.println("####################<end>####################");
		ArrayList<PaletteCategory> all_palette_categories = new ArrayList<PaletteCategory>();
		
		try {
				all_palette_categories = queryAllPaletteCategories();

				if (debug_properties){
					for (int index = 0; index < all_palette_categories.size(); index++){
						System.out.println("Category "+index+": ");
					}
				}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}
		
		
		String json = gson.toJson(all_palette_categories);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}
	
	private ArrayList<PaletteCategory> queryAllPaletteCategories() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<PaletteCategory> result = new ArrayList<PaletteCategory>();
		
		queryStr.append("SELECT ?category ?label ?orderNumber WHERE {");
		queryStr.append("?category rdf:type* obme:PaletteCategory .");
		queryStr.append("?category rdfs:label ?label .");
		queryStr.append("OPTIONAL{");
		queryStr.append("?category obme:paletteCategoryOrderNumber ?orderNumber .");
		queryStr.append("}");
		queryStr.append("}");
		queryStr.append("ORDER BY ?orderNumber");

		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();
		
		if (results.hasNext()) {
			while (results.hasNext()) {
				PaletteCategory tempPaletteCategory = new PaletteCategory();
				
				QuerySolution soln = results.next();
				tempPaletteCategory.setId(soln.get("?category").toString());
				tempPaletteCategory.setLabel(soln.get("?label").toString());
				
				if (soln.get("?orderNumber") != null){
					tempPaletteCategory.setOrderNumber(FormatConverter.ParseOntologyInteger(soln.get("?orderNumber").toString(),soln.get("?category").toString()));
				}
				
				result.add(tempPaletteCategory);
			}
		}
		qexec.close();
		return result;
}

	private ArrayList<PaletteElement> addChildElements(ArrayList<PaletteElement> all_palette_elements){
	ArrayList<PaletteElement> parentList = new ArrayList<PaletteElement>();
	System.out.println("pre: " + all_palette_elements.size());
	for (int i = 0; i < all_palette_elements.size(); i++){
		if (all_palette_elements.get(i).getParentElement() == null){
			parentList.add(all_palette_elements.get(i));
		}
	}
	System.out.println("post: " + all_palette_elements.size());
	System.out.println("Number of parents: " + parentList.size());
		for (int i = 0; i < parentList.size();i++){
			if (parentList.get(i).getChildElements().size() == 0){
				//System.out.println("=========");
				addChilds(parentList.get(i), all_palette_elements);
				//System.out.println("1. Analysing " + all_palette_elements.get(i).getId());
			}
		}
		return parentList;
	}
	
	
	private void addChilds(PaletteElement parent, ArrayList<PaletteElement> list){
		ArrayList<PaletteElement> childList = getChildren(parent, list);
		for (int i = 0; i < childList.size(); i++){
			//System.out.println("3. Adding child of " + list.get(i).getId());
			parent.getChildElements().add(childList.get(i));
			addChilds(childList.get(i), list);
		}
	}
	
	private ArrayList<PaletteElement> getChildren(PaletteElement parent, ArrayList<PaletteElement> list){
		ArrayList<PaletteElement> result = new ArrayList<PaletteElement>();
		for (int i = 0; i < list.size(); i++){
			if (list.get(i).getParentElement() != null &&
			list.get(i).getParentElement().equals(parent.getId())){
				//System.out.println("2. Found a child of " + parent.getId() + " -> " + list.get(i).getId());
				result.add(list.get(i));
			}
		}
		return result;
	}
	
}
