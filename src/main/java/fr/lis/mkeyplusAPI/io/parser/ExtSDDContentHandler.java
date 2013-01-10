package fr.lis.mkeyplusAPI.io.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import model.Dataset;
import model.DescriptionElementState;
import model.Descriptor;
import model.DescriptorNode;
import model.State;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import IO.parser.SDDContentHandler;
import fr.lis.mkeyplusAPI.model.ExtCategoricalDescriptor;
import fr.lis.mkeyplusAPI.model.ExtQuantitativeDescriptor;

public class ExtSDDContentHandler extends SDDContentHandler implements ContentHandler {
	public ExtSDDContentHandler() {
		super();
		this.dataset = new Dataset();
		this.ratingsCounter = new HashMap<Descriptor, Integer>();
	}

	public void startElement(String nameSpaceURI, String localName, String rawName, Attributes attributes)
			throws SAXException {

		if (isFirstDataset) {
			// <CategoricalCharacter> in <Characters>
			if (localName.equals("CategoricalCharacter") && inCharacters) {
				inCategoricalCharacter = true;
				currentCategoricalCharacter = new ExtCategoricalDescriptor();
				descriptorIdKey.put(attributes.getValue("id"), currentCategoricalCharacter);
			}

			// <QuantitativeCharacter> in <Characters>
			else if (localName.equals("QuantitativeCharacter") && inCharacters) {
				inQuantitativeCharacter = true;
				currentQuantitativeCharacter = new ExtQuantitativeDescriptor();
				descriptorIdKey.put(attributes.getValue("id"), currentQuantitativeCharacter);
			} else
				super.startElement(nameSpaceURI, localName, rawName, attributes);
		}
	}

	public void endElement(String nameSpaceURI, String localName, String rawName) throws SAXException {
		if (isFirstDataset) {
			// <CharNode> in <Nodes>
			if (localName.equals("CharNode") && inNodes) {
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
						List<model.State> tempList = new ArrayList<State>(
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

			// <Categorical> in <SummaryData>
			else if (localName.equals("Categorical") && inSummaryData) {
				inCategorical = false;
				currentDescriptionElementState = new DescriptionElementState();
				if (dataUnavailableFlag) {
					currentDescriptionElementState.setUnknown(true);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							currentDescriptionElementState);
				} else {
					currentDescriptionElementState.setStates(currentStatesList);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							currentDescriptionElementState);
				}
				currentDescriptionDescriptor = null;
				currentStatesList = null;
				dataUnavailableFlag = false;
			}

			// <Quantitative> in <SummaryData>
			else if (localName.equals("Quantitative") && inSummaryData) {
				inQuantitative = false;
				 currentDescriptionElementState = new DescriptionElementState();
				if (dataUnavailableFlag) {
					currentDescriptionElementState.setUnknown(true);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							currentDescriptionElementState);
				} else {
					currentDescriptionElementState.setQuantitativeMeasure(currentQuantitativeMeasure);
					currentDescription.addDescriptionElement(currentDescriptionDescriptor,
							currentDescriptionElementState);
				}
				currentDescriptionDescriptor = null;
				currentQuantitativeMeasure = null;
				dataUnavailableFlag = false;
			}

			else
				super.endElement(nameSpaceURI, localName, rawName);

		}
	}

}
