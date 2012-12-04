package fr.lis.mkeyplusAPI.services;

import java.util.List;
import java.util.ResourceBundle;

import model.Descriptor;
import model.Item;

import org.apache.log4j.Logger;
import org.junit.Test;

import services.DescriptorManagementService;
import services.ItemManagementService;
import utils.Utils;

public class InteractiveIdentificationServiceTest {
	public Logger logger = Logger.getRootLogger();
	private static String login;
	private static String dbName;
	private static String password;

	private static List<Item> itemsInKB;
	private static List<Descriptor> descriptorsInKb;

	@Test
	public void init() throws Exception {

		// set test properties file
		Utils.setBundleConf(ResourceBundle.getBundle("confTest"));
		login = Utils.getBundleConfElement("hibernate.connection.database.username");
		dbName = Utils.getBundleConfElement("hibernate.connection.database.dbName");
		password = Utils.getBundleConfElement("hibernate.connection.database.password");

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

	@Test
	public void testGetDescriptiveDataXper() throws Exception {
		InteractiveIdentificationService.getDescriptiveData(descriptorsInKb, itemsInKB, dbName, login,
				password, InteractiveIdentificationService.XPER_SORT);
	}

//	@Test
//	public void testGetDescriptiveDataJaccard() throws Exception {
//		InteractiveIdentificationService.getDescriptiveData(descriptorsInKb, itemsInKB, dbName, login,
//				password, InteractiveIdentificationService.JACCARD);
//	}
//
//	@Test
//	public void testGetDescriptiveDataSokalMichener() throws Exception {
//		InteractiveIdentificationService.getDescriptiveData(descriptorsInKb, itemsInKB, dbName, login,
//				password, InteractiveIdentificationService.SOKAL_MICHENER);
//	}
}
