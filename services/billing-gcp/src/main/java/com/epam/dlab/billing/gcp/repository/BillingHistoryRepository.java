package com.epam.dlab.billing.gcp.repository;

import com.epam.dlab.billing.gcp.model.BillingHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BillingHistoryRepository extends MongoRepository<BillingHistory, String> {
}
