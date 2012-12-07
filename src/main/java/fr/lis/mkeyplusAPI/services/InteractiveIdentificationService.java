package fr.lis.mkeyplusAPI.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import model.CategoricalDescriptor;
import model.Description;
import model.Descriptor;
import model.DescriptorNode;
import model.DescriptorTree;
import model.Item;
import model.QuantitativeDescriptor;
import model.QuantitativeMeasure;
import model.State;
import services.DescriptorTreeManagementService;
import utils.Utils;

/**
 * This class contains all the methods necessary to perform an Interactive Identification
 * 
 * @author Thomas Burguiere
 * 
 */
/**
 * @author Thomas Burguiere
 * 
 */
public class InteractiveIdentificationService {

	// private static DescriptorTree dependencyTree;

	public static final int XPER_SORT = 0;
	public static final int SOKAL_MICHENER = 1;
	public static final int JACCARD = 2;

	// true, dbName, login, password);

	/**
	 * returns a {@link LinkedHashMap} containing in keys the descriptors, and in values their discriminant
	 * power. This map is sorted by the discriminant power of the descriptors, in descending order
	 * 
	 * @param descriptors
	 * @param items
	 * @param dbName
	 * @param login
	 * @param password
	 * @param scoreMethod
	 * @param considerChildScore
	 * @return
	 * @throws Exception
	 */
	public static LinkedHashMap<Descriptor, Float> getDescriptorsScoreMap(List<Descriptor> descriptors,
			List<Item> items, String dbName, String login, String password, int scoreMethod,
			boolean considerChildScore) throws Exception {
		LinkedHashMap<Descriptor, Float> descriptorsScoresMap = new LinkedHashMap<Descriptor, Float>();
		DescriptorTree dependencyTree = DescriptorTreeManagementService.read(DescriptorTree.DEPENDENCY_TYPE,
				true, dbName, login, password);

		if (items.size() > 1) {
			HashMap<Descriptor, Float> tempMap = new HashMap<Descriptor, Float>();
			float discriminantPower = -1;
			for (Descriptor descriptor : descriptors) {
				if (!descriptor.isCalculatedType()) {
					if (descriptor.isCategoricalType()
							&& ((CategoricalDescriptor) descriptor).getStates().size() <= 0)
						discriminantPower = 0;
					else {
						if (descriptor.isCategoricalType()) {
							discriminantPower = categoricalDescriptorScore(
									(CategoricalDescriptor) descriptor, items, dbName, login, password,
									dependencyTree, 0);
						} else if (descriptor.isQuantitativeType())
							discriminantPower = quantitativeDescriptorScore(
									(QuantitativeDescriptor) descriptor, items, dbName, login, password,
									scoreMethod, dependencyTree);

						if (considerChildScore) {
							// asserting the discrimant power of the child descriptors (if any) and setting
							// the
							// discriminant power of a child node to its father, if it is greater
							float tempDiscriminantPower = 0;
							discriminantPower = considerChildNodeDiscriminantPower(descriptors, items,
									dbName, login, password, scoreMethod, dependencyTree, discriminantPower,
									descriptor, tempDiscriminantPower);
						}

						tempMap.put(descriptor, new Float(discriminantPower));
					}
				}
			}

			// sorting the final LinkedHashMap
			List<Float> mapValues = new ArrayList<Float>(tempMap.values());
			Collections.sort(mapValues, Collections.reverseOrder());

			for (Float dpScore : mapValues) {
				for (Descriptor desc : tempMap.keySet()) {
					float dp1 = tempMap.get(desc);
					float dp2 = dpScore;

					if (dp1 == dp2)
						descriptorsScoresMap.put(desc, dpScore);

				}
			}

		} else {
			for (Descriptor descriptor : descriptors)
				descriptorsScoresMap.put(descriptor, new Float(-1));
		}

		return descriptorsScoresMap;
	}

	/**
	 * @param descriptors
	 * @param items
	 * @param dbName
	 * @param login
	 * @param password
	 * @param scoreMethod
	 * @param dependencyTree
	 * @param discriminantPower
	 * @param descriptor
	 * @param tempDiscriminantPower
	 * @return
	 * @throws Exception
	 */
	private static float considerChildNodeDiscriminantPower(List<Descriptor> descriptors, List<Item> items,
			String dbName, String login, String password, int scoreMethod, DescriptorTree dependencyTree,
			float discriminantPower, Descriptor descriptor, float tempDiscriminantPower) throws Exception {
		for (DescriptorNode childNode : dependencyTree.getNodeContainingDescriptor(
				descriptor.getId()).getChildNodes()) {
			Descriptor childDescriptorInList = null;
			long childDescriptorId = childNode.getDescriptor().getId();
			for (Descriptor temp : descriptors)
				if (childDescriptorId == temp.getId())
					childDescriptorInList = temp;

			if (childDescriptorInList.isCategoricalType()) {
				tempDiscriminantPower = categoricalDescriptorScore(
						(CategoricalDescriptor) childDescriptorInList, items, dbName,
						login, password, dependencyTree, scoreMethod);
			} else if (childDescriptorInList.isQuantitativeType()) {
				tempDiscriminantPower = quantitativeDescriptorScore(
						(QuantitativeDescriptor) childDescriptorInList, items, dbName,
						login, password, scoreMethod, dependencyTree);
			}
			if (tempDiscriminantPower > discriminantPower)
				discriminantPower = tempDiscriminantPower;
		}
		return discriminantPower;
	}

	/**
	 * @param descriptions
	 */
	public static void getRemainingItems(Description descriptions, String dbName, String login,
			String password) {
		List<Item> remainingItems = new ArrayList<Item>();
		for (Descriptor descriptor : descriptions.getDescriptionElements().keySet()) {
			if (descriptor.isCategoricalType()) {
				List<State> selectedStates = descriptions.getDescriptionElement(descriptor.getId())
						.getStates();
				
			} else if (descriptor.isQuantitativeType()) {
				QuantitativeMeasure measure = descriptions.getDescriptionElement(descriptor.getId())
						.getQuantitativeMeasure();
				
			}
		}
	}

	/**
	 * @param descriptor
	 * @param remainingItems
	 * @param dbName
	 * @param login
	 * @param password
	 * @param dependencyTree
	 * @param scoreMethod
	 * @return
	 * @throws Exception
	 */
	public static float categoricalDescriptorScore(CategoricalDescriptor descriptor,
			List<Item> remainingItems, String dbName, String login, String password,
			DescriptorTree dependencyTree, int scoreMethod) throws Exception {
		int cpt = 0;
		float score = 0;
		boolean isAlwaysDescribed = true;
		DescriptorNode node = dependencyTree.getNodeContainingDescriptor(descriptor.getId());

		for (int i = 0; i < remainingItems.size() - 1; i++) {
			for (int j = i + 1; j < remainingItems.size(); j++) {

				if (remainingItems.get(i).getDescription() != null
						&& remainingItems.get(j).getDescription() != null) {

					// if the descriptor is applicable for both of these items
					if ((!isInapplicable(node, remainingItems.get(i)) && !isInapplicable(node,
							remainingItems.get(j)))) {

						List<State> statesList1 = remainingItems.get(i)
								.getDescriptionElement(descriptor.getId()).getStates();
						List<State> statesList2 = remainingItems.get(j)
								.getDescriptionElement(descriptor.getId()).getStates();

						// if at least one description is empty for the current character
						if ((statesList1 != null && statesList1.size() == 0)
								|| (statesList2 != null && statesList2.size() == 0)) {
							isAlwaysDescribed = false;
						}

						// if one description is unknown and the other have 0 state checked
						if ((statesList1 == null && statesList2 != null && statesList2.size() == 0)
								|| (statesList2 == null && statesList1 != null && statesList1.size() == 0)) {
							score++;
						} else if (statesList1 != null && statesList2 != null) {

							// nb of common states which are absent
							float commonAbsent = 0;
							// nb of common states which are present
							float commonPresent = 0;
							float other = 0;

							// search common state
							for (int k = 0; k < descriptor.getStates().size(); k++) {
								State state = descriptor.getStates().get(k);

								if (statesList1.contains(state)) {
									if (statesList2.contains(state)) {
										commonPresent++;
									} else {
										other++;
									}
									// !(statesList2.contains(state))
								} else {
									if (statesList2.contains(state)) {
										other++;
									} else {
										commonAbsent++;
									}
								}
							}
							score += applyScoreMethod(commonPresent, commonAbsent, other, 0);
						}
						cpt++;
					}

				}
			}
		}

		if (cpt >= 1) {
			score = score / cpt;
		}

		// increasing artificially the score of character containing only described taxa
		// if (isAlwaysDescribed && score > 0) {
		// score = (float) ((float) score + (float) 2.0);
		// }

		// fewStatesCharacterFirst option handling
		// if (utils.isFewStatesCharacterFirst() && score > 0 && character.getStates().size() >= 2) {
		// // increasing artificially score of character with few states
		// float coeff = (float) 1
		// - ((float) character.getStates().size() / (float) maxNbStatesPerCharacter);
		// score = (float) (score + coeff);
		// }

		return score;
	}

	/**
	 * @param descriptor
	 * @param remainingItems
	 * @param scoreMethod
	 * @param descriptorAlreadyUsed
	 * @return
	 * @throws Exception
	 */
	public static float quantitativeDescriptorScore(QuantitativeDescriptor descriptor,
			List<Item> remainingItems, String dbName, String login, String password, int scoreMethod,
			DescriptorTree dependencyTree) throws Exception {

		int cpt = 0;
		float score = 0;
		boolean isAlwaysDescribed = true;
		DescriptorNode node = dependencyTree.getNodeContainingDescriptor(descriptor.getId());

		for (int i = 0; i < remainingItems.size() - 1; i++) {
			for (int j = i + 1; j < remainingItems.size(); j++) {
				// if the descriptor is applicable for both of these items
				if ((!isInapplicable(node, remainingItems.get(i)) && !isInapplicable(node,
						remainingItems.get(j)))) {

					float tmp = -1;

					tmp = applyScoreMethodNum(remainingItems.get(i), remainingItems.get(j), descriptor,
							scoreMethod);

					if (tmp >= 0) {
						score += tmp;
						cpt++;
						// }
					}

				}
			}
		}

		return score;
	}

	/**
	 * @param commonPresent
	 * @param commonAbsent
	 * @param other
	 * @param scoreMethod
	 * @return float, the score using the method requested
	 */
	private static float applyScoreMethod(float commonPresent, float commonAbsent, float other,
			int scoreMethod) {

		float out = 0;

		// // Sokal & Michener method
		if (scoreMethod == SOKAL_MICHENER) {
			out = 1 - ((commonPresent + commonAbsent) / (commonPresent + commonAbsent + other));
			// round to 10^-3
			out = Utils.roundFloat(out, 3);
		}
		// // Jaccard Method
		else if (scoreMethod == SOKAL_MICHENER) {
			try {
				// // case where description are empty
				out = 1 - (commonPresent / (commonPresent + other));
				// // round to 10^-3
				out = Utils.roundFloat(out, 3);
			} catch (ArithmeticException a) {
				out = 0;
			}
		}
		// // yes or no method (Xper)
		else {
			if ((commonPresent == 0) && (other > 0)) {
				out = 1;
			} else {
				out = 0;
			}
		}
		return out;
	}

	/**
	 * @param item1
	 * @param item2
	 * @param descriptor
	 * @param scoreMethod
	 * @return
	 */
	private static float applyScoreMethodNum(Item item1, Item item2, QuantitativeDescriptor descriptor,
			int scoreMethod) {
		float out = 0;

		float commonPercentage = 0; // percentage of common values which are shared

		QuantitativeMeasure quantitativeMeasure1 = item1.getDescription()
				.getDescriptionElement(descriptor.getId()).getQuantitativeMeasure();
		QuantitativeMeasure quantitativeMeasure2 = item2.getDescription()
				.getDescriptionElement(descriptor.getId()).getQuantitativeMeasure();
		if (quantitativeMeasure1 == null || quantitativeMeasure2 == null) {
			return 0;
		} else {
			if (quantitativeMeasure1.getCalculateMinimum() == null
					|| quantitativeMeasure1.getCalculateMaximum() == null
					|| quantitativeMeasure2.getCalculateMinimum() == null
					|| quantitativeMeasure2.getCalculateMaximum() == null) {
				return 0;
			} else {
				commonPercentage = calculateCommonPercentage(quantitativeMeasure1.getCalculateMinimum()
						.doubleValue(), quantitativeMeasure1.getCalculateMaximum().doubleValue(),
						quantitativeMeasure2.getCalculateMinimum().doubleValue(), quantitativeMeasure2
								.getCalculateMaximum().doubleValue());
			}

		}

		if (commonPercentage <= 0) {
			commonPercentage = 0;
		}

		switch (scoreMethod) {
		case XPER_SORT:
			if ((commonPercentage <= 0)) {
				out = 1;
			} else {
				out = 0;
			}
			break;
		//
		case SOKAL_MICHENER:
			out = 1 - (commonPercentage / 100);
			break;
		//
		case JACCARD:
			out = 1 - (commonPercentage / 100);
			break;

		default:
			if ((commonPercentage <= 0)) {
				out = 1;
			} else {
				out = 0;
			}
			break;
		}

		return out;
	}

	/**
	 * @param min1
	 * @param max1
	 * @param min2
	 * @param max2
	 * @return
	 */
	private static float calculateCommonPercentage(double min1, double max1, double min2, double max2) {
		double minLowerTmp = 0;
		double maxUpperTmp = 0;
		double minUpperTmp = 0;
		double maxLowerTmp = 0;
		float res = 0;

		if (min1 <= min2) {
			minLowerTmp = min1;
			minUpperTmp = min2;
		} else {
			minLowerTmp = min2;
			minUpperTmp = min1;
		}

		if (max1 >= max2) {
			maxUpperTmp = max1;
			maxLowerTmp = max2;
		} else {
			maxUpperTmp = max2;
			maxLowerTmp = max1;
		}

		res = new Double((maxLowerTmp - minUpperTmp) / (maxUpperTmp - minLowerTmp)).floatValue();

		if (res < 0) {
			res = 0;
		}
		return res;
	}

	private static boolean isInapplicable(DescriptorNode descriptorNode, Item item) {
		if (descriptorNode.getParentNode() != null) {
			for (State state : descriptorNode.getInapplicableStates()) {
				if (item.getDescriptionElement(descriptorNode.getParentNode().getDescriptor().getId())
						.containsState(state.getId())) {
					return true;
				}
			}
			return isInapplicable(descriptorNode.getParentNode(), item);
		}
		return false;
	}

	/**
	 * @param descriptorNode
	 * @return
	 */
	private float getMaximumChildDescriptorScore(DescriptorNode descriptorNode, List<Item> remainingItems) {
		float max = -1;
		List<DescriptorNode> childrenDescriptorNodes = descriptorNode.getChildNodes();
		if (descriptorNode.getParentNode() != null)
			max = -1;
		else {
			for (DescriptorNode childDescriptorNode : childrenDescriptorNodes) {
				Descriptor childDescriptor = childDescriptorNode.getDescriptor();
			}
		}

		return max;
	}

}
