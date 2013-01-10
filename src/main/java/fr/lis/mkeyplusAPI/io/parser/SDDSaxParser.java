package fr.lis.mkeyplusAPI.io.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import model.Dataset;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @authors Thomas Burguiere, Florian Causse
 * 
 */
public class SDDSaxParser {
	private Dataset dataset = null;

	/**
	 * Parses an SDD file retrieved using its fileSystem path
	 * 
	 * @param filePath
	 * @throws SAXException
	 * @throws IOException
	 */
	public SDDSaxParser(String filePath) throws SAXException, IOException {
		XMLReader saxReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");

		ExtSDDContentHandler handler = new ExtSDDContentHandler();
		saxReader.setContentHandler(handler);

		InputSource is = null;
		is = new InputSource(new FileInputStream(new File(filePath)));

		saxReader.parse(is);
		this.setDataset(handler.getDataset());

	}

	/**
	 * Constructor which parses an SDD file retrieved using its URL
	 * 
	 * @param sddFilePublicURL
	 * @throws IOException
	 * @throws SAXException
	 */
	public SDDSaxParser(URL sddFilePublicURL) throws IOException, SAXException {
		XMLReader saxReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");

		ExtSDDContentHandler handler = new ExtSDDContentHandler();
		saxReader.setContentHandler(handler);

		InputSource is = null;
		is = new InputSource(sddFilePublicURL.openStream());

		saxReader.parse(is);
		this.setDataset(handler.getDataset());
	}

	/**
	 * get the current dataset
	 * 
	 * @return Dataset, the current dataset
	 */
	public Dataset getDataset() {
		return dataset;
	}

	/**
	 * set the current dataset
	 * 
	 * @param Dataset
	 *            , the current dataset
	 */
	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

}
