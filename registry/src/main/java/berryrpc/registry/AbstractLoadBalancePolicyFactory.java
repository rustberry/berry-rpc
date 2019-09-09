package berryrpc.registry;

import java.util.List;

public abstract class AbstractLoadBalancePolicyFactory {
    protected abstract LoadBalancePolicy policyFactory(List<?> toSelectFrom);

    public int select(List<?> toSelectFrom) {
        LoadBalancePolicy policy = policyFactory(toSelectFrom);
        return policy.select(toSelectFrom);
    }
}
