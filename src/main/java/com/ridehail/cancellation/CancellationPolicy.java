package com.ridehail.cancellation;

import com.ridehail.domain.Ride;

public interface CancellationPolicy {
    double feeFor(Ride ride);
}
