package berryrpc.registry;

import java.util.List;

@FunctionalInterface
public interface LoadBalancePolicy {
    int select(List<?> toChooseFrom);
}
