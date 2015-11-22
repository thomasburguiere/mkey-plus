package fr.lis.mkeyplusAPI.services;

import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.Description;
import fr.lis.xper3API.model.DescriptionElementState;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.DescriptorTree;
import fr.lis.xper3API.model.Item;
import fr.lis.xper3API.model.State;
import fr.lis.xper3API.services.DescriptorManagementService;
import fr.lis.xper3API.services.DescriptorTreeManagementService;
import fr.lis.xper3API.services.ItemManagementService;
import fr.lis.xper3API.utils.Utils;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.ResourceBundle;


public class InteractiveIdentificationServiceTestDB {
    public Logger logger = Logger.getRootLogger();
    private static String login;
    private static String dbName;
    private static String password;

    private static List<Item> itemsInKB;
    private static List<Descriptor> descriptorsInKb;
    private static DescriptorTree dependencyTreeInKB;

    @BeforeClass
    public static void init() throws Exception {

        // set test properties file
        Utils.setBundleConf(ResourceBundle.getBundle("confTest"));
        login = Utils.getBundleConfElement("hibernate.connection.database.username");
        dbName = Utils.getBundleConfElement("hibernate.connection.database.dbName");
        password = Utils.getBundleConfElement("hibernate.connection.database.password");

        dependencyTreeInKB = DescriptorTreeManagementService.read(DescriptorTree.DEPENDENCY_TYPE, true,
                dbName, login, password);
        itemsInKB = ItemManagementService.readAll(true, true, dbName, login, password);
        descriptorsInKb = DescriptorManagementService.readAll(true, dbName, login, password);
    }

    // @Test
    // public void calculateSingleDiscriminantPowerScore() throws Exception {
    //
    // Descriptor descriptor = descriptorsInKb.get(0);
    // float score = 0;
    // if (descriptor.isCategoricalType()) {
    // score = InteractiveIdentificationService.categoricalDescriptorScore(
    // (CategoricalDescriptor) descriptor, itemsInKB, dbName, login, password, true);
    //
    // logger.info("catDesc " + descriptor.getName() + " score: " + score);
    //
    // } else if (descriptor.isQuantitativeType()) {
    //
    // score = InteractiveIdentificationService.quantitativeDescriptorScore(
    // (QuantitativeDescriptor) descriptor, itemsInKB, dbName, login, password);
    // logger.info("numDesc " + descriptor.getName() + " score: " + score);
    // }
    //
    // }

//	@Test
//	public void testGetDescriptorScoreMapXperSort() throws Exception {
//		InteractiveIdentificationService.getDescriptorsScoreMap(descriptorsInKb, itemsInKB,
//				dependencyTreeInKB, InteractiveIdentificationService.SCORE_XPER, true);
//	}

    // @Test
    // public void testGetDescriptiveDataJaccard() throws Exception {
    // InteractiveIdentificationService.getDescriptiveData(descriptorsInKb, itemsInKB, dbName, login,
    // password, InteractiveIdentificationService.JACCARD);
    // }
    //
    // @Test
    // public void testGetDescriptiveDataSokalMichener() throws Exception {
    // InteractiveIdentificationService.getDescriptiveData(descriptorsInKb, itemsInKB, dbName, login,
    // password, InteractiveIdentificationService.SOKAL_MICHENER);
    // }

    @Test
    public void testGetRemainingItems() throws Exception {
        Description testDescription = new Description();
        CategoricalDescriptor ringsOnTail = null;
        for (Descriptor desc : descriptorsInKb) {
            if (desc.getName().toLowerCase().startsWith("rings on tail")) {
                ringsOnTail = (CategoricalDescriptor) desc;
            }
        }
        DescriptionElementState ringsOnTailDescriptionElementState = new DescriptionElementState();

        State selectedState = new State("present");
        State unSelectedState = new State("absent");
        selectedState.setCategoricalDescriptor(ringsOnTail);
        unSelectedState.setCategoricalDescriptor(ringsOnTail);

        ringsOnTailDescriptionElementState.addState(selectedState);
        testDescription.addDescriptionElement(ringsOnTail, ringsOnTailDescriptionElementState);

        InteractiveIdentificationService.getRemainingItems(testDescription, itemsInKB);

    }
}
