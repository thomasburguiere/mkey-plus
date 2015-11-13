package fr.lis.mkeyplusWSREST.service;

import fr.lis.mkeyplusAPI.services.InteractiveIdentificationService;
import fr.lis.mkeyplusWSREST.model.JsonDescriptor;
import fr.lis.mkeyplusWSREST.model.JsonItem;
import fr.lis.xper3API.model.Dataset;
import fr.lis.xper3API.model.Description;
import fr.lis.xper3API.model.DescriptionElementState;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.DescriptorNode;
import fr.lis.xper3API.model.DescriptorTree;
import fr.lis.xper3API.model.Item;
import fr.lis.xper3API.model.QuantitativeMeasure;
import fr.lis.xper3API.model.Resource;
import fr.lis.xper3API.model.State;
import fr.lis.xper3API.services.DatasetManagementService;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive Identification webservice using REST protocol
 *
 * @author Thomas Burguiere
 * @created 2012/12/07
 */

@Path("/identification")
public class IdentificationWebService {

    /**
     * Basic function to call every time a new description is submitted
     *
     * @param sddURLString
     * @param jsonDescriptions
     * @param jsonRemainingItemsIDs
     * @param jsonDiscardedDescriptorsIDs
     * @param withScoreMap
     * @param withGlobalWeigth
     * @param callback
     * @return
     */
    @SuppressWarnings("unchecked")
    @Path("/getRemainingItemsAndDescriptorsUsingIDs")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String getJSONRemainingItemsAndRemainingDescriptorsScoreUsingIds(
            @QueryParam("sddURL") String sddURLString, @QueryParam("descriptions") String jsonDescriptions,
            @QueryParam("remainingItemsIDs") String jsonRemainingItemsIDs,
            @QueryParam("discardedDescriptorsIDs") String jsonDiscardedDescriptorsIDs,
            @QueryParam("withScoreMap") Boolean withScoreMap,
            @QueryParam("withGlobalWeigth") Boolean withGlobalWeigth, @QueryParam("callback") String callback) {
        String jsonData = null;
        Dataset datasetInSDD;
        List<Item> itemsInSDD = new ArrayList<>();
        List<Descriptor> descriptorsInSDD = new ArrayList<>();

        DescriptorTree dependencyTreeInSDD;

        // Get the data set in the session manager
        datasetInSDD = getDataset(sddURLString);

        if (datasetInSDD != null) {
            List<Descriptor> discardedDescriptors = new ArrayList<>();
            List<Item> remainingItems = new ArrayList<>();
            List<Descriptor> remainingDescriptors = new ArrayList<>();
            Map<String, Object> data = new HashMap<>();

            // Copy the Item object from the invariant SDD
            for (Object item : datasetInSDD.getItems().toArray()) {
                itemsInSDD.add((Item) item);
            }

            // Copy the Descriptor object from the invariant SDD
            for (Object descriptor : datasetInSDD.getDescriptors().toArray()) {
                descriptorsInSDD.add((Descriptor) descriptor);
            }

            // Select the dependency Tree ( this Tree should always be the first (index 0))
            // Normaly this is initialized in the Manager
            dependencyTreeInSDD = datasetInSDD.getDescriptorTrees().get(0);

            // Creat the DescriptionMatrix, improve calculated performance
            DescriptionElementState[][] descriptionMatrix = getDescriptionMatrix(sddURLString);
            DescriptorNode[] descriptorNodeMap = getDescriptorNodeMap(sddURLString);

            // Get the information send by the Json
            ObjectMapper mapper = new ObjectMapper();
            List<Integer> remainingItemsIDs = null;
            List<Integer> discardedDescriptorsIDs = null;
            List<Map<String, Object>> descriptionMapList = null;
            try {
                remainingItemsIDs = mapper.readValue(jsonRemainingItemsIDs, List.class);
                discardedDescriptorsIDs = mapper.readValue(jsonDiscardedDescriptorsIDs, List.class);
                descriptionMapList = mapper.readValue(jsonDescriptions, List.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Get the remaining Descriptors
            if (discardedDescriptorsIDs != null) {
                if (discardedDescriptorsIDs.size() > 0) {
                    // For every descriptor in SDD
                    for (Descriptor descriptorInSDD : descriptorsInSDD) {
                        boolean isDiscarded = false;
                        // Get if this descriptor is discarded
                        for (Integer discardedDescriptorID : discardedDescriptorsIDs) {
                            Long castedID = Long.valueOf(discardedDescriptorID);
                            if (castedID.equals(descriptorInSDD.getId())) {
                                isDiscarded = true;
                                // Get out of the for
                                break;
                            }
                        }
                        // If not discarded add to remainingDescriptors
                        if (!isDiscarded)
                            remainingDescriptors.add(descriptorInSDD);
                    }
                } else {
                    remainingDescriptors = descriptorsInSDD;
                }
            }

            // Get the remaining Items
            // For every remaining Items id ( given in parameter )
            for (Item itemInSDD : itemsInSDD) {
                // For every item in SDD
                if (remainingItemsIDs != null) {
                    for (int i = 0; i < remainingItemsIDs.size(); i++) {
                        Long castedID = Long.valueOf(remainingItemsIDs.get(i));
                        // If this item in SDD == this remaning item id
                        if (castedID.equals(itemInSDD.getId())) {
                            remainingItems.add(itemInSDD);
                            // Get out of the for
                            break;
                        }
                    }
                }
            }

            // Creat a new description based on the submitted information
            if (descriptionMapList != null) {
                for (Map<String, Object> descriptionMap : descriptionMapList) {
                    Description description = new Description();
                    for (Map.Entry<String, Object> descriptionMapEntry : descriptionMap.entrySet()) {
                        Descriptor descriptor = null;
                        Integer id = Integer.parseInt(descriptionMapEntry.getKey());
                        // For each remainingDescriptors
                        for (int i = 0; i < remainingDescriptors.size(); i++) {
                            if (((int) remainingDescriptors.get(i).getId()) == id) {
                                descriptor = remainingDescriptors.get(i);
                                break;
                            }
                        }

                        if (descriptor == null) {
                            return "false";
                        }

                        // Create the descriptionElementMap for this descriptor
                        Map<String, Object> descriptionElementStateMap = (Map<String, Object>) descriptionMapEntry.getValue();
                        DescriptionElementState descriptionElementState = new DescriptionElementState();

                        if (descriptor.isCategoricalType()) {

                            // Warning only Object class is supported, casting directly to Integer or String fire
                            // exception
                            ArrayList<Object> stateIds = (ArrayList<Object>) descriptionElementStateMap
                                    .get("selectedStatesNames");
                            for (Object stateId : stateIds) {
                                State state = datasetInSDD.getStateById(Long.parseLong(stateId.toString()));
                                descriptionElementState.addState(state);
                            }
                        } else if (descriptor.isQuantitativeType()) {
                            Map<String, Object> qmMap = (Map<String, Object>) descriptionElementStateMap
                                    .get("quantitativeMeasure");
                            QuantitativeMeasure qm = new QuantitativeMeasure();
                            qm.setMean(Double.parseDouble(qmMap.get("value").toString()));
                            qm.setMin(Double.parseDouble(qmMap.get("value").toString()));
                            qm.setMax(Double.parseDouble(qmMap.get("value").toString()));

                            descriptionElementState.setQuantitativeMeasure(qm);
                        }
                        //else if (descriptor.isCalculatedType()) {
                        // TODO mkey do not have calculatedType for now
                        //}
                        description.addDescriptionElement(descriptor, descriptionElementState);
                        // remove this descriptor fro remaining
                        remainingDescriptors.remove(descriptor);
                        // add this descriptor in the discarded list
                        discardedDescriptors.add(descriptor);
                    }
                    // Get the RemainingItems based on the created description
                    remainingItems = InteractiveIdentificationService.getRemainingItems(description,
                            remainingItems);
                }
            }
            // Put the data to send in Json
            data.put("remainingDescriptors", createJsonDescriptorList(remainingDescriptors));
            data.put("discardedDescriptorsInIteration", createJsonDescriptorList(discardedDescriptors));
            data.put("remainingItems", createJsonItemList(remainingItems));

            // Add the ScoreMap in the Json
            if (withScoreMap == null || withScoreMap) {
                HashMap<Long, Float> descriptorIdScoreMap = new HashMap<>();
                HashMap<Descriptor, Float> descriptorScoreMap = InteractiveIdentificationService
                        .getDescriptorsScoreMapFuture(remainingDescriptors, remainingItems,
                                dependencyTreeInSDD, InteractiveIdentificationService.SCORE_XPER, true,
                                descriptionMatrix, descriptorNodeMap, withGlobalWeigth);

                for (Map.Entry<Descriptor, Float> descriptorScoreEntry : descriptorScoreMap.entrySet())
                    descriptorIdScoreMap.put(descriptorScoreEntry.getKey().getId(), descriptorScoreEntry.getValue());
                data.put("descriptorScoreMap", descriptorIdScoreMap);
            }
            try {
                jsonData = mapper.writeValueAsString(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "(" + jsonData + ")";
        return jsonData;
    }

    /**
     * Get the entire description for this itemName.
     *
     * @param itemName
     * @param sddURLString
     * @param callback
     * @return
     */
    @Path("/getDescription")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String getDescription(@QueryParam("itemName") String itemName,
                                 @QueryParam("sddURL") String sddURLString, @QueryParam("callback") String callback) {
        String jsonData = null;
        Dataset datasetInSDD;
        DescriptorTree dependencyTreeInSDD;
        List<Descriptor> descriptorsInSDD = new ArrayList<>();

        // Get the data set in the session manager
        datasetInSDD = getDataset(sddURLString);

        if (datasetInSDD != null) {
            Map<String, Object> data = new HashMap<>();
            ArrayList<Long> innapDescriptorID = new ArrayList<>();

            List<Item> items = datasetInSDD.getItems();
            int i = 0;
            Item item = items.get(i);
            // get the item with this itemName
            while (i < items.size() && !item.getName().equals(itemName)) {
                item = items.get(i);
                i++;
            }

            // Copy the Descriptor object from the invariant SDD
            for (Object descriptor : datasetInSDD.getDescriptors().toArray()) {
                descriptorsInSDD.add((Descriptor) descriptor);
            }

            // Creat the DescriptionMatrix, improve calculated performance
            DescriptionElementState[] descriptionMatrix = getDescriptionMatrix(sddURLString)[(int) item
                    .getId()];

            dependencyTreeInSDD = datasetInSDD.getDescriptorTrees().get(0);

            // Clean backref
            for (int index = 0; index < descriptionMatrix.length; index++) {
                DescriptionElementState descriptionElement = descriptionMatrix[index];
                if (descriptionElement != null) {

                    Descriptor descriptor = descriptorsInSDD.get(index);

                    DescriptorNode descriptorNode = dependencyTreeInSDD
                            .getNodeContainingDescriptor(descriptor.getId());

                    // If this descriptor is inapplicable add to innapDescriptorID
                    if (descriptorNode != null && DescriptorTree.isInapplicableRecursive(descriptorNode, item)) {
                        innapDescriptorID.add(descriptor.getId());
                    }

                    // Prune ressources from descriptionElement
                    descriptionElement.setResources(new ArrayList<Resource>());

                    // if categorical descriptor
                    if (descriptionElement.getStates() != null
                            && descriptionElement.getStates().size() > 0) {
                        // prune states ref
                        for (State s : descriptionElement.getStates()) {
                            s.setResources(new ArrayList<Resource>());
                            s.setCategoricalDescriptor(null);
                        }
                    }
                    // else if quantitative descriptor
                    else if (descriptionElement.getQuantitativeMeasure() != null) {
                        descriptionElement.setStates(new ArrayList<State>());
                    }
                    // Set id corresponding to the description position in the global matrix
                    descriptionElement.setId(index);
                }
            }

            data.put("description", descriptionMatrix);
            data.put("innapDescriptorId", innapDescriptorID);

            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonData = mapper.writeValueAsString(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "(" + jsonData + ")";
        return jsonData;
    }

    /**
     * First function to be called, getDescriptiveData set information in both server and client side
     *
     * @param sddURLString
     * @param withGlobalWeigth
     * @param callback
     * @return
     */
    @Path("getDescriptiveData")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String getJSONDescriptiveData(@QueryParam("sddURL") String sddURLString,
                                         @QueryParam("withGlobalWeigth") Boolean withGlobalWeigth, @QueryParam("callback") String callback) {
        String jsonDescriptiveData = "";

        Dataset datasetInSDD;
        List<Item> itemsInSDD;
        List<Descriptor> descriptorsInSDD;
        DescriptorTree dependencyTreeInSDD;
        HashMap<String, Object> descriptiveData = new LinkedHashMap<>();
        // Get the data set in the session manager
        // Put Dataset independant an Unique information in the Json Format
        datasetInSDD = getDatasetAndInitialize(sddURLString, descriptiveData);

        if (datasetInSDD != null) {
            // Get the dataset Item/descriptor/dependencyTree
            itemsInSDD = datasetInSDD.getItems();
            descriptorsInSDD = datasetInSDD.getDescriptors();
            dependencyTreeInSDD = datasetInSDD.getDescriptorTrees().get(0);

            // Initialize descriptionMatrix and NodeMap for faster computation times
            DescriptionElementState[][] descriptionMatrix = getDescriptionMatrix(sddURLString);
            DescriptorNode[] descriptorNodeMap = getDescriptorNodeMap(sddURLString);

            LinkedHashMap<Descriptor, Float> descriptorScoreMap;
            try {
                HashMap<Long, Float> descriptorIdScoreMap = new HashMap<>();

                descriptorScoreMap = InteractiveIdentificationService.getDescriptorsScoreMapFuture(
                        descriptorsInSDD, itemsInSDD, dependencyTreeInSDD,
                        InteractiveIdentificationService.SCORE_XPER, true, descriptionMatrix,
                        descriptorNodeMap, withGlobalWeigth);

                // as descriptorScoreMap return <descriptor,float> and we need <descriptor.id(long),Float>
                for (Map.Entry<Descriptor, Float> descriptorScoreMapEntry : descriptorScoreMap.entrySet()) {
                    descriptorIdScoreMap.put(descriptorScoreMapEntry.getKey().getId(), descriptorScoreMapEntry.getValue());
                }
                // put the descriptorIdScoreMap in the Json
                descriptiveData.put("descriptorsScoreMap", descriptorIdScoreMap);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ObjectMapper mapper = new ObjectMapper();

            try {
                jsonDescriptiveData = mapper.writeValueAsString(descriptiveData);
            } catch (IOException io) {
                io.printStackTrace();
            }

        }

        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "(" + jsonDescriptiveData + ")";
        return jsonDescriptiveData;
    }

    @Path("removeSDD")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String removeSDD(@QueryParam("sddURLtoRemove") String sddURLStringToDel,
                            @QueryParam("callback") String callback) {
        SessionSddManager manager;
        // Dataset dataset = null;
        try {
            manager = SessionSddManager.INSTANCE;
            manager.deleteDataset(sddURLStringToDel);
        } catch (Exception e) {
            e.printStackTrace();
            return "{error}";
        }
        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "('youlose')";
        return "youwin";
    }

    /**
     * Get the similarity Map with the submitted description has referential description. The similarity is
     * then computed relative to this description
     *
     * @param sddURLString
     * @param jsonDescriptions
     * @param jsonRemainingItemsIDs
     * @param callback
     * @return
     */
    @SuppressWarnings("unchecked")
    @Path("getSimilarityMap")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String getSimilarityMap(@QueryParam("sddURL") String sddURLString,
                                   @QueryParam("descriptions") String jsonDescriptions,
                                   @QueryParam("remainingItemsIDs") String jsonRemainingItemsIDs,
                                   @QueryParam("callback") String callback) {

        String jsonData = null;
        Dataset datasetInSDD;
        List<Item> itemsInSDD = new ArrayList<>();
        List<Descriptor> descriptorsInSDD = new ArrayList<>();
        LinkedHashMap<Long, Float> similarityMap;
        // Get the data set in the session manager
        datasetInSDD = getDataset(sddURLString);

        if (datasetInSDD != null) {
            Map<String, Object> data = new HashMap<>();
            List<Item> toRemove = new ArrayList<>();
            Description description = new Description();

            // Copy the Item object from the invariant SDD
            for (Object item : datasetInSDD.getItems().toArray()) {
                itemsInSDD.add((Item) item);
            }

            // Copy the Descriptor object from the invariant SDD
            for (Object descriptor : datasetInSDD.getDescriptors().toArray()) {
                descriptorsInSDD.add((Descriptor) descriptor);
            }

            // Get the information send by the Json
            ObjectMapper mapper = new ObjectMapper();
            List<Integer> remainingItemsIDs = null;
            List<Map<String, Object>> descriptionMapList = null;
            try {
                remainingItemsIDs = mapper.readValue(jsonRemainingItemsIDs, List.class);
                descriptionMapList = mapper.readValue(jsonDescriptions, List.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // remove remaining Items to get the discarded one
            for (Item itemInSDD : itemsInSDD) {
                // For every item in SDD
                if (remainingItemsIDs != null) {
                    for (int i = 0; i < remainingItemsIDs.size(); i++) {
                        Long castedID = Long.valueOf(remainingItemsIDs.get(i));
                        // If this item in SDD == this remaning item id
                        if (castedID.equals(itemInSDD.getId())) {
                            toRemove.add(itemInSDD);
                            // Get out of the for
                            break;
                        }
                    }
                }
            }

            itemsInSDD.removeAll(toRemove);

            // Creat a new description based on the submitted information
            if (descriptionMapList != null) {
                for (Map<String, Object> descriptionMap : descriptionMapList) {

                    for (Map.Entry<String, Object> descriptionMapEntry : descriptionMap.entrySet()) {
                        Descriptor descriptor = null;
                        // For each remainingDescriptors
                        Integer id = Integer.parseInt(descriptionMapEntry.getKey());
                        for (int i = 0; i < descriptorsInSDD.size(); i++) {
                            if (((int) descriptorsInSDD.get(i).getId()) == id) {
                                descriptor = descriptorsInSDD.get(i);
                                break;
                            }
                        }

                        // Creat the descriptionElementMap for this descriptor
                        Map<String, Object> descriptionElementStateMap = (Map<String, Object>) descriptionMapEntry.getValue();
                        DescriptionElementState descriptionElementState = new DescriptionElementState();

                        if (descriptor != null) {
                            if (descriptor.isCategoricalType()) {

                                // Warning only Object class is supported, casting directly to Integer or String fire
                                // exeption
                                ArrayList<Object> stateIds = (ArrayList<Object>) descriptionElementStateMap
                                        .get("selectedStatesNames");
                                for (Object stateId : stateIds) {
                                    State state = datasetInSDD.getStateById(Long.parseLong(stateId.toString()));
                                    descriptionElementState.addState(state);
                                }
                            } else if (descriptor.isQuantitativeType()) {
                                Map<String, Object> qmMap = (Map<String, Object>) descriptionElementStateMap
                                        .get("quantitativeMeasure");
                                QuantitativeMeasure qm = new QuantitativeMeasure();
                                qm.setMean(Double.parseDouble(qmMap.get("value").toString()));
                                qm.setMin(Double.parseDouble(qmMap.get("value").toString()));
                                qm.setMax(Double.parseDouble(qmMap.get("value").toString()));

                                descriptionElementState.setQuantitativeMeasure(qm);
                            }
                        }
                        description.addDescriptionElement(descriptor, descriptionElementState);

                        // shorten the list of every descriptor
                        descriptorsInSDD.remove(descriptor);
                    }
                }
            }
            similarityMap = InteractiveIdentificationService.getSimilarityMapFuture(description, itemsInSDD);

            data.put("similarityMap", similarityMap);

            try {
                jsonData = mapper.writeValueAsString(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "(" + jsonData + ")";
        return jsonData;
    }

    /**
     * Get the similarity Map with the global description issue from the remaining items has referential
     * description. The similarity is then computed relative to this description
     *
     * @param sddURLString
     * @param jsonDescriptions
     * @param jsonRemainingItemsIDs
     * @param callback
     * @return
     */
    @SuppressWarnings("unchecked")
    @Path("getSimilarityMapForRemainingItem")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String getSimilarityMapForRemainingItem(@QueryParam("sddURL") String sddURLString,
                                                   @QueryParam("remainingItemsIDs") String jsonRemainingItemsIDs,
                                                   @QueryParam("callback") String callback) {

        String jsonData = null;
        Dataset datasetInSDD;
        DescriptorTree dependencyTreeInSDD;
        List<Item> itemsInSDD = new ArrayList<>();
        List<Descriptor> descriptorsInSDD = new ArrayList<>();
        LinkedHashMap<Long, Float> similarityMap;
        // Get the data set in the session manager
        datasetInSDD = getDataset(sddURLString);

        if (datasetInSDD != null) {
            Map<String, Object> data = new HashMap<>();
            List<Item> remainingItem = new ArrayList<>();
            Description description = new Description();

            // Copy the Item object from the invariant SDD
            for (Object item : datasetInSDD.getItems().toArray()) {
                itemsInSDD.add((Item) item);
            }

            // Copy the Descriptor object from the invariant SDD
            for (Object descriptor : datasetInSDD.getDescriptors().toArray()) {
                descriptorsInSDD.add((Descriptor) descriptor);
            }

            dependencyTreeInSDD = datasetInSDD.getDescriptorTrees().get(0);

            // Get the information send by the Json
            ObjectMapper mapper = new ObjectMapper();
            List<Integer> remainingItemsIDs = null;
            // List<Map<String, Object>> descriptionMapList = null;
            try {
                remainingItemsIDs = mapper.readValue(jsonRemainingItemsIDs, List.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // remove remaining Items to get the discarded one
            for (Item itemInSDD : itemsInSDD) {
                // For every item in SDD
                if (remainingItemsIDs != null) {
                    for (int i = 0; i < remainingItemsIDs.size(); i++) {
                        Long castedID = Long.valueOf(remainingItemsIDs.get(i));
                        // If this item in SDD == this remaning item id
                        if (castedID.equals(itemInSDD.getId())) {
                            remainingItem.add(itemInSDD);
                            // Get out of the for
                            break;
                        }
                    }
                }
            }

            itemsInSDD.removeAll(remainingItem);

            // Create a new Description based on the remaining Items
            for (Descriptor descriptor : descriptorsInSDD) {

                ArrayList<Boolean> inapplicableStats = new ArrayList<>();
                DescriptorNode descriptorNode = dependencyTreeInSDD.getNodeContainingDescriptor(descriptor
                        .getId());
                for (Item item : remainingItem) {
                    inapplicableStats.add(DescriptorTree.isInapplicableRecursive(descriptorNode, item));
                }

                DescriptionElementState descriptionElementState = new DescriptionElementState();
                if (descriptor.isCategoricalType()) {
                    // Warning here intersection is taken but it could be union
                    // Get Intersection between every Items
                    ArrayList<State> states = DatasetManagementService.getIntersectionCategorical(
                            remainingItem, descriptor, inapplicableStats);
                    // Add the intersection states as basic descriptionElementState for this descriptor
                    descriptionElementState.setStates(states);

                } else if (descriptor.isQuantitativeType()) {
                    // Same as categorical
                    QuantitativeMeasure qm = DatasetManagementService.getIntersectionQuantitative(
                            remainingItem, descriptor, inapplicableStats);

                    descriptionElementState.setQuantitativeMeasure(qm);
                }
                //else {
                // TODO calculated Type not supported in Mkey
                //}
                description.addDescriptionElement(descriptor, descriptionElementState);
            }
            similarityMap = InteractiveIdentificationService.getSimilarityMapFuture(description, itemsInSDD);

            data.put("similarityMap", similarityMap);

            try {
                jsonData = mapper.writeValueAsString(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "(" + jsonData + ")";
        return jsonData;
    }

    /**
     * Fire a change in Description, recompute the description history for every descriptions given in
     * parameter. Warning : this function may slice the description history if there is unconsistant
     * descriptor lineage.
     *
     * @param sddURLString
     * @param descriptions , the whole description
     * @param callback
     * @return
     */
    @SuppressWarnings("unchecked")
    @Path("changeDescriptionHistory")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String changeDescriptionHistory(@QueryParam("sddURL") String sddURLString,
                                           @QueryParam("descriptions") String jsonDescriptions, @QueryParam("callback") String callback) {
        String jsonData = null;
        Dataset datasetInSDD;
        List<Item> itemsInSDD = new ArrayList<>();
        List<Descriptor> descriptorsInSDD = new ArrayList<>();
        List<Descriptor> discardedDescriptor = new ArrayList<>();
        DescriptorTree dependencyTreeInSDD;
        // Get the data set in the session manager
        datasetInSDD = getDataset(sddURLString);

        List<Map<String, Object>> newDescriptions = new ArrayList<>();

        if (datasetInSDD != null) {
            Map<String, Object> data = new HashMap<>();

            // Copy the Item object from the invariant SDD
            for (Object item : datasetInSDD.getItems().toArray()) {
                itemsInSDD.add((Item) item);
            }

            // Copy the Descriptor object from the invariant SDD
            for (Object descriptor : datasetInSDD.getDescriptors().toArray()) {
                descriptorsInSDD.add((Descriptor) descriptor);
            }

            dependencyTreeInSDD = datasetInSDD.getDescriptorTrees().get(0);

            // Creat the DescriptionMatrix, improve calculated performance
            DescriptionElementState[][] descriptionMatrix = getDescriptionMatrix(sddURLString);
            DescriptorNode[] descriptorNodeMap = getDescriptorNodeMap(sddURLString);

            // Get the information send by the Json
            ObjectMapper mapper = new ObjectMapper();
            // List<Integer> remainingItemsIDs = null;
            List<Map<String, Object>> descriptionMapList = null;
            try {
                descriptionMapList = mapper.readValue(jsonDescriptions, List.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Description description = new Description();
            DescriptorNode node;
            // Creat a new description based on the submitted information
            //TODO warning null pointer fire here
            if (descriptionMapList != null) {
                for (Map<String, Object> descriptionMap : descriptionMapList) {
                    if (itemsInSDD.size() > 0) {
                        Integer id = ((Map<String, Integer>) descriptionMap.get("descriptor"))
                                .get("id");
                        Descriptor descriptor = null;
                        for (Descriptor descriptorInList : descriptorsInSDD) {
                            if (((int) descriptorInList.getId()) == id) {
                                descriptor = descriptorInList;
                                break;
                            }
                        }

                        DescriptionElementState descriptionElementState = new DescriptionElementState();
                        QuantitativeMeasure qm = new QuantitativeMeasure();
                        Map<String, Object> qmMap;

                        if (descriptor != null) {
                            if (descriptor.isCategoricalType()) {

                                // Warning only Object class is supported, casting directly to Integer or String fire
                                // exeption
                                ArrayList<Object> stateIds = (ArrayList<Object>) descriptionMap.get("selectedStates");
                                for (Object stateId : stateIds) {
                                    State state = datasetInSDD.getStateById(Long.parseLong(stateId.toString()));
                                    descriptionElementState.addState(state);
                                }
                            } else if (descriptor.isQuantitativeType()) {
                                qmMap = (Map<String, Object>) descriptionMap.get("quantitativeMeasure");
                                qm.setMean(Double.parseDouble(qmMap.get("value").toString()));
                                qm.setMin(Double.parseDouble(qmMap.get("value").toString()));
                                qm.setMax(Double.parseDouble(qmMap.get("value").toString()));

                                descriptionElementState.setQuantitativeMeasure(qm);
                            }

                            node = descriptorNodeMap[(int) descriptor.getId()];
                            if (!InteractiveIdentificationService.getIsInaplicable(node, discardedDescriptor)) {

                                description.addDescriptionElement(descriptor, descriptionElementState);

                                // shorten the list of every descriptor
                                descriptorsInSDD.remove(descriptor);
                                discardedDescriptor.add(descriptor);

                                // Get the RemainingItems based on the created description
                                itemsInSDD = InteractiveIdentificationService.getRemainingItems(description,
                                        itemsInSDD);

                                newDescriptions.add(descriptionMap);
                            }
                        }

                    }
                }
            }

            data.put("remainingDescriptors", createJsonDescriptorList(descriptorsInSDD));
            data.put("remainingItems", createJsonItemList(itemsInSDD));
            data.put("discardedDescriptors", createJsonDescriptorList(discardedDescriptor));
            data.put("descriptions", newDescriptions);

            HashMap<Long, Float> descriptorIdScoreMap = new HashMap<>();
            HashMap<Descriptor, Float> descriptorScoreMap = InteractiveIdentificationService
                    .getDescriptorsScoreMapFuture(descriptorsInSDD, itemsInSDD, dependencyTreeInSDD,
                            InteractiveIdentificationService.SCORE_XPER, true, descriptionMatrix,
                            descriptorNodeMap, true);

            for (Map.Entry<Descriptor, Float> descriptorScoreEntry : descriptorScoreMap.entrySet()) {
                descriptorIdScoreMap.put(descriptorScoreEntry.getKey().getId(), descriptorScoreEntry.getValue());
            }

            data.put("descriptorScoreMap", descriptorIdScoreMap);

            try {
                jsonData = mapper.writeValueAsString(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // returning JSON or JSONP formatted descriptive data
        if (callback != null && callback.trim().length() > 0)
            return callback + "(" + jsonData + ")";
        return jsonData;
    }

    /**
     * @param resources
     * @return
     */
    // private ArrayList<JsonResource> createResourceJson(List<Resource> resources) {
    // ArrayList<JsonResource> jsonResource = new ArrayList<JsonResource>();
    // for (Resource resource : resources) {
    // jsonResource.add(new JsonResource(resource));
    // }
    // return jsonResource;
    // }
    private Dataset getDatasetAndInitialize(String sddUrlString, HashMap<String, Object> data) {
        SessionSddManager manager;
        Dataset dataset = null;
        try {
            manager = SessionSddManager.INSTANCE;
            dataset = manager.getDataset(sddUrlString);
            if (dataset != null) {
                data.put("Items", manager.getJsonItem(sddUrlString));
                data.put("Descriptors", manager.getJsonDescriptor(sddUrlString));
                data.put("States", manager.getJsonState(sddUrlString));
                data.put("Resources", manager.jsonResource(sddUrlString));
                data.put("DependancyTable", manager.getDependancyTable(sddUrlString));
                data.put("DescriptorRootId", manager.getRootDescriptorId(sddUrlString));
                data.put("InvertedDependancyTable", manager.getInvertedDependancyTable(sddUrlString));
                data.put("NameDataset", manager.getDatasetName(sddUrlString));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataset;
    }

    /**
     * Get the dataset from the SessionSddManager
     *
     * @param sddUrlString
     * @return Dataset
     */
    private Dataset getDataset(String sddUrlString) {
        SessionSddManager manager;
        Dataset dataset = null;
        try {
            manager = SessionSddManager.INSTANCE;
            dataset = manager.getDataset(sddUrlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataset;
    }

    private DescriptionElementState[][] getDescriptionMatrix(String sddUrlString) {
        SessionSddManager manager;
        DescriptionElementState[][] des = null;
        try {
            manager = SessionSddManager.INSTANCE;
            des = manager.getDescriptionMatrix(sddUrlString);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return des;
    }

    private DescriptorNode[] getDescriptorNodeMap(String sddUrlString) {
        SessionSddManager manager;
        DescriptorNode[] dn = null;
        try {
            manager = SessionSddManager.INSTANCE;
            dn = manager.getDescriptorNode(sddUrlString);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dn;
    }

    /**
     * Convert the list of Descriptor to JsonDescriptor to be exported in Json
     *
     * @param descriptorList List<Descriptor>
     * @return List<JsonDescriptor>
     */
    private List<JsonDescriptor> createJsonDescriptorList(List<Descriptor> descriptorList) {
        List<JsonDescriptor> jsonDescriptors = new ArrayList<>();
        for (Descriptor descriptor : descriptorList)
            jsonDescriptors.add(new JsonDescriptor(descriptor));
        return jsonDescriptors;
    }

    /**
     * Convert the list of item to Jsonitem to be exported in Json
     *
     * @param itemList List<Item>
     * @return List<JsonItem>
     */
    private List<JsonItem> createJsonItemList(List<Item> itemList) {
        List<JsonItem> jsonItems = new ArrayList<>();
        for (Item item : itemList)
            jsonItems.add(new JsonItem(item));
        return jsonItems;
    }

    /**
     *
     * @param sddUrlString
     * @param data
     */
    // private void setInitialParameter(String sddUrlString, HashMap<String, Object> data) {
    // SessionSddManager manager = null;
    // try {
    // manager = SessionSddManager.INSTANCE();
    // data.put("Items", manager.getJsonItem(sddUrlString));
    // data.put("Descriptors", manager.getJsonDescriptor(sddUrlString));
    // data.put("States", manager.getJsonState(sddUrlString));
    // data.put("Resources", manager.JsonResource(sddUrlString));
    // data.put("DependancyTable", manager.getDependancyTable(sddUrlString));
    // data.put("DescriptorRootId", manager.getRootDescriptorId(sddUrlString));
    // data.put("InvertedDependancyTable", manager.getInvertedDependancyTable(sddUrlString));
    // data.put("NameDataset", manager.getDatasetName(sddUrlString));
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

    /**
     * @param descriptorNode
     * @param item
     * @return true, if the descriptor is inapplicable for this item
     */
//	private boolean isInapplicableDesc(DescriptorNode descriptorNode, Item item) {
//
//		if (descriptorNode.getParentNode() != null) {
//			List<State> inapplicableStates = descriptorNode.getInapplicableStates();
//			if (inapplicableStates.size() > 0) {
//				for (State state : inapplicableStates) {
//					if (item.getDescriptionElement(descriptorNode.getParentNode().getDescriptor().getId())
//							.containsState(state.getId())) {
//						return true;
//					}
//				}
//				return isInapplicableDesc(descriptorNode.getParentNode(), item);
//			} else {
//				return false;
//			}
//		}
//
//		return false;
//	}

}
