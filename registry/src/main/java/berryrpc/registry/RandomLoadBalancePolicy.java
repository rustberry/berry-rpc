package berryrpc.registry;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancePolicy extends AbstractLoadBalancePolicyFactory {

    @Override
    protected LoadBalancePolicy policyFactory(List<?> toSelectFrom) {
        return toChooseFrom -> ThreadLocalRandom.current().nextInt(toSelectFrom.size());
    }
}
