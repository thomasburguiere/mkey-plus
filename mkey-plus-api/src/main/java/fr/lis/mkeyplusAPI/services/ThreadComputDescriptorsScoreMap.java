package fr.lis.mkeyplusAPI.services;

import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.DescriptionElementState;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.DescriptorNode;
import fr.lis.xper3API.model.DescriptorTree;
import fr.lis.xper3API.model.Item;

import java.util.List;
import java.util.concurrent.Callable;


/**
 * Basic Thread, fire a discriminant power calculation, return an array[{@link Descriptor},(float)DP]
 *
 * @author bergamaschi
 */
public class ThreadComputDescriptorsScoreMap implements Callable<Object[]> {

    private Descriptor descriptor = null;
    private List<Item> items = null;
    private DescriptorTree dependencyTree = null;
    private boolean considerChildScores = true;
    private int scoreMethod = -1;
    private DescriptionElementState[][] descriptionMatrix;
    private DescriptorNode[] descriptorNodeMap;
    private boolean withGlobalWeigth;

    public ThreadComputDescriptorsScoreMap(List<Item> items, DescriptorTree dependencyTree, int scoreMethod,
                                           boolean considerChildScores, Descriptor descriptor,
                                           DescriptionElementState[][] descriptionMatrix, DescriptorNode[] descriptorNodeMap,
                                           boolean withGlobalWeight) {
        this.descriptor = descriptor;
        this.items = items;
        this.dependencyTree = dependencyTree;
        this.considerChildScores = considerChildScores;
        this.scoreMethod = scoreMethod;
        this.descriptionMatrix = descriptionMatrix;
        this.descriptorNodeMap = descriptorNodeMap;
        this.withGlobalWeigth = withGlobalWeight;
    }

    public Object[] call() {
        Object[] output = new Object[2];

        double discriminantPower = -1;
        if (descriptor.isCategoricalType() && ((CategoricalDescriptor) descriptor).getStates().size() <= 0) {
            discriminantPower = 0;
        } else {
            discriminantPower = InteractiveIdentificationService.getDiscriminantPower(descriptor, items, 0,
                    scoreMethod, considerChildScores, dependencyTree, descriptionMatrix, descriptorNodeMap,
                    withGlobalWeigth);

        }

        output[0] = descriptor;
        output[1] = discriminantPower;
        return output;
    }

}
