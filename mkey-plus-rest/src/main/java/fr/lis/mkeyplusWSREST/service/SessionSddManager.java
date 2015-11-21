package fr.lis.mkeyplusWSREST.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.Dataset;
import fr.lis.xper3API.model.DescriptionElementState;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.DescriptorNode;
import fr.lis.xper3API.model.DescriptorTree;
import fr.lis.xper3API.model.Item;
import fr.lis.xper3API.model.Resource;
import fr.lis.xper3API.model.State;


import fr.lis.mkeyplusWSREST.model.JsonDescriptor;
import fr.lis.mkeyplusWSREST.model.JsonItem;
import fr.lis.mkeyplusWSREST.model.JsonResource;
import fr.lis.mkeyplusWSREST.model.JsonState;

/**
 * @author thomas burguiere This Singleton class manages a pool of SessionFactory
 */
public enum SessionSddManager {
    INSTANCE;

    private final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
    private boolean lockInitialization = false;
    /**
     * the actual {@link SessionFactory} pool
     */
    private final Map<String, Dataset> sessionDatasetPool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JsonResource[]> sessionResourcePool = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<JsonDescriptor>> sessionDescriptorPool = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<JsonItem>> sessionItemPool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JsonState[]> sessionStatePool = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<Long>> sessionRootDescriptorIdPool = new ConcurrentHashMap<>();
    private final Map<String, HashMap<Long, long[]>> sessionDependencyTablePool = new ConcurrentHashMap<>();
    private final Map<String, HashMap<Long, Long>> sessionInvertedDependencyTablePool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DescriptionElementState[][]> sessionDescriptionMatrixPool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DescriptorNode[]> sessionDescriptorNodePool = new ConcurrentHashMap<>();
    // private static Map<String, Boolean[][]> sessionInaplicablePool;
    private final Map<String, Date> sessionDatasetLastUsed = new ConcurrentHashMap<>();
    private long lastFlushDataset = 0;

//    private static SessionSddManager instance;

    /**
     * Associates a {@link SessionFactory} with the date of its last use
     */
//	private SessionSddManager() throws Exception {
    // SessionSddManager.sessionInaplicablePool = new HashMap<String, Boolean[][]>();
//	}

    /**
     * returns the unique instance of SessionFactoryManagementService
     *
     * @return SessionFactoryManager
     */
//	public static SessionSddManager getInstance() throws Exception {
//		if (instance == null) {
//			instance = new SessionSddManager();
//			launchJobs();
//		}
//		return INSTANCE;
//	}

    /**
     * This method returns the {@link SessionFactory} associated with a database name. If the sessionFactory
     * doesn't exists in the sessionFactoryPool, it is created with the createSessionFactory method
     *
     * @param sddURLString
     * @param login
     * @param password
     * @return a {@link SessionFactory}
     */
    public Dataset getDataset(String sddURLString) throws Exception {
        Dataset dataset = sessionDatasetPool.get(sddURLString);


        if (dataset == null) {
            while (lockInitialization) {
                Thread.sleep(10);
            }

            // Delete Old dataset
            deleteOldDataset();

            //Lock initialization
            lockInitialization = true;
            try {
                dataset = createDataset(sddURLString);
                initializeDatasetContent(dataset, sddURLString);
            } catch (Exception ex) {
                ex.printStackTrace();
                flushDataset(sddURLString);
                dataset = null;
            } finally {
                //unlock initialization
                lockInitialization = false;
            }

        } else {
            updateDatasetLastUsed(sddURLString);
        }

        return dataset;
    }

    public JsonResource[] jsonResource(String sddURLString) {
        return sessionResourcePool.get(sddURLString);
    }

    public List<JsonDescriptor> getJsonDescriptor(String sddURLString) {
        return sessionDescriptorPool.get(sddURLString);
    }

    public List<JsonItem> getJsonItem(String sddURLString) {
        return sessionItemPool.get(sddURLString);
    }

    public JsonState[] getJsonState(String sddURLString) {
        return sessionStatePool.get(sddURLString);
    }

    public List<Long> getRootDescriptorId(String sddURLString) {
        return sessionRootDescriptorIdPool.get(sddURLString);
    }

    public Map<Long, long[]> getDependancyTable(String sddURLString) {
        return sessionDependencyTablePool.get(sddURLString);
    }

    public Map<Long, Long> getInvertedDependancyTable(String sddURLString) {
        return sessionInvertedDependencyTablePool.get(sddURLString);
    }

    public DescriptionElementState[][] getDescriptionMatrix(String sddURLString) {
        return sessionDescriptionMatrixPool.get(sddURLString);
    }

    public DescriptorNode[] getDescriptorNode(String sddURLString) {
        return sessionDescriptorNodePool.get(sddURLString);
    }

    public String getDatasetName(String sddURLString) throws Exception {
        Dataset dataset;
        //The dataset may have been deleted so check value before getting it
        if (!sessionDatasetPool.keySet().contains(sddURLString)) {
            dataset = getDataset(sddURLString);
        } else {
            dataset = sessionDatasetPool.get(sddURLString);
        }
        return dataset.getName();
    }
    // public Boolean[][] getInaplicationMatrix(String sddURLString) {
    // Boolean[][] output = SessionSddManager.sessionInaplicablePool.get(sddURLString);
    // return output;
    // }

    public void deleteDataset(String sddURLString) {
        sessionDatasetPool.remove(sddURLString);
        sessionResourcePool.remove(sddURLString);
        sessionDescriptorPool.remove(sddURLString);
        sessionItemPool.remove(sddURLString);
        sessionStatePool.remove(sddURLString);
        sessionRootDescriptorIdPool.remove(sddURLString);
        sessionDependencyTablePool.remove(sddURLString);
        sessionInvertedDependencyTablePool.remove(sddURLString);
        sessionDescriptionMatrixPool.remove(sddURLString);
        sessionDescriptorNodePool.remove(sddURLString);
        sessionDatasetLastUsed.remove(sddURLString);
        // sessionInaplicablePool.remove(sddURLString);
    }

    /**
     * creates a new {@link SessionFactory}
     *
     * @param sddURLString
     * @return {@link SessionFactory}
     */
    private Dataset createDataset(String sddURLString) throws Exception {

        URLConnection urlConnection;
        Dataset dataset;
        // testing the sdd URL validity

        URL sddFileUrl = new URL(sddURLString);
        // open URL (HTTP query)
        urlConnection = sddFileUrl.openConnection();
        // Open data stream to test the connection
        urlConnection.getInputStream();

        // parsing the sdd to retrieve the dataset
        dataset = new fr.lis.xper3API.IO.parser.SDDSaxParser(sddFileUrl).getDataset();

        return dataset;
    }

    /**
     * @param dbName
     */
    public void destroyDataset(String dbName) throws Exception {
        Dataset dataset = sessionDatasetPool.get(dbName);
        if (dataset != null) {
            flushDataset(dbName);
//			if (sessionDatasetLastUsed.get(dbName) != null)
//				sessionDatasetLastUsed.remove(dbName);
//			sessionDatasetPool.remove(dbName);
        }
    }

    /**
     * Updates the last used date of a given sessionFactory to the current date
     *
     * @param dbName the name of the database associated with the {@link SessionFactory}
     * @return true if the the last used date was successfully updated
     */
    public boolean updateDatasetLastUsed(String dbName) throws Exception {
        return updateDatasetLastUsed(dbName, new Date());
    }

    /**
     * Updates the last used date of a given sessionFactory, with an arbitrary date
     *
     * @param dbName  the name of the database associated with the {@link SessionFactory}
     * @param newDate the arbitrary date
     * @return
     */
    public boolean updateDatasetLastUsed(String dbName, Date newDate) throws Exception {
        Dataset dataset = sessionDatasetPool.get(dbName);
        if (dataset != null) {
            sessionDatasetLastUsed.put(dbName, newDate);
            return true;
        }
        return false;
    }

    /**
     * destroys the entire {@link SessionFactory} pool, as well as the
     */
    private void flushDatasetPool() throws Exception {
        sessionDatasetPool.clear();
        sessionResourcePool.clear();
        sessionDescriptorPool.clear();
        sessionItemPool.clear();
        sessionStatePool.clear();
        sessionRootDescriptorIdPool.clear();
        sessionDependencyTablePool.clear();
        sessionInvertedDependencyTablePool.clear();
        sessionDescriptionMatrixPool.clear();
        sessionDescriptorNodePool.clear();
        sessionDatasetLastUsed.clear();
    }

    private void flushDataset(String keyUrl) {
        sessionDatasetPool.remove(keyUrl);
        sessionResourcePool.remove(keyUrl);
        sessionDescriptorPool.remove(keyUrl);
        sessionItemPool.remove(keyUrl);
        sessionStatePool.remove(keyUrl);
        sessionRootDescriptorIdPool.remove(keyUrl);
        sessionDependencyTablePool.remove(keyUrl);
        sessionInvertedDependencyTablePool.remove(keyUrl);
        sessionDescriptionMatrixPool.remove(keyUrl);
        sessionDescriptorNodePool.remove(keyUrl);
        sessionDatasetLastUsed.remove(keyUrl);
    }

    /**
     * This method launch the job to flush the pool of sessionFactory each day and to flush the pool of
     * process progression each hour
     *
     * @throws Exception
     */
    private static void launchJobs() throws Exception {

        // SchedulerFactory sf = new StdSchedulerFactory();
        // Scheduler sched = null;
        //
        // try {
        // sched = sf.getScheduler();
        // } catch (SchedulerException e) {
        // // if the scheduler already exist do not throw the exception
        // if (e.getLocalizedMessage()
        // .equals("Scheduler with name 'DefaultQuartzScheduler' already exists.")) {
        // sched = null;
        // } else {
        // throw e;
        // }
        // }
        //
        // if (sched != null) {
        // // SessionFactory pool flusher job
        // JobDetail sessionFactoryPoolFlusherJob = JobBuilder.newJob(SessionFactoryPoolFlusherJob.class)
        // .withIdentity("sessionFactoryPoolFlusherJob", "sessionFactoryPoolFlusherGroup").build();
        // CronTrigger sessionFactoryPoolFlusherTrigger = TriggerBuilder
        // .newTrigger()
        // .withIdentity("sessionFactoryPoolFlusherTrigger", "sessionFactoryPoolFlusherGroup")
        // .withSchedule(
        // CronScheduleBuilder.cronSchedule(Utils
        // .getBundleConfElement("sessionFactoryFlusherJob.cronSchedule"))).build();
        //
        // // Process progression pool flusher job
        // JobDetail processProgressPoolFlusherJob = JobBuilder.newJob(ProcessProgressPoolFlusherJob.class)
        // .withIdentity("processProgressPoolFlusherJob", "processProgressPoolFlusherGroup").build();
        // CronTrigger processProgressPoolFlusherTrigger = TriggerBuilder
        // .newTrigger()
        // .withIdentity("processProgressPoolFlusherTrigger", "processProgressPoolFlusherGroup")
        // .withSchedule(
        // CronScheduleBuilder.cronSchedule(Utils
        // .getBundleConfElement("processProgressFlusherJob.cronSchedule"))).build();
        //
        // sched.scheduleJob(sessionFactoryPoolFlusherJob, sessionFactoryPoolFlusherTrigger);
        // sched.scheduleJob(processProgressPoolFlusherJob, processProgressPoolFlusherTrigger);
        // sched.start();
        // }
    }

    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private void initializeDatasetContent(Dataset dataset, String keyUrl) {

        List<JsonResource> tmpArrayResource = new ArrayList<>();
        List<JsonState> tmpArrayState = new ArrayList<>();
        ArrayList<JsonDescriptor> arrayDescriptor = new ArrayList<>(dataset.getDescriptors()
                .size());
        ArrayList<JsonItem> arrayItem = new ArrayList<>(dataset.getItems().size());

        DescriptorTree dependencyTree = new DescriptorTree();
        if (!dataset.getDescriptorTrees().isEmpty()) {
            dependencyTree.setType(DescriptorTree.DEPENDENCY_TYPE);
            dependencyTree.setNodes(dataset.getDescriptorTrees().get(0).getNodes());
            for (int i = 1; i < dataset.getDescriptorTrees().size(); i++) {
                DescriptorTree tree = dataset.getDescriptorTrees().get(i);
                if (tree.getType().equalsIgnoreCase(DescriptorTree.DEPENDENCY_TYPE)) {
                    dependencyTree.setNodes(tree.getNodes());
                }
                // Get the resource in the Part_Type tree ( node group can have resources attached
                else if (tree.getType().equalsIgnoreCase(DescriptorTree.GROUP_TYPE)) {
                    for (DescriptorNode node : tree.getNodes()) {
                        if (node.getDescriptor() == null) {
                            for (Resource resource : node.getResources()) {
                                JsonResource jsResource = new JsonResource(resource);
                                tmpArrayResource.add(jsResource);
                            }
                        }
                    }
                }
            }

        } else {
            dependencyTree.setType(DescriptorTree.DEPENDENCY_TYPE);
            for (Descriptor descriptor : dataset.getDescriptors()) {
                dependencyTree.addNode(new DescriptorNode(descriptor));
            }
            dataset.addDescriptorTree(dependencyTree);
        }

        // initialize IDs
        for (Descriptor descriptor : dataset.getDescriptors()) {
            JsonDescriptor jsDescriptor = new JsonDescriptor(descriptor);
            if (descriptor.isCategoricalType()) {
                // get States
                for (State s : ((CategoricalDescriptor) descriptor).getStates()) {
                    JsonState jsState = new JsonState(s);
                    // get State resources
                    for (Resource resource : s.getResources()) {
                        JsonResource jsResource = new JsonResource(resource);
                        tmpArrayResource.add(jsResource);
                    }
                    tmpArrayState.add(jsState);
                }

            }
            // get Descriptor resources
            for (Resource resource : descriptor.getResources()) {
                JsonResource jsResource = new JsonResource(resource);
                tmpArrayResource.add(jsResource);
            }

            // Set inapplicable State
            DescriptorNode descriptorNode = dependencyTree.getNodeContainingDescriptor(descriptor.getId());
            if (descriptorNode != null) {
                long[] inapplicableIds = new long[descriptorNode.getInapplicableStates().size()];
                int posIS = 0;
                for (State state : descriptorNode.getInapplicableStates()) {
                    inapplicableIds[posIS] = state.getId();
                    posIS++;
                }
                jsDescriptor.setInapplicableState(inapplicableIds);
            }

            arrayDescriptor.add(jsDescriptor);
        }
        //
        for (Item item : dataset.getItems()) {
            JsonItem jsItem = new JsonItem(item);
            // getResources
            for (Resource resource : item.getResources()) {
                JsonResource jsResource = new JsonResource(resource);
                tmpArrayResource.add(jsResource);
            }
            arrayItem.add(jsItem);
        }

        // Set the dataBase ressources
        for (Resource resource : dataset.getResources()) {
            JsonResource jsResource = new JsonResource(resource);
            tmpArrayResource.add(jsResource);
        }

        // Create Resource and State array

        int resourceSize = tmpArrayResource.size();
        int stateSize = tmpArrayState.size();

        // Warning Bug here why +2 ??
        // TODO
        JsonState[] arrayState = new JsonState[stateSize + 2];
        JsonResource[] arrayResource = new JsonResource[resourceSize + 2];

        // Set the resources and states in there final position in the array
        for (int indexR = 0; indexR < resourceSize; indexR++) {
            JsonResource jsResource = tmpArrayResource.get(indexR);
            arrayResource[(int) jsResource.getId()] = jsResource;
        }
        for (int indexS = 0; indexS < stateSize; indexS++) {
            JsonState jsState = tmpArrayState.get(indexS);
            arrayState[(int) jsState.getId()] = jsState;
        }

        // Lock the ressources

        sessionResourcePool.put(keyUrl, arrayResource);
        sessionDescriptorPool.put(keyUrl, arrayDescriptor);
        sessionItemPool.put(keyUrl, arrayItem);
        sessionStatePool.put(keyUrl, arrayState);

        HashMap<Long, Long> invertedDependencyTable = new HashMap<>();
        HashMap<Long, long[]> dependencyTable = new HashMap<>();
        ArrayList<Long> childDescriptorId = new ArrayList<>();
        ArrayList<Long> rootDescriptorId = new ArrayList<>();

        // Constructe DependencyTable
        for (DescriptorNode node : dependencyTree.getNodes()) {
            getChildNode(node, invertedDependencyTable, dependencyTable, childDescriptorId);
        }

        for (DescriptorNode node : dependencyTree.getNodes()) {
            boolean isIn = false;
            for (Long nodeID : childDescriptorId) {
                if (nodeID == node.getDescriptor().getId()) {
                    isIn = true;
                    break;
                }
            }
            if (!isIn) {
                rootDescriptorId.add(node.getDescriptor().getId());
            }
        }

        // Lock Ressources

        sessionInvertedDependencyTablePool.put(keyUrl, invertedDependencyTable);
        sessionDependencyTablePool.put(keyUrl, dependencyTable);
        sessionRootDescriptorIdPool.put(keyUrl, rootDescriptorId);

        initializeDescriptionMatrix(dataset, keyUrl);
        initializeDescriptorNodeMap(dataset, keyUrl);
        // Lock DataSet

        sessionDatasetPool.put(keyUrl, dataset);
        sessionDatasetLastUsed.put(keyUrl, new Date());
    }

    /**
     * Recursivly create dependencyTable InvetedDependancyTable and childDescriptorId from a Node ( every node
     * are scanned thus add the if (1) to not be redondante.
     *
     * @param descriptorNode
     * @param invertedDependencyTable
     * @param dependencyTable
     * @param childDescriptorId
     * @return Long , the ID of the this node
     */
    private long getChildNode(DescriptorNode descriptorNode, HashMap<Long, Long> invertedDependencyTable,
                              HashMap<Long, long[]> dependencyTable, ArrayList<Long> childDescriptorId) {
        int size = descriptorNode.getChildNodes().size();
        long nodeID = descriptorNode.getDescriptor().getId();

        if (!invertedDependencyTable.containsKey(nodeID)) {
            if (descriptorNode.getParentNode() != null) {
                invertedDependencyTable.put(nodeID, descriptorNode.getParentNode().getDescriptor()
                        .getId());
            }
        }
        if (!dependencyTable.containsKey(nodeID)) {
            if (size > 0) {
                long[] childIDs = new long[size];
                int pos = 0;
                for (DescriptorNode child : descriptorNode.getChildNodes()) {

                    childIDs[pos] = getChildNode(child, invertedDependencyTable, dependencyTable,
                            childDescriptorId);
                    pos++;

                    boolean isIn = false;
                    for (Long nodeChildID : childDescriptorId) {
                        if (nodeChildID == child.getDescriptor().getId()) {
                            isIn = true;
                            break;
                        }
                    }
                    if (!isIn) {
                        childDescriptorId.add(child.getDescriptor().getId());
                    }

                }
                dependencyTable.put(nodeID, childIDs);
            } else {

                return nodeID;
            }
        }

        return nodeID;
    }

    /**
     * @param items
     * @param descriptors
     * @return
     */
    private void initializeDescriptionMatrix(Dataset dataset, String keyUrl) {
        List<Item> items = dataset.getItems();
        List<Descriptor> descriptors = dataset.getDescriptors();

        int nItems = items.size();
        int nDescriptors = descriptors.size();
        DescriptionElementState[][] descriptionMatrix = new DescriptionElementState[nItems][nDescriptors];
        for (int itemIndex = 0; itemIndex < nItems; itemIndex++) {
            for (int descriptorIndex = 0; descriptorIndex < nDescriptors; descriptorIndex++) {
                DescriptionElementState desc = items.get(itemIndex).getDescriptionElement(
                        descriptors.get(descriptorIndex).getId());

                if (desc != null) {
                    descriptionMatrix[(int) items.get(itemIndex).getId()][(int) descriptors.get(
                            descriptorIndex).getId()] = desc;
                }
                // Quantitative case && Categorical
                else {
                    descriptionMatrix[(int) items.get(itemIndex).getId()][(int) descriptors.get(
                            descriptorIndex).getId()] = new DescriptionElementState();
                }
            }
        }

        // Lock the ressource
        sessionDescriptionMatrixPool.put(keyUrl, descriptionMatrix);
    }

    private void initializeDescriptorNodeMap(Dataset dataset, String keyUrl) {
        List<Descriptor> descriptors = dataset.getDescriptors();
        // Warning dependancy tree should be the first ( normaly initialize in the initialize DatasetContent
        DescriptorTree dependencyTree = dataset.getDescriptorTrees().get(0);
        int nDescriptors = descriptors.size();
        DescriptorNode[] descriptorNodeMap = new DescriptorNode[nDescriptors];

        for (int descriptorIndex = 0; descriptorIndex < nDescriptors; descriptorIndex++) {
            int currentDescriptorIndex = (int) descriptors.get(descriptorIndex).getId();
            descriptorNodeMap[currentDescriptorIndex] = dependencyTree
                    .getNodeContainingDescriptor(descriptors.get(descriptorIndex).getId());
        }
        // Lock resources
        sessionDescriptorNodePool.put(keyUrl, descriptorNodeMap);
    }

    private void deleteOldDataset() {
        while (!checkHeapSize()) {
            flushDataset(getOldestDataset());
        }

        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastFlushDataset) > 60000) {
            //1800000 == 30min
            //300000 == 5 min
            //86400000 == 24h
            //60000 == 1min
            //10000 == 10sec
            lastFlushDataset = currentTime;

            for (String keyUrl : sessionDatasetLastUsed.keySet()) {
                //604800000 = 7j
                //600000 = 10min
                //3600000 = 1h
                //60000 = 1min
                if ((lastFlushDataset - sessionDatasetLastUsed
                        .get(keyUrl).getTime()) > 600000) {
                    flushDataset(keyUrl);
                    memoryMxBean.gc();
                }
            }

        }
    }


    private String getOldestDataset() {
        String sddUrl = "";
        long time = 0;
        for (String keyUrl : sessionDatasetLastUsed.keySet()) {
            long testedTime = sessionDatasetLastUsed.get(keyUrl).getTime();
            if (sessionDatasetLastUsed.get(keyUrl).getTime() > time) {
                time = testedTime;
                sddUrl = keyUrl;
            }
        }
        return sddUrl;
    }

    /**
     * Check if heap size is full, return true if not else return false
     *
     * @return boolean
     */
    public boolean checkHeapSize() {
        MemoryUsage mu = memoryMxBean.getHeapMemoryUsage();
//		double m = 1000000;
        //If Heap Size - 10 * 1M is inferior to current Used HeapSize
        return (mu.getMax() * 0.8) > mu.getUsed();
    }

}
