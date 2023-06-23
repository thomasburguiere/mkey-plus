package fr.lis.mkeyplusAPI.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.Description;
import fr.lis.xper3API.model.DescriptionElementState;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.DescriptorNode;
import fr.lis.xper3API.model.DescriptorTree;
import fr.lis.xper3API.model.Item;
import fr.lis.xper3API.model.QuantitativeDescriptor;
import fr.lis.xper3API.model.QuantitativeMeasure;
import fr.lis.xper3API.model.State;
import fr.lis.xper3API.utils.Utils;


/**
 * This class contains all the methods necessary to perform an Interactive Identification it is closely based
 * on the DiscriminatingPower class from Xper2
 * 
 * @author Thomas Burguiere
 */
public class InteractiveIdentificationService {

	public static final int SCORE_XPER = 0;
	public static final int SCORE_SOKAL_MICHENER = 1;
	public static final int SCORE_JACCARD = 2;

	public static final int LOGICAL_OPERATOR_AND = 3;
	public static final int LOGICAL_OPERATOR_OR = 4;

	public static final int COMPARISON_OPERATOR_GREATER_THAN = 5;
	public static final int COMPARISON_OPERATOR_STRICTLY_GREATER_THAN = 6;
	public static final int COMPARISON_OPERATOR_LOWER_THAN = 7;
	public static final int COMPARISON_OPERATOR_STRICTLY_LOWER_THAN = 8;
	public static final int COMPARISON_OPERATOR_CONTAINS = 9;
	public static final int COMPARISON_OPERATOR_DOES_NOT_CONTAIN = 10;

	// DISCRIMINANT POWER FUNCTIONS

	/**
	 * returns a {@link LinkedHashMap} containing in keys the descriptors, and in values their discriminant
	 * power. This map is sorted by the discriminant power of the descriptors, in descending order.
	 * Multi-Thread algorithm
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
	public static LinkedHashMap<Descriptor, Float> getDescriptorsScoreMapFuture(
		final List<Descriptor> descriptors,
		final List<Item> items, final DescriptorTree dependencyTree, final int scoreMethod, final boolean considerChildScores,
		final DescriptionElementState[][] descriptionMatrix, final DescriptorNode[] descriptorNodeMap,
		final boolean withGlobalWeigth) {
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final LinkedHashMap<Descriptor, Float> descriptorsScoresMap = new LinkedHashMap<Descriptor, Float>();

		if (items.size() > 1) {
			@SuppressWarnings("unchecked") final Future<Object[]>[] futures = new Future[descriptors.size()];
			int i = 0;
			for (final Descriptor descriptor : descriptors) {
				futures[i] = exec.submit(new ThreadComputDescriptorsScoreMap(items, dependencyTree,
						scoreMethod, considerChildScores, descriptor, descriptionMatrix, descriptorNodeMap,
						withGlobalWeigth));
				i++;
			}
			try {
				for (final Future<Object[]> fute : futures) {
					final Object[] result = fute.get();
					descriptorsScoresMap.put((Descriptor) result[0], (Float) result[1]);
				}
				// InteractiveIdentificationService.exec.shutdown();

			} catch (final InterruptedException e) {
				e.printStackTrace();
			} catch (final ExecutionException ex) {
				ex.printStackTrace();
			} finally {
				exec.shutdown();
				exec = null;
			}
		} else {
			for (final Descriptor descriptor : descriptors) {
				descriptorsScoresMap.put(descriptor, new Float(-1));
			}
		}

		return descriptorsScoresMap;
	}

	/**
	 * returns a {@link LinkedHashMap} containing in keys the descriptors, and in values their discriminant
	 * power. This map is sorted by the discriminant power of the descriptors, in descending order Mono-Thread
	 * algorithm
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
	public static LinkedHashMap<Descriptor, Float> getDescriptorsScoreMap(
		final List<Descriptor> descriptors,
		final List<Item> items, final DescriptorTree dependencyTree, final int scoreMethod, final boolean considerChildScores,
		final DescriptionElementState[][] descriptionMatrix, final DescriptorNode[] descriptorNodeMap,
		final boolean withGlobalWeigth) {
		final LinkedHashMap<Descriptor, Float> descriptorsScoresMap = new LinkedHashMap<Descriptor, Float>();

		if (items.size() > 1) {
			final HashMap<Descriptor, Float> tempMap = new HashMap<Descriptor, Float>();
			float discriminantPower = -1;

			for (final Descriptor descriptor : descriptors) {
				if (descriptor.isCategoricalType()
						&& ((CategoricalDescriptor) descriptor).getStates().size() <= 0) {
					discriminantPower = 0;
				} else {
					discriminantPower = getDiscriminantPower(descriptor, items, 0, scoreMethod,
							considerChildScores, dependencyTree, descriptionMatrix, descriptorNodeMap,
							withGlobalWeigth);
				}

				tempMap.put(descriptor, discriminantPower);
			}

			// sorting the final LinkedHashMap
			final List<Float> mapValues = new ArrayList<Float>(tempMap.values());
			Collections.sort(mapValues, Collections.reverseOrder());

			for (final Float dpScore : mapValues) {
				for (final Descriptor desc : tempMap.keySet()) {
					final float dp1 = tempMap.get(desc);
					final float dp2 = dpScore;

					if (dp1 == dp2) {
						descriptorsScoresMap.put(desc, dpScore);
					}
				}
			}
		} else {
			for (final Descriptor descriptor : descriptors) {
				descriptorsScoresMap.put(descriptor, new Float(-1));
			}
		}

		return descriptorsScoresMap;
	}

	@Deprecated
	private static class DescriptorScoreMapRunnable implements Runnable {
		private final List<Descriptor> descriptorList;
		private final List<Item> items;
		private final int scoreMethod;
		private final boolean considerChildScores;
		private final DescriptorTree dependencyTree;
		private final HashMap<Descriptor, Float> tempMap;
		private final DescriptionElementState[][] descriptionMatrix;
		private final boolean withGlobalWeight;

		public DescriptorScoreMapRunnable(
			final List<Descriptor> descriptorList, final List<Item> items, final int scoreMethod,
			final boolean considerChildScores, final DescriptorTree dependencyTree,
			final DescriptionElementState[][] descriptionMatrix, final boolean withGlobalWeight) {
			this.descriptorList = descriptorList;
			this.items = items;
			this.scoreMethod = scoreMethod;
			this.considerChildScores = considerChildScores;
			this.dependencyTree = dependencyTree;
			this.tempMap = new HashMap<Descriptor, Float>();
			this.descriptionMatrix = descriptionMatrix;
			this.withGlobalWeight = withGlobalWeight;
		}

		@Override
		public void run() {
			float discriminantPower = -1;
			for (final Descriptor descriptor : descriptorList) {
				if (descriptor.isCategoricalType()
						&& ((CategoricalDescriptor) descriptor).getStates().size() <= 0) {
					discriminantPower = 0;
				} else {
					discriminantPower = getDiscriminantPower(descriptor, items, 0, scoreMethod,
							considerChildScores, dependencyTree, descriptionMatrix, null, withGlobalWeight);
				}
				tempMap.put(descriptor, discriminantPower);
			}
		}

		public HashMap<Descriptor, Float> getTempMap() {
			return tempMap;
		}
	}

	/**
	 * 
	 * returns a {@link LinkedHashMap} containing in keys the descriptors, and in values their discriminant
	 * power. This map is sorted by the discriminant power of the descriptors, in descending order. The map is
	 * generated using 4 separate threads, each working on sublist of descriptors
	 * 
	 * @deprecated
	 * @param descriptors
	 * @param items
	 * @param dependencyTree
	 * @param scoreMethod
	 * @param considerChildScores
	 * @return
	 * @throws InterruptedException
	 */
	public static LinkedHashMap<Descriptor, Float> getDescriptorsScoreMapUsing4Threads(
		final List<Descriptor> descriptors, final List<Item> items, final DescriptorTree dependencyTree, final int scoreMethod,
		final boolean considerChildScores, final DescriptionElementState[][] descriptionMatrix)
			throws InterruptedException {
		final LinkedHashMap<Descriptor, Float> descriptorsScoresMap = new LinkedHashMap<Descriptor, Float>();

		if (items.size() > 1) {

			final int quarter = descriptors.size() / 4;
			final List<Descriptor> descriptorList1 = descriptors.subList(0, quarter);
			final List<Descriptor> descriptorList2 = descriptors.subList(quarter, quarter * 2);
			final List<Descriptor> descriptorList3 = descriptors.subList(quarter * 2, quarter * 3);
			final List<Descriptor> descriptorList4 = descriptors.subList(quarter * 3, descriptors.size());
			// descriptors.

			final HashMap<Descriptor, Float> tempMap = new HashMap<Descriptor, Float>();

			HashMap<Descriptor, Float> tempMap1 = new HashMap<Descriptor, Float>();
			HashMap<Descriptor, Float> tempMap2 = new HashMap<Descriptor, Float>();
			HashMap<Descriptor, Float> tempMap3 = new HashMap<Descriptor, Float>();
			HashMap<Descriptor, Float> tempMap4 = new HashMap<Descriptor, Float>();

			final DescriptorScoreMapRunnable r1 = new DescriptorScoreMapRunnable(descriptorList1, items,
					scoreMethod, considerChildScores, dependencyTree, descriptionMatrix, false);
			final Thread t1 = new Thread(r1);

			final DescriptorScoreMapRunnable r2 = new DescriptorScoreMapRunnable(descriptorList2, items,
					scoreMethod, considerChildScores, dependencyTree, descriptionMatrix, false);
			final Thread t2 = new Thread(r2);

			final DescriptorScoreMapRunnable r3 = new DescriptorScoreMapRunnable(descriptorList3, items,
					scoreMethod, considerChildScores, dependencyTree, descriptionMatrix, false);
			final Thread t3 = new Thread(r3);

			final DescriptorScoreMapRunnable r4 = new DescriptorScoreMapRunnable(descriptorList4, items,
					scoreMethod, considerChildScores, dependencyTree, descriptionMatrix, false);
			final Thread t4 = new Thread(r4);
			t1.start();
			t2.start();
			t3.start();
			t4.start();
			t1.join();
			t2.join();
			t3.join();
			t4.join();
			tempMap1 = r1.getTempMap();
			tempMap2 = r2.getTempMap();
			tempMap3 = r3.getTempMap();
			tempMap4 = r4.getTempMap();
			tempMap.putAll(tempMap1);
			tempMap.putAll(tempMap2);
			tempMap.putAll(tempMap3);
			tempMap.putAll(tempMap4);

			// sorting the final LinkedHashMap
			final List<Float> mapValues = new ArrayList<Float>(tempMap.values());
			Collections.sort(mapValues, Collections.reverseOrder());

			for (final Float dpScore : mapValues) {
				for (final Descriptor desc : tempMap.keySet()) {
					final float dp1 = tempMap.get(desc);
					final float dp2 = dpScore;

					if (dp1 == dp2) {
						descriptorsScoresMap.put(desc, dpScore);
					}
				}
			}

		}

		return descriptorsScoresMap;
	}

	@Deprecated
	public static LinkedHashMap<Descriptor, Float> getDescriptorsScoreMapUsingNThreads(
		final List<Descriptor> descriptors, final List<Item> items, final DescriptorTree dependencyTree, final int scoreMethod,
		final boolean considerChildScores, final int nThreads, final DescriptionElementState[][] descriptionMatrix,
		final boolean withGlobalWeigth) throws InterruptedException {
		final LinkedHashMap<Descriptor, Float> descriptorsScoresMap = new LinkedHashMap<Descriptor, Float>();

		if (items.size() > 1) {

			final int slicer = descriptors.size() / nThreads;

			final List<List<Descriptor>> subLists = new ArrayList<List<Descriptor>>();

			int i = 0;
			for (i = 0; i < nThreads - 1; i++) {
				subLists.add(descriptors.subList(slicer * i, slicer * (i + 1)));
			}
			subLists.add(descriptors.subList(slicer * i, descriptors.size()));

			final HashMap<Descriptor, Float> tempMap = new HashMap<Descriptor, Float>();

			final DescriptorScoreMapRunnable[] runnables = new DescriptorScoreMapRunnable[nThreads];
			final Thread[] threads = new Thread[nThreads];

			for (int j = 0; j < nThreads; j++) {
				runnables[j] = new DescriptorScoreMapRunnable(subLists.get(j), items, scoreMethod,
						considerChildScores, dependencyTree, descriptionMatrix, withGlobalWeigth);
				threads[j] = new Thread(runnables[j]);
			}

			for (int j = 0; j < nThreads; j++) {
				threads[j].start();
			}

			for (int j = 0; j < nThreads; j++) {
				threads[j].join();
			}

			for (int j = 0; j < nThreads; j++) {
				tempMap.putAll(runnables[j].getTempMap());
			}

			// sorting the final LinkedHashMap
			final List<Float> mapValues = new ArrayList<Float>(tempMap.values());
			Collections.sort(mapValues, Collections.reverseOrder());

			for (final Float dpScore : mapValues) {
				for (final Descriptor desc : tempMap.keySet()) {
					final float dp1 = tempMap.get(desc);
					final float dp2 = dpScore;

					if (dp1 == dp2) {
						descriptorsScoresMap.put(desc, dpScore);
					}
				}
			}

		}

		return descriptorsScoresMap;
	}

	/**
	 * Basic function used to compute discriminant Power. This function is called recursively on the child
	 * descriptor.
	 * 
	 * @param descriptor
	 *            {@link Descriptor}, the {@link Descriptor} to evaluate.
	 * @param items
	 *            {@link List}, the ArrayList of {@link Item} used to evaluate this descriptor.
	 * @param value
	 *            {@link float}, the current score in the recursive call
	 * @param scoreMethod
	 *            {@link int}, the score methode to be use
	 * @param considerChildScores
	 * @param dependencyTree
	 * @param descriptionMatrix
	 * @param descriptorNodeMap
	 * @param withGlobalWeigth
	 * @return {@link float} the discriminant power of this descriptor
	 */
	public static float getDiscriminantPower(
		final Descriptor descriptor, final List<Item> items, final float value,
		final int scoreMethod, final boolean considerChildScores, final DescriptorTree dependencyTree,
		final DescriptionElementState[][] descriptionMatrix, final DescriptorNode[] descriptorNodeMap,
		final boolean withGlobalWeigth) {
		float out = 0;
		int cpt = 0;

		if (descriptor.isQuantitativeType()) {
			for (int i1 = 0; i1 < items.size() - 1; i1++) {
				final Item item1 = items.get(i1);
				for (int i2 = i1 + 1; i2 < items.size(); i2++) {
					final Item item2 = items.get(i2);
					float tmp = -1;
					tmp = compareWithQuantitativeDescriptor((QuantitativeDescriptor) descriptor, item1,
							item2, scoreMethod, dependencyTree, descriptionMatrix, descriptorNodeMap);
					if (tmp >= 0) {
						out += tmp;
						cpt++;
					}
				}
			}
		} else if (descriptor.isCategoricalType()) {
			if (((CategoricalDescriptor) descriptor).getStates().size() <= 0) {
				out += 0;
				cpt++;
			} else {
				for (int i1 = 0; i1 < items.size() - 1; i1++) {
					final Item item1 = items.get(i1);
					for (int i2 = i1 + 1; i2 < items.size(); i2++) {
						final Item item2 = items.get(i2);
						float tmp = -1;
						tmp = compareWithCategoricalDescriptor((CategoricalDescriptor) descriptor, item1,
								item2, scoreMethod, dependencyTree, descriptionMatrix, descriptorNodeMap);
						if (tmp >= 0) {
							out += tmp;
							cpt++;
						}
					}
				}
			}
		}
		if (out != 0 && cpt != 0)
			// to normalize the number
		{
			out = out / cpt;
		}

		if (withGlobalWeigth) {
			if (out != 0) {
				out += descriptor.getGlobalWeight();
			} else {
				out = -1;
			}
		}

		// recursive DP calculation of child descriptors

		DescriptorNode node = null;
		if (descriptorNodeMap != null) {
			node = descriptorNodeMap[(int) descriptor.getId()];
		} else {
			node = dependencyTree.getNodeContainingDescriptor(descriptor.getId(), false);
		}

		if (considerChildScores && node != null) {
			for (final DescriptorNode childNode : node.getChildNodes()) {
				final Descriptor childDescriptor = childNode.getDescriptor(); // WILL NOT WORK WITH HIBERNATE (lazy
																		// instanciation exception)
				out = Math.max(
						value,
						getDiscriminantPower(childDescriptor, items, out, scoreMethod, true, dependencyTree,
								descriptionMatrix, descriptorNodeMap, withGlobalWeigth));
			}
		}
		return Math.max(out, value);

	}

	/**
	 * Compare two item based on a categorical descriptor, return a float between 0 (no match) and 1 (same
	 * value)
	 * 
	 * @param descriptor
	 * @param item1
	 * @param item2
	 * @param scoreMethod
	 * @param dependencyTree
	 * @param descriptionMatrix
	 * @param descriptorNodeMap
	 * @return
	 */
	private static float compareWithCategoricalDescriptor(
		final CategoricalDescriptor descriptor, final Item item1,
		final Item item2, final int scoreMethod, final DescriptorTree dependencyTree,
		final DescriptionElementState[][] descriptionMatrix, final DescriptorNode[] descriptorNodeMap) {
		float out = 0;
		// boolean isAlwaysDescribed = true;

		// int all = v.getNbModes();
		float commonAbsent = 0; // nb of common points which are absent
		float commonPresent = 0; // nb of common points which are present
		float other = 0;

		final DescriptorNode node;
		if (descriptorNodeMap == null) {
			node = dependencyTree.getNodeContainingDescriptor(descriptor.getId(), false);
		} else {
			node = descriptorNodeMap[(int) descriptor.getId()];
		}

		if ((isInapplicable(node, item1, descriptionMatrix) || isInapplicable(node, item2, descriptionMatrix))) {
			return -1;
		}

		DescriptionElementState des1 = null;
		DescriptionElementState des2 = null;
		if (descriptionMatrix == null) {
			des1 = item1.getDescriptionElement(descriptor.getId());
			des2 = item2.getDescriptionElement(descriptor.getId());
		} else {
			des1 = descriptionMatrix[(int) item1.getId()][(int) descriptor.getId()];
			des2 = descriptionMatrix[(int) item2.getId()][(int) descriptor.getId()];
		}
		List<State> statesList1 = des1.getStates();
		List<State> statesList2 = des2.getStates();
		final List<State> everyStates = descriptor.getStates();

		// if at least one description is empty for the current
		// character
		// if ((statesList1 != null && statesList1.size() == 0)
		// || (statesList2 != null && statesList2.size() == 0)) {
		// isAlwaysDescribed = false;
		// }

		if (des1.isUnknown()) {
			statesList1 = everyStates;
		}
		if (des2.isUnknown()) {
			statesList2 = everyStates;
		}

		for (final State state : everyStates) {
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
		// // Sokal & Michener method
		if (scoreMethod == SCORE_SOKAL_MICHENER) {
			out = 1 - ((commonPresent + commonAbsent) / (commonPresent + commonAbsent + other));
			// round to 10^-3
			out = Utils.roundFloat(out, 3);
		}
		// // Jaccard Method
		else if (scoreMethod == SCORE_SOKAL_MICHENER) {
			try {
				// // case where description are empty
				out = 1 - (commonPresent / (commonPresent + other));
				// // round to 10^-3
				out = Utils.roundFloat(out, 3);
			} catch (final ArithmeticException a) {
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
	 * Compare two item based on a quantitative descriptor, return a float between 0 (no match) and 1 (same
	 * value)
	 * 
	 * @param descriptor
	 * @param item1
	 * @param item2
	 * @param scoreMethod
	 * @param dependencyTree
	 * @param descriptionMatrix
	 * @param descriptorNodeMap
	 * @return float
	 */
	private static float compareWithQuantitativeDescriptor(
		final QuantitativeDescriptor descriptor, final Item item1,
		final Item item2, final int scoreMethod, final DescriptorTree dependencyTree,
		final DescriptionElementState[][] descriptionMatrix, final DescriptorNode[] descriptorNodeMap) {
		float out = 0;
		float commonPercentage = 0; // percentage of common values which are
		// shared

		DescriptorNode node = null;
		if (descriptorNodeMap == null) {
			node = dependencyTree.getNodeContainingDescriptor(descriptor.getId(), false);
		} else {
			node = descriptorNodeMap[(int) descriptor.getId()];
		}

		if ((isInapplicable(node, item1, descriptionMatrix) || isInapplicable(node, item2, descriptionMatrix))) {
			return -1;
		}

		DescriptionElementState des1 = null;
		DescriptionElementState des2 = null;

		if (descriptionMatrix == null) {
			des1 = item1.getDescription().getDescriptionElement(descriptor.getId());
			des2 = item2.getDescription().getDescriptionElement(descriptor.getId());
		} else {
			des1 = descriptionMatrix[(int) item1.getId()][(int) descriptor.getId()];
			des2 = descriptionMatrix[(int) item2.getId()][(int) descriptor.getId()];
		}
		final QuantitativeMeasure quantitativeMeasure1 = des1.getQuantitativeMeasure();
		final QuantitativeMeasure quantitativeMeasure2 = des2.getQuantitativeMeasure();

		if (quantitativeMeasure1 == null || quantitativeMeasure2 == null) {
			return 0;
		} else {
			if (quantitativeMeasure1.getCalculatedMinimum() == null
					|| quantitativeMeasure1.getCalculatedMaximum() == null
					|| quantitativeMeasure2.getCalculatedMinimum() == null
					|| quantitativeMeasure2.getCalculatedMaximum() == null) {
				return 0;
			} else {
				commonPercentage = calculateCommonPercentage(quantitativeMeasure1.getCalculatedMinimum()
						.doubleValue(), quantitativeMeasure1.getCalculatedMaximum().doubleValue(),
						quantitativeMeasure2.getCalculatedMinimum().doubleValue(), quantitativeMeasure2
								.getCalculatedMaximum().doubleValue());
			}

		}

		if (commonPercentage <= 0) {
			commonPercentage = 0;
		}

		switch (scoreMethod) {
		case SCORE_XPER:
			if ((commonPercentage <= 0)) {
				out = 1;
			} else {
				out = 0;
			}
			break;
		//
		case SCORE_SOKAL_MICHENER:
			out = 1 - (commonPercentage / 100);
			break;
		//
		case SCORE_JACCARD:
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

	private static boolean isInapplicable(
		final DescriptorNode descriptorNode, final Item item,
		final DescriptionElementState[][] descriptionMatrix) {
		if (descriptorNode != null && descriptorNode.getParentNode() != null) {
			final List<State> inapplicableStates = descriptorNode.getInapplicableStates();
			final DescriptionElementState description = descriptionMatrix[(int) item.getId()][(int) descriptorNode
					.getParentNode().getDescriptor().getId()];
			int numberOfDescriptionStates = description.getStates().size();

			for (int i = 0; i < inapplicableStates.size(); i++) {
				final State state = inapplicableStates.get(i);
				if (description.containsState(state.getId())) {
					numberOfDescriptionStates--;
				}

			}

			if (numberOfDescriptionStates == 0) {
				return true;
			}

			return isInapplicable(descriptorNode.getParentNode(), item, descriptionMatrix);

		}
		return false;
	}

	/**
	 * Return True if this descriptor Node has is parent already in the descriptors list, or if this
	 * descriptor node dont have parents.
	 * 
	 * @param descriptorNode
	 * @param description
	 * @param descriptors
	 * @return
	 */
	public static boolean getIsInaplicable(final DescriptorNode descriptorNode, final List<Descriptor> descriptors) {
		final DescriptorNode descriptorNodeParent = descriptorNode.getParentNode();
		if (descriptorNodeParent == null) {
			return false;
		}
		return !descriptors.contains(descriptorNodeParent.getDescriptor());
	}

	/**
	 * @param min1
	 * @param max1
	 * @param min2
	 * @param max2
	 * @return the common percentage
	 */
	private static float calculateCommonPercentage(final double min1, final double max1, final double min2, final double max2) {
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

	// END OF DISCRIMINANT POWER CALCULATION FUNCTIONS

	// IDENTIFICATION FUNCTIONS
	/**
	 * This method receives a list of {@link Item}s, and a {@link Description}, which contains the description
	 * of one unitentified Item, according to one or several {@link Descriptor}s. It then loops over the Items
	 * passed in parameter, eliminates those who are not compatible with the description of the unidentified
	 * Item, and returns the list of the remaining Items compatible with the description of the unidentified
	 * Item
	 * 
	 * @param description
	 * @param remainingItems
	 * @return
	 */
	public static List<Item> getRemainingItems(final Description description, final List<Item> remainingItems) {
		final List<Item> itemsToRemove = new ArrayList<Item>();
		for (final Item item : remainingItems) {
			for (final Descriptor descriptor : description.getDescriptionElements().keySet()) {
				if (!item.getDescription().getDescriptionElement(descriptor.getId()).isUnknown()) {

					if (descriptor.isCategoricalType()) {
						final List<State> checkedStatesInSubmittedDescription = description.getDescriptionElement(
								descriptor.getId()).getStates();
						final List<State> checkedStatesInKnowledgeBaseDescription = item.getDescription()
								.getDescriptionElement(descriptor.getId()).getStates();

						if (!matchDescriptionStates(checkedStatesInSubmittedDescription,
								checkedStatesInKnowledgeBaseDescription, LOGICAL_OPERATOR_OR)) {
							itemsToRemove.add(item);
						}

					} else if (descriptor.isQuantitativeType()) {
						final QuantitativeMeasure submittedMeasure = description.getDescriptionElement(
								descriptor.getId()).getQuantitativeMeasure();
						final QuantitativeMeasure knowledgeBaseMeasure = item.getDescription()
								.getDescriptionElement(descriptor.getId()).getQuantitativeMeasure();

						if (!matchDescriptionsQuantitativeMeasures(submittedMeasure, knowledgeBaseMeasure,
								COMPARISON_OPERATOR_CONTAINS)) {
							itemsToRemove.add(item);
						}
					}
				}
			}
		}
		remainingItems.removeAll(itemsToRemove);
		return remainingItems;
	}

	/**
	 * This method loops over the states checked in a submitted description (e.g. by a user), compares them
	 * with the states checked in a reference description (e.g. a knowledge base description) an returns true
	 * if the states from the first description are compatible with the reference description, using a
	 * specified logical operator
	 * 
	 * @param selectedStatesInSubmittedDescription
	 * @param checkedStatesInReferenceDescription
	 * @param logicalOperator
	 * @return
	 */
	private static boolean matchDescriptionStates(
		final List<State> selectedStatesInSubmittedDescription,
		final List<State> checkedStatesInReferenceDescription, final int logicalOperator) {
		int commonValues = 0;

		for (final State selectedStateInSubmittedDescription : selectedStatesInSubmittedDescription) {
			for (final State checkedStateInReferenceDescription : checkedStatesInReferenceDescription) {
				if (checkedStateInReferenceDescription
						.hasSameNameAsState(selectedStateInSubmittedDescription)) {
					commonValues++;
				}
			}
		}

		switch (logicalOperator) {
		case LOGICAL_OPERATOR_AND:
			return checkedStatesInReferenceDescription.size() == commonValues;
			case LOGICAL_OPERATOR_OR:
			return commonValues >= 1;

			default:
			return false;
		}
	}

	/**
	 * This methods compares the {@link QuantitativeMeasure} in a submitted description (e.g. by a user)
	 * 
	 * @param submittedMeasure
	 * @param referenceMeasure
	 * @param comparisonOperator
	 * @return
	 */
	private static boolean matchDescriptionsQuantitativeMeasures(
		final QuantitativeMeasure submittedMeasure,
		final QuantitativeMeasure referenceMeasure, final int comparisonOperator) {
		switch (comparisonOperator) {
		case COMPARISON_OPERATOR_CONTAINS:

			if ((referenceMeasure.isNotFilled() || submittedMeasure.isNotFilled())
					&& referenceMeasure.getMean() != null && submittedMeasure.getMean() != null) {
				return referenceMeasure.getMean().equals(submittedMeasure.getMean());
			} else {
				return referenceMeasure.contains(submittedMeasure);
			}

		case COMPARISON_OPERATOR_GREATER_THAN:
			return referenceMeasure.isGreaterOrEqualTo(submittedMeasure, true);

		case COMPARISON_OPERATOR_LOWER_THAN:
			return referenceMeasure.isLowerOrEqualTo(submittedMeasure, true);

		default:
			return false;
		}

	}

	/**
	 * Naive integration of getSimilarityMap
	 * 
	 * @param description
	 * @param discardedItem
	 * @return LinkedHashMap, association between an item and the similarity score
	 */
	public static LinkedHashMap<Item, Float> getSimilarityMap(
		final Description description,
		final List<Item> discardedItem) {
		final LinkedHashMap<Item, Float> descriptorsScoresMap = new LinkedHashMap<Item, Float>();
		// for each discardedItem
		for (final Item item : discardedItem) {
			descriptorsScoresMap.put(item, computeSimilarity(description, item));
		}
		return descriptorsScoresMap;
	}

	/**
	 * Multi-thread integration of getSimilarityMap
	 * 
	 * @param description
	 * @param discardedItem
	 * @return LinkedHashMap, association between an item and the similarity score
	 */
	public static LinkedHashMap<Long, Float> getSimilarityMapFuture(
		final Description description,
		final List<Item> discardedItem) {
		final LinkedHashMap<Long, Float> itemSimilarityMap = new LinkedHashMap<Long, Float>();
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		@SuppressWarnings("unchecked") final Future<Object[]>[] futures = new Future[discardedItem.size()];
		int i = 0;

		// for each discardedItem
		for (final Item item : discardedItem) {
			futures[i] = exec.submit(new ThreadComputeSimilarity(description, item));
			i++;
		}
		try {
			for (final Future<Object[]> fute : futures) {
				final Object[] result = fute.get();
				itemSimilarityMap.put((Long) result[0], (Float) result[1]);
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final ExecutionException ex) {
			ex.printStackTrace();
		} finally {
			exec.shutdown();
			exec = null;
		}

		return itemSimilarityMap;
	}

	/**
	 * Compute a float between 0 and 1 that represents the similarity between this discardedItem and this
	 * description. 1 is for complete matching, and 0 for no match The way this function compute the
	 * similarity take for bases that the description is the reference.
	 * 
	 * @param description
	 * @param discardedItem
	 * @return float, between 0 (min) and 1 (max) the similarity between this description and this
	 *         discardedItem
	 */
	public static float computeSimilarity(final Description description, final Item discardedItem) {
		final Map<Descriptor, DescriptionElementState> descriptionElements = description.getDescriptionElements();
		final Map<Descriptor, DescriptionElementState> ItemdescriptionElements = discardedItem.getDescription()
				.getDescriptionElements();

		float commonValues = 0;
		float result = 0;

		// For each descriptor in this reference description
		for (final Descriptor descriptor : descriptionElements.keySet()) {
			// Get the discardedItem and the reference description DescriptionElementState for this descriptor
			final DescriptionElementState descriptionElement = descriptionElements.get(descriptor);
			final DescriptionElementState ItemdescriptionElement = ItemdescriptionElements.get(descriptor);
			// CategoricalType
			if (descriptor.isCategoricalType()) {
				commonValues = 0;
				// For every State in the reference description
				for (final State selectedStateReferenceDescription : descriptionElement.getStates()) {
					// For every State in the discardedItem
					for (final State stateInTheDiscardedItem : ItemdescriptionElement.getStates()) {
						// If the two States have the same name, assume there are equals commomValues + 1
						if (selectedStateReferenceDescription.hasSameNameAsState(stateInTheDiscardedItem)) {
							commonValues++;
						}
					}
				}
				// if commonValues == max ( description.nS , discardedItem.nS ) => return 1
				// else if commonValues = 0 => return 0,
				// else return ]0,1[
				// Result = commonValues / maximum(description.numberOfState or discardedItem.numberOfState)
				result += commonValues
						/ ((float) Math.max(descriptionElement.getStates().size(), ItemdescriptionElement
								.getStates().size()));
			}
			// QuantitativeType
			else if (descriptor.isQuantitativeType()) {
				final QuantitativeMeasure descriptionQm = descriptionElement.getQuantitativeMeasure();
				final QuantitativeMeasure discardedItemQm = ItemdescriptionElement.getQuantitativeMeasure();

				// If the ref description quantitaive measure contains the discardedItem quantative measure,
				// return 1; ( no diff )
				result += descriptionQm.containsPercent(discardedItemQm);

			} else {
				// TODO add calculated Type
			}

		}

		result = result / ((float) descriptionElements.keySet().size());

		return result;
	}

}
