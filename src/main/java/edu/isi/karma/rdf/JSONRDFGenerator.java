package edu.isi.karma.rdf;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.Command.CommandTag;
import edu.isi.karma.controller.history.WorksheetCommandHistoryExecutor;
import edu.isi.karma.imp.json.JsonImport;
import edu.isi.karma.kr2rml.ErrorReport;
import edu.isi.karma.kr2rml.KR2RMLMapping;
import edu.isi.karma.kr2rml.KR2RMLWorksheetRDFGenerator;
import edu.isi.karma.kr2rml.R2RMLMappingIdentifier;
import edu.isi.karma.kr2rml.WorksheetR2RMLJenaModelParser;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.rep.WorkspaceManager;
import edu.isi.karma.util.JSONUtil;
import edu.isi.karma.webserver.ExecutionController;
import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.WorkspaceRegistry;

public class JSONRDFGenerator {

	private static Logger logger = LoggerFactory.getLogger(JSONRDFGenerator.class);
	private HashMap<String, R2RMLMappingIdentifier> modelIdentifiers;
	private HashMap<String, WorksheetR2RMLJenaModelParser> readModelParsers;
	
	private Workspace workspace;
	
	private JSONRDFGenerator() {
		this.modelIdentifiers = new HashMap<String, R2RMLMappingIdentifier>();
		this.readModelParsers = new HashMap<String, WorksheetR2RMLJenaModelParser>();
		this.workspace = WorkspaceManager.getInstance().createWorkspace();
		WorkspaceRegistry.getInstance().register(new ExecutionController(this.workspace));
	}
	
	private static JSONRDFGenerator instance = null;
	public static JSONRDFGenerator getInstance() {
		if(instance == null) {
			instance = new JSONRDFGenerator();
		}
		return instance;
	}
	
	public void addModel(R2RMLMappingIdentifier modelIdentifier) {
		this.modelIdentifiers.put(modelIdentifier.getName(), modelIdentifier);
	}
	
	public void generateRDF(String sourceName, String jsonData, boolean addProvenance, PrintWriter pw) throws KarmaException, JSONException, IOException {
		R2RMLMappingIdentifier id = this.modelIdentifiers.get(sourceName);
		if(id == null) {
			throw new KarmaException("Cannot generate RDF. Model named " + sourceName + " does not exist");
		}
		
		//Generate worksheet from the json data
		Object json = JSONUtil.createJson(jsonData);
        JsonImport imp = new JsonImport(json, sourceName, workspace, "utf-8", -1);
        Worksheet worksheet = imp.generateWorksheet();
        
		//Check if the parser for this model exists, else create one
		WorksheetR2RMLJenaModelParser modelParser = readModelParsers.get(sourceName);
		if(modelParser == null) {
			modelParser = loadModel(id);
		}
		
        //Generate mappping data for the worksheet using the model parser
		KR2RMLMapping mapping = modelParser.parse();
		
		WorksheetCommandHistoryExecutor wchr = new WorksheetCommandHistoryExecutor(worksheet.getId(), workspace);
		try
		{
			List<CommandTag> tags = new ArrayList<CommandTag>();
			tags.add(CommandTag.Transformation);
			wchr.executeCommandsByTags(tags, mapping.getWorksheetHistory());
		}
		catch (CommandException | KarmaException e)
		{
			logger.error("Unable to execute column transformations", e);
		}

		//Generate RDF using the mapping data
		ErrorReport errorReport = new ErrorReport();
		KR2RMLWorksheetRDFGenerator rdfGen = new KR2RMLWorksheetRDFGenerator(worksheet,
		        workspace.getFactory(), workspace.getOntologyManager(), pw,
		        mapping, errorReport, addProvenance);
		rdfGen.generateRDF(false);
	}
	
	private WorksheetR2RMLJenaModelParser loadModel(R2RMLMappingIdentifier modelIdentifier) throws JSONException, KarmaException {
		WorksheetR2RMLJenaModelParser parser = new WorksheetR2RMLJenaModelParser(modelIdentifier);
		this.readModelParsers.put(modelIdentifier.getName(), parser);
		return parser;
	}
}
