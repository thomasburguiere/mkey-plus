package fr.lis.mkeyplusAPI.services;

import fr.lis.mkeyplusAPI.services.InteractiveIdentificationService.Score;
import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.DescriptionElementState;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.DescriptorNode;
import fr.lis.xper3API.model.DescriptorTree;
import fr.lis.xper3API.model.Item;

import java.util.List;
import java.util.concurrent.Callable;

import static fr.lis.mkeyplusAPI.services.InteractiveIdentificationService.Score.*;


/**
 * Basic Thread, fire a discriminant power calculation, return an array[{@link Descriptor},(float)DP]
 *
 * @author bergamaschi
 */
public class ThreadComputeDescriptorsScoreMap implements Callable<Object[]> {

    private Descriptor descriptor = null;
    private List<Item> items = null;
    private DescriptorTree dependencyTree = null;
    private boolean considerChildScores = true;
    private Score scoreMethod = UNKNOWN;
    private final DescriptionElementState[][] descriptionMatrix;
    private final DescriptorNode[] descriptorNodeMap;
    private final boolean withGlobalWeight;

    public ThreadComputeDescriptorsScoreMap(
        final List<Item> items, final DescriptorTree dependencyTree, final Score scoreMethod,
        final boolean considerChildScores, final Descriptor descriptor,
        final DescriptionElementState[][] descriptionMatrix, final DescriptorNode[] descriptorNodeMap,
        final boolean withGlobalWeight) {
        this.descriptor = descriptor;
        this.items = items;
        this.dependencyTree = dependencyTree;
        this.considerChildScores = considerChildScores;
        this.scoreMethod = scoreMethod;
        this.descriptionMatrix = descriptionMatrix;
        this.descriptorNodeMap = descriptorNodeMap;
        this.withGlobalWeight = withGlobalWeight;
    }

    @Override
    public Object[] call() {
        final Object[] output = new Object[2];

        final double discriminantPower;
        if (descriptor.isCategoricalType() && ((CategoricalDescriptor) descriptor).getStates().size() <= 0) {
            discriminantPower = 0;
        } else {
            discriminantPower = InteractiveIdentificationService.getDiscriminantPower(descriptor, items, 0,
                    scoreMethod, considerChildScores, dependencyTree, descriptionMatrix, descriptorNodeMap,
                    withGlobalWeight);

        }

        output[0] = descriptor;
        output[1] = discriminantPower;
        return output;
    }

}
