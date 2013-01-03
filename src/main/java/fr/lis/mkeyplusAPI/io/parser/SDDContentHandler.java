package fr.lis.mkeyplusAPI.io.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Dataset;
import model.Description;
import model.DescriptionElementState;
import model.Descriptor;
import model.DescriptorNode;
import model.DescriptorTree;
import model.Item;
import model.ItemNode;
import model.ItemTree;
import model.QuantitativeMeasure;
import model.Resource;
import model.State;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import utils.Utils;
import fr.lis.mkeyplusAPI.model.ExtCategoricalDescriptor;
import fr.lis.mkeyplusAPI.model.ExtQuantitativeDescriptor;

/**
 * @authors Thomas Burguiere, Florian Causse
 * 
 */
public class SDDContentHandler implements ContentHandler {
	private Locator locator = null;

	// flag to know if we are in or out a tag
	private boolean inDataset = false;
	private boolean inRepresentation = false;
	private boolean inRevisionData = false;
	private boolean inIPRStatements = false;
	private boolean inIPRStatement = false;
	private boolean iprStatementIsCopyright = false;
	private boolean inConceptStates = false;
	private boolean inStates = false;
	private boolean inStateDefinition = false;
	private boolean inStatesReference = false;
	private boolean inNodes = false;
	private boolean inCharNode = false;
	private boolean inDependencyRules = false;
	private boolean inInapplicableIf = false;
	private boolean inOnlyApplicableIf = false;
	private boolean inTaxonNames = false;
	private boolean inTaxonName = false;
	private boolean inCodedDescriptions = false;
	private boolean inCodedDescription = false;
	private boolean inScope = false;
	private boolean inSummaryData = false;
	private boolean inCategorical = false;
	private boolean inQuantitative = false;
	private boolean inMeasurementUnit = false;
	private boolean inCategoricalCharacter = false;
	private boolean inQuantitativeCharacter = false;
	private boolean inCharacterTree = false;
	private boolean inCharacters = false;
	private boolean inRatings = false;
	private boolean inTaxonHierarchies = false;
	private boolean inTaxonHierarchy = false;
	private boolean inNode = false;
	private boolean isFirstTaxonHierarchy = true;
	private boolean inMediaObject = false;
	private boolean inAgents = false;
	private boolean inAgent = false;
	private boolean dataUnavailableFlag = false;

	// to test if the MediaObject is of type image
	private boolean isImageType = false;
	// to test if the MediaObject is of type video
	private boolean isVideoType = false;
	// to test if the MediaObject is of type sound
	private boolean isSoundType = false;

	// to test if the Label is the Dataset Label
	private boolean isDatasetLabel = true;
	// to test if the Detail is the Dataset Detail
	private boolean isDatasetDetail = true;
	// to parse only the first Dataset
	private boolean isFirstDataset = true;
	// to parse only the first Dataset
	private boolean isFirstCharacterTree = true;
	// to add the descriptor tree only if it's a dependency descriptor tree
	private boolean isDependencyCharacterHierarchy = false;
	// buffer to collect text value
	private StringBuffer buffer = null;
	// kwnoledge base
	private Dataset dataset = null;

	// current quantitative descriptor
	private ExtCategoricalDescriptor currentCategoricalCharacter = null;
	// current quantitative descriptor
	private ExtQuantitativeDescriptor currentQuantitativeCharacter = null;
	// current state
	private State currentState = null;
	// current Description
	private Description currentDescription = null;
	// current item
	private Item currentItem = null;
	// <Detail> content of the current Item
	private String currentItemDetail = null;
	// current label of coded description
	private String currentCodedDescriptionLabel = null;
	// current MediaObject
	private Resource currentResource = null;

	private List<String> itemMediaObjectRefList = null;

	// current Description descriptor
	private Descriptor currentDescriptionDescriptor = null;

	private List<State> currentStatesList = null;

	private QuantitativeMeasure currentQuantitativeMeasure = null;

	private DescriptorNode currentDescriptorNode = null;
	private DescriptorTree currentDescriptorTree = null;
	private ItemNode currentItemNode = null;
	private ItemTree currentItemTree = null;
	private String curentItemNodeIdKey = null;

	List<State> currentInapplicableStates = null;
	List<State> currentOnlyApplicableStates = null;

	private String mediaObjectId = null;
	private HashMap<String, Object> sddMediaKeyObjectMap = new HashMap<String, Object>();

	private String currentAgentId = null;
	private HashMap<String, String> agentIdNameMap = new HashMap<String, String>();

	private Map<Descriptor, Integer> ratingsCounter = null;

	private HashMap<String, State> stateIdKey = new HashMap<String, State>();
	private HashMap<String, Descriptor> descriptorIdKey = new HashMap<String, Descriptor>();
	private HashMap<String, Item> itemIdKey = new HashMap<String, Item>();
	private HashMap<String, ItemNode> itemNodeIdKey = new HashMap<String, ItemNode>();
	private HashMap<String, String> parentItemNodeIdKey = new HashMap<String, String>();

	/**
	 * The constructor by default
	 */
	public SDDContentHandler() {
		super();
		this.dataset = new Dataset();
		this.ratingsCounter = new HashMap<Descriptor, Integer>();
	}

	@Override
	public void startElement(String nameSpaceURI, String localName, String rawName, Attributes attributes)
			throws SAXException {

		if (isFirstDataset) {

			// if the Label is not the dataset Label and the Detail is not the dataset Detail
			if (inDataset && !localName.equals("Representation") && !localName.equals("Label")
					&& !localName.equals("Detail")) {
				isDatasetLabel = false;
				isDatasetDetail = false;
			}

			// <Dataset>
			if (localName.equals("Dataset")) {
				inDataset = true;
			}

			// <Representation> in <Dataset>
			else if (localName.equals("Representation") && inDataset) {
				inRepresentation = true;
			}

			// <Label> in <Dataset>
			else if (localName.equals("Label") && inDataset) {
				buffer = new StringBuffer();
			}

			// <Detail> in <Dataset>
			else if (localName.equals("Detail") && inDataset) {
				buffer = new StringBuffer();
			}

			// <RevisionData> in <Dataset>
			else if (localName.equals("RevisionData") && inDataset) {
				inRevisionData = true;
			}

			// <DateCreated> in <RevisionData> in <Dataset>
			else if (localName.equals("DateCreated") && inDataset && inRevisionData) {
				buffer = new StringBuffer();
			}

			// <IPRStatements> in <Dataset>
			else if (localName.equals("IPRStatements") && inDataset) {
				inIPRStatements = true;
			}

			// <IPRStatement> in <IPRStatements>
			else if (localName.equals("IPRStatement") && inIPRStatements) {
				inIPRStatement = true;
				if (attributes.getValue("role").equals("Copyright"))
					iprStatementIsCopyright = true;
			}

			// <ConceptStates>
			else if (localName.equals("ConceptStates")) {
				inConceptStates = true;

			}

			// <StateDefinition> in <ConceptStates>
			else if (localName.equals("StateDefinition") && inConceptStates) {

			}

			// <Characters>
			else if (localName.equals("Characters")) {
				inCharacters = true;
			}

			// <CategoricalCharacter> in <Characters>
			else if (localName.equals("CategoricalCharacter") && inCharacters) {
				inCategoricalCharacter = true;
				currentCategoricalCharacter = new ExtCategoricalDescriptor();
				descriptorIdKey.put(attributes.getValue("id"), currentCategoricalCharacter);
			}

			// <Representation> in <CategoricalCharacter>
			else if (localName.equals("Representation") && inCategoricalCharacter) {
				inRepresentation = true;
			}

			// <MediaObject> in <Representation> in <CategoricalCharacter> (not in <States>)
			else if (localName.equals("MediaObject") && inRepresentation && inCategoricalCharacter
					&& !inStates) {
				if (attributes.getValue("ref") != null && currentCategoricalCharacter != null) {
					sddMediaKeyObjectMap.put(attributes.getValue("ref"), currentCategoricalCharacter);
				}
			}

			// <QuantitativeCharacter> in <Characters>
			else if (localName.equals("QuantitativeCharacter") && inCharacters) {
				inQuantitativeCharacter = true;
				currentQuantitativeCharacter = new ExtQuantitativeDescriptor();
				descriptorIdKey.put(attributes.getValue("id"), currentQuantitativeCharacter);
			}

			// <States>
			else if (localName.equals("States")) {
				inStates = true;
			}

			// <StateDefinition> in <States>
			else if (localName.equals("StateDefinition") && inStates) {
				inStateDefinition = true;
				currentState = new State();
				String id = attributes.getValue("id");
				stateIdKey.put(id, currentState);
			}

			// <MediaObject> in <Representation> in <StateDefinition> in <States>
			else if (localName.equals("MediaObject") && inStateDefinition && inRepresentation && inStates) {
				if (attributes.getValue("ref") != null && currentState != null) {
					sddMediaKeyObjectMap.put(attributes.getValue("ref"), currentState);
				}
			}

			// <StateReference> in <States>
			else if (localName.equals("StateReference") && inStates) {
				inStatesReference = true;
			}

			// <ConceptState> in <StateReference>
			else if (localName.equals("ConceptState") && inStatesReference) {

			}

			// <CharacterTree>
			else if (localName.equals("CharacterTree")) {
				if (isFirstCharacterTree) {
					inCharacterTree = true;
					currentDescriptorTree = new DescriptorTree();
				}
			}

			// <Nodes>
			else if (localName.equals("Nodes")) {
				inNodes = true;
			}

			// <CharNode> in <Nodes>
			else if (localName.equals("CharNode") && inNodes) {
				inCharNode = true;
				currentInapplicableStates = new ArrayList<State>();
				currentOnlyApplicableStates = new ArrayList<State>();
			}

			// <DependencyRules> in <CharNode>
			else if (localName.equals("DependencyRules") && inCharNode) {
				inDependencyRules = true;
			}

			// <InapplicableIf> in <DependencyRules>
			else if (localName.equals("InapplicableIf") && inDependencyRules) {
				inInapplicableIf = true;
			}

			// <OnlyApplicableIf> in <DependencyRules>
			else if (localName.equals("OnlyApplicableIf") && inDependencyRules) {
				inOnlyApplicableIf = true;
			}

			// <State> in <InapplicableIf> in <CharacterTree> & isFirstCharacterTree
			else if (localName.equals("State") && inInapplicableIf && inCharacterTree && isFirstCharacterTree) {
				State state = stateIdKey.get(attributes.getValue("ref"));
				currentInapplicableStates.add(state);
			}

			// <State> in <OnlyApplicableIf> in <CharacterTree> && isFirstCharacterTree
			else if (localName.equals("State") && inOnlyApplicableIf && inCharacterTree
					&& isFirstCharacterTree) {
				State state = stateIdKey.get(attributes.getValue("ref"));
				currentOnlyApplicableStates.add(state);
			}

			// <Character> in <CharNode> in <CharacterTree> && isFirstCharacterTree
			else if (localName.equals("Character") && inCharNode && inCharacterTree && isFirstCharacterTree) {
				currentDescriptorNode = new DescriptorNode(descriptorIdKey.get(attributes.getValue("ref")));
				currentDescriptorTree.addNode(currentDescriptorNode);
			}

			// <TaxonNames>
			else if (localName.equals("TaxonNames")) {
				inTaxonNames = true;
			}

			// <TaxonName> in <TaxonNames>
			else if (localName.equals("TaxonName") && inTaxonNames) {
				inTaxonName = true;
				currentItem = new Item();
				itemIdKey.put(attributes.getValue("id"), currentItem);
			}

			// <MediaObject> in <TaxonName>
			else if (localName.equals("MediaObject") && inTaxonName && currentItem != null
					&& attributes.getValue("ref") != null) {
				sddMediaKeyObjectMap.put(attributes.getValue("ref"), currentItem);
			}

			// <CodedDescriptions>
			else if (localName.equals("CodedDescriptions")) {
				inCodedDescriptions = true;
			}

			// <CodedDescription> in <CodedDescriptions>
			else if (localName.equals("CodedDescription") && inCodedDescriptions) {
				inCodedDescription = true;
				currentDescription = new Description();
				itemMediaObjectRefList = new ArrayList<String>();
			}

			// <Representation> in <CodedDescription> in <CodedDescriptions>
			else if (localName.equals("Representation") && inCodedDescription && inCodedDescriptions) {
				inRepresentation = true;
			}

			// <MediaObject> in <Representation> <CodedDescription> in <CodedDescriptions>
			else if (localName.equals("MediaObject") && inRepresentation && inCodedDescription
					&& inCodedDescriptions) {
				if (attributes.getValue("ref") != null)
					itemMediaObjectRefList.add(attributes.getValue("ref"));
			}

			// <Scope>
			else if (localName.equals("Scope")) {
				inScope = true;
			}

			// <TaxonName> in <Scope>
			else if (localName.equals("TaxonName") && inScope) {
				currentItem = itemIdKey.get(attributes.getValue("ref"));
			}

			// <SummaryData>
			else if (localName.equals("SummaryData")) {
				inSummaryData = true;
				if (currentItem == null) {
					currentItem = new Item(currentCodedDescriptionLabel);
					for (String mediaObjectRef : itemMediaObjectRefList) {
						sddMediaKeyObjectMap.put(mediaObjectRef, currentItem);
					}
					if (currentItemDetail != null)
						currentItem.setDetail(currentItemDetail);
				}
			}

			// <Categorical> in <SummaryData>
			else if (localName.equals("Categorical") && inSummaryData) {
				inCategorical = true;
				currentDescriptionDescriptor = descriptorIdKey.get(attributes.getValue("ref"));
				currentStatesList = new ArrayList<State>();
			}

			// <State> in <Categorical>
			else if (localName.equals("State") && inCategorical) {
				currentStatesList.add(stateIdKey.get(attributes.getValue("ref")));
			}

			// <Status> in <Categorical>
			else if (localName.equals("Status") && inCategorical) {
				if (attributes.getValue("code") != null
						&& attributes.getValue("code").equals("DataUnavailable")) {
					dataUnavailableFlag = true;
				}
			}

			// <Quantitative> in <SummaryData>
			else if (localName.equals("Quantitative") && inSummaryData) {
				inQuantitative = true;
				currentDescriptionDescriptor = descriptorIdKey.get(attributes.getValue("ref"));
				currentQuantitativeMeasure = new QuantitativeMeasure();
			}

			// <Measure> in <Quantitative>
			else if (localName.equals("Measure") && inQuantitative) {
				if (currentQuantitativeMeasure != null) {
					if (attributes.getValue("type").equals("Min")) {
						currentQuantitativeMeasure.setMin(Double.parseDouble(attributes.getValue("value")));
					} else if (attributes.getValue("type").equals("Max")) {
						currentQuantitativeMeasure.setMax(Double.parseDouble(attributes.getValue("value")));
					} else if (attributes.getValue("type").equals("Mean")) {
						currentQuantitativeMeasure.setMean(Double.parseDouble(attributes.getValue("value")));
					} else if (attributes.getValue("type").equals("SD")) {
						currentQuantitativeMeasure.setSD(Double.parseDouble(attributes.getValue("value")));
					} else if (attributes.getValue("type").equals("UMethLower")) {
						currentQuantitativeMeasure.setUMethLower(Double.parseDouble(attributes
								.getValue("value")));
					} else if (attributes.getValue("type").equals("UMethUpper")) {
						currentQuantitativeMeasure.setUMethUpper(Double.parseDouble(attributes
								.getValue("value")));
					}
				}
			}

			// <Status> in <Quantitative>
			else if (localName.equals("Status") && inCategorical) {
				if (attributes.getValue("code") != null
						&& attributes.getValue("code").equals("DataUnavailable")) {
					dataUnavailableFlag = true;
				}
			}

			// <MeasurementUnit>
			else if (localName.equals("MeasurementUnit")) {
				inMeasurementUnit = true;
			}

			// <Ratings>
			else if (localName.equals("Ratings")) {
				inRatings = true;
			}

			// <Rating> in <Ratings>
			else if (localName.equals("Rating") && inRatings) {
				if (attributes.getValue("context").equals("")) {
					int currentRating = Utils.ratings.indexOf(attributes.getValue("rating")) + 1;
					if (currentDescriptionDescriptor != null) {
						if (this.ratingsCounter.get(currentDescriptionDescriptor) == null) {
							this.ratingsCounter.put(currentDescriptionDescriptor, 0);
							currentDescriptionDescriptor.setGlobalWeight(0);
						}
						currentDescriptionDescriptor.setGlobalWeight(currentDescriptionDescriptor
								.getGlobalWeight() + currentRating);
						this.ratingsCounter.put(currentDescriptionDescriptor,
								this.ratingsCounter.get(currentDescriptionDescriptor) + 1);
						currentDescription.getDescriptionElement(currentDescriptionDescriptor.getId())
								.setContextualWeight(currentRating);
					}
				}
			}

			// <TaxonHierarchies>
			else if (localName.equals("TaxonHierarchies")) {
				inTaxonHierarchies = true;
			}

			// <TaxonHierarchy> in <TaxonHierarchies> && isFirstTaxonHierarchie
			else if (localName.equals("TaxonHierarchy") && inTaxonHierarchies && isFirstTaxonHierarchy) {
				inTaxonHierarchy = true;
				currentItemTree = new ItemTree();
			}

			// <Node> in <Nodes> in <TaxonHierarchy>
			else if (localName.equals("Node") && inNodes && inTaxonHierarchy) {
				inNode = true;
				currentItemNode = new ItemNode();
				currentItemTree.addNode(currentItemNode);
				itemNodeIdKey.put(attributes.getValue("id"), currentItemNode);
				curentItemNodeIdKey = attributes.getValue("id");
			}

			// <TaxonName> in <Node> in <TaxonHierarchie>
			else if (localName.equals("TaxonName") && inNode && inTaxonHierarchy) {
				currentItemNode.setItem(itemIdKey.get(attributes.getValue("ref")));
			}

			// <Parent> in <Node> in <TaxonHierarchie>
			else if (localName.equals("Parent") && inNode && inTaxonHierarchy) {
				parentItemNodeIdKey.put(curentItemNodeIdKey, attributes.getValue("ref"));
			}

			// <Agents> in <Dataset>
			else if (localName.equals("Agents") && inDataset) {
				inAgents = true;
			}

			// <Agent> in <Agents>
			else if (localName.equals("Agent") && inDataset && inAgents) {
				inAgent = true;
				currentAgentId = attributes.getValue("id");
			}

			// <Agent> reference in <MediaObject>
			else if (localName.equals("Agent") && inDataset && !inAgents && inMediaObject) {
				if (currentResource.getAuthor() == null)
					currentResource.setAuthor(agentIdNameMap.get(attributes.getValue("ref")));
			}

			// <MediaObject>
			else if (localName.equals("MediaObject")) {
				inMediaObject = true;
				if (attributes.getValue("id") != null) {
					mediaObjectId = attributes.getValue("id");
					currentResource = new Resource();
				} else
					mediaObjectId = null;
			}

			// <Type> in <MediaObject>
			else if (localName.equals("Type") && inMediaObject) {
				buffer = new StringBuffer();
			}

			// <Source> in <MediaObject>
			else if (localName.equals("Source") && inMediaObject && mediaObjectId != null) {
				if (attributes.getValue("href") != null) {
					Object o = sddMediaKeyObjectMap.get(mediaObjectId);
					currentResource.setUrl(attributes.getValue("href"));
					if (isImageType)
						currentResource.setType(Utils.IMAGE_TYPE);
					else if (isVideoType)
						currentResource.setType(Utils.VIdEO_TYPE);
					else if (isSoundType)
						currentResource.setType(Utils.SOUND_TYPE);
					else
						currentResource.setType(Utils.OTHER_TYPE);
					if (o != null) {
						if (o instanceof Descriptor)
							((Descriptor) o).addResource(currentResource);
						else if (o instanceof Item)
							((Item) o).addResource(currentResource);
						else if (o instanceof State)
							((State) o).addResource(currentResource);
					}
				}
			}
		}
	}

	@Override
	public void endElement(String nameSpaceURI, String localName, String rawName) throws SAXException {
		if (isFirstDataset) {
			// <Dataset>
			if (localName.equals("Dataset")) {
				inDataset = false;
				isFirstDataset = false;

				// null description will be considered as unknown data. Empty
				// states list or
				// QuantitativeMeasure
				// will be considered as not specified (not described)
				for (Item item : this.dataset.getItems()) {
					for (Descriptor descriptor : this.dataset.getDescriptors()) {
						if (item.getDescription() == null) {
							Description description = new Description();
							DescriptionElementState descriptionElementState = new DescriptionElementState();
							if (descriptor.isQuantitativeType()) {
								descriptionElementState.setQuantitativeMeasure(new QuantitativeMeasure());
							}
							description.addDescriptionElement(descriptor, descriptionElementState);
							item.setDescription(description);
						}
					}
				}
			}

			// <Representation> in <Dataset>
			else if ((localName).equals("Representation") && inDataset) {
				inRepresentation = false;
			}

			// <RevisionData> in <Dataset>
			else if (localName.equals("RevisionData") && inDataset) {
				inRevisionData = false;
			}

			// <DateCreated> in <RevisionData> in <Dataset>
			else if (localName.equals("DateCreated") && inDataset && inRevisionData) {
				String dateCreatedString = buffer.toString();
				try {
					Date dateCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateCreatedString);
					dataset.setCreationDate(dateCreated);
				} catch (ParseException e) {

				}
			}

			// <IPRStatements> in <Dataset>
			else if ((localName).equals("IPRStatements") && inDataset) {
				inIPRStatements = false;
			}

			// <IPRStatement> in <IPRStatements>
			else if (localName.equals("IPRStatement") && inIPRStatements) {
				inIPRStatement = false;
				iprStatementIsCopyright = false;
			}

			// <Label> in <IPRStatement>
			else if (localName.equals("Label") && inIPRStatement && iprStatementIsCopyright) {
				if (dataset.getCopyright() == null || dataset.getCopyright().trim().length() == 0)
					dataset.setCopyright(buffer.toString());
				else
					dataset.setCopyright(dataset.getCopyright() + ", " + buffer.toString());
			}

			// <Label> in <Representation> in <Dataset>
			else if (localName.equals("Label") && inRepresentation && inDataset) {
				if (inTaxonName) {
					currentItem.setName(buffer.toString());
				} else if (inCodedDescription) {
					currentCodedDescriptionLabel = buffer.toString();
				} else if (inCategoricalCharacter && inStateDefinition) {
					currentState.setName(buffer.toString());
				} else if (inCategoricalCharacter) {
					currentCategoricalCharacter.setName(buffer.toString());
				} else if (inQuantitativeCharacter) {
					currentQuantitativeCharacter.setName(buffer.toString());
				} else if (inCharacterTree && isFirstCharacterTree) {
					if (currentDescriptorTree.getName() == null) {
						currentDescriptorTree.setName(buffer.toString());
					}
				} else if (inTaxonHierarchy && isFirstTaxonHierarchy) {
					if (currentItemTree.getName() == null) {
						currentItemTree.setName(buffer.toString());
					}
				} else if (isDatasetLabel) {
					this.dataset.setName(buffer.toString());
					isDatasetLabel = false;
				} else if (inAgent) {
					this.dataset.getAuthors().add(buffer.toString());
					agentIdNameMap.put(currentAgentId, buffer.toString());
				} else if (inMediaObject) {
					this.currentResource.setName(buffer.toString());
				}
			}

			// <Detail> in <Representation> in <Dataset>
			else if (localName.equals("Detail") && inRepresentation && inDataset) {
				if (inStateDefinition) {
					currentState.setDetail(buffer.toString());
				} else if (inCategoricalCharacter) {
					currentCategoricalCharacter.setDetail(buffer.toString());
				} else if (inQuantitativeCharacter) {
					currentQuantitativeCharacter.setDetail(buffer.toString());
				} else if (inTaxonName || inCodedDescription) {
					currentItemDetail = buffer.toString();
				} else if (inCharacterTree && isFirstCharacterTree) {
					if (currentDescriptorTree.getDetail() == null)
						currentDescriptorTree.setDetail(buffer.toString());
				} else if (inTaxonHierarchy && isFirstTaxonHierarchy) {
					if (currentItemTree.getDetail() == null)
						currentItemTree.setDetail(buffer.toString());
				} else if (isDatasetDetail) {
					this.dataset.setDetail(buffer.toString());
					isDatasetDetail = false;
				} else if (inMediaObject) {
					// this
				}
			}

			// <Label> in <MeasurementUnit> in <Dataset>
			else if (localName.equals("Label") && inMeasurementUnit && inDataset) {
				if (currentQuantitativeCharacter != null)
					currentQuantitativeCharacter.setMeasurementUnit(buffer.toString());
			}

			// <ConceptStates>
			else if (localName.equals("ConceptStates"))
				inConceptStates = false;

			// <StateDefinition> in <ConceptStates>
			else if (localName.equals("StateDefinition") && inConceptStates) {

			}

			// <Characters>
			else if (localName.equals("Characters"))
				inCharacters = false;

			// <CategoricalCharacter> in <Characters>
			else if (localName.equals("CategoricalCharacter") && inCharacters) {
				inCategoricalCharacter = false;
				this.dataset.addDescriptor(currentCategoricalCharacter);
				currentCategoricalCharacter = null;
			}

			// <Representation> in <CategoricalCharacter> (NOT in <States>)
			else if (localName.equals("Representation") && inCategoricalCharacter && !inStates) {
				inRepresentation = false;
			}

			// <QuantitativeCharacter> in <Characters>
			else if (localName.equals("QuantitativeCharacter") && inCharacters) {
				inQuantitativeCharacter = false;
				this.dataset.addDescriptor(currentQuantitativeCharacter);
				currentQuantitativeCharacter = null;
			}

			// <Representation> in <QuantitativeCharacter> (NOT in <States>)
			else if (localName.equals("Representation") && inQuantitativeCharacter && !inStates) {
				inRepresentation = false;
			}

			// <States>
			else if (localName.equals("States"))
				inStates = false;

			// <StateDefinition> in <States>
			else if (localName.equals("StateDefinition") && inStates) {
				inStateDefinition = false;
				currentCategoricalCharacter.addState(currentState);
				currentState = null;
			}

			// <StateReference> in <States>
			else if (localName.equals("StateReference") && inStates)
				inStatesReference = false;

			// <ConceptState> in <StateReference>
			else if (localName.equals("ConceptState") && inStatesReference) {

			}

			// <CharacterTree>
			else if (localName.equals("CharacterTree")) {
				if (isFirstCharacterTree && isDependencyCharacterHierarchy) {
					if (currentDescriptorTree.getName() == null) {
						currentDescriptorTree.setName("");
					}
					dataset.addDescriptorTree(currentDescriptorTree);
				}
				isFirstCharacterTree = false;
				inCharacterTree = false;
				currentDescriptorTree = null;
			}

			// <Nodes>
			else if (localName.equals("Nodes")) {
				inNodes = false;
			}

			// <CharNode> in <Nodes>
			else if (localName.equals("CharNode") && inNodes) {
				inCharNode = false;
				if (currentInapplicableStates.size() > 0) {
					isDependencyCharacterHierarchy = true;
					currentDescriptorNode.setParentNode(Dataset.getDescriptorNodeByState(
							currentDescriptorTree, currentInapplicableStates.get(0)));
					Descriptor d = currentDescriptorNode.getDescriptor();
					if (d.isCategoricalType()) {
						((ExtCategoricalDescriptor) d).setParentDescriptor(Dataset.getDescriptorNodeByState(
								currentDescriptorTree, currentInapplicableStates.get(0)).getDescriptor());
						((ExtCategoricalDescriptor) d).setInapplicableStates(currentInapplicableStates);
					} else if (d.isQuantitativeType()) {
						((ExtQuantitativeDescriptor) d).setParentDescriptor(Dataset.getDescriptorNodeByState(
								currentDescriptorTree, currentInapplicableStates.get(0)).getDescriptor());

						((ExtQuantitativeDescriptor) d).setInapplicableStates(currentInapplicableStates);
					}
					currentDescriptorNode.setInapplicableStates(currentInapplicableStates);
				} else if (currentOnlyApplicableStates.size() > 0) {
					isDependencyCharacterHierarchy = true;
					DescriptorNode descriptorNode = Dataset.getDescriptorNodeByState(currentDescriptorTree,
							currentInapplicableStates.get(0));
					if (descriptorNode.getDescriptor() != null
							&& descriptorNode.getDescriptor().isCategoricalType()) {
						currentDescriptorNode.setParentNode(Dataset.getDescriptorNodeByState(
								currentDescriptorTree, currentInapplicableStates.get(0)));
						List<model.State> tempList = new ArrayList<model.State>(
								((ExtCategoricalDescriptor) descriptorNode.getDescriptor()).getStates());
						tempList.removeAll(currentOnlyApplicableStates);
						currentDescriptorNode.getInapplicableStates().addAll(tempList);

						currentDescriptorNode.setInapplicableStates(tempList);
						Descriptor d = currentDescriptorNode.getDescriptor();
						if (d.isCategoricalType()) {
							((ExtCategoricalDescriptor) d).getInapplicableStates().addAll(tempList);
							((ExtCategoricalDescriptor) d).setInapplicableStates(tempList);
						} else if (d.isQuantitativeType()) {
							((ExtQuantitativeDescriptor) d).getInapplicableStates().addAll(tempList);
							((ExtQuantitativeDescriptor) d).setInapplicableStates(tempList);
						}
					}
				}
				currentInapplicableStates = null;
				currentOnlyApplicableStates = null;
				currentDescriptorNode = null;
			}

			// <DependencyRules> in <CharNode>
			else if (localName.equals("DependencyRules") && inCharNode) {
				inDependencyRules = false;
			}

			// <InapplicableIf> in <DependencyRules>
			else if (localName.equals("InapplicableIf") && inDependencyRules) {
				inInapplicableIf = false;
			}

			// <OnlyApplicableIf> in <DependencyRules>
			else if (localName.equals("OnlyApplicableIf") && inDependencyRules) {
				inOnlyApplicableIf = false;

			}

			// <State> in <InapplicableIf> in <CharacterTree> && isFirstCharacterTree
			else if (localName.equals("State") && inInapplicableIf && inCharacterTree && isFirstCharacterTree) {

			}

			// <State> in <OnlyApplicableIf> in <CharacterTree> && isFirstCharacterTree
			else if (localName.equals("State") && inOnlyApplicableIf && inCharacterTree
					&& isFirstCharacterTree) {

			}

			// <Character> in <CharNode> in <CharacterTree> && isFirstCharacterTree
			else if (localName.equals("Character") && inCharNode && inCharacterTree && isFirstCharacterTree) {

			}

			// <TaxonNames>
			else if (localName.equals("TaxonNames")) {
				inTaxonNames = false;
			}

			// <TaxonName> in <TaxonNames>
			else if (localName.equals("TaxonName") && inTaxonNames) {
				inTaxonName = false;
				currentItem = null;
			}

			// <CodedDescriptions>
			else if (localName.equals("CodedDescriptions")) {
				inCodedDescriptions = false;
			}

			// <CodedDescription> in <CodedDescriptions>
			else if (localName.equals("CodedDescription") && inCodedDescriptions) {
				inCodedDescription = false;
				currentItem.setDescription(currentDescription);
				this.dataset.getItems().add(currentItem);
				currentItem = null;
				currentCodedDescriptionLabel = null;
			}

			// <Representation> in <CodedDescription> in <CodedDescriptions>
			else if (localName.equals("Representation") && inCodedDescription && inCodedDescriptions) {
				inRepresentation = false;
			}

			// <Scope>
			else if (localName.equals("Scope")) {
				inScope = false;
			}

			// <TaxonName> in <Scope>
			else if (localName.equals("TaxonName") && inScope) {

			}

			// <SummaryData>
			else if (localName.equals("SummaryData")) {
				inSummaryData = false;
				currentItemDetail = null;
			}

			// <Categorical> in <SummaryData>
			else if (localName.equals("Categorical") && inSummaryData) {
				inCategorical = false;
				DescriptionElementState descriptionElementState = new DescriptionElementState();
				if (dataUnavailableFlag) {
					descriptionElementState.setUnknown(true);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							descriptionElementState);
				} else {
					descriptionElementState.setStates(currentStatesList);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							descriptionElementState);
				}
				currentDescriptionDescriptor = null;
				currentStatesList = null;
				dataUnavailableFlag = false;
			}

			// <Quantitative> in <SummaryData>
			else if (localName.equals("Quantitative") && inSummaryData) {
				inQuantitative = false;
				DescriptionElementState descriptionElementState = new DescriptionElementState();
				if (dataUnavailableFlag) {
					descriptionElementState.setUnknown(true);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							descriptionElementState);
				} else {
					descriptionElementState.setQuantitativeMeasure(currentQuantitativeMeasure);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							descriptionElementState);
				}
				currentDescriptionDescriptor = null;
				currentQuantitativeMeasure = null;
				dataUnavailableFlag = false;
			}

			// <Measure> in <Quantitative>
			else if (localName.equals("Measure") && inQuantitative) {

			}

			// <MeasurementUnit>
			else if (localName.equals("MeasurementUnit")) {
				inMeasurementUnit = false;

			}

			// <Ratings>
			else if (localName.equals("Ratings")) {
				inRatings = false;
			}

			// <TaxonHierarchies>
			else if (localName.equals("TaxonHierarchies")) {
				inTaxonHierarchies = false;
			}

			// <TaxonHierarchy> in <TaxonHierarchies>
			else if (localName.equals("TaxonHierarchy") && inTaxonHierarchies) {
				if (isFirstTaxonHierarchy == true) {
					// set the parent node for each itemNode
					for (String itemNodeKey : parentItemNodeIdKey.keySet()) {
						ItemNode itemNode = itemNodeIdKey.get(itemNodeKey);
						ItemNode parentNode = itemNodeIdKey.get(parentItemNodeIdKey.get(itemNodeKey));
						itemNode.setParentNode(parentNode);
					}
					dataset.addItemTree(currentItemTree);
				}
				inTaxonHierarchy = false;
				isFirstTaxonHierarchy = false;

			}

			// <Node> in <Nodes> in <TaxonHierarchy>
			else if (localName.equals("Node") && inNodes && inTaxonHierarchy) {
				inNode = false;
				currentItemNode = null;
				curentItemNodeIdKey = null;
			}

			// <Agents> in <Dataset>
			else if (localName.equals("Agents") && inDataset) {
				inAgents = false;
			}

			// <Agents> in <Dataset>
			else if (localName.equals("Agent") && inDataset && inAgents) {
				inAgent = false;
			}

			// <MediaObject>
			else if (localName.equals("MediaObject")) {
				inMediaObject = false;
				isImageType = false;
				isVideoType = false;
				isSoundType = false;
				mediaObjectId = null;
			}

			// <Type> in <MediaObject>
			else if (localName.equals("Type") && inMediaObject && mediaObjectId != null) {
				if (buffer != null && buffer.toString().equalsIgnoreCase("image")) {
					isImageType = true;
				} else {
					isImageType = false;
				}
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String data = new String(ch, start, length);

		// replace all the \t character by space. It corresponds to the xml indentation characters.
		data = data.replaceAll("\t", " ");
		data = data.replaceAll("\n", " ");
		data = data.replaceAll("[\\s]+", " ");
		if (this.buffer != null) {
			this.buffer.append(data);
		}
	}

	@Override
	public void endDocument() throws SAXException {

	}

	@Override
	public void endPrefixMapping(String arg0) throws SAXException {

	}

	@Override
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {

	}

	@Override
	public void processingInstruction(String arg0, String arg1) throws SAXException {

	}

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	@Override
	public void skippedEntity(String arg0) throws SAXException {

	}

	@Override
	public void startDocument() throws SAXException {

	}

	@Override
	public void startPrefixMapping(String arg0, String arg1) throws SAXException {

	}

	/**
	 * @return the dataset
	 */
	public Dataset getDataset() {
		return dataset;
	}

	/**
	 * @param dataset
	 */
	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

}
