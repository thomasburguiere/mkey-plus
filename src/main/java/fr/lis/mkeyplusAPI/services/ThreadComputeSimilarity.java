package fr.lis.mkeyplusAPI.services;

import java.util.concurrent.Callable;

import model.Description;
import model.Item;

public class ThreadComputeSimilarity implements Callable<Object[]> {

	private Description description = null;
	private Item item = null;

	public ThreadComputeSimilarity(Description description, Item item) {
		this.description = description;
		this.item = item;
	}

	public Object[] call() {
		Object[] output = new Object[2];
		Float similarity = null;
		similarity = new Float(InteractiveIdentificationService.computeSimilarity(description, item));
		output[0] = this.item;
		output[1] = similarity;
		return output;
	}

}
