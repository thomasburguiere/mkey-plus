package fr.lis.mkeyplusWSREST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.IDatasetObjectWithName;
import fr.lis.xper3API.model.QuantitativeDescriptor;
import fr.lis.xper3API.model.State;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.lis.xper3API.utils.Utils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import fr.lis.mkeyplusWSREST.service.IdentificationWebService;

/**
 * @author Thomas Burguiere
 */
public class TestIdentificationWebservice {
    public Logger logger = Logger.getRootLogger();
    private static IdentificationWebService testService;

    @BeforeClass
    public static void init() {
        Utils.setBundleConf(ResourceBundle.getBundle("confTest"));

        testService = new IdentificationWebService();
    }

    @Test
    public void testSDDParsing() throws Exception {
        // testService.getJSONDescriptiveData("http://localhost:8080/miscFiles/feuillesSDD.xml",
        // "fakeDateString");
        String jsonData = testService.getJSONDescriptiveData(
                "http://localhost:8080/miscFiles/genetta.sdd.xml", false, null);
        System.out.println(jsonData);
        // logger.info("done");
    }

    @Test
    public void testJSQuery() throws Exception {
        String jsonData = testService.getJSONDescriptiveData(
                "http://localhost:8080/miscFiles/Cichorieae-fullSDD.xml", false, null);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(jsonData, Map.class);

        Iterable<Map<String, Object>> itemsAsMapsList = (List<Map<String, Object>>) map.get("itemList");
        Collection<Map<String, Object>> jsonRemainingItems = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> itemAsMap : itemsAsMapsList) {
            Map<String, Object> i = new HashMap<String, Object>();
            i.put("name", itemAsMap.get("name"));
            i.put("alternativeName", itemAsMap.get("alternativeName"));
            i.put("id", new Long((Integer) itemAsMap.get("id")));
            // i.getDescription().setItem(null); // to avoid infinite loop during JSON generation
            jsonRemainingItems.add(i);
        }

        Iterable<Map<String, Object>> descriptorsAsMapsList = (List<Map<String, Object>>) map
                .get("descriptorList");
        Descriptor d = null;
        IDatasetObjectWithName s = new State("auriculate");
        for (Map<String, Object> descriptorAsMap : descriptorsAsMapsList) {
            if (((String) descriptorAsMap.get("name")).toLowerCase().equals("cauline leaves <base>")) {
                if ((Boolean) descriptorAsMap.get("categoricalType") == true) {
                    d = new CategoricalDescriptor((String) descriptorAsMap.get("name"));
                } else if ((Boolean) descriptorAsMap.get("quantitativeType") == true) {
                    d = new QuantitativeDescriptor((String) descriptorAsMap.get("name"));
                }
                d.setId(new Long((Integer) descriptorAsMap.get("id")));
            }
        }

        Collection<Object> descriptionsMapsForJson = new ArrayList<Object>();
        Map<String, Object> descriptionElementStateForJson = new HashMap<String, Object>();
        Collection<String> selectedStates = new ArrayList<String>();
        selectedStates.add(s.getName());
        descriptionElementStateForJson.put("selectedStatesNames", selectedStates);
        descriptionElementStateForJson.put("quantitativeMeasure", null);

        Map<String, Object> descriptionMapForJson = new HashMap<String, Object>();
        descriptionMapForJson.put(d.getName(), descriptionElementStateForJson);

        descriptionsMapsForJson.add(descriptionMapForJson);

        String jsonDescriptions = mapper.writeValueAsString(descriptionsMapsForJson);

//		testService.getJSONRemainingItemsAndRemainingDescriptorsScore(
//				"http://localhost:8080/miscFiles/Cichorieae-fullSDD.xml", jsonDescriptions,
//				mapper.writeValueAsString(jsonRemainingItems));

        logger.info("done");
    }

    @Test
    public void testWSQuery() throws Exception {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        WebResource service = client.resource("http://localhost:9080/mkey-plus-webservice-REST");

        // create params
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("input", "toto");

        String[] stringArray = new String[1];
        stringArray[0] = MediaType.TEXT_PLAIN;

        // Fluent interfaces
        // System.out.println(service.path("identificationKey").accept(stringArray).post(ClientResponse.class,
        // queryParams));

        // create Identification Key
        System.out.println(service.path("/test/testWS").accept(MediaType.TEXT_PLAIN)
                .post(String.class, queryParams));

    }

}
