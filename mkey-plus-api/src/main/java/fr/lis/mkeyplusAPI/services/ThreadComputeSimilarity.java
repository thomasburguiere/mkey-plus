package fr.lis.mkeyplusAPI.services;

import fr.lis.xper3API.model.Description;
import fr.lis.xper3API.model.Item;

import java.util.concurrent.Callable;


/**
 * Basic Thread fire a similarity calculation, return an array [(long)item.id,(float)similarity]
 *
 * @author bergamaschi
 */
public class ThreadComputeSimilarity implements Callable<Object[]> {

    private Description description = null;
    private Item item = null;

    public ThreadComputeSimilarity(Description description, Item item) {
        this.description = description;
        this.item = item;
    }

    public Object[] call() {
        Object[] output = new Object[2];
        Float similarity;
        similarity = InteractiveIdentificationService.computeSimilarity(description, item);
        output[0] = this.item.getId();
        output[1] = similarity;
        return output;
    }

}
